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
        
    
    
                     
        
        
        
        
        

