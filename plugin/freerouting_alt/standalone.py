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

import subprocess, os
from . import dsn
from .tracks import Tracks
from .messages import MessageReceiver


class HandleMessageConsole:
    def __init__(self):
        self.messages = []
    def __call__(self, msg):
        if not msg:
            print()
            return
            
        mm = "%8d %7.1fs %8s: %-120.120s" % (msg['index'], msg['time']/1000000000, msg['msg_type'], msg['msg'])
        self.messages.append(mm)
        
        if msg['msg_type'] in ('progress','info',):
            print(f"\r{mm}", end='', flush=True)
        
        


def run_freerouting(board, fanout=False, optimize_passes=0, ignore_zones=True):
    board_filename = board.GetFileName()
    
    fixed_wiring=True
    if board.GetConnectivity().GetUnconnectedCount(True)==0 and optimize_passes>0:
        fixed_wiring=False
    
    dsn_obj = dsn.board_to_dsn(board_filename, board, include_zones=(not ignore_zones), fixed_wiring=fixed_wiring)
        
    dsn_text = str(dsn_obj)+'\n'
    
    tracks=Tracks(board)
    
    remove_objs = list(board.Tracks())
    for i in remove_objs:
        self.board.Remove(i)
    
    
    jar_file = os.path.join(os.path.dirname(__file__), 'freerouting_stdout.jar')
        
    args = ['java','-jar',jar_file,'-ms','-ap','5000']
    
    if fanout:
        args.append('-fo')
    if optimize_passes:
        args.extend(['-pp',str(optimize_passes)])
    args.extend(['-tr','2'])
            
    process = subprocess.Popen(args, stdout=subprocess.PIPE, stdin=subprocess.PIPE)
    get_process = lambda: process
    
    
    requests = {
        'design_file_text': {'file_name': board_filename+'.dsn', 'design_file_text': dsn_text},
        'continue_autoroute': lambda a: {'continue':True},
        'continue_optimize': lambda a: {'continue':True},
    }
    
    handle_message = HandleMessageConsole()
    
    message_receiver = MessageReceiver(tracks, handle_message, requests, get_process)
    message_receiver.read_all()
    process.wait()
    
    return handle_message.messages
    
    
