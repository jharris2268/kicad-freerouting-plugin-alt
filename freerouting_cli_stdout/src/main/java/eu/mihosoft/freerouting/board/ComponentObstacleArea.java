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
 * ComponentObstacleArea.java
 *
 * Created on 8. Mai 2005, 07:28
 */

package eu.mihosoft.freerouting.board;


import eu.mihosoft.freerouting.geometry.planar.Area;
import eu.mihosoft.freerouting.geometry.planar.Vector;

/**
 * Describes areas of the board, where components are not allowed.
 *
 * @author Alfons Wirtz
 */
public class ComponentObstacleArea extends ObstacleArea
{
    /**
     * Creates a new instance of  ComponentObstacleArea
     * If p_is_obstacle ist false, the new instance is not regarded as obstacle and used only for displaying on the screen.
     */
    ComponentObstacleArea(Area p_area, int p_layer, Vector p_translation, double p_rotation_in_degree,
            boolean p_side_changed, int p_clearance_type, int p_id_no, int p_component_no,
            String p_name, FixedState p_fixed_state, BasicBoard p_board)
    {
        super(p_area, p_layer, p_translation, p_rotation_in_degree, p_side_changed, new int[0],
                p_clearance_type, p_id_no, p_component_no, p_name, p_fixed_state, p_board);
    }
    
    public Item copy(int p_id_no)
    {
        return new ComponentObstacleArea(get_relative_area(), get_layer(), get_translation(),
                get_rotation_in_degree(), get_side_changed(), clearance_class_no(), p_id_no,
                get_component_no(), this.name, get_fixed_state(), board);
    }
    
    public boolean is_obstacle(Item p_other)
    {
        return p_other != this && p_other instanceof ComponentObstacleArea
                && p_other.get_component_no() != this.get_component_no();
    }
    
    public boolean is_trace_obstacle(int p_net_no)
    {
        return false;
    }
    
    public boolean is_selected_by_filter(ItemSelectionFilter p_filter)
    {
        if (!this.is_selected_by_fixed_filter(p_filter))
        {
            return false;
        }
        return p_filter.is_selected(ItemSelectionFilter.SelectableChoices.COMPONENT_KEEPOUT);
    }
    
    public boolean is_selectrd_by_filter(ItemSelectionFilter p_filter)
    {
        if (!this.is_selected_by_fixed_filter(p_filter))
        {
            return false;
        }
        return p_filter.is_selected(ItemSelectionFilter.SelectableChoices.COMPONENT_KEEPOUT);
    }
    
}
