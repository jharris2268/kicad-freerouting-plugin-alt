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

from .misc import LA, NL, SP, QS, TU, make_via_name

from .structure import make_structure
from .footprints import handle_footprints, make_placement, make_library, make_network
from .wiring import make_wiring






def board_to_dsn(filename, board, include_zones=False, inc_outlines=False, selected_pads=None, selected_tracks=None, box=None, fixed_wiring=True, quarter_smd_clearance=False):
    
    structure, vias = make_structure(board,include_zones, box, quarter_smd_clearance)
    footprints, nets, pads = handle_footprints(board, inc_outlines, selected_pads, box)
    pads.update((b,c) for _,(b,c) in vias.items())
    
    result = [LA("pcb"), SP(), LA(filename)]
    parser = TU([
            LA("parser"), NL(4),
                TU([LA("string_quote"),SP(),LA("\"")]), NL(4),
                TU([LA("space_in_quoted_tokens"),SP(),LA("on")]), NL(4),
                TU([LA("host_cad"),SP(),QS("KiCad's Pcbnew")]), NL(4),
                TU([LA("host_version"),SP(),LA("7.0.11-7.0.11~ubuntu22.04.1")]),NL(2),
        ])
    result.extend([NL(2), parser])
    
    result.extend([NL(2), TU([LA("resolution"),SP(),LA("um"),SP(),LA("10")])])
    result.extend([NL(2), TU([LA("unit"),SP(),LA("um")])])
    
    result.extend([NL(2), structure])
    
    result.extend([NL(2), make_placement(footprints)])
    
    result.extend([NL(2), make_library(footprints, pads)])
    
    result.extend((NL(2), make_network(board, vias, nets)))
    
    result.extend((NL(2), make_wiring(board, vias, selected_tracks, fixed_wiring)))
    result.append(NL())
    return TU(result)
