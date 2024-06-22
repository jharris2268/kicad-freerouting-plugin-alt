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
 * ClearanceViolations.java
 *
 * Created on 3. Oktober 2004, 09:13
 */

package eu.mihosoft.freerouting.interactive;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Iterator;

import eu.mihosoft.freerouting.board.Item;
import eu.mihosoft.freerouting.board.ClearanceViolation;

/**
 * To display the clearance violations between items on the screen.
 *
 * @author Alfons Wirtz
 */
public class ClearanceViolations
{
    
    /** Creates a new instance of ClearanceViolations */
    public ClearanceViolations(Collection<Item> p_item_list)
    {
        this.list = new LinkedList<ClearanceViolation>();
        Iterator<Item> it = p_item_list.iterator();
        while (it.hasNext())
        {
            Item curr_item = it.next();
            this.list.addAll(curr_item.clearance_violations());
        }
    }
    
    /** The list of clearance violations. */
    public final Collection<ClearanceViolation> list;
}
