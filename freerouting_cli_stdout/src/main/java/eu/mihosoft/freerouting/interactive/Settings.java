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
 *   Copyright (C) 2024 James Harris
 *   Website https://github.com/jharris2268/kicad-freerouting-plugin-alt
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
 * Settings.java
 *
 * Created on 29. August 2003, 11:33
 */

package eu.mihosoft.freerouting.interactive;

import eu.mihosoft.freerouting.board.RoutingBoard;
import eu.mihosoft.freerouting.logger.FRLogger;

import java.util.Arrays;

/**
 * Contains the values of the interactive settings of the board handling.
 *
 * @author Alfons Wirtz
 */
public class Settings implements java.io.Serializable
{

/** Creates a new interactive settings variable. */
    public Settings(RoutingBoard p_board)//,  ActivityReplayFile p_activityReplayFile)
    {
        // Initialise with default values.
        layer = 0;
        push_enabled = true;
        drag_components_enabled = true;
        select_on_all_visible_layers = true; // else selection is only on the current layer
        is_stitch_route = false; // else interactive routing is dynamic
        trace_pull_tight_region_width = Integer.MAX_VALUE;
        trace_pull_tight_accuracy = 500;
        via_snap_to_smd_center = true;
        horizontal_component_grid = 0;
        vertical_component_grid = 0;
        automatic_neckdown = true;
        hilight_routing_obstacle = false;
        autoroute_settings = new AutorouteSettings(p_board);
        check_continue = false;
    }


    /**
     * Copy constructor
     */
    public Settings(Settings p_settings)
    {
        this.layer = p_settings.layer;
        this.push_enabled = p_settings.push_enabled;
        this.drag_components_enabled = p_settings.drag_components_enabled;
        this.select_on_all_visible_layers = p_settings.select_on_all_visible_layers;
        this.is_stitch_route = p_settings.is_stitch_route;
        this.trace_pull_tight_region_width = p_settings.trace_pull_tight_region_width;
        this.trace_pull_tight_accuracy = p_settings.trace_pull_tight_accuracy;
        this.via_snap_to_smd_center = p_settings.via_snap_to_smd_center;
        this.horizontal_component_grid = p_settings.horizontal_component_grid;
        this.vertical_component_grid = p_settings.vertical_component_grid;
        this.automatic_neckdown = p_settings.automatic_neckdown;
        this.hilight_routing_obstacle = p_settings.hilight_routing_obstacle;
        
        this.check_continue = p_settings.check_continue;
    }
    
    public  int get_layer()
    {
        return this.layer;
    }

    /** The accuracy of the pull tight algorithm. */
    public  int get_trace_pull_tight_accuracy()
    {
        return this.trace_pull_tight_accuracy;
    }

    /**
     * If true, the trace width at static pins smaller the the trace width will be lowered
     * automatically to the pin with, if necessary.
     */
    public  boolean get_automatic_neckdown()
    {
        return this.automatic_neckdown;
    }

    /** the current layer */
    int layer;
    
    /** allows pushing obstacles aside */
    boolean push_enabled;
    
    /** allows dragging components with the route */
    boolean drag_components_enabled;
    
    /** indicates if interactive selections are made on all visible layers or only on the current layer */
    boolean select_on_all_visible_layers ;
    
    /** Route mode: stitching or dynamic */
    boolean is_stitch_route;
    
    /** The width of the pull tight region of traces around the cursor */
    int trace_pull_tight_region_width;
    
    /** The accuracy of the pull tight algorithm. */
    int trace_pull_tight_accuracy;
    
    /**
     * Via snaps to smd center, if attach smd is alllowed.
     */
    boolean via_snap_to_smd_center;
    
    /**
     * The horizontal placement grid when moving components, if {@literal >} 0.
     */
    int horizontal_component_grid;
    
    /**
     * The vertical placement grid when moving components, if {@literal >} 0.
     */
    int vertical_component_grid;
    
    /**
     * If true, the trace width at static pins smaller the the trace width will be lowered
     * automatically to the pin with, if necessary.
     */
    boolean automatic_neckdown;
    
    /** If true, the current routing obstacle is hilightet in dynamic routing. */
    boolean hilight_routing_obstacle;
    
    public AutorouteSettings autoroute_settings;
    public boolean check_continue;
}
