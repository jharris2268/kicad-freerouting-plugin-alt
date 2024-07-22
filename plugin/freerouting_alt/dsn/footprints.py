from .misc import *
from .geometry import make_path, make_shape, fix_angle

def make_placement(footprints):
    placement = [LA('placement')]
    for comp_name, (_,places) in footprints.items():
        placement_item = [LA("component"),SP(),LQ(comp_name)]
        for place in places:
            placement_item.extend([NL(6), place])
        placement.extend([NL(4), TU(placement_item)])
    placement.append(NL(2))
    return TU(placement)
    
def make_library(footprints, pads):
    library = [LA('library')]
    for comp_name, (comp,_) in footprints.items():
        library.extend((NL(4), comp))
    for _,padstack in pads.items():
        library.extend((NL(4), padstack))
    library.append(NL(2))
    return TU(library)

def make_network(board, vias, nets):
    
    net_classes=dict((str(a),[b.GetTrackWidth(),b.GetClearance(),[], b.GetViaDiameter(), b.GetViaDrill()]) for a,b in board.GetAllNetClasses().items())
    network = [LA('network')]
    for net_name, pins in nets.items():
        network.append(NL(4))
        net_item = [LA('net'),SP(),LQ(net_name),NL(6)]
        
        pin_item = [LA('pins'),SP()]
        sp=12
        for p in pins:
            if sp>80:
                pin_item.append(NL(8))
                sp=8
            else:
                pin_item.append(SP())
                sp+=1
            l=LA(p) #LQ(p)
            pin_item.append(l)
            sp += len(str(l))
        net_item.append(TU(pin_item))
        net_item.append(NL(4))
        network.append(TU(net_item))
        
        nc = board.FindNet(net_name).GetNetClassName()
        
        if not nc in net_classes:
            raise Exception("unexpected netclass "+nc)
            
        net_classes[nc][2].append(net_name)
    
    for name, (track_width, clearance,nets, via_dia, via_drl) in net_classes.items():
        use_name = 'kicad_default' if name=='Default' else name
        
        class_item = [LA('class'),SP(),LQ(use_name)]
        sp=sum(len(str(c)) for c in class_item)
        for n in nets:
            if sp>80:
                class_item.append(NL(6))
                sp=6
            else:
                class_item.append(SP())
                sp+=1
            l=LQ(n)
            class_item.append(l)
            sp+=len(str(l))
        class_item.append(NL(6))
        
        class_item.append(TU([LA('circuit'),NL(8),TU([LA('use_via'),SP(), LQ(vias[via_dia, via_drl][0])]),NL(6)]))
        class_item.append(NL(6))
        
        class_item.append(TU([LA('rule'),NL(8),TU([LA('width'),SP(), LV(track_width)]),NL(8),TU([LA('clearance'),SP(), LV(clearance)]),NL(6)]))
        class_item.append(NL(4))
        network.append(NL(4))
        network.append(TU(class_item))
        
    network.append(NL(2))
    return TU(network)

        
        
    
    
    
def handle_footprints(board, inc_outlines, selected_pads=None, box=None):
    dupe_names = {}
    components = {}
    pads = Pads()
    all_network = {}
    for fp in board.Footprints():
        sel_pads=None
        
        if selected_pads is not None:
            if not fp.GetReference() in selected_pads:
                if box is None:
                    continue
                elif not box.Intersects(fp.GetBoundingBox()):
                    continue
            sel_pads=selected_pads.get(fp.GetReference()) or []
        comp_name, comp_image, comp_network, place = process_component_shape(pads, fp, inc_outlines, sel_pads)
        if comp_name in dupe_names:
            actual_comp_name=None
            for dupe in dupe_names[comp_name]:
                if comp_image == components[dupe][0]:
                    actual_comp_name=dupe
            
            if actual_comp_name is None:
                suff = '::%d' % len(dupe_names[comp_name])
                assert comp_image.vals[2].val==comp_name
                actual_comp_name = comp_name+suff
                comp_image.vals[2].val=actual_comp_name
                dupe_names[comp_name].append(actual_comp_name)
                components[actual_comp_name]=[comp_image, []]
            
            comp_name = actual_comp_name
                
        else:
            dupe_names[comp_name]=[comp_name]
            components[comp_name]=[comp_image,[]]
        
        components[comp_name][1].append(place)
        for k,v in comp_network.items():
            if not k in all_network:
                all_network[k]=[]
            all_network[k].extend(v)
        
    return components, all_network, pads.pads
        


class Pads:
    def __init__(self):
        self.pads = {}
    
    def __call__(self, pad_obj, side='front'):
        pad_name, pad_tup = make_pad(pad_obj,side)
        if pad_name is None:
            return None
        if not pad_name in self.pads:
            self.pads[pad_name]=pad_tup
        return pad_name
        
def make_pad(pad_obj, side='front'):
    name = pad_obj.ShowPadShape()
    #attr = pad_obj.ShowPadAttr()
    size = pad_obj.GetSize()
    offset = pad_obj.GetOffset()
    
    #drill_size = None
    #drill_shape=None
    #if attr in ('PTH','NPTH'):
    #    drill_size = pad_obj.GetDrillSize()
    #    drill_shape = 'Oval' if pad_obj.GetDrillShape()==1 else 'Circle'
    
    on_top = pad_obj.IsOnLayer(0)    
    on_bottom = pad_obj.IsOnLayer(31)
        
    
    if side=='back':
        on_bottom,on_top = on_top,on_bottom
    
    if not (on_top or on_bottom):
        return None, None
    
    letter = 'A' if on_top and on_bottom else 'T' if on_top else 'B' if on_bottom else 'N'
    
    offset_str = ''
    if tuple(offset) != (0,0):
        a,b=offset
        offset_str="[%d,%d]" % (a/1000,-b/1000)
    
    pad_name = None
    if name=='Circle':# or (name=='Oval' and size[0]==size[1]):
        pad_name = "Round[%s]%sPad_%d_um" % (letter,offset_str,size[0]/1000)
    else:
        name_cap='RoundRect' if name.lower()=='roundrect' else name.capitalize()
        x,y = size
        size_str = '%dx%d' % (x/1000,y/1000)    
        pad_name = "%s[%s]%sPad_%s_um" % (name_cap, letter,offset_str,size_str)
    
    board = pad_obj.GetBoard()
    layer_names = [board.GetLayerName(i) for i in board.GetEnabledLayers().Seq() if i<32 and pad_obj.IsOnLayer(i)]
        
    top_layer_name=pad_obj.GetBoard().GetLayerName(0)
    bottom_layer_name=pad_obj.GetBoard().GetLayerName(31)
    shape = [LA('padstack'), SP(), LQ(pad_name)]
    for ln in layer_names:
        shape.extend([NL(6),TU([LA('shape'),SP(),make_shape(name, ln, size, offset, obj=pad_obj)])]) 
    
    #if on_top:
    #    shape.extend([NL(6),TU([LA('shape'),SP(),make_shape(name, top_layer_name, size, offset, obj=pad_obj)])]) 
    #if on_bottom:
    #    shape.extend([NL(6),TU([LA('shape'),SP(),make_shape(name, bottom_layer_name, size, offset, obj=pad_obj)])]) 
    shape.extend([NL(6), TU([LA('attach'),SP(), LA('off')]), NL(4)])
    pad_obj = TU(shape)
    
    return pad_name, pad_obj
        
    
        
def get_number(numbers, num):
    if num in numbers:
        suf='@%d' % len(numbers[num])
        numbers[num].append(suf)
        return num+suf
    else:
        numbers[num]=['']
        return num

    



def process_component_shape(pads, fp, inc_outlines, sel_pads=None):
    
    hole_buffer = fp.GetBoard().GetDesignSettings().m_HoleClearance*2
    copper_layers = [b for a,b,c in get_board_layers(fp.GetBoard()) if a<32]
        
    name = fp.GetFPIDAsString()
    
    parts = [LA('image'), SP(), LQ(name)]
    if inc_outlines:
        for d in fp.GraphicalItems():
        
            if d.GetTypeDesc()=='Graphic': #and d.GetLayerName()!='Edge.Cuts'
                parts.append(NL(6))
                parts.append(TU([LA('outline'),SP(),make_path('signal', get_coords(d.Cast(),False,True),d.GetWidth()/1000)]))
    
    ref = fp.GetReference()
    
    numbers = {'':['']}
    
    npths=[]
    
    nets={}
    
    is_flipped = fp.IsFlipped()
    if is_flipped:
        assert fp.GetLayer()==31
        fp.SetLayerAndFlip(0)
    
    
    
    
    
    for pd in fp.Pads():
        #if not sel_pads is None:
        #    if not pd.GetNumber() in sel_pads:
        #        continue
        side='front'
        if pd.ShowPadAttr()=='NPTH':
            
            pad_shape = pd.ShowPadShape()
            pad_size = pd.GetSize()
            x,y = pd.GetPos0()
            hole_size=(pad_size[0]+hole_buffer,pad_size[1]+hole_buffer)
            for layer in copper_layers:
                npths.append(TU([LA('keepout'),SP(), QS(""), SP(), make_shape(pad_shape, layer, hole_size, (x, y))]))
                    
        else:
            pad_name=pads(pd,side)
            if pad_name is None:
                continue
            pad_number = get_number(numbers, pd.GetNumber())
            angle = fix_angle(fp.GetOrientationDegrees(),pd.GetOrientationDegrees())
            x,y = pd.GetPos0()
            
            if side=='back':
                x=-x
                y=-y
            
            xx = [LA('pin'),SP(),LQ(pad_name),SP()]
            if angle!=0:
                xx+=[TU([LA('rotate'),SP(), LA(angle)]), SP()]
            xx += [LQ(pad_number), SP()]
            xx += [LV(x),SP(),LV(-y)]
            
            parts.append(NL(6))
            parts.append(TU(xx))
            
            if sel_pads is None or pd.GetNumber() in sel_pads:
                net = pd.GetNet().GetNetname()
                if net:
                    if not net in nets:
                        nets[net]=[]
                    nets[net].append("%s-%s" % (ref, pad_number))
    
    if is_flipped:
        fp.SetLayerAndFlip(31)
    
    side = 'front' if fp.GetLayer()==0 else 'back'
    for np in npths:
        parts.extend((NL(6),np))
    
    parts.append(NL(4))
    
    
    ref = fp.GetReference()
    x, y = fp.GetPosition()
    
    #angle = fp.GetOrientationDegrees()
    angle = fix_angle(0,fp.GetOrientationDegrees(),side)
    val = fp.GetValue()
    place = TU([LA('place'), SP(), LQ(ref), SP(), LV(x), SP(), LV(-y), SP(), LA(side), SP(), LA(angle), SP(), TU([LA('PN'), SP(), LA(val)])])
    
    return name, TU(parts), nets, place
