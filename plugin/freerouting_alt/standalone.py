import subprocess, os
from . import dsn_exporter
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
    
    dsn_obj = dsn_exporter.board_to_dsn(board_filename, board, include_zones=(not ignore_zones), fixed_wiring=fixed_wiring)
        
    dsn_text = str(dsn_obj)+'\n'
    
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
    
    tracks=Tracks(board)
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
    
    
