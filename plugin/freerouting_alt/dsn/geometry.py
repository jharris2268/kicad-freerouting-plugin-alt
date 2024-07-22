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

from .misc import *

from math import sin,cos,acos,pi


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


    
def fix_angle(a, b,side='front'):
    if side=='back':
        a += 180
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
