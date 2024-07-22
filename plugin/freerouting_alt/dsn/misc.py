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

from . import s_tuple_parser as stp
import pcbnew

def get_board_layers(board):
    return [(i,board.GetLayerName(i),pcbnew.LAYER_ShowType(board.GetLayerType(i))) for i in board.GetDesignSettings().GetEnabledLayers().Seq()]

    


TU = lambda vals: stp.Tuple(vals)
NL = lambda sp=0: stp.Whitespace("\n"+(" "*sp))
SP = lambda: stp.Whitespace(" ")
LA = lambda la: stp.Label(str(la))
QS = lambda la: stp.QuotedString(str(la))


reserved_chars = {"(", ")", " ", ";", "-", "{", "}"} #, "_"

def LQ(val):
    if not val:
        return QS("")
    if any(c in reserved_chars for c in val):
        return QS(val)
    return LA(val)

def LV(val, nd=1):
    if (val % 1000) == 0:
        return LA("%d" % (val//1000,))
    fmt="%%0.%df" % nd
    return LA(fmt % (val/1000,))

def make_via_name(via_dia, via_drl, num_layers):
    return 'Via[0-%d]_%d:%d_um' % (num_layers-1, via_dia//1000, via_drl//1000)
    
