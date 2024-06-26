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
 * Via.java
 *
 * Created on 5. Juni 2003, 10:36
 */
package eu.mihosoft.freerouting.board;

import java.util.Collection;
import java.util.Iterator;

import eu.mihosoft.freerouting.geometry.planar.Point;
import eu.mihosoft.freerouting.geometry.planar.IntPoint;
import eu.mihosoft.freerouting.geometry.planar.TileShape;
import eu.mihosoft.freerouting.geometry.planar.Shape;
import eu.mihosoft.freerouting.geometry.planar.Vector;
import eu.mihosoft.freerouting.library.Padstack;
import eu.mihosoft.freerouting.logger.FRLogger;

/**
 * Class describing the functionality of an electrical Item on the board,
 * which may have a shape on several layer, whose geometry is described by a
 * padstack.
 *
 * @author Alfons Wirtz
 */
public class Via extends DrillItem implements java.io.Serializable
{

    /** Creates a new instance of Via with the input parameters*/
    public Via(Padstack p_padstack, Point p_center, int[] p_net_no_arr, int p_clearance_type, int p_id_no,
            int p_group_no, FixedState p_fixed_state, boolean p_attach_allowed, BasicBoard p_board)
    {
        super(p_center, p_net_no_arr, p_clearance_type, p_id_no, p_group_no, p_fixed_state, p_board);
        this.padstack = p_padstack;
        this.attach_allowed = p_attach_allowed;
    }

    public Item copy(int p_id_no)
    {
        return new Via(padstack, get_center(), net_no_arr, clearance_class_no(), p_id_no, get_component_no(),
                get_fixed_state(), attach_allowed, board);
    }

    public Shape get_shape(int p_index)
    {
        if (padstack == null)
        {
            FRLogger.warn("Via.get_shape: padstack is null");
            return null;
        }
        if (this.precalculated_shapes == null)
        {
            this.precalculated_shapes = new Shape[padstack.to_layer() - padstack.from_layer() + 1];
            for (int i = 0; i < this.precalculated_shapes.length; ++i)
            {
                int padstack_layer = i + this.first_layer();
                Vector translate_vector = get_center().difference_by(Point.ZERO);
                Shape curr_shape = padstack.get_shape(padstack_layer);

                if (curr_shape == null)
                {
                    this.precalculated_shapes[i] = null;
                }
                else
                {
                    this.precalculated_shapes[i] = (Shape) curr_shape.translate_by(translate_vector);
                }
            }
        }
        return this.precalculated_shapes[p_index];
    }

    public Padstack get_padstack()
    {
        return padstack;
    }

    public void set_padstack(Padstack p_padstack)
    {
        padstack = p_padstack;
    }

    public boolean is_route()
    {
        return !is_user_fixed() && this.net_count() > 0;
    }

    public boolean is_obstacle(Item p_other)
    {
        if (p_other == this || p_other instanceof ComponentObstacleArea)
        {
            return false;
        }
        if ((p_other instanceof ConductionArea) && !((ConductionArea) p_other).get_is_obstacle())
        {
            return false;
        }
        if (!p_other.shares_net(this))
        {
            return true;
        }
        if (p_other instanceof Trace)
        {
            return false;
        }
        return !this.attach_allowed || !(p_other instanceof Pin) || !((Pin) p_other).drill_allowed();
    }

    /**
     * Checks, if the Via has contacts on at most 1 layer.
     */
    public boolean is_tail()
    {
        Collection<Item> contact_list = this.get_normal_contacts();
        if (contact_list.size() <= 1)
        {
            return true;
        }
        Iterator<Item> it = contact_list.iterator();
        Item curr_contact_item = it.next();
        int first_contact_first_layer = curr_contact_item.first_layer();
        int first_contact_last_layer = curr_contact_item.last_layer();
        while (it.hasNext())
        {
            curr_contact_item = it.next();
            if (curr_contact_item.first_layer() != first_contact_first_layer || curr_contact_item.last_layer() != first_contact_last_layer)
            {
                return false;
            }
        }
        return true;
    }

    public void change_placement_side(IntPoint p_pole)
    {
        if (this.board == null)
        {
            return;
        }
        Padstack new_padstack = this.board.library.get_mirrored_via_padstack(this.padstack);
        if (new_padstack == null)
        {
            return;
        }
        this.padstack = new_padstack;
        super.change_placement_side(p_pole);
        clear_derived_data();
    }

    public eu.mihosoft.freerouting.autoroute.ExpansionDrill get_autoroute_drill_info(ShapeSearchTree p_autoroute_tree)
    {
        if (this.autoroute_drill_info == null)
        {
            eu.mihosoft.freerouting.autoroute.ItemAutorouteInfo via_autoroute_info = this.get_autoroute_info();
            TileShape curr_drill_shape = TileShape.get_instance(this.get_center());
            this.autoroute_drill_info =
                    new eu.mihosoft.freerouting.autoroute.ExpansionDrill(curr_drill_shape, this.get_center(), this.first_layer(), this.last_layer());
            int via_layer_count = this.last_layer() - this.first_layer() + 1;
            for (int i = 0; i < via_layer_count; ++i)
            {
                this.autoroute_drill_info.room_arr[i] = via_autoroute_info.get_expansion_room(i, p_autoroute_tree);
            }
        }
        return this.autoroute_drill_info;
    }

    public void clear_derived_data()
    {
        super.clear_derived_data();
        this.precalculated_shapes = null;
        this.autoroute_drill_info = null;
    }
    
    public void clear_autoroute_info()
    {
        super.clear_autoroute_info();
        this.autoroute_drill_info = null;
    }

    public boolean is_selected_by_filter(ItemSelectionFilter p_filter)
    {
        if (!this.is_selected_by_fixed_filter(p_filter))
        {
            return false;
        }
        return p_filter.is_selected(ItemSelectionFilter.SelectableChoices.VIAS);
    }

    public boolean write(java.io.ObjectOutputStream p_stream)
    {
        try
        {
            p_stream.writeObject(this);
        } catch (java.io.IOException e)
        {
            return false;
        }
        return true;
    }
    private Padstack padstack;
    /** True, if coppersharing of this via with smd pins of the same net  is allowed. */
    public final boolean attach_allowed;
    transient private Shape[] precalculated_shapes = null;
    /** Temporary data used in the autoroute algorithm. */
    transient private eu.mihosoft.freerouting.autoroute.ExpansionDrill autoroute_drill_info = null;
}
