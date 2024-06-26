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
 * AutorouteSettings.java
 *
 * Created on 1. Maerz 2007, 07:10
 *
 */
package eu.mihosoft.freerouting.designforms.specctra;

import eu.mihosoft.freerouting.datastructures.IndentFileWriter;
import eu.mihosoft.freerouting.datastructures.IdentifierType;
import eu.mihosoft.freerouting.logger.FRLogger;

/**
 *
 * @author Alfons Wirtz
 */
public class AutorouteSettings
{

    static eu.mihosoft.freerouting.interactive.AutorouteSettings read_scope(Scanner p_scanner, LayerStructure p_layer_structure)
    {
        eu.mihosoft.freerouting.interactive.AutorouteSettings result = new eu.mihosoft.freerouting.interactive.AutorouteSettings(p_layer_structure.arr.length);
        boolean dsn_fanout_request = false;
        boolean dsn_autoroute_request = true;
        boolean dsn_postroute_request = true;
	// the default passes are affected by command line options passed to freeroutng.cli
        Object next_token = null;
        for (;;)
        {
            Object prev_token = next_token;
            try
            {
                next_token = p_scanner.next_token();
            } catch (java.io.IOException e)
            {
                FRLogger.error("AutorouteSettings.read_scope: IO error scanning file", e);
                return null;
            }
            if (next_token == null)
            {
                FRLogger.warn("AutorouteSettings.read_scope: unexpected end of file");
                return null;
            }
            if (next_token == Keyword.CLOSED_BRACKET)
            {
                // end of scope
                break;
            }
            if (prev_token == Keyword.OPEN_BRACKET)
            {
                if (next_token == Keyword.FANOUT)
                {
                    dsn_fanout_request = DsnFile.read_on_off_scope(p_scanner);
                }
                else if (next_token == Keyword.AUTOROUTE)
                {
                    dsn_autoroute_request = DsnFile.read_on_off_scope(p_scanner);
                }
                else if (next_token == Keyword.POSTROUTE)
                {
                    dsn_postroute_request = DsnFile.read_on_off_scope(p_scanner);
                }
                else if (next_token == Keyword.VIAS)
                {
                    result.set_vias_allowed(DsnFile.read_on_off_scope(p_scanner));
                }
                else if (next_token == Keyword.VIA_COSTS)
                {
                    result.set_via_costs(DsnFile.read_integer_scope(p_scanner));
                }
                else if (next_token == Keyword.PLANE_VIA_COSTS)
                {
                    result.set_plane_via_costs(DsnFile.read_integer_scope(p_scanner));
                }
                else if (next_token == Keyword.START_RIPUP_COSTS)
                {
                    result.set_start_ripup_costs(DsnFile.read_integer_scope(p_scanner));
                }
                else if (next_token == Keyword.START_PASS_NO)
                {
                    result.set_start_pass_no(DsnFile.read_integer_scope(p_scanner));
                }
                else if (next_token == Keyword.LAYER_RULE)
                {
                    result = read_layer_rule(p_scanner, p_layer_structure, result);
                    if (result == null)
                    {
                        return null;
                    }
                }
                else
                {
                    ScopeKeyword.skip_scope(p_scanner);
                }
            }
        }
        result.set_dsn_fanout_requested(dsn_fanout_request);
        result.set_dsn_autoroute_requested(dsn_autoroute_request);
        result.set_dsn_postroute_requested(dsn_postroute_request);
        return result;
    }

    static eu.mihosoft.freerouting.interactive.AutorouteSettings read_layer_rule(Scanner p_scanner, LayerStructure p_layer_structure,
                                                                                 eu.mihosoft.freerouting.interactive.AutorouteSettings p_settings)
    {
        p_scanner.yybegin(SpecctraFileScanner.NAME);
        Object next_token;
        try
        {
            next_token = p_scanner.next_token();
        } catch (java.io.IOException e)
        {
            FRLogger.error("AutorouteSettings.read_layer_rule: IO error scanning file", e);
            return null;
        }
        if (!(next_token instanceof String))
        {
            FRLogger.warn("AutorouteSettings.read_layer_rule: String expected");
            return null;
        }
        int layer_no = p_layer_structure.get_no((String) next_token);
        if (layer_no < 0)
        {
            FRLogger.warn("AutorouteSettings.read_layer_rule: layer not found");
            return null;
        }
        for (;;)
        {
            Object prev_token = next_token;
            try
            {
                next_token = p_scanner.next_token();
            } catch (java.io.IOException e)
            {
                FRLogger.error("AutorouteSettings.read_layer_rule: IO error scanning file", e);
                return null;
            }
            if (next_token == null)
            {
                FRLogger.warn("AutorouteSettings.read_layer_rule: unexpected end of file");
                return null;
            }
            if (next_token == Keyword.CLOSED_BRACKET)
            {
                // end of scope
                break;
            }
            if (prev_token == Keyword.OPEN_BRACKET)
            {
                if (next_token == Keyword.ACTIVE)
                {
                    p_settings.set_layer_active(layer_no, DsnFile.read_on_off_scope(p_scanner));
                }
                else if (next_token == Keyword.PREFERRED_DIRECTION)
                {
                    try
                    {
                        boolean pref_dir_is_horizontal = true;
                        next_token = p_scanner.next_token();
                        if (next_token == Keyword.VERTICAL)
                        {
                            pref_dir_is_horizontal = false;
                        }
                        else if (next_token != Keyword.HORIZONTAL)
                        {
                            FRLogger.warn("AutorouteSettings.read_layer_rule: unexpected key word");
                            return null;
                        }
                        p_settings.set_preferred_direction_is_horizontal(layer_no, pref_dir_is_horizontal);
                        next_token = p_scanner.next_token();
                        if (next_token != Keyword.CLOSED_BRACKET)
                        {
                            FRLogger.warn("AutorouteSettings.read_layer_rule: uclosing bracket expected");
                            return null;
                        }
                    } catch (java.io.IOException e)
                    {
                        FRLogger.error("AutorouteSettings.read_layer_rule: IO error scanning file", e);
                        return null;
                    }
                }
                else if (next_token == Keyword.PREFERRED_DIRECTION_TRACE_COSTS)
                {
                    p_settings.set_preferred_direction_trace_costs(layer_no, DsnFile.read_float_scope(p_scanner));
                }
                else if (next_token == Keyword.AGAINST_PREFERRED_DIRECTION_TRACE_COSTS)
                {
                    p_settings.set_against_preferred_direction_trace_costs(layer_no, DsnFile.read_float_scope(p_scanner));
                }
                else
                {
                    ScopeKeyword.skip_scope(p_scanner);
                }
            }
        }
        return p_settings;
    }

    static void write_scope(IndentFileWriter p_file, eu.mihosoft.freerouting.interactive.AutorouteSettings p_settings,
                            eu.mihosoft.freerouting.board.LayerStructure p_layer_structure, IdentifierType p_identifier_type) throws java.io.IOException
    {
        p_file.start_scope();
        p_file.write("autoroute_settings");
        p_file.new_line();
        p_file.write("(fanout ");
        if (p_settings.get_with_fanout())
        {
            p_file.write("on)");
        }
        else
        {
            p_file.write("off)");
        }
        p_file.new_line();
        p_file.write("(eu.mihosoft.freerouting.autoroute ");
        if (p_settings.get_with_autoroute())
        {
            p_file.write("on)");
        }
        else
        {
            p_file.write("off)");
        }
        p_file.new_line();
        p_file.write("(postroute ");
        if (p_settings.get_with_postroute())
        {
            p_file.write("on)");
        }
        else
        {
            p_file.write("off)");
        }
        p_file.new_line();
        p_file.write("(vias ");
        if (p_settings.get_vias_allowed())
        {
            p_file.write("on)");
        }
        else
        {
            p_file.write("off)");
        }
        p_file.new_line();
        p_file.write("(via_costs ");
        {
            Integer via_costs = p_settings.get_via_costs();
            p_file.write(via_costs.toString());
        }
        p_file.write(")");
        p_file.new_line();
        p_file.write("(plane_via_costs ");
        {
            Integer via_costs = p_settings.get_plane_via_costs();
            p_file.write(via_costs.toString());
        }
        p_file.write(")");
        p_file.new_line();
        p_file.write("(start_ripup_costs ");
        {
            Integer ripup_costs = p_settings.get_start_ripup_costs();
            p_file.write(ripup_costs.toString());
        }
        p_file.write(")");
        p_file.new_line();
        p_file.write("(start_pass_no ");
        {
            Integer pass_no = p_settings.get_start_pass_no();
            p_file.write(pass_no.toString());
        }
        p_file.write(")");
        for (int i = 0; i < p_layer_structure.arr.length; ++i)
        {
            eu.mihosoft.freerouting.board.Layer curr_layer = p_layer_structure.arr[i];
            p_file.start_scope();
            p_file.write("layer_rule ");
            p_identifier_type.write(curr_layer.name, p_file);
            p_file.new_line();
            p_file.write("(active ");
            if (p_settings.get_layer_active(i))
            {
                p_file.write("on)");
            }
            else
            {
                p_file.write("off)");
            }
            p_file.new_line();
            p_file.write("(preferred_direction ");
            if (p_settings.get_preferred_direction_is_horizontal(i))
            {
                p_file.write("horizontal)");
            }
            else
            {
                p_file.write("vertical)");
            }
            p_file.new_line();
            p_file.write("(preferred_direction_trace_costs ");
            Float trace_costs = (float) p_settings.get_preferred_direction_trace_costs(i);
            p_file.write(trace_costs.toString());
            p_file.write(")");
            p_file.new_line();
            p_file.write("(against_preferred_direction_trace_costs ");
            trace_costs = (float) p_settings.get_against_preferred_direction_trace_costs(i);
            p_file.write(trace_costs.toString());
            p_file.write(")");
            p_file.end_scope();
        }
        p_file.end_scope();
    }
}
