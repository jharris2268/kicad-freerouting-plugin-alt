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
 * ViaInfo.java
 *
 * Created on 31. Maerz 2005, 05:34
 */

package eu.mihosoft.freerouting.rules;
import eu.mihosoft.freerouting.library.Padstack;

/**
 * Information about a combination of via_padstack, via clearance class and drill_to_smd_allowed
 * used in interactive and automatic routing.
 *
 * @author Alfons Wirtz
 */
public class ViaInfo implements Comparable<ViaInfo>, java.io.Serializable
{
    
    /** Creates a new instance of ViaRule */
    public ViaInfo(String p_name, Padstack p_padstack, int p_clearance_class, boolean p_drill_to_smd_allowed,
            BoardRules p_board_rules)
    {
        name = p_name;
        padstack = p_padstack;
        clearance_class = p_clearance_class;
        attach_smd_allowed = p_drill_to_smd_allowed;
        board_rules = p_board_rules;
    }
    
    public String get_name()
    {
        return name;
    }
    
    public void set_name(String p_name)
    {
        name = p_name;
    }
    
    public String toString()
    {
        return this.name;
    }
    
    public Padstack get_padstack()
    {
        return padstack;
    }
    
    public void set_padstack(Padstack p_padstack)
    {
        padstack = p_padstack;
    }
    
    public int get_clearance_class()
    {
        return clearance_class;
    }
    
    public void set_clearance_class(int p_clearance_class)
    {
        clearance_class = p_clearance_class;
    }
    
    public boolean attach_smd_allowed()
    {
        return attach_smd_allowed;
    }
    
    public void set_attach_smd_allowed(boolean p_attach_smd_allowed)
    {
        attach_smd_allowed = p_attach_smd_allowed;
    }
    
    public int compareTo(ViaInfo p_other)
    {
        return this.name.compareTo(p_other.name);
    }
    
    private String name;
    private Padstack padstack;
    private int clearance_class;
    private boolean attach_smd_allowed;
    private final BoardRules board_rules;
}
