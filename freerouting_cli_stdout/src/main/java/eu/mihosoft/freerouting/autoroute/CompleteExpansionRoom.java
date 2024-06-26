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
 * CompleteExpansionRoom.java
 *
 * Created on 16. April 2006, 07:47
 *
 */

package eu.mihosoft.freerouting.autoroute;

import java.util.Collection;

/**
 *
 * @author Alfons Wirtz
 */
public interface CompleteExpansionRoom extends ExpansionRoom
{
    
    /**
     * Returns the list of doors to target items of this room
     */
    Collection<TargetItemExpansionDoor> get_target_doors();
    
    /**
     * Returns the object of tthis complete_expansion_rooom.
     */
    eu.mihosoft.freerouting.board.SearchTreeObject get_object();
    
}
