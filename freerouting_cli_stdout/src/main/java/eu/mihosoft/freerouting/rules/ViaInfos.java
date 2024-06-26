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
 * ViaInfos.java
 *
 * Created on 2. April 2005, 06:49
 */

package eu.mihosoft.freerouting.rules;

import java.util.List;
import java.util.LinkedList;

/**
 * Contains the lists of different ViaInfo's, which can be used in interactive and automatic routing.
 *
 * @author Alfons Wirtz
 */
public class ViaInfos implements java.io.Serializable
{
    /**
     * Adds a via info consisting of padstack, clearance class and drill_to_smd_allowed.
     * Return false, if the insertion failed, for example if the name existed already.
     */
    public boolean add(ViaInfo p_via_info)
    {
        if (name_exists(p_via_info.get_name()))
        {
            return false;
        }
        this.list.add(p_via_info);
        return true;
    }
    
    /**
     * Returns the number of different vias, which can be used for routing.
     */
    public int count()
    {
        return this.list.size();
    }
    
    /**
     * Returns the p_no-th via af the via types, which can be used for routing.
     */
    public ViaInfo get(int p_no)
    {
        assert p_no >= 0 && p_no < this.list.size();
        return this.list.get(p_no);
    }
    
    /**
     * Returns the via info with name p_name, or null, if no such via exists.
     */
    public ViaInfo get(String p_name)
    {
        for (ViaInfo curr_via : this.list)
        {
            if (curr_via.get_name().equals(p_name))
            {
                return curr_via;
            }
        }
        return null;
    }
    
    /**
     * Returns true, if a via info with name p_name is already wyisting in the list.
     */
    public boolean name_exists(String p_name)
    {
        for (ViaInfo curr_via : this.list)
        {
            if (curr_via.get_name().equals(p_name))
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Removes p_via_info from this list.
     * Returns false, if p_via_info was not contained in the list.
     */
    public boolean remove(ViaInfo p_via_info)
    {
        return this.list.remove(p_via_info);
    }
    
    private List<ViaInfo> list = new LinkedList<ViaInfo>();
}
