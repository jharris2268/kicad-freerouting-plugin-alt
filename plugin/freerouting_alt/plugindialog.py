# 
# This file is part of the kicad-freerouting-plugin-alt distribution
# (https://github.com/jharris2268/kicad-freerouting-plugin-alt), a freerouting plugin for the Kicad
# EDA CAD application.
# 
# Copyright (c) 2024 James Harris.
# Copyright (C) 2024 KiCad Developers,
#   see https://github.com/KiCad/kicad-source-mirror/blob/master/AUTHORS.txt for contributors.
#
# This program is free software: you can redistribute it and/or modify  
# it under the terms of the GNU General Public License as published by  
# the Free Software Foundation, either version 3 of the License, or (at your
# option) any later version.
#
# This program is distributed in the hope that it will be useful, but 
# WITHOUT ANY WARRANTY; without even the implied warranty of 
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
# General Public License for more details.
#
# You should have received a copy of the GNU General Public License 
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#

import pcbnew

import subprocess, tempfile, time, os
from .messages import MessageReceiver
from .tracks import Tracks
from . import dsn

from .misc import Selection, ShowMessages

from .dialog_base import FreeroutingAltBase
import wx


def autorouter_continue_dialog(m):
    return {'continue':True}

def optimizer_continue_dialog(m):
    return {'continue':True}
      
    
    

class PluginDialog(FreeroutingAltBase):
    def __init__(self, board, parent):
        super().__init__(None)
        
        
        self.board = board
        
        self.selection = None
        self.total_num_pads = 0
        
        self.parent = parent
        
        self.last_update_time=0
        
        self._process = None
        self.is_running=False
        self.remove_objs=None
        self.added_objs=None
        
        self.all_messages = None
        self.message_receiver = None
    
    def get_process(self):
        return self._process
    
    def init(self):
        self.selection = Selection()
        for fp in self.board.Footprints():
            nn = set(pd.GetNumber() for pd in fp.Pads() if pd.GetNetname())
            self.total_num_pads+=len(nn)              
        
        self.Bind( wx.EVT_CLOSE, self.on_close )

        self.save_dsn_button.Bind( wx.EVT_BUTTON, self.on_save_dsn )
        self.save_log_button.Bind( wx.EVT_BUTTON, self.on_save_log )
        self.close_button.Bind( wx.EVT_BUTTON, self.on_close )
        self.run_button.Bind( wx.EVT_BUTTON, self.on_run )
        self.revert_button.Bind(wx.EVT_BUTTON, self.on_revert)

        self.update(True)
        self.SetFocus()
        
        
        
    
    def update(self, force=False):
        """update text at bottom of window"""
        if time.time()-self.last_update_time < 0.2 and not force:
            return           
        
        is_only_selected=self.only_route_selected_checkbox.IsChecked()
        if is_only_selected:
            self.selected_label.SetLabel("SELECTED")
            self.pads_label.SetLabel("%d" % self.selection.num_pads)
        else:
            self.selected_label.SetLabel("")
            self.pads_label.SetLabel("%d" % self.total_num_pads)
            
        num_vias,num_tracks, total_length = 0, 0, 0
        
        nets=set([])
        for t in self.board.Tracks():
            if is_only_selected and not t.IsSelected():
                continue
            if t.Type() == pcbnew.PCB_VIA_T:
                num_vias+=1
            else:
                num_tracks += 1
            total_length += t.GetLength()/1000000.0
            nets.add(t.GetNetname())
            
        
        self.vias_label.SetLabel("%d" % num_vias)
        self.track_segments_label.SetLabel("%d" % num_tracks)
        self.nets_label.SetLabel("%d" % len(nets))
        self.unrouted_label.SetLabel("%d" % self.board.GetConnectivity().GetUnconnectedCount(True))
        self.total_length_label.SetLabel("%0.1fmm" % total_length)
        
        self.last_update_time=time.time()
        wx.Yield()
    
    def on_close(self, event):
        try:
        
            if self.is_running:
                if self.message_receiver is not None:
                    self.message_receiver.cancel()
                       
            
            if self._process:
                try:
                    self._process.wait(0.5)
                except subprocess.TimeoutExpired:
                    self._process.kill()
        except Exception as ex:
            print("on_close exception??",ex)
        finally:
            self.EndModal(wx.ID_OK)
    
    def on_save_log(self, event):
        """open FileDialog, write logged messages"""
        
        
        with wx.FileDialog(self, "Save text file",
            wildcard="text files (*.txt)|*.txt",
            style=wx.FD_SAVE | wx.FD_OVERWRITE_PROMPT) as fileDialog:


            if fileDialog.ShowModal() == wx.ID_CANCEL:
                return     # the user changed their mind

            # save the current contents in the file
            pathname = fileDialog.GetPath()
            try:
                with open(pathname, 'w') as file:
                    file.write("\n".join(self.all_messages)+"\n")
            except IOError:
                wx.LogError("Cannot save current data in file '%s'." % pathname)
    
    def on_save_dsn(self, event):
        curr_filename = self.board.GetFileName()
        suggested_filename = os.path.splitext(curr_filename)[0]+'.dsn'
        with wx.FileDialog(self, "Save text file",
            wildcard="specctra dsn files (*.dsn)|*.dsn",
            style=wx.FD_SAVE | wx.FD_OVERWRITE_PROMPT,
            defaultFile = suggested_filename) as fileDialog:
        
        
            if fileDialog.ShowModal() == wx.ID_CANCEL:
                return     # the user changed their mind

            # save the current contents in the file
            pathname = fileDialog.GetPath()
            try:
                with open(pathname, 'w') as file:
                    
                    file.write(self.prep_dsn_text())
            except IOError:
                wx.LogError("Cannot save current data in file '%s'." % pathname)
    
    def prep_dsn_text(self):
        dsn_obj=''
        if self.selection.has_selection and self.only_route_selected_checkbox.IsChecked():
            dsn_obj = dsn.board_to_dsn('autoroute.dsn', self.board,
                include_zones = not self.route_within_zones_checkbox.IsChecked(),
                selected_pads = self.selection.objs,
                selected_tracks=self.selection.tracks,
                box=self.selection.box
            )
            
        else:
            fixed_wiring=True
            if self.board.GetConnectivity().GetUnconnectedCount(True)==0 and int(self.optimize_combobox.GetValue())>0:
                fixed_wiring=False
            
            dsn_obj = dsn.board_to_dsn('autoroute.dsn', self.board, fixed_wiring=fixed_wiring)
        
        return str(dsn_obj)+'\n'
    
    def on_run(self, event):
        if self.is_running:
            if self.message_receiver is not None:
                self.message_receiver.cancel()
            
            return
        
        else:
            
            self.run_button.SetLabel("Stop")
            self.is_running=True
            self.call_run()
            
    def on_revert(self, event):
        self.revert_objs()
        self.update(True)
        
    def set_text(self, which, text):
        if which=='progress':
            self.progress_text.SetLabel(text)
        elif which=='info':
            self.info_text.SetLabel(text)
    
    def call_run(self):

        board_filename = self.board.GetFileName()
        fanout = self.fanout_checkbox.IsChecked()
        autoroute_passes = 5000 if self.autoroute_checkbox.IsChecked() else 0
        optimize_passes = int(self.optimize_combobox.GetValue())    
        
        
        dsn_text=self.prep_dsn_text()

        handle_message = ShowMessages(self)
        
        jar_file = os.path.join(os.path.dirname(__file__), 'freerouting_stdout.jar')
        
        args = ['java','-jar',jar_file,'-ms','-ap',str(autoroute_passes)]
        
        if fanout:
            args.append('-fo')
        if optimize_passes:
            args.extend(['-pp',str(optimize_passes)])
        args.extend(['-tr','2'])
        
        print(args, board_filename, repr(dsn_text)[:50], len(dsn_text))
        
        tracks=Tracks(self.board, self.update) #make this before remove existing tracks and vias
        
        self.remove_objs = []
        if self.selection.has_selection:
            self.remove_objs = self.selection.tracks
        else:
            self.remove_objs=list(self.board.Tracks())
        
        for i in self.remove_objs:
            self.board.Remove(i)
        
        
        self._process = subprocess.Popen(args, stdout=subprocess.PIPE, stdin=subprocess.PIPE)
        
        
        
        requests = {
            'design_file_text': {'file_name': board_filename+'.dsn', 'design_file_text': dsn_text},
            'continue_autoroute': autorouter_continue_dialog,
            'continue_optimize': optimizer_continue_dialog,
        }
        
        self.message_receiver = MessageReceiver(tracks, handle_message, requests, self.get_process)
        self.message_receiver.read_all()
        
        
        self.get_process().wait()
        self.update(True)
        
        self.added_objs = tracks.all_objs()
        self.all_messages = handle_message.all_messages
        print(f"{len(self.added_objs)} added_objs, {len(self.all_messages)} messages")
        self.revert_button.Enable()
        self.save_log_button.Enable()
        
        self.message_receiver=None
        self._process = None
        self.is_running=False
        self.run_button.SetLabel("Run")
        pcbnew.Refresh()
        
        
        
    def revert_objs(self):
        
        if self.added_objs:
            for o in self.added_objs:
                self.board.Remove(o)
            
            
        if self.remove_objs:
            for o in self.remove_objs:
                self.board.Add(o)
        pcbnew.Refresh()
        self.added_objs, self.remove_objs = None, None
        self.revert_button.Disable()

