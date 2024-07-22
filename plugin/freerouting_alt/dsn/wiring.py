from .misc import *


def make_track_connection(track, fixed_wiring):
    layer = track.GetLayerName()
    
    width = track.GetWidth()
    x0,y0 = track.GetStart()
    x1,y1 = track.GetEnd()
    net = track.GetNetname()
    
    vals = [LA('wire'), SP(), TU([
        LA('path'), SP(), LA(layer), SP(), LV(width), SP(), LV(x0), SP(), LV(-y0), SP(), LV(x1), SP(), LV(-y1)]),
        SP(), TU([LA('net'), SP(), LQ(net)])]
    
    if fixed_wiring:
        vals.extend([SP(), TU([LA('type'), SP(), LA('route')])])
    
    return TU(vals)    


    
#(via "Via[0-1]_800:400_um"  105536 -140463 (net /A0)(type route))    
def make_via_connection(track, vias, fixed_wiring):
    
    net_class = track.GetNetClassName()
    via_key = (track.GetWidth(), track.GetDrill())
    if not via_key in vias:
        raise Exception("?? not via_spec for %d %d" % via_key)
    via_name = vias[via_key][0]
    
    x, y = track.GetPosition()
    net = track.GetNetname()
    
    vals = [LA('via'), SP(), LQ(via_name), SP(), LV(x), SP(), LV(-y), SP(),
        TU([LA('net'), SP(), LQ(net)])]

    if fixed_wiring:
        vals.extend([SP(), TU([LA('type'), SP(), LA('route')])])
    
    return TU(vals)    

        
def make_wiring(board, vias,selected_tracks, fixed_wiring):
    res = [LA('wiring')]
    
    all_tracks = board.Tracks() if selected_tracks is None else selected_tracks
    
    for track in all_tracks:
        
        res.append(NL(4))
        if track.Type() == pcbnew.PCB_VIA_T:
            res.append(make_via_connection(track, vias, fixed_wiring))
        else:
            res.append(make_track_connection(track, fixed_wiring))
    res.append(NL(2))
    return TU(res)
