/*
 *   Copyright (C) 2014  Alfons Wirtz
 *   website www.freerouting.net
 *
 *   Copyright (C) 2017 Michael Hoffer <info@michaelhoffer.de>
 *   Website www.freerouting.mihosoft.eu
 *
 *   Copyright (C) 2021 Erich S. Heinzle
 *   Website http://www.repo.hu/projects/freerouting_cli/
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
 * NetIncompletes.java
 *
 * Created on 16. Maerz 2004, 06:47
 */

package eu.mihosoft.freerouting.interactive;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;

import eu.mihosoft.freerouting.datastructures.PlanarDelaunayTriangulation;

import eu.mihosoft.freerouting.geometry.planar.FloatPoint;
import eu.mihosoft.freerouting.geometry.planar.Point;

import eu.mihosoft.freerouting.logger.FRLogger;
import eu.mihosoft.freerouting.rules.Net;

import eu.mihosoft.freerouting.board.Item;
import eu.mihosoft.freerouting.board.BasicBoard;

/**
 * Creates the Incompletes (Ratsnest) of one net to display them on the screen.
 *
 * @author Alfons Wirtz
 */
public class NetIncompletes
{
    
    /** Creates a new instance of NetIncompletes */
    public NetIncompletes(int p_net_no, Collection<Item> p_net_items, BasicBoard p_board)
    {
        this.draw_marker_radius = p_board.rules.get_min_trace_half_width() * 2;
        this.incompletes = new LinkedList<RatsNest.AirLine>();
        this.net = p_board.rules.nets.get(p_net_no);
        
        // Create an array of Item-connected_set pairs.
        NetItem [] net_items = calculate_net_items(p_net_items);
        if (net_items.length <= 1)
        {
            return;
        }
        
        // create a Delauny Triangulation for the net_items
        Collection<PlanarDelaunayTriangulation.Storable> triangulation_objects =
                new LinkedList<PlanarDelaunayTriangulation.Storable>();
        for (PlanarDelaunayTriangulation.Storable curr_object : net_items)
        {
            triangulation_objects.add(curr_object);
        }
        PlanarDelaunayTriangulation triangulation = new PlanarDelaunayTriangulation(triangulation_objects);
        
        // sort the result edges of the triangulation by length in ascending order.
        Collection<PlanarDelaunayTriangulation.ResultEdge> triangulation_lines = triangulation.get_edge_lines();
        SortedSet<Edge> sorted_edges = new TreeSet<Edge>();
        
        for (PlanarDelaunayTriangulation.ResultEdge curr_line : triangulation_lines)
        {
            Edge new_edge = new Edge((NetItem) curr_line.start_object, curr_line.start_point.to_float(),
                    (NetItem) curr_line.end_object, curr_line.end_point.to_float());
            sorted_edges.add(new_edge);
        }
        
        // Create the Airlines. Skip edges, whose from_item and to_item are already in the same connected set
        // or whose connected sets have already an airline.
        Net curr_net = p_board.rules.nets.get(p_net_no);
        Iterator<Edge> it =  sorted_edges.iterator();
        while(it.hasNext())
        {
            Edge curr_edge = it.next();
            if (curr_edge.from_item.connected_set == curr_edge.to_item.connected_set)
            {
                continue; // airline exists already
            }
            this.incompletes.add(new RatsNest.AirLine(curr_net, curr_edge.from_item.item, 
                    curr_edge.from_corner, curr_edge.to_item.item, curr_edge.to_corner));
            join_connected_sets(net_items, curr_edge.from_item.connected_set, curr_edge.to_item.connected_set);
        }
        calc_length_violation();
    }
    
    
    /**
     * Returns the number of incompletes of this net.
     */
    public int count()
    {
        return incompletes.size();
    }
    
    /**
     * Recalculates the length violations.
     * Return false, if the lenght violation has not changed.
     */
    boolean calc_length_violation()
    {
        double old_violation = this.length_violation;
        double max_length = this.net.get_class().get_maximum_trace_length();
        double min_length = this.net.get_class().get_minimum_trace_length();
        if (max_length <= 0 && min_length <= 0)
        {
            this.length_violation = 0;
            return false;
        }
        double new_violation = 0;
        double trace_length = this.net.get_trace_length();
        if (max_length > 0 && trace_length > max_length)
        {
            new_violation = trace_length - max_length;
        }
        if (min_length > 0  && trace_length < min_length && this.incompletes.size() == 0)
        {
            new_violation = trace_length - min_length;
        }
        this.length_violation = new_violation;
        boolean result = Math.abs(new_violation - old_violation) > 0.1;
        return result;
    }
    
    /**
     *  Returns the length of the violation of the length restriction of the net,
     *  > 0, if the cumulative trace length is to big,
     *  < 0, if the trace length is to smalll,
     *  0, if the thace length is ok or the net has no length restrictions 
     */
    double  get_length_violation()
    {
        return this.length_violation;
    }
    
    /**
     * Calculates an array of Item-connected_set pairs for the items of this net.
     * Pairs belonging to the same connected set are located next to each other.
     */
    private NetItem[] calculate_net_items(Collection<Item> p_item_list)
    {
        NetItem [] result = new NetItem [p_item_list.size()];
        Collection<Item> handeled_items = new LinkedList<Item>();
        int curr_index = 0;
        while (!p_item_list.isEmpty())
        {
            Item start_item = p_item_list.iterator().next();
            Collection<Item> curr_connected_set = start_item.get_connected_set(this.net.net_number);
            handeled_items.addAll(curr_connected_set);
            p_item_list.removeAll(curr_connected_set);
            Iterator<Item> it = curr_connected_set.iterator();
            while (it.hasNext())
            {
                Item curr_item = it.next();
                if (curr_index >= result.length)
                {
                    FRLogger.warn("NetIncompletes.calculate_net_items: to many items");
                    return result;
                }
                result[curr_index] = new NetItem(curr_item, curr_connected_set);
                ++curr_index;
            }
        }
        if (curr_index < result.length)
        {
            FRLogger.warn("NetIncompletes.calculate_net_items: to few items");
        }
        return result;
    }
    
    /**
     * Joins p_from_connected_set to p_to_connected_set and updates the connected sets of the items in p_net_items.
     */
    private void join_connected_sets(NetItem [] p_net_items, Collection<Item> p_from_connected_set, Collection<Item> p_to_connected_set)
    {
        for (int i = 0; i < p_net_items.length; ++i)
        {
            NetItem curr_item = p_net_items[i];
            if (curr_item.connected_set == p_from_connected_set)
            {
                p_to_connected_set.add(curr_item.item);
                curr_item.connected_set = p_to_connected_set;
            }
        }
    }
    
    /** Collection of elements of class AirLine. */
    final Collection<RatsNest.AirLine> incompletes;
    
    /**
     * The length of the violation of the length restriction of the net,
     *  > 0, if the cumulative trace length is to big,
     *  < 0, if the trace length is to smalll,
     *  0, if the thace length is ok or the net has no length restrictions 
     */
    private double length_violation = 0;
    
    private final Net net;
    private final double draw_marker_radius;
    
    private static class Edge implements Comparable<Edge>
    {
        private Edge(NetItem p_from_item, FloatPoint p_from_corner, NetItem p_to_item, FloatPoint p_to_corner)
        {
            from_item = p_from_item;
            from_corner = p_from_corner;
            to_item = p_to_item;
            to_corner =  p_to_corner;
            length_square = p_to_corner.distance_square(p_from_corner);
        }
        
        public final NetItem from_item;
        public final FloatPoint from_corner;
        public final NetItem to_item;
        public final FloatPoint to_corner;
        public final double length_square;
        
        public int compareTo(Edge p_other)
        {
            double result = this.length_square - p_other.length_square;
            if (result == 0)
            {
                // prevent result 0, so that edges with the same length as another edge are not skipped in the set
                result = this.from_corner.x - p_other.from_corner.x;
                if (result == 0)
                {
                    result = this.from_corner.y - p_other.from_corner.y;
                }
                if (result == 0)
                {
                    result = this.to_corner.x - p_other.to_corner.x;
                }
                if (result == 0)
                {
                    result = this.to_corner.y - p_other.to_corner.y;
                }
            }
            return eu.mihosoft.freerouting.datastructures.Signum.as_int(result);
        }
    }
    
    private static class NetItem implements PlanarDelaunayTriangulation.Storable
    {
        NetItem(Item p_item, Collection<Item> p_connected_set)
        {
            item = p_item;
            connected_set = p_connected_set;
        }
        
        public Point[] get_triangulation_corners()
        {
            return this.item.get_ratsnest_corners();
        }
        
        final Item item;
        Collection<Item> connected_set;
        
    }
}
