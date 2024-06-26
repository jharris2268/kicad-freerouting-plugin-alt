import pcbnew

def pcbpoint(p):
    return pcbnew.VECTOR2I(int(p[0]*100), int(p[1]*-100))


def split_coords(coords):
    for i in range(0, len(coords)-3, 2):
        yield pcbpoint(coords[i:i+2]), pcbpoint(coords[i+2:i+4])




class Tracks:
    def __init__(self, pcb, select_new_objs=False, update_call=None):
        self.pcb = pcb
        
        self.nets = pcb.GetNetsByName()
        self.via_sizes = dict((v.GetName(),(v.GetViaDiameter(),v.GetViaDrill())) for _,v in pcb.GetAllNetClasses().items())
        
        self.tracks = {}
        self.vias = {}
        
        self.curr_net=None
        self.last_refresh=0
        self.curr_id = 0
        self.update_call=update_call
        
        self.select_new_objs=False
        
        
    def __call__(self, p):
        if p is None:
            pcbnew.Refresh()
            return
            
        net=None
        if p['object_type']=='track':
            net = self.track(p)
        elif p['object_type']=='via':
            net = self.via(p)
        else:
            return
        
        #if net!=self.curr_net:
        #    print("refresh",self.curr_net, self.curr_id, self.curr_id-self.last_refresh)
        #    pcbnew.Refresh()
        #    self.last_refresh=self.curr_id
        #    self.curr_net=net
        #elif (self.curr_id-self.last_refresh) > 10000:
        if (self.curr_id-self.last_refresh) >= 1000:
            #print("\trefresh",self.curr_net, self.curr_id, self.curr_id-self.last_refresh,end='',flush=True)
            pcbnew.Refresh()
            self.last_refresh=self.curr_id
            if self.update_call:
                self.update_call()
        self.curr_id += 1

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
        widths = self.via_sizes[net.GetNetClassName()]
        via.SetWidth(widths[0])
        via.SetDrill(widths[1])
        
        return via    

    
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
    
