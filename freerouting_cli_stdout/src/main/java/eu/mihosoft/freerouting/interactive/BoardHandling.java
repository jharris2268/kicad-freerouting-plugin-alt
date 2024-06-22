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
 * BoardHandling.java
 *
 * Created on 5. November 2003, 13:02
 *
 */
package eu.mihosoft.freerouting.interactive;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Set;

import eu.mihosoft.freerouting.designforms.specctra.SessionFile;
import eu.mihosoft.freerouting.geometry.planar.FloatPoint;
import eu.mihosoft.freerouting.geometry.planar.IntBox;
import eu.mihosoft.freerouting.geometry.planar.IntPoint;
import eu.mihosoft.freerouting.geometry.planar.PolylineShape;

import eu.mihosoft.freerouting.gui.BoardPanel;
import eu.mihosoft.freerouting.logger.FRLogger;
import eu.mihosoft.freerouting.rules.BoardRules;
import eu.mihosoft.freerouting.board.LayerStructure;
import eu.mihosoft.freerouting.board.RoutingBoard;
import eu.mihosoft.freerouting.board.Item;
import eu.mihosoft.freerouting.board.PolylineTrace;
import eu.mihosoft.freerouting.board.FixedState;
import eu.mihosoft.freerouting.board.ItemSelectionFilter;

import eu.mihosoft.freerouting.board.CoordinateTransform;
import eu.mihosoft.freerouting.board.Unit;
import eu.mihosoft.freerouting.board.TestLevel;

import eu.mihosoft.freerouting.designforms.specctra.DsnFile;

/**
 *
 * Central connection class between the graphical user interface and
 * the board database.
 *
 * @author Alfons Wirtz
 */
public class BoardHandling extends BoardHandlingImpl
{

    /**
     * Creates a new BoardHandling
     */
    public BoardHandling(BoardPanel p_panel)
    {
        this.panel = p_panel;
    }

    /**
     * returns the number of layers of the board design.
     */
    public int get_layer_count()
    {
        if (board == null)
        {
            return 0;
        }
        return board.get_layer_count();
    }


    /**
     * * Tells the router, if conduction areas should be ignored..
     */
    public void set_ignore_conduction(boolean p_value)
    {
        board.change_conduction_is_obstacle(!p_value);

//        activityReplayFile.start_scope(ActivityReplayFileScope.SET_IGNORE_CONDUCTION, p_value);
    }

    public void set_pin_edge_to_turn_dist(double p_value)
    {
        double edge_to_turn_dist = this.coordinate_transform.user_to_board(p_value);
        if (edge_to_turn_dist != board.rules.get_pin_edge_to_turn_dist())
        {
            // unfix the pin exit stubs
            Collection<eu.mihosoft.freerouting.board.Pin> pin_list = board.get_pins();
            for (eu.mihosoft.freerouting.board.Pin curr_pin : pin_list)
            {
                if (curr_pin.has_trace_exit_restrictions())
                {
                    Collection<Item> contact_list = curr_pin.get_normal_contacts();
                    for (Item curr_contact : contact_list)
                    {
                        if ((curr_contact instanceof PolylineTrace) && curr_contact.get_fixed_state() == FixedState.SHOVE_FIXED)
                        {
                            if (((PolylineTrace) curr_contact).corner_count() == 2)
                            {
                                curr_contact.set_fixed_state(FixedState.UNFIXED);
                            }
                        }
                    }
                }
            }
        }
        board.rules.set_pin_edge_to_turn_dist(edge_to_turn_dist);
    }

    /**
     * Changes the default trace halfwidth currently used in interactive routing on the input layer.
     */
    public void set_default_trace_halfwidth(int p_layer, int p_value)
    {
        if (p_layer >= 0 && p_layer <= board.get_layer_count())
        {
            board.rules.set_default_trace_half_width(p_layer, p_value);
  //          activityReplayFile.start_scope(ActivityReplayFileScope.SET_TRACE_HALF_WIDTH, p_layer);
  //          activityReplayFile.add_int(p_value);
        }
    }

    /**
     * Recalculates the incomplete connections, if the ratsnest is active.
     */
    void update_ratsnest()
    {
        if (ratsnest != null)
        {
            ratsnest = new RatsNest(this.board);
        }
    }

    /**
     * Removes the incomplete connections.
     */
    public void remove_ratsnest()
    {
        ratsnest = null;
    }

    /**
     * Returns the ratsnest with the information about the incomplete connections.
     */
    public RatsNest get_ratsnest()
    {
        if (ratsnest == null)
        {
            ratsnest = new RatsNest(this.board);
        }
        return this.ratsnest;
    }

    /**
     * Creates the Routingboard, the graphic context and the interactive settings.
     */
    @Override
    public void create_board(IntBox p_bounding_box, LayerStructure p_layer_structure,
                             PolylineShape[] p_outline_shapes, String p_outline_clearance_class_name,
                             BoardRules p_rules, eu.mihosoft.freerouting.board.Communication p_board_communication, TestLevel p_test_level)
    {
        super.create_board(p_bounding_box, p_layer_structure, p_outline_shapes, p_outline_clearance_class_name, p_rules,
                p_board_communication, p_test_level);
	// create the interactive settings with default
        double unit_factor = p_board_communication.coordinate_transform.board_to_dsn(1);
        this.coordinate_transform = new CoordinateTransform(1, p_board_communication.unit, unit_factor, p_board_communication.unit);


    }

    /**
     * Reads an existing board design from the input stream.
     * Returns false,  if the input stream does not contains a legal board design.
     */
    public boolean read_design(java.io.ObjectInputStream p_design, TestLevel p_test_level)
    {
        try
        {
            board = (RoutingBoard) p_design.readObject();
            settings = (Settings) p_design.readObject();
    //        settings.set_logfile(this.activityReplayFile);
            coordinate_transform = (CoordinateTransform) p_design.readObject();
        }
        catch (Exception e)
        {
            FRLogger.error("Couldn't read design file", e);
            return false;
        }
        board.set_test_level(p_test_level);
     //   screen_messages.set_layer(board.layer_structure.arr[settings.layer].name);
        return true;
    }

    /**
     * Imports a board design from a Specctra dsn-file.
     * The parameters p_item_observers and p_item_id_no_generator are used,
     * in case the board is embedded into a host system.
     * Returns false, if the dsn-file is corrupted.
     */
    public DsnFile.ReadResult import_design(java.io.InputStream p_design,
                                            eu.mihosoft.freerouting.board.BoardObservers p_observers,
                                            eu.mihosoft.freerouting.datastructures.IdNoGenerator p_item_id_no_generator, TestLevel p_test_level)
    {
        if (p_design == null)
        {
            return DsnFile.ReadResult.ERROR;
        }
        DsnFile.ReadResult read_result;
        try
        {
            read_result =
                    DsnFile.read(p_design, this, p_observers,
                    p_item_id_no_generator, p_test_level);
        }
        catch (Exception e)
        {
            read_result = DsnFile.ReadResult.ERROR;
            FRLogger.error("There was an error while reading DSN file.", e);
        }
        if (read_result == DsnFile.ReadResult.OK)
        {
            this.board.reduce_nets_of_route_items();
      //      this.set_layer(0);
        }
        try
        {
            p_design.close();
        }
        catch (java.io.IOException e)
        {
            read_result = DsnFile.ReadResult.ERROR;
        }
        return read_result;
    }

    /**
     * Writes the currently edited board design to a text file in the Specctra dsn format.
     * If p_compat_mode is true, only standard specctra dsn scopes are written, so that any
     * host system with an specctra interface can read them.
     */
    public boolean export_to_dsn_file(OutputStream p_output_stream, String p_design_name, boolean p_compat_mode)
    {
        if (p_output_stream == null)
        {
            return false;
        }
        return DsnFile.write(this, p_output_stream, p_design_name, p_compat_mode);
    }

    /**
     * Writes a session file ins the Specctra ses-format.
     */
    public boolean export_specctra_session_file(String p_design_name, OutputStream p_output_stream)
    {
        return SessionFile.write(this.get_routing_board(), p_output_stream, p_design_name);
    }

    /**
     * Saves the currently edited board design to p_design_file.
     */
    public boolean save_design_file(java.io.ObjectOutputStream p_object_stream)
    {
        boolean result = true;
        try
        {
            p_object_stream.writeObject(board);
            p_object_stream.writeObject(settings);
        }
        catch (Exception e)
        {
	    FRLogger.error("unable to write board to file", e);
            result = false;
        }
        return result;
    }

    /**
     * Start the batch autorouter on the whole Board
     */
    public InteractiveActionThread start_batch_autorouter()
    {
        this.interactive_action_thread = InteractiveActionThread.get_batch_autorouter_instance(this);

        this.interactive_action_thread.start();

        return this.interactive_action_thread;
    }

    /**
     * Sets all references inside this class to null, so that it can be recycled
     * by the garbage collector.
     */
    public void dispose()
    {
        coordinate_transform = null;
        settings = null;
        ratsnest = null;
        clearance_violations = null;
        board = null;
    }
    /** For transforming coordinates between the user and the board coordinate space */
    public CoordinateTransform coordinate_transform = null;
    /**
     * Used for running an interactive action in a separate thread.
     */
    private InteractiveActionThread interactive_action_thread = null;
    /** To display all incomplete connections on the screen. */
    private RatsNest ratsnest = null;
    /** To display all clearance violations between items on the screen. */
    private ClearanceViolations clearance_violations = null;
    /** The graphical panel used for displaying the board. */
    private final BoardPanel panel;
}
