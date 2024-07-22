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

import os

import wx

import pcbnew

from .plugindialog import PluginDialog
from .misc import test_java

class FreeroutingAlt(pcbnew.ActionPlugin):
    """
    FreeroutingAlt: An alternative Freerouting plugin
    """

    def defaults(self):
        self.name = "Freerouting Alt"
        self.category = "PCB auto routing"
        self.description = "Closer integration between freerouting and kicad"
        self.show_toolbar_button = True
        self.icon_file_name = os.path.join(os.path.dirname(__file__), 'icon_24x24.png') # Optional, defaults to ""
    def Run(self):
        
        test_java()
        
        board = pcbnew.GetBoard()
        
        rt = PluginDialog(board, self)
        try:
            rt.init()
            rt.ShowModal()
            
        except ex:
            print(ex)
        finally:
            if rt.get_process():
                try:
                    rt.get_process().wait(0.1)
                except subprocess.TimeoutExpired:
                    rt.get_process().kill()
            rt.Destroy()
        
    
    
                     
        
        
        
        
        

