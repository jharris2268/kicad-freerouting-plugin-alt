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
 * ClearanceViolation.java
 *
 * Created on 4. Oktober 2004, 08:56
 */

package eu.mihosoft.freerouting.board;

import eu.mihosoft.freerouting.geometry.planar.ConvexShape;


/**
 * Information of a clearance violation between 2 items.
 *
 * @author Alfons Wirtz
 */
public class ClearanceViolation
{
    
    /** Creates a new instance of ClearanceViolation */
    public ClearanceViolation(Item p_first_item, Item p_second_item, ConvexShape p_shape, int p_layer)
    {
        first_item = p_first_item;
        second_item = p_second_item;
        shape = p_shape;
        layer = p_layer;
    }
    
    /** The first item of the clearance violation */
    public final Item first_item;
    /** The second item of the clearance violation */
    public final Item second_item;
    /** The shape of the clearance violation */
    public final ConvexShape shape;
    /** The layer of the clearance violation */
    public final int layer;
}
