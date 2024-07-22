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

from .misc import *

from .geometry import merge_all_drawings, make_polygon, make_path, make_shape




#  (structure
#    (layer F.Cu
#      (type signal)
#      (property
#        (index 0)
#      )
#    )
#    (layer B.Cu
#      (type signal)
#      (property
#        (index 1)
#      )
#    )
#    (boundary
#      (path pcb 0  /*boundary coords*/ )
#    )
#    (keepout "" (polygon F.Cu 0  /* coords */))
#    (via "Via[0-1]_800:400_um" "Via[0-1]_1100:700_um")
#    (rule
#      (width 400)
#      (clearance 200.1)
#      (clearance 200.1 (type default_smd))
#      (clearance 50 (type smd_smd))
#    )
#  )



def make_structure(board,include_zones, box=None, quarter_smd_clearance=False):
    
    board_layers = get_board_layers(board)
    copper_layers = [(a,b,c) for a,b,c in board_layers if a<32]#[:board.GetCopperLayerCount()]
    
    layer_lookup = dict((a,i) for i,(a,b,c) in enumerate(copper_layers))
    
    structure_parts = []
    for idx, (_, name, layer_type) in enumerate(copper_layers):
        structure_parts.append(TU([LA("layer"), SP(), LA(name), NL(6),
                            TU([LA("type"), SP(), LA(layer_type)]), NL(6),
                                TU([LA("property"), NL(8),
                                TU([LA("index"), SP(), LA(idx)]), NL(6)
                            ]), NL(4)
                        ]))
    
    
    
    boundary_shape=None
    if not box is None:
        boundary_shape = TU([
            LA('rect'),SP(),LA('pcb'),SP(),
            LV(box.GetLeft()),SP(),
            LV(-box.GetBottom()),SP(),
            LV(box.GetRight()),SP(),
            LV(-box.GetTop())
        ])
            
    else:
        board_edge_merged = merge_all_drawings(board, 'Edge.Cuts')
        assert len(board_edge_merged)==1
        assert board_edge_merged[0][0]==board_edge_merged[0][-1]
        boundary_shape = make_path('pcb', board_edge_merged[0])
    
    structure_parts.append(TU([LA("boundary"), NL(6), boundary_shape, NL(4)]))
    
    
    
    zones = []
    for zone in board.Zones():
        layers = [b for a,b,c in copper_layers if zone.IsOnLayer(a)]
        
        zone_type = None
        
        if zone.GetIsRuleArea():
            zone_settings = zone.GetParent().GetZoneSettings()
            if zone_settings.GetDoNotAllowTracks():
                if zone_settings.GetDoNotAllowVias():
                    zone_type='keepout'
                else:
                    zone_type='track_keepout'
            else:
                if zone_settings.GetDoNotAllowVias():
                    zone_type='via_keepout'
                else:
                    print("not a keepout??")
            if not zone_type:
                continue
        
        
        net = zone.GetNet().GetNetname()
        if zone_type is None and net and layers:
            zone_type = 'plane'
        
        if zone_type:
            if zone_type=='plane':
                if not include_zones:
                    continue
            for layer in layers:
                poly = make_polygon(layer, zone)
                zones.append(TU([LA(zone_type),SP(),LQ(net),SP(),poly]))
    
    zones.sort(key=lambda x: (0 if x.vals[0].val=='plane' else 1))
    structure_parts.extend(zones)
    
    
    
    vias_all = {}
       
    #we are assuming all vias go from top to bottom
    
    for _,net_class in board.GetAllNetClasses().items():
        
        via_dia = net_class.GetViaDiameter()
        via_drl = net_class.GetViaDrill()
        
        via_spec = make_via_name(via_dia, via_drl, len(copper_layers))
        
        vias_all[via_dia, via_drl] = [via_spec, None]
    
    #check for vias with different sizes to those specified by netclass
    for item in board.Tracks():
        if item.GetTypeDesc()=='Via':
            via_dia, via_drl = item.GetWidth(), item.GetDrill()
            if not (via_dia, via_drl) in vias_all:
                via_spec = make_via_name(via_dia, via_drl, len(copper_layers))
    
                vias_all[via_dia, via_drl] = [via_spec, None]
    
    
    
    for (via_dia, via_drl), via_spec in vias_all.items():
        
        #increase via size to min of 2*board.GetDesignSettings().m_HoleClearance + 0.5*net_class.GetViaDrill() - net_class.GetClearance()
        #not neccessary for latest freerouting: bug in mihosoft codebase?
        min_via_size = 2*board.GetDesignSettings().m_HoleClearance + 0.5*net_class.GetViaDrill() - net_class.GetClearance()
        via_pad_dia = max(via_dia, min_via_size)
        
        top_layer_name=copper_layers[0][1]
        bottom_layer_name=copper_layers[-1][1]
        via_obj = [LA('padstack'), SP(), LQ(via_spec[0])]
        
        for _,layer_name,_ in copper_layers:
            via_obj.extend([NL(6),TU([LA('shape'),SP(),make_shape('Round', layer_name, (via_pad_dia,via_pad_dia), (0,0))])]) 
        
        
        via_obj.extend([NL(6),TU([LA('attach'),SP(), LA('off')]), NL(4)])
        
        via_spec[1] = TU(via_obj)
        
    vias = TU([LA('via')])
    for _,(n,_) in vias_all.items():
        vias.vals.extend([SP(),LQ(n)])
    structure_parts.append(vias)
    
    
    default_netclass = board.GetAllNetClasses()['Default']
    track_width = default_netclass.GetTrackWidth()
    clearance = default_netclass.GetClearance()+100
    
    #clearance +0.1 and /4 to match kicad: no rationale provided?
    rule = TU([LA('rule'),NL(6),
        TU([LA('width'),SP(),LV(track_width)]),NL(6),
        TU([LA('clearance'),SP(),LV(clearance)]),NL(6),
        TU([LA('clearance'),SP(),LV(clearance),SP(),TU([LA("type"),SP(),LA("default_smd")])]),NL(6),
        TU([LA('clearance'),SP(),LV(track_width/4 if quarter_smd_clearance else clearance),SP(), TU([LA("type"),SP(),LA("smd_smd")])]),NL(4),
    ])
    structure_parts.append(rule)    
    
    result = TU([LA("structure")])
    
    for pp in structure_parts:
        result.vals.extend([NL(4),pp])
    result.vals.append(NL(2))
    
    
    return result, vias_all
