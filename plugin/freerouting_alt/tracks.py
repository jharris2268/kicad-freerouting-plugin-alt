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

from .dsn import make_via_name

def pcbpoint(p):
    return pcbnew.VECTOR2I(int(p[0]*100), int(p[1]*-100))


def split_coords(coords):
    for i in range(0, len(coords)-3, 2):
        yield pcbpoint(coords[i:i+2]), pcbpoint(coords[i+2:i+4])




class Tracks:
    """Converts track and vias from freerouting format to Kicad. Monitors changes using freerouting
    id field."""
    def __init__(self, pcb, select_new_objs=False, update_call=None):
        
        self.pcb = pcb
        
        self.nets = pcb.GetNetsByName()
        
        #self.via_sizes = dict((v.GetName(),(v.GetViaDiameter(),v.GetViaDrill())) for _,v in pcb.GetAllNetClasses().items())
        via_widths = set()
                
        self.via_sizes = {}
        for _, v in pcb.GetAllNetClasses().items():
            via_dia, via_drl = v.GetViaDiameter(),v.GetViaDrill()
            via_widths.add((via_dia, via_drl))
        for t in pcb.Tracks():
            if t.GetTypeDesc()=='Via':
                via_dia, via_drl = t.GetWidth(),t.GetDrill()
                via_widths.add((via_dia, via_drl))
        for via_dia, via_drl in via_widths:
            
            via_name = make_via_name(via_dia, via_drl, pcb.GetCopperLayerCount())
            self.via_sizes[via_name] = (via_dia, via_drl)

        
        self.tracks = {}
        self.vias = {}
        
        self.curr_net=None
        self.last_refresh=0
        self.call_count = 0
        self.update_call=update_call
        
        self.select_new_objs=False
        
        
    def __call__(self, p):
        if p is None:
            pcbnew.Refresh()
            return
            
        
        if p['object_type']=='track':
            self.track(p)
        elif p['object_type']=='via':
            self.via(p)
        
        if (self.call_count-self.last_refresh) >= 1000:
            pcbnew.Refresh() #redraws pcb design on screen
            self.last_refresh=self.call_count
            if self.update_call:
                self.update_call()
        self.call_count += 1

    def make_track(self, obj, fr, to):
        track = pcbnew.PCB_TRACK(self.pcb)
        
        track.SetStart(fr)
        track.SetEnd(to)
        
        track.SetWidth(int(obj['width'])*100)
        track.SetLayer(self.pcb.GetLayerID(obj['layer']))
        track.SetNet(self.nets[obj['nets'][0]])
        if self.select_new_objs:
            track.SetSelected()
        return track
        
    def make_via(self, obj):
        via = pcbnew.PCB_VIA(self.pcb)
        
        via.SetTopLayer(self.pcb.GetLayerID(obj['from_layer']))
        via.SetBottomLayer(self.pcb.GetLayerID(obj['to_layer']))
        via.SetPosition(pcbpoint([obj['x'], obj['y']]))
        
        netn = obj['nets'][0]
        net = self.nets[netn]
        via.SetNet(net)
        via_dia, via_drl = self.via_sizes[obj['padstack']]
        via.SetWidth(via_dia)
        via.SetDrill(via_drl)
        
        return via    
    
    def all_objs(self):
        """returns list of all tracks and vias created by Tracks"""
        result = []
        for _,tt in self.tracks.items():
            result.extend(tt)
        for _,v in self.vias.items():
            result.append(v)
        return result
    
    def track(self, p):
        i = p['id']
        op = p['operation']
        
        #p['lengthx'] = coords_len(p['coords'])
        
        if op == 'new':
            if i in self.tracks:
                print("\n??? %s already present?" % p)
                for t in self.tracks[i]:
                    self.pcb.Remove(t)
            
            tt = [self.make_track(p, fr, to) for fr, to in split_coords(p['coords'])]
            for t in tt:
                self.pcb.Add(t)
            self.tracks[i] = tt
        elif op == 'deleted':
            if not i in self.tracks:
                print("\n??? %s not present?" % p)
            else:
                for t in self.tracks[i]:
                    self.pcb.Remove(t)
                del self.tracks[i]
        
        elif op == 'changed':
            if not i in self.tracks:
                print("\n??? %s not present?" % p)
            else:
                for t in self.tracks[i]:
                    self.pcb.Remove(t)
            tt = [self.make_track(p, fr, to) for fr, to in split_coords(p['coords'])]
            for t in tt:
                self.pcb.Add(t)
            self.tracks[i] = tt
        
        return p['nets'][0]


    
    def via(self, p):
        i = p['id']
        op = p['operation']
        
        if op == 'new':
            if i in self.vias:
                print("\n??? %s already present?" % p)
                self.pcb.Remove(self.vias[i])
            v = self.make_via(p)
            self.pcb.Add(v)
            self.vias[i] = v
        elif op == 'deleted':
            if not i in self.vias:
                print("\n??? %s not present?" % p)
            else:
                self.pcb.Remove(self.vias[i])
                del self.vias[i]
        elif op == 'changed':
            if not i in self.vias:
                print("\n??? %s not present?" % p)
            else:
                self.pcb.Remove(self.vias[i])
                
            v = self.make_via(p)
            self.pcb.Add(v)
            self.vias[i] = v
        
        return p['nets'][0]
    
