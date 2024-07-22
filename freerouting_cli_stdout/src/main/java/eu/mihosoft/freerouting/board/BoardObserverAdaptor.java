/*
 *   Copyright (C) 2014  Alfons Wirtz
 *   website www.freerouting.net
 *
 *   Copyright (C) 2017 Michael Hoffer <info@michaelhoffer.de>
 *   Website www.freerouting.mihosoft.eu
 *
 *   Copyright (C) 2024 James Harris
 *   Website https://github.com/jharris2268/kicad-freerouting-plugin-alt
 *  
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License at <http://www.gnu.org/licenses/> 
 *   for more details.
 *
 * BoardObserverAdaptor.java
 *
 * Created on 20. September 2007, 07:44
 *
 */

package eu.mihosoft.freerouting.board;
import eu.mihosoft.freerouting.logger.FRLogger;
import eu.mihosoft.freerouting.logger.MessageServer;

import eu.mihosoft.freerouting.board.PolylineTrace;
import eu.mihosoft.freerouting.board.Via;
import eu.mihosoft.freerouting.library.Padstack;
import eu.mihosoft.freerouting.rules.Net;
import eu.mihosoft.freerouting.geometry.planar.Polyline;
import eu.mihosoft.freerouting.geometry.planar.Point;
import eu.mihosoft.freerouting.geometry.planar.FloatPoint;
import org.json.JSONStringer;
import java.io.IOException;
/**
 * Empty adaptor implementing the BoardObservers interface.
 *
 * @author Alfons Wirtz
 */



public class BoardObserverAdaptor implements BoardObservers
{
    
    private int[] get_pt(Point p) {
        FloatPoint curr_float_coors = p.to_float();
        int [] curr_coors = new int[2];
        curr_coors[0] = (int) Math.round(curr_float_coors.x);
        curr_coors[1] = (int) Math.round(curr_float_coors.y);
        return curr_coors;
    }
    
    private int[] get_coords(PolylineTrace p_wire) {
        Point[] corner_arr = p_wire.polyline().corner_arr();
        int [] coors = new int [2 * corner_arr.length];
        int corner_index = 0;
        int [] prev_coors = null;
        for (int i = 0; i < corner_arr.length; ++i)
        {
            int[] curr_coors = get_pt(corner_arr[i]);
            if (i == 0  || ( curr_coors[0] != prev_coors[0] || curr_coors[1] != prev_coors[1])) {
                coors[corner_index] = curr_coors[0];
                ++corner_index;
                coors[corner_index] = curr_coors[1];
                ++corner_index;
                prev_coors = curr_coors;
            }
        }
        if (corner_index < coors.length)
        {
            int [] adjusted_coors = new int[corner_index];
            for (int i = 0; i < adjusted_coors.length; ++i)
            {
                adjusted_coors[i] = coors[i];
            }
            coors = adjusted_coors;
        }
        return coors;
    }
    
    private double line_length(int[] coords) {
        double res=0;
        for (int i=0; i < (coords.length-3); i+=2) {
            double x=coords[i+2]-coords[i];
            double y=coords[i+3]-coords[i+1];
            res += java.lang.Math.sqrt(x*x + y*y);
        }
        return res;
    }           
    
    
    private synchronized void send_polyline_trace_json(String op, PolylineTrace polyline) throws IOException {
        JSONStringer polyline_obj = MessageServer.getInstance().start_message("board_notify");
        
        polyline_obj.key("operation").value(op);
        polyline_obj.key("object_type").value("track");
        
        polyline_obj.key("id").value(polyline.get_id_no());
        
        String[] nets = new String[polyline.net_no_arr.length];
        for (int i=0; i < polyline.net_no_arr.length; i++) {
            Net n = polyline.board.rules.nets.get(polyline.net_no_arr[i]);
            if (n != null) {
                nets[i] = n.name;
            }
        }
        polyline_obj.key("nets").value(nets);
        
        String layer = polyline.board.layer_structure.arr[polyline.get_layer()].name;
        polyline_obj.key("layer").value(layer);
        polyline_obj.key("width").value((int) Math.round(2 * polyline.get_half_width()));
        int[] coords = get_coords(polyline);
        
        polyline_obj.key("length").value(line_length(coords));
        polyline_obj.key("coords").value(coords);
        
        polyline_obj.key("user_fixed").value(polyline.is_user_fixed());
        
        send_obj(polyline_obj);
    }
    
    private synchronized void send_obj(JSONStringer polyline_obj) {
        
        _count+=1;
        boolean wait_reply=false;
        
        /*
        if ((_count - _last_reply_req)>=1000) {
        */
        if (_bytes_count > 32768) {
            
            try {
                FRLogger.plop("BoardObserverAdaptor send_obj with wait, _count="+_count+", _bytes_count="+_bytes_count);
            } catch (Exception e) {}
            
            _bytes_count = 0;
            _last_reply_req=_count;
            polyline_obj.key("wait_reply").value(true);
            wait_reply=true;
        }
        
        polyline_obj.endObject();
        
        _bytes_count += polyline_obj.toString().length();
        
        try {
            if (wait_reply) {
                if (MessageServer.getInstance().send_json_expect_one_byte_response(polyline_obj) != '\1'){
                    if (!MessageServer.getInstance().request_stop() ){
                    
                        throw new IOException("wrong reply");
                    }
                }
            } else {
                MessageServer.getInstance().send_json_no_reply(polyline_obj);
            }
        } catch (Exception e) {
            //pass
        }
    }
    
    private synchronized void send_via_json(String op, Via via)  throws IOException {
        JSONStringer via_obj = MessageServer.getInstance().start_message("board_notify");
        
        
        via_obj.key("operation").value(op);
        via_obj.key("object_type").value("via");
        
        via_obj.key("id").value(via.get_id_no());
        String[] nets = new String[via.net_no_arr.length];
        for (int i=0; i < via.net_no_arr.length; i++) {
            Net n = via.board.rules.nets.get(via.net_no_arr[i]);
            if (n != null) {
                nets[i] = n.name;
            }
        }
        via_obj.key("nets").value(nets);
        
        int[] center = get_pt(via.get_center());
        via_obj.key("x").value(center[0]);
        via_obj.key("y").value(center[1]);
        
        Padstack padstack = via.get_padstack();
        via_obj.key("padstack").value(padstack.name);
        via_obj.key("from_layer").value(via.board.layer_structure.arr[padstack.from_layer()]);
        via_obj.key("to_layer").value(via.board.layer_structure.arr[padstack.to_layer()]);
        
        via_obj.key("user_fixed").value(via.is_user_fixed());
        send_obj(via_obj);
        
    }
 
    
    private void handle_item(String op, Item p_item) {
        if (!FRLogger.use_message_server) {
            return;
        }
        
        if (p_item instanceof eu.mihosoft.freerouting.board.BoardOutline) {
            FRLogger.send_message("boardobserver", "notify "+op+" BoardOutline");
        } else if (p_item instanceof eu.mihosoft.freerouting.board.ComponentOutline) {
            FRLogger.send_message("boardobserver", "notify "+op+" ComponentOutline");
        
        } else if (p_item instanceof eu.mihosoft.freerouting.board.Pin) {
            FRLogger.send_message("boardobserver", "notify "+op+" Pin");
        } else if (p_item instanceof Via) {
            try {
                send_via_json(op, (Via) p_item);
            } catch (IOException ex) {
                FRLogger.send_message("boardobserver", "notify "+op+" PolylineTrace (json failed)");
            }
            //FRLogger.send_message("boardobserver", "notify "+op+" Via");
        } else if (p_item instanceof eu.mihosoft.freerouting.board.DrillItem) {
            FRLogger.send_message("boardobserver", "notify "+op+" DrillItem");
            
        } else if (p_item instanceof eu.mihosoft.freerouting.board.ComponentObstacleArea) {
            FRLogger.send_message("boardobserver", "notify "+op+" ComponentObstacleArea");
        } else if (p_item instanceof eu.mihosoft.freerouting.board.ConductionArea) {
            FRLogger.send_message("boardobserver", "notify "+op+" ConductionArea");
        } else if (p_item instanceof eu.mihosoft.freerouting.board.ViaObstacleArea) {
            FRLogger.send_message("boardobserver", "notify "+op+" ViaObstacleArea");
        } else if (p_item instanceof eu.mihosoft.freerouting.board.ObstacleArea) {
            FRLogger.send_message("boardobserver", "notify "+op+" ObstacleArea");
            
        } else if (p_item instanceof PolylineTrace) {
            //FRLogger.send_message("boardobserver", "notify "+op+" PolylineTrace");
            try {
                send_polyline_trace_json(op, (PolylineTrace) p_item);
            } catch (IOException ex) {
                FRLogger.send_message("boardobserver", "notify "+op+" PolylineTrace (json failed)");
            }
            
        } else if (p_item instanceof eu.mihosoft.freerouting.board.Trace) {
            FRLogger.send_message("boardobserver", "notify "+op+" Trace");
        } else {
            
            FRLogger.send_message("boardobserver", "notify "+op+" unknown Item");
        }
    }
    
    
    /**
     * Tell the observers the deletion p_object.
     */
    public void notify_deleted(Item p_item)
    {
        handle_item("deleted",p_item);
    }
    
    /**
     * Notify the observers, that they can syncronize the changes on p_object.
     */
    public void notify_changed(Item p_item)
    {
        handle_item("changed",p_item);
    }
    
    /**
     * Enable the observers to syncronize the new created item.
     */
    public void notify_new(Item p_item)
    {
        handle_item("new",p_item);
        
    }
    
    /**
     * Enable the observers to syncronize the moved component.
     */
    public void notify_moved(Component p_component)
    {
        FRLogger.send_message("boardobserver", "notify_moved");
    }
    
    /**
     * activate the observers
     */
    public void activate()
    {
        active = true;
    }
    
    /**
     * Deactivate the observers.
     **/
    public void deactivate()
    {
        active = false;
    }
    
    /**
     * Returns, if the observer is activated.
     */
    public boolean is_active()
    {
        return active;
    }
    
    private boolean active = false;
    
    private int _last_reply_req=0;
    private int _count=0;
    private int _bytes_count = 0;
}
