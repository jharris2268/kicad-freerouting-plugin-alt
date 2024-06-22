
import pcbnew
import s_tuple_parser as stp

from math import sin,cos,acos,pi

# dsn template:
#(pcb pp.dsn
#  (parser
#    (string_quote ")
#    (space_in_quoted_tokens on)
#    (host_cad "KiCad's Pcbnew")
#    (host_version "7.0.11-7.0.11~ubuntu22.04.1")
#  )
#  (resolution um 10)
#  (unit um)
#  (structure
#      ...
#  )
#  (placement
#      ...
#  )
#  (library
#      ...
#  )
#  (network
#      ...
#  )
#  (wiring
#      ...
#  )
#)
#

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
    

def board_to_dsn(filename, board, include_zones=False, inc_outlines=False, selected_pads=None, selected_tracks=None, box=None):
    
    structure, vias = make_structure(board,include_zones, box)
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
    
    result.extend((NL(2), make_wiring(board, vias, selected_tracks)))
    result.append(NL())
    return TU(result)
    
    
    
    
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
    
    net_classes=dict((str(a),[b.GetTrackWidth(),b.GetClearance(),[]]) for a,b in board.GetAllNetClasses().items())
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
    
    for name, (track_width, clearance,nets) in net_classes.items():
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
        
        class_item.append(TU([LA('circuit'),NL(8),TU([LA('use_via'),SP(), LQ(vias[name][0])]),NL(6)]))
        class_item.append(NL(6))
        
        class_item.append(TU([LA('rule'),NL(8),TU([LA('width'),SP(), LV(track_width)]),NL(8),TU([LA('clearance'),SP(), LV(clearance)]),NL(6)]))
        class_item.append(NL(4))
        network.append(NL(4))
        network.append(TU(class_item))
        
    network.append(NL(2))
    return TU(network)

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

get_start=lambda d, use_local: tuple(d.GetStart0() if use_local else d.GetStart())

def get_end(d, use_local):
    if d.ShowShape()=='Rect':
        return get_start(d, use_local)
    return tuple(d.GetEnd0() if use_local else d.GetEnd())

def merge_drawings(dd, use_local):

    
    parts = [[get_start(d,use_local), get_end(d,use_local), [(d,False)]] for d in dd]
    
    last_len = len(parts)+1
    while len(parts)>1 and len(parts) < last_len:
        last_len=len(parts)
        parts = merge_parts(parts)
        
    return parts

def add_to_parts(result, a,b,c):
    for r in result:
        if a==r[0]:
            r[0] = b
            r[2] = [(x,not y) for x,y in reversed(c)]+r[2]
            return True
        elif a==r[1]:
            r[1] = b
            r[2] = r[2] + c
            return True
        elif b==r[0]:
            r[0] = a
            r[2] = c + r[2]
            return True
        elif b==r[1]:
            r[1] = a
            r[2] = r[2]+[(x,not y) for x,y in reversed(c)]
            return True
    return False

def merge_parts(parts):
    if len(parts)==1:
        return parts
    a,b,c=parts[0]
    result = [[a,b,c[:]]]
    
    for a,b,c in parts[1:]:
            
        if not add_to_parts(result, a,b,c):
            result.append([a,b,c])
        
    return result

num_segs=lambda angle,radius: round(pi*angle/360/acos(1-5000/radius))
arc_pos=lambda cx, cy, r, a: (cx+r*cos(a*pi/180), cy+r*sin(a*pi/180))
def arc_coords(arc, is_circle=False,use_local=False):
    cx,cy=arc.GetCenter0() if use_local else arc.GetCenter()
    r=arc.GetRadius()
    sa,sc=0,360
    if not is_circle:
        if use_local:
            sa = round(arc.GetArcAngleStart().AsDegrees() + (arc.GetArcAngle0().AsDegrees()-arc.GetArcAngle().AsDegrees()),1)
            sc = round(arc.GetArcAngle0().AsDegrees(),1)
            sa = fix_angle(0,sa)
            if sc<0:
                sc+=360
            
        else:
            sa = round(arc.GetArcAngleStart().AsDegrees(),1)
            sc = round(arc.GetArcAngle().AsDegrees(),1)
    
    nstp=num_segs(sc,r)
    stp=sc/nstp
    if is_circle:
        return [arc_pos(cx,cy,r,sa+i*stp) for i in range(0,nstp+1)]   
    else:
        return [get_start(arc,use_local)]+[arc_pos(cx,cy,r,sa+i*stp) for i in range(1,nstp)]+[get_end(arc, use_local)]
    

def get_coords(shape, is_reversed, use_local=False):
    res = []
    
    if shape.GetShapeStr()=='Line':
        res = [get_start(shape,use_local),get_end(shape,use_local)]
    elif shape.GetShapeStr()=='Arc':
        res = arc_coords(shape,False,use_local)
        
    elif shape.GetShapeStr()=='Circle':
        res = arc_coords(shape, True,use_local)
        
    elif shape.GetShapeStr() in ('Polygon','Rect'):
        print("shape ",shape.GetShapeStr())
        x0,y0 = shape.GetParent().GetPosition() if use_local else (0,0)
        res = [(x-x0,y-y0) for x,y in shape.GetCorners()]
        res.append(res[0])
    else:
        print("??",shape,shape.GetShapeStr())
    
    if is_reversed:
        res.reverse()
    return res

def merge_all_drawings(obj, layer, use_local=False):
    drawings = []
    if isinstance(obj,pcbnew.BOARD):
        drawings = [d for d in obj.Drawings() if d.GetLayerName()==layer and d.GetTypeDesc()=='Graphic']
        for fp in obj.Footprints():
            drawings.extend([d for d in fp.GraphicalItems() if d.GetLayerName()==layer and d.GetTypeDesc()=='Graphic'])
    else:
        drawings = [d for d in obj.GraphicalItems() if d.GetLayerName()==layer and d.GetTypeDesc()=='Graphic']
    
    drawings_merged = merge_drawings(drawings,use_local)
    
    
    paths = []
    
    for merged in drawings_merged:
        path=[]
        for x,y in merged[2]:
            cc = get_coords(x,y,use_local)
            if not path:
                path.extend(cc)
            else:
                assert path[-1]==cc[0]
                path.extend(cc[1:])
        
        paths.append(path)
    return paths

def add_coords(xx, coords):
    for i,(x,y) in enumerate(coords):
        if i>0 and (i%4)==0:
            xx.append(NL(12))
        else:
            xx.append(SP())
        xx.extend([LV(x), SP(), LV(-y)])
        

def make_path(layer, coords, width=0):
    xx=[LA("path"), SP(), LQ(layer), SP(), LA("%d" % width)]
    add_coords(xx, coords)
    
    return TU(xx)

def make_polygon(layer, zone):
    outline = zone.Outline()
    if outline.HasHoles():
        raise Exception("can't handle zone with a hole")
    if outline.OutlineCount()!=1:
        raise Exception("can't handle zone with a outline count != 1")
    outline_boundary = outline.Outline(0)
    num_verts = outline_boundary.GetPointCount()
    vertices = [outline_boundary.GetPoint(i) for i in range(num_verts)]
    
    vertices.append(vertices[0])
    
    polygon_parts=[LA('polygon'),SP(),LQ(layer), SP(), LA("0")]
    add_coords(polygon_parts, vertices)
    return TU(polygon_parts)

def make_structure(board,include_zones, box=None):
    
    board_layers = get_board_layers(board)
    copper_layers = [(a,b,c) for a,b,c in board_layers if a<32]#[:board.GetCopperLayerCount()]
    
    layer_lookup = dict((a,i) for i,(a,b,c) in enumerate(copper_layers))
    
    structure_parts = []
    for idx, (_, name, layer_type) in enumerate(copper_layers):
        structure_parts.append(TU([LA("layer"), SP(), name, NL(6),
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
    for name,net_class in board.GetAllNetClasses().items():
        #Via[0-1]_800:400_um
        via_dia = net_class.GetViaDiameter()
        via_drl = net_class.GetViaDrill()
        
        via_spec = 'Via[0-%d]_%d:%d_um' % (len(copper_layers)-1, via_dia//1000, via_drl//1000)
        
        top_layer_name=copper_layers[0][1]
        bottom_layer_name=copper_layers[-1][1]
        via_obj = [LA('padstack'), SP(), LQ(via_spec)]
        
        via_obj.extend([NL(6),TU([LA('shape'),SP(),make_shape('Round', top_layer_name, (via_dia,via_dia), (0,0))])]) 
        via_obj.extend([NL(6),TU([LA('shape'),SP(),make_shape('Round', bottom_layer_name, (via_dia,via_dia), (0,0))])]) 
        
        via_obj.extend([NL(6),TU([LA('attach'),SP(), LA('off')]), NL(4)])
        
        vias_all[str(name)] = (via_spec,TU(via_obj))
        
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
        TU([LA('clearance'),SP(),LV(track_width/4),SP(), TU([LA("type"),SP(),LA("smd_smd")])]),NL(4),
    ])
    structure_parts.append(rule)    
    
    result = TU([LA("structure")])
    
    for pp in structure_parts:
        result.vals.extend([NL(4),pp])
    result.vals.append(NL(2))
    
    
    return result, vias_all
        
        
    
    
    
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
        if comp_name in components:
            if comp_image == components[comp_name][0]:
                pass
            else:
                suff = '::%d' % len(dupe_names[comp_name])
                assert comp_image.vals[2].val==comp_name
                new_comp_name = comp_name+suff
                comp_image.vals[2].val=new_comp_name
                components[new_comp_name]=[comp_image, []]
                comp_name = new_comp_name
        else:
            dupe_names[comp_name]=['']
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
    
    def __call__(self, pad_obj):
        pad_name, pad_tup = make_pad(pad_obj)
        if pad_name is None:
            return None
        if not pad_name in self.pads:
            self.pads[pad_name]=pad_tup
        return pad_name
        
def make_pad(pad_obj):
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
        x,y = size
        size_str = '%dx%d' % (x/1000,y/1000)    
        pad_name = "%s[%s]%sPad_%s_um" % (name, letter,offset_str,size_str)
    
    
    top_layer_name=pad_obj.GetBoard().GetLayerName(0)
    bottom_layer_name=pad_obj.GetBoard().GetLayerName(31)
    shape = [LA('padstack'), SP(), LQ(pad_name)]
    if on_top:
        shape.extend([NL(6),TU([LA('shape'),SP(),make_shape(name, top_layer_name, size, offset, obj=pad_obj)])]) 
    if on_bottom:
        shape.extend([NL(6),TU([LA('shape'),SP(),make_shape(name, bottom_layer_name, size, offset, obj=pad_obj)])]) 
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
    
def fix_angle(a, b):
    if a==b:
        return 0
    
    c=b-a
    if c<=-180:
        c+=360
    if c>180:
        c-=360
    return c

def make_shape(shape, layer, size, pos,obj=None):
    if shape=='Circle' or shape=='Round':# or shape=='Oval' and size[0]==size[1]:
        x,y=pos
        return TU([LA('circle'), SP(), LA(layer), SP(),
            LV(size[0])]+([] if x==0 and y==0 else [SP(),LV(x), SP(), LV(-y)]))
    
    if shape=='Oval':
        w,h=size
        x,y=pos
        if w>h:
            #path from (-(w-h)/2+x,y) to ((w-h)/2+x,y), width w
            ln,wd = (h-w)/2, h
            return TU([LA('path'),SP(), LA(layer),SP(),LV(wd), SP(), LV(-ln+x), SP(), LV(y), SP(), LV(ln+x), SP(), LV(y)])
            
        else:
            #path from (x,-(h-w)/2+y) to (x,(h-w)/2+y), width h
            ln,wd = (w-h)/2, w
            return TU([LA('path'),SP(), LA(layer),SP(),LV(wd), SP(), LV(x), SP(), LV(-ln-y), SP(), LV(x), SP(), LV(ln-y)])
    
    if shape=='Rect':
        w,h=size[0]/2, size[1]/2
        x,y=pos
        return TU([LA('rect'),SP(),LA(layer),SP(),LV(x-w),SP(),LV(-y-h),SP(),LV(x+w),SP(),LV(-y+h)])
        
    if shape=='Roundrect':
        w,h=size[0]/2,size[1]/2
        x,y=pos
        R = obj.GetRoundRectCornerRadius()
        S,C=0.5*R,(1-3**0.5/2)*R
        #           |---  top   --  |--   top right   --|
        vertices = [(-w+R,h),(w-R,h),(w-S,h-C),(w-C,h-S),
        #           |--    right    --|--   bottom right   --|
                    (w,h-R), (w, -h+R),(w-C,-h+S), (w-S,-h+C),
        #           |--   bottom    --|--   bottom left    --|
                    (w-R,-h),(-w+R,-h),(-w+S,-h+C),(-w+C,-h+S),
        #           |--     left     --|-- top left -- |
                    (-w,-h+R),(-w, h-R),(-w+C,h-S),(-w+S,h-C),(-w+R,h)]
        vertices = [(a+x,b+y) for a,b in vertices]
        
        polygon_parts=[LA('polygon'),SP(),LQ(layer), SP(), LA("0")]
        add_coords(polygon_parts, vertices)
        return TU(polygon_parts)
    if shape=='CustomShape':
        bx=obj.GetEffectiveShape().BBox()
        a,b,c,d = bx.GetLeft(),bx.GetBottom(),bx.GetRight(),bx.GetTop()
        x0,y0 = obj.GetPosition()
        return TU([LA('rect'),SP(),LA(layer),SP(),LV(a-x0),SP(),LV(y0-d),SP(),LV(c-x0),SP(),LV(y0-b)])
        
        
    print(shape,layer,size,pos)
    raise Exception("can't make shape %s" % shape)
    



def process_component_shape(pads, fp, inc_outlines, sel_pads=None):
    
    hole_buffer = fp.GetBoard().GetDesignSettings().m_HoleClearance*2
    copper_layers = [b for a,b,c in get_board_layers(fp.GetBoard()) if a<32]
        
    name = fp.GetFPIDAsString()
    
    parts = [LA('image'), SP(), LA(name)]
    if inc_outlines:
        for d in fp.GraphicalItems():
        
            if d.GetTypeDesc()=='Graphic': #and d.GetLayerName()!='Edge.Cuts'
                parts.append(NL(6))
                parts.append(TU([LA('outline'),SP(),make_path('signal', get_coords(d.Cast(),False,True),d.GetWidth()/1000)]))
    
    ref = fp.GetReference()
    
    numbers = {'':['']}
    
    npths=[]
    
    nets={}
    
    for pd in fp.Pads():
        #if not sel_pads is None:
        #    if not pd.GetNumber() in sel_pads:
        #        continue
        
        if pd.ShowPadAttr()=='NPTH':
            
            pad_shape = pd.ShowPadShape()
            pad_size = pd.GetSize()
            x,y = pd.GetPos0()
            hole_size=(pad_size[0]+hole_buffer,pad_size[1]+hole_buffer)
            for layer in copper_layers:
                npths.append(TU([LA('keepout'),SP(), QS(""), SP(), make_shape(pad_shape, layer, hole_size, (x, y))]))
                    
        else:
            pad_name=pads(pd)
            if pad_name is None:
                continue
            pad_number = get_number(numbers, pd.GetNumber())
            angle = fix_angle(fp.GetOrientationDegrees(),pd.GetOrientationDegrees())
            x,y = pd.GetPos0()
            
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
        
    
    for np in npths:
        parts.extend((NL(6),np))
    
    parts.append(NL(4))
    
    
    ref = fp.GetReference()
    x, y = fp.GetPosition()
    side = 'front' if fp.GetLayer()==0 else 'back'
    angle = fp.GetOrientationDegrees()
    
    val = fp.GetValue()
    place = TU([LA('place'), SP(), LQ(ref), SP(), LV(x), SP(), LV(-y), SP(), LA(side), SP(), LA(angle), SP(), TU([LA('PN'), SP(), LA(val)])])
    
    return name, TU(parts), nets, place


#(wire (path B.Cu 400  91684.4 -57626.8  81911.5 -67399.7)(net "Net-(J3-Pad14)")(type route))

def make_track_connection(track):
    layer = track.GetLayerName()
    
    width = track.GetWidth()
    x0,y0 = track.GetStart()
    x1,y1 = track.GetEnd()
    net = track.GetNetname()
    
    return TU([LA('wire'), SP(), TU([
        LA('path'), SP(), LA(layer), SP(), LV(width), SP(), LV(x0), SP(), LV(-y0), SP(), LV(x1), SP(), LV(-y1)]),
        SP(), TU([LA('net'), SP(), LQ(net)]), SP(), TU([LA('type'), SP(), LA('route')])])
        


    
#(via "Via[0-1]_800:400_um"  105536 -140463 (net /A0)(type route))    
def make_via_connection(track, vias):
    
    net_class = track.GetNetClassName()
    via_name = vias[net_class][0]
    
    x, y = track.GetPosition()
    net = track.GetNetname()
    return TU([LA('via'), SP(), LQ(via_name), SP(), LV(x), SP(), LV(-y), SP(),
        TU([LA('net'), SP(), LQ(net)]), SP(), TU([LA('type'), SP(), LA('route')])])


        
def make_wiring(board, vias,selected_tracks):
    res = [LA('wiring')]
    
    all_tracks = board.Tracks() if selected_tracks is None else selected_tracks
    
    for track in all_tracks:
        
        res.append(NL(4))
        if track.Type() == pcbnew.PCB_VIA_T:
            res.append(make_via_connection(track, vias))
        else:
            res.append(make_track_connection(track))
    res.append(NL(2))
    return TU(res)
            
            
import sys
if __name__ == "__main__":
    infn = sys.argv[1] if len(sys.argv)>1 else '/home/james/elec/picocomputer/schematic/alt_compact/pp/rp6502.kicad_pcb'
    outfn = sys.argv[2] if len(sys.argv)>2 else 'pp2.dsn'
    inc_zones = sys.argv[3]=='zones' if len(sys.argv)>3 else False
    inc_outlines=sys.argv[4]=='outlines' if len(sys.argv)>4 else False
    pcb = pcbnew.LoadBoard(infn)
    dd=board_to_dsn(outfn,pcb,inc_zones,inc_outlines)
    with open(outfn,'w') as w:
        w.write(str(dd))
        w.write("\n")
        

    
    



