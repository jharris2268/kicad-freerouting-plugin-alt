import pcbnew
import subprocess

def test_java():
    result=None
    try:
        result=subprocess.run(['java', '--version'], capture_output=True)
    except FileNotFoundError:
        raise Exception("java executable not present")
    
    stdout = result.stdout.decode('utf-8').split("\n")
    if not len(stdout)>=1:
        raise Exception("unexpected result of java -version")
    
    version_number = stdout[0].split()
    assert len(version_number)>=2
    if not len(stdout)>=1:
        raise Exception("unexpected result of java -version")
        
    version_int = int(version_number[1].split(".")[0])
    
    if version_int < 19:
        raise Exception("java version 19 or higher required")
        
    return True

        
class Selection:
    def __init__(self):
        
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


class ShowMessages:
    def __init__(self, parent_dialog):
        self.parent_dialog = parent_dialog
        self.visible_messages = ['']
        self.all_messages = []
        
    def __call__(self, msg):
        if msg is None:
            return
        force=False
        if msg['msg_type']=='progress':
            force=True
            self.parent_dialog.set_text('progress',msg['msg'])
            #self.parent_dialog.progress_text.SetLabel(msg['msg'])
            #print(f"progress_text={self.parent_dialog.progress_text.GetLabel()}")
        elif msg['msg_type']=='info':
            force=True
            #self.parent_dialog.info_text.SetLabel(msg['msg'])
            self.parent_dialog.set_text('info',msg['msg'])
        
        #mm = "%d\t\t%7.1fs\t%s:\t%-200.200s" % (msg['index'], msg['time']/1000000000, msg['msg_type'], msg['msg'])
        mm = "%8d %7.1fs %8s: %-120.120s" % (msg['index'], msg['time']/1000000000, msg['msg_type'], msg['msg'])
        self.all_messages.append(mm)
        
        self.visible_messages[-1]=mm
        if msg['msg_type'] in ('info',):
            self.visible_messages.append("")
            
        txt="\n".join(self.visible_messages)
        
        self.parent_dialog.logging_text.SetValue(txt)
        
        self.parent_dialog.logging_text.ShowPosition(len(txt)-len(self.visible_messages[-1])+1) #first character of last line
        
        
        self.parent_dialog.update(force)
        


