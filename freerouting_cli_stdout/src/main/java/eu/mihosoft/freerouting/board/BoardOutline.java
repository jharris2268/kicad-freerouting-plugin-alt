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
 * BoardOutline.java
 *
 * Created on 18. August 2004, 07:24
 */
package eu.mihosoft.freerouting.board;

import eu.mihosoft.freerouting.geometry.planar.IntBox;
import eu.mihosoft.freerouting.geometry.planar.IntPoint;
import eu.mihosoft.freerouting.geometry.planar.TileShape;
import eu.mihosoft.freerouting.geometry.planar.PolylineShape;
import eu.mihosoft.freerouting.geometry.planar.PolylineArea;
import eu.mihosoft.freerouting.geometry.planar.Area;
import eu.mihosoft.freerouting.geometry.planar.Vector;
import eu.mihosoft.freerouting.geometry.planar.FloatPoint;

import eu.mihosoft.freerouting.logger.FRLogger;

import java.util.Arrays;

/**
 * Class describing a board outline.
 *
 * @author Alfons Wirtz
 */
public class BoardOutline extends Item implements java.io.Serializable
{

    /** Creates a new instance of BoardOutline */
    public BoardOutline(PolylineShape[] p_shapes, int p_clearance_class_no, int p_id_no, BasicBoard p_board)
    {
        super(new int[0], p_clearance_class_no, p_id_no, 0, FixedState.SYSTEM_FIXED, p_board);
        shapes = p_shapes;
    }

    public int tile_shape_count()
    {
        int result;
        if (this.keepout_outside_outline)
        {
            TileShape[] tile_shapes = this.get_keepout_area().split_to_convex();
            if (tile_shapes == null)
            {
                // an error accured while dividing the area
                result = 0;
            }
            else
            {
                result = tile_shapes.length * this.board.layer_structure.arr.length;
            }
        }
        else
        {
            result = this.line_count() * this.board.layer_structure.arr.length;
        }
        return result;
    }

    public int shape_layer(int p_index)
    {
        int shape_count = this.tile_shape_count();
        int result;
        if (shape_count > 0)
        {
            result = p_index * this.board.layer_structure.arr.length / shape_count;
        }
        else
        {
            result = 0;
        }
        if (result < 0 || result >= this.board.layer_structure.arr.length)
        {
            FRLogger.warn("BoardOutline.shape_layer: p_index out of range");
        }
        return result;
    }

    public boolean is_obstacle(Item p_other)
    {
        return !(p_other instanceof BoardOutline || p_other instanceof ObstacleArea);
    }

    public IntBox bounding_box()
    {
        IntBox result = IntBox.EMPTY;
        for (PolylineShape curr_shape : this.shapes)
        {
            result = result.union(curr_shape.bounding_box());
        }
        return result;
    }

    public int first_layer()
    {
        return 0;
    }

    public int last_layer()
    {
        return this.board.layer_structure.arr.length - 1;
    }

    public boolean is_on_layer(int p_layer)
    {
        return true;
    }

    public void translate_by(Vector p_vector)
    {
        for (PolylineShape curr_shape : this.shapes)
        {
            curr_shape = curr_shape.translate_by(p_vector);
        }
        if (keepout_area != null)
        {
            keepout_area = keepout_area.translate_by(p_vector);
        }
        keepout_lines = null;
    }

    public void turn_90_degree(int p_factor, IntPoint p_pole)
    {
        for (PolylineShape curr_shape : this.shapes)
        {
            curr_shape = curr_shape.turn_90_degree(p_factor, p_pole);
        }
        if (keepout_area != null)
        {
            keepout_area = keepout_area.turn_90_degree(p_factor, p_pole);
        }
        keepout_lines = null;
    }

    public void rotate_approx(double p_angle_in_degree, FloatPoint p_pole)
    {
        double angle = Math.toRadians(p_angle_in_degree);
        for (PolylineShape curr_shape : this.shapes)
        {
            curr_shape = curr_shape.rotate_approx(angle, p_pole);

        }
        if (keepout_area != null)
        {
            keepout_area = keepout_area.rotate_approx(angle, p_pole);
        }
        keepout_lines = null;
    }

    public void change_placement_side(IntPoint p_pole)
    {
        for (PolylineShape curr_shape : this.shapes)
        {
            curr_shape = curr_shape.mirror_vertical(p_pole);
        }
        if (keepout_area != null)
        {
            keepout_area = keepout_area.mirror_vertical(p_pole);
        }
        keepout_lines = null;
    }

    public int shape_count()
    {
        return this.shapes.length;
    }

    public PolylineShape get_shape(int p_index)
    {
        if (p_index < 0 || p_index >= this.shapes.length)
        {
            FRLogger.warn("BoardOutline.get_shape: p_index out of range");
            return null;
        }
        return this.shapes[p_index];
    }

    public boolean is_selected_by_filter(ItemSelectionFilter p_filter)
    {
        if (!this.is_selected_by_fixed_filter(p_filter))
        {
            return false;
        }
        return p_filter.is_selected(ItemSelectionFilter.SelectableChoices.BOARD_OUTLINE);
    }

    /**
     * The board shape outside the outline curves, where a keepout will be generated
     * The outline curves are holes of the keepout_area.
     */
    Area get_keepout_area()
    {
        if (this.keepout_area == null)
        {
            PolylineShape[] hole_arr = new PolylineShape[this.shapes.length];
            for (int i = 0; i < hole_arr.length; ++i)
            {
                hole_arr[i] = this.shapes[i];
            }
            keepout_area = new PolylineArea(this.board.bounding_box, hole_arr);
        }
        return this.keepout_area;
    }

    TileShape[] get_keepout_lines()
    {
        if (this.keepout_lines == null)
        {
            this.keepout_lines = new TileShape[0];
        }
        return this.keepout_lines;
    }

    public Item copy(int p_id_no)
    {
        return new BoardOutline(this.shapes, this.clearance_class_no(), p_id_no, this.board);
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

    /**
     *  Returns, if keepout is generated outside the board outline.
     *  Otherwise only the line shapes of the outlines  are inserted as keepout.
     */
    public boolean keepout_outside_outline_generated()
    {
        return keepout_outside_outline;
    }

    /**
     *  Makes the area outside this Outline to Keepout, if p_valus = true.
     *  Reinserts this Outline into the search trees, if the value changes.
     */
    public void generate_keepout_outside(boolean p_value)
    {
        if (p_value == keepout_outside_outline)
        {
            return;
        }
        keepout_outside_outline = p_value;
        if (this.board == null || this.board.search_tree_manager == null)
        {
            return;
        }
        this.board.search_tree_manager.remove(this);
        this.board.search_tree_manager.insert(this);

    }

    /**
     * Returns the sum of the lines of all outline poligons.
     */
    public int line_count()
    {
        int result = 0;
        for (PolylineShape curr_shape : this.shapes)
        {
            result += curr_shape.border_line_count();
        }
        return result;
    }

    /**
     *  Returns the half width of the lines of this outline.
     */
    public int get_half_width()
    {
        return HALF_WIDTH;
    }

    protected TileShape[] calculate_tree_shapes(ShapeSearchTree p_search_tree)
    {
        return p_search_tree.calculate_tree_shapes(this);
    }
    /** The board shapes inside the outline curves. */
    private PolylineShape[] shapes;
    /**
     * The board shape outside the outline curves, where a keepout will be generated
     * The outline curves are holes of the keepout_area.
     */
    private Area keepout_area = null;
    /**
     *  Used instead of keepout_area if  only the line shapes of the outlines  are inserted as keepout.
     */
    private TileShape[] keepout_lines = null;
    private boolean keepout_outside_outline = false;
    private static final int HALF_WIDTH = 100;
}
