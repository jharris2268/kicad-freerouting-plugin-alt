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
 * CoordinateTransform.java
 *
 * Created on 17. Dezember 2004, 07:34
 */

package eu.mihosoft.freerouting.board;

import eu.mihosoft.freerouting.geometry.planar.FloatPoint;
import eu.mihosoft.freerouting.logger.FRLogger;

/**
 * Class for transforming objects between user coordinate space and board coordinate space.
 *
 * @author Alfons Wirtz
 */
public class CoordinateTransform implements java.io.Serializable
{
    
    /** Creates a new instance of CoordinateTransform */
    public CoordinateTransform(double p_user_unit_factor, Unit p_user_unit,
            double p_board_unit_factor, Unit p_board_unit)
    {
        user_unit =  p_user_unit;
        board_unit =  p_board_unit;
        user_unit_factor = p_user_unit_factor;
        board_unit_factor = p_board_unit_factor;
        scale_factor = board_unit_factor / user_unit_factor;
    }
    
    /**
     * Scale a value from the board to the user coordinate system.
     */
    public double board_to_user(double p_value)
    {
        return Unit.scale(p_value * scale_factor, board_unit, user_unit);
    }
    
    /**
     * Scale a value from the user to the board coordinate system.
     */
    public double user_to_board(double p_value)
    {
        return Unit.scale(p_value / scale_factor, user_unit, board_unit) ;
    }
    
    
    /**
     * Transforms a geometry.planar.FloatPoint from the board coordinate space
     * to the user coordinate space.
     */
    public FloatPoint board_to_user(FloatPoint p_point)
    {
        return  new FloatPoint(board_to_user(p_point.x), board_to_user(p_point.y));
    }
    
    /**
     * Transforms a geometry.planar.FloatPoint from the user coordinate space.
     * to the board coordinate space.
     */
    public FloatPoint user_to_board(FloatPoint p_point)
    {
        return  new FloatPoint(user_to_board(p_point.x), user_to_board(p_point.y));
    }
    
    /** The unit used for user coordinates */
    public final Unit user_unit;
    
    
    /** The factor of the user unit */
    public final double user_unit_factor;
    
    /** The unit used for board coordinates */
    public final Unit board_unit;
    
    /** The factor of the board unit */
    public final double board_unit_factor;
    
    /** The factor used for transforming coordinates between user coordinate space and board coordinate space */
    private final double scale_factor;
    
}
