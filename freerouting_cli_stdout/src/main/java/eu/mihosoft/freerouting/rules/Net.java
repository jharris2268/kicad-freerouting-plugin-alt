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
 * Net.java
 *
 * Created on 11. Juni 2004, 08:17
 */
package eu.mihosoft.freerouting.rules;

import eu.mihosoft.freerouting.board.Item;

import eu.mihosoft.freerouting.datastructures.UndoableObjects;

/**
 * Describes properties for an individual electrical net.
 *
 * @author Alfons Wirtz
 */
public class Net implements Comparable<Net>, java.io.Serializable
{

    /**
     * Creates a new instance of Net.
     * p_net_list is the net list, where this net belongs to.
     */
    public Net(String p_name, int p_subnet_number, int p_no, Nets p_net_list, boolean p_contains_plane)
    {
        name = p_name;
        subnet_number = p_subnet_number;
        net_number = p_no;
        contains_plane = p_contains_plane;
        net_list = p_net_list;
        net_class = p_net_list.get_board().rules.get_default_net_class();
    }

    public String toString()
    {
        return this.name;
    }

    /**
     * Compares 2 nets by name.
     * Useful for example to display nets in alphabetic order.
     */
    public int compareTo(Net p_other)
    {
        return this.name.compareToIgnoreCase(p_other.name);
    }

    /** Returns the class of this net. */
    public NetClass get_class()
    {
        return this.net_class;
    }

    /** Sets the class of this net */
    public void set_class(NetClass p_rule)
    {
        this.net_class = p_rule;
    }

    /**
     * Returns the pins and conduction areas of this net.
     */
    public java.util.Collection<Item> get_terminal_items()
    {
        java.util.Collection<Item> result = new java.util.LinkedList<Item>();
        eu.mihosoft.freerouting.board.BasicBoard board = this.net_list.get_board();
        java.util.Iterator<UndoableObjects.UndoableObjectNode> it = board.item_list.start_read_object();
        for (;;)
        {
            Item curr_item = (Item) board.item_list.read_object(it);
            if (curr_item == null)
            {
                break;
            }
            if (curr_item instanceof eu.mihosoft.freerouting.board.Connectable)
            {
                if (curr_item.contains_net(this.net_number) && !curr_item.is_route())
                {
                    result.add(curr_item);
                }
            }
        }
        return result;
    }

    /**
     * Returns the pins of this net.
     */
    public java.util.Collection<eu.mihosoft.freerouting.board.Pin> get_pins()
    {
        java.util.Collection<eu.mihosoft.freerouting.board.Pin> result = new java.util.LinkedList<eu.mihosoft.freerouting.board.Pin>();
        eu.mihosoft.freerouting.board.BasicBoard board = this.net_list.get_board();
        java.util.Iterator<UndoableObjects.UndoableObjectNode> it = board.item_list.start_read_object();
        for (;;)
        {
            Item curr_item = (Item) board.item_list.read_object(it);
            if (curr_item == null)
            {
                break;
            }
            if (curr_item instanceof eu.mihosoft.freerouting.board.Pin)
            {
                if (curr_item.contains_net(this.net_number))
                {
                    result.add((eu.mihosoft.freerouting.board.Pin) curr_item);
                }
            }
        }
        return result;
    }

    /**
     * Returns all items of this net.
     */
    public java.util.Collection<eu.mihosoft.freerouting.board.Item> get_items()
    {
        java.util.Collection<eu.mihosoft.freerouting.board.Item> result = new java.util.LinkedList<eu.mihosoft.freerouting.board.Item>();
        eu.mihosoft.freerouting.board.BasicBoard board = this.net_list.get_board();
        java.util.Iterator<UndoableObjects.UndoableObjectNode> it = board.item_list.start_read_object();
        for (;;)
        {
            Item curr_item = (Item) board.item_list.read_object(it);
            if (curr_item == null)
            {
                break;
            }
            if (curr_item.contains_net(this.net_number))
            {
                result.add(curr_item);
            }
        }
        return result;
    }

    /**
     * Returns the cumulative trace length of all traces on the board belonging to this net.
     */
    public double get_trace_length()
    {
        double cumulative_trace_length = 0;
        java.util.Collection<Item> net_items = net_list.get_board().get_connectable_items(this.net_number);
        for (Item curr_item : net_items)
        {

            if (curr_item instanceof eu.mihosoft.freerouting.board.Trace)
            {
                cumulative_trace_length += ((eu.mihosoft.freerouting.board.Trace) curr_item).get_length();
            }
        }
        return cumulative_trace_length;
    }

    /**
     * Returns the count of vias on the board belonging to this net.
     */
    public int get_via_count()
    {
        int result = 0;
        java.util.Collection<Item> net_items = net_list.get_board().get_connectable_items(this.net_number);
        for (Item curr_item : net_items)
        {
            if (curr_item instanceof eu.mihosoft.freerouting.board.Via)
            {
                ++result;
            }
        }
        return result;
    }

    public void set_contains_plane(boolean p_value)
    {
        contains_plane = p_value;
    }

    /** 
     * Indicates, if this net contains a power plane.
     * Used by the autorouter for setting the via costs to the cheap plane via costs.
     * May also be true, if a layer covered with a conduction_area of this net is
     * is a signal layer.
     */
    public boolean contains_plane()
    {
        return contains_plane;
    }

    /** The name of the net */
    public final String name;
    /**
     * Used only if a net is divided internally because of fromto rules for example
     * For normal nets it is always 1.
     */
    public final int subnet_number;
    /** The unique strict positive number of the net */
    public final int net_number;
    /** Indicates, if this net contains a power plane */
    private boolean contains_plane;
    /** The routing rule of this net */
    private NetClass net_class;
    /** The net list, where this net belongs to. */
    public final Nets net_list;
}
