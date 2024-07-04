import pcbnew

import subprocess, tempfile, time, os
from .messages import MessageReceiver
from .tracks import Tracks
from . import dsn_exporter
from .dialog_base import FreeroutingAltBase
import wx


def autorouter_continue_dialog(m):
    return {'continue':True}

def optimizer_continue_dialog(m):
    return {'continue':True}

def handle_message(msg):
    if msg is None:
        print()
        return
        
    print("\r%d %10d %s: %-200.200s" % (msg['index'], msg['time'], msg['msg_type'], msg['msg']), end='', flush=True)
    if msg['msg_type']=='plop':
        print()

check_min = lambda curr, new: new if (new is None or curr is None or new < curr) else curr
check_max = lambda curr, new: new if (new is None or curr is None or new > curr) else curr
        

box_tuple = lambda bx: (bx.GetLeft(), bx.GetBottom(), bx.GetRight(), bx.GetTop())
        
        
class Selection:
    def __init__(self):
        self.objs={}
        self.tracks=[]
        self.box = [0,0,0,0]
        self.message_receiver=None
    
    def init(self):
        self.objs = {}
        self.tracks=[]
        self.box = pcbnew.BOX2I()
        for obj_ in pcbnew.GetCurrentSelection():
            
            obj = obj_.Cast()
            
            if obj.GetTypeDesc() == 'Footprint':
                self.objs[obj.GetReference()] = set(p.GetNumber() for p in obj.Pads() if p.GetNetname())
            elif obj.GetTypeDesc() == 'Pad':
                k = obj.GetParent().GetReference()
                if k in self.objs:
                    self.objs[k].add(obj.GetNumber())
                else:
                    self.objs[k] = set([obj.GetNumber()])
            elif obj.GetTypeDesc() in ('Track','Via'):
                selected_tracks.append(obj)
            self.box.Merge(obj.GetBoundingBox())
        self.num_pads = sum(len(v) for k,v in self.objs.items())
        
    
    @property
    def has_selection(self):
        return len(self.objs)>0 or len(self.tracks)>0
    

class PluginDialog(FreeroutingAltBase):
    def __init__(self, board, parent):
        super().__init__(None)
        
        
        self.board = board
        
        self.selection = Selection()
        self.total_num_pads = 0
        
        self.parent = parent
        
        self.last_update_time=0
        
        self._process = None
        self.is_running=False
        self.remove_objs=None
        self.added_objs=None
    
    def get_process(self):
        return self._process
    
    def init(self):
        self.selection.init()
        for fp in self.board.Footprints():
            nn = set(pd.GetNumber() for pd in fp.Pads() if pd.GetNetname())
            self.total_num_pads+=len(nn)
               
        
        self.Bind( wx.EVT_CLOSE, self.on_close )
        self.curr=['']
        
        self.all_msgs = []
        self.update(True)
        
        self.save_dsn_button.Bind( wx.EVT_BUTTON, self.on_save_dsn )
        self.save_log_button.Bind( wx.EVT_BUTTON, self.on_save_log )
        self.close_button.Bind( wx.EVT_BUTTON, self.on_close )
        self.run_button.Bind( wx.EVT_BUTTON, self.on_run )
        self.revert_button.Bind(wx.EVT_BUTTON, self.on_revert)
        self.SetFocus()
        
        
    def handle_message(self, msg):
        if msg is None:
            return
        force=False
        if msg['msg_type']=='progress':
            force=True
            self.progress_text.SetLabel(msg['msg'])
        elif msg['msg_type']=='info':
            force=True
            self.info_text.SetLabel(msg['msg'])
        
        mm = "%d\t\t%7.1fs\t%s:\t%-200.200s" % (msg['index'], msg['time']/1000000000, msg['msg_type'], msg['msg'])
        self.all_msgs.append(mm)
        
        self.curr[-1]=mm
        if msg['msg_type'] in ('info',):
            self.curr.append("")
            
        txt="\n".join(self.curr)
        self.logging_text.SetValue(txt)
        
        self.logging_text.ShowPosition(len(txt)-len(self.curr[-1])+1) #first character of last line
        
        
        self.update(force)
                
        
    
    def update(self, force=False):
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
        if self.is_running:
            if self.message_receiver is not None:
                self.message_receiver.cancel()
        
        
        if self._process:
            try:
                self._process.wait(0.5)
            except subprocess.TimeoutExpired:
                self._process.kill()
            except Exception as ex:
                print("??",ex)
                
        self.EndModal(wx.ID_OK)
    
    def on_save_log(self, event):
        with wx.FileDialog(self, "Save text file",
            wildcard="text files (*.txt)|*.txt",
            style=wx.FD_SAVE | wx.FD_OVERWRITE_PROMPT) as fileDialog:


            if fileDialog.ShowModal() == wx.ID_CANCEL:
                return     # the user changed their mind

            # save the current contents in the file
            pathname = fileDialog.GetPath()
            try:
                with open(pathname, 'w') as file:
                    file.write("\n".join(self.all_msgs)+"\n")
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
        if self.selection.has_selection and self.route_within_zones_checkbox.IsChecked():
            dsn_obj = dsn_exporter.board_to_dsn('autoroute.dsn', self.board,
                include_zones = not self.route_within_zones_checkbox.IsChecked(),
                selected_pads = self.selection.objs,
                selected_tracks=self.selection.tracks,
                box=self.selection.box
            )
            
        else:
            fixed_wiring=True
            if self.board.GetConnectivity().GetUnconnectedCount(True)==0 and int(self.optimize_combobox.GetValue())>0:
                fixed_wiring=False
            
            dsn_obj = dsn_exporter.board_to_dsn('autoroute.dsn', self.board, fixed_wiring=fixed_wiring)
        
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
        
        
    
    def call_run(self):
        
        
        board_filename = self.board.GetFileName()
        fanout = self.fanout_checkbox.IsChecked()
        autoroute_passes = 5000 if self.autoroute_checkbox.IsChecked() else 0
        optimize_passes = int(self.optimize_combobox.GetValue())
        
        dsn_text=self.prep_dsn_text()

        #print("fanout?",fanout, "autoroute passes", autoroute_passes, "optimize passes", optimize_passes)
        self.curr=['']
        self.all_msgs=[]
        
        
        #jar_file = os.path.join(os.path.dirname(__file__), 'freerouting_sockets.jar')
        jar_file = os.path.join(os.path.dirname(__file__), 'freerouting_stdout.jar')
        
        #args = ['java','-jar',jar_file,'-ms','-ap',str(autoroute_passes)]
        args = ['java','-jar',jar_file,'-ms','-ap',str(autoroute_passes)]
        
        if fanout:
            args.append('-fo')
        if optimize_passes:
            args.extend(['-pp',str(optimize_passes)])
        args.extend(['-tr','2'])
        #,'-pp','5']
        
        
        print(args, board_filename, repr(dsn_text)[:50], len(dsn_text))
        
        self.remove_objs = []
        if self.selection.has_selection:
            self.remove_objs = self.selection.tracks
        else:
            self.remove_objs=list(self.board.Tracks())
        
        for i in self.remove_objs:
            self.board.Remove(i)
        
        
        self._process = subprocess.Popen(args, stdout=subprocess.PIPE, stdin=subprocess.PIPE)
        #self._process.stdin.write(dsn_text.encode('utf-8'))
        #self._process.stdin.flush()
        #time.sleep(0.2)
        
        
        tracks=Tracks(self.board, self.update)
        requests = {
            'design_file_text': {'file_name': board_filename+'.dsn', 'design_file_text': dsn_text},
            'continue_autoroute': autorouter_continue_dialog,
            'continue_optimize': optimizer_continue_dialog,
        }
        
        self.message_receiver = MessageReceiver(tracks, self.handle_message, requests, self.get_process)
        self.message_receiver.read_all()
        
        
        self.get_process().wait()
        self.update(True)
        #print(tracks)
        self.added_objs = tracks.all_objs()
        self.revert_button.Enable()
        
        self.message_receiver=None
        self._process = None
        self.is_running=False
        self.run_button.SetLabel("Run")
        pcbnew.Refresh()
        #self.EndModal(wx.ID_OK)
        
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

class FreeroutingAlt(pcbnew.ActionPlugin):
    """
    test_by_date: A sample plugin as an example of ActionPlugin
    Add the date to any text field of the board containing '$date$'
    How to use:
    - Add a text on your board with the content '$date$'
    - Call the plugin
    - The text will automatically be updated with the date (format YYYY-MM-DD)
    """

    def defaults(self):
        """
        Method defaults must be redefined
        self.name should be the menu label to use
        self.category should be the category (not yet used)
        self.description should be a comprehensive description
          of the plugin
        """
        self.name = "Freerouting Alt"
        self.category = "PCB auto routing"
        self.description = "Closer integration between freerouting and kicad"
        self.show_toolbar_button = True
        self.icon_file_name = os.path.join(os.path.dirname(__file__), 'icon_24x24.png') # Optional, defaults to ""
    def Run(self):
        board = pcbnew.GetBoard()
        
        rt = PluginDialog(board, self)
        try:
            rt.init()
            rt.ShowModal()
            
        except Exception as ex:
            print(ex)
        finally:
            if rt.get_process():
                try:
                    rt.get_process().wait(0.1)
                except subprocess.TimeoutExpired:
                    rt.get_process().kill()
            rt.Destroy()
        
    
    
                     
        
        
        
        
        

