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
 */

package eu.mihosoft.freerouting.gui;

import java.io.File;

import eu.mihosoft.freerouting.datastructures.IdNoGenerator;

import eu.mihosoft.freerouting.board.TestLevel;
import eu.mihosoft.freerouting.board.BoardObservers;

import eu.mihosoft.freerouting.designforms.specctra.DsnFile;
import eu.mihosoft.freerouting.logger.FRLogger;

/**
 *
 * Graphical frame of for interactive editing of a routing board.
 *
 * @author Alfons Wirtz
 */

public class BoardFrame
{
    public enum Option
    {
        FROM_START_MENU, SINGLE_FRAME, SESSION_FILE, WEBSTART, EXTENDED_TOOL_BAR
    }
    
    /**
     * Creates new form BoardFrame.
     * If p_option = FROM_START_MENU this frame is created from a start menu frame.
     * If p_option = SINGLE_FRAME, this frame is created directly a single frame.
     * If p_option = Option.IN_SAND_BOX, no security sensitive actions like for example choosing
     *  If p_option = Option.WEBSTART, the application has  been started with Java Webstart.
     * files are allowed, so that the frame can be used in an applet.
     * Currently Option.EXTENDED_TOOL_BAR is used only if a new board is
     * created by the application from scratch.
     * If p_test_level {@literal >} RELEASE_VERSION, functionality not yet ready for release is included.
     * Also the warning output depends on p_test_level.
     */
    public BoardFrame(DesignFile p_design, Option p_option, TestLevel p_test_level,
            boolean p_confirm_cancel)
    {
        this(p_design, p_option, p_test_level,
                new eu.mihosoft.freerouting.board.BoardObserverAdaptor(), new eu.mihosoft.freerouting.board.ItemIdNoGenerator(),
                p_confirm_cancel);
    }
    
    /**
     * Creates new form BoardFrame.
     * The parameters p_item_observers and p_item_id_no_generator are used for syncronizing purposes,
     * if the frame is embedded into a host system,
     */
    BoardFrame(DesignFile p_design, Option p_option, TestLevel p_test_level, BoardObservers p_observers,
               eu.mihosoft.freerouting.datastructures.IdNoGenerator p_item_id_no_generator, boolean p_confirm_cancel)
    {
        this.design_file = p_design;
        this.test_level = p_test_level;
        
        this.confirm_cancel = p_confirm_cancel;
        this.board_observers = p_observers;
        this.item_id_no_generator = p_item_id_no_generator;
        boolean session_file_option = (p_option == Option.SESSION_FILE);
        this.board_panel = new BoardPanel(this);
    }
    
    
    /**
     * Reads an existing board design from file.
     * If p_is_import, the design is read from a specctra dsn file.
     * Returns false, if the file is invalid.
     */
    boolean read(java.io.InputStream p_input_stream, boolean p_is_import) {
        DsnFile.ReadResult read_result = null;
        if (p_is_import) {
            read_result = board_panel.board_handling.import_design(p_input_stream, this.board_observers,
                    this.item_id_no_generator, this.test_level);
            if (read_result == DsnFile.ReadResult.OK) {
            }
        } else {
            java.io.ObjectInputStream object_stream = null;
            try {
                object_stream = new java.io.ObjectInputStream(p_input_stream);
            } catch (java.io.IOException e) {
                return false;
            }
            boolean read_ok = board_panel.board_handling.read_design(object_stream, this.test_level);
            if (!read_ok) {
                return false;
            }
        }
        try {
            p_input_stream.close();
        } catch (java.io.IOException e) {
            return false;
        }

        return true; //update_gui(p_is_import, read_result, viewport_position, p_message_field);
    }

    /**
     * Saves the interactive settings and the design file to disk.
     * Returns false, if the save failed.
     */
    boolean save()
    {
        if (this.design_file == null)
        {
            return false;
        }
        FRLogger.info("Saving '"+design_file.get_output_file().getName()+"'...");

        java.io.OutputStream output_stream = null;
        java.io.ObjectOutputStream object_stream = null;
        try
        {
            output_stream = new java.io.FileOutputStream(this.design_file.get_output_file());
            object_stream = new java.io.ObjectOutputStream(output_stream);
        }
        catch (java.io.IOException e)
        {
	    FRLogger.error("unable to save board to file", e);
            return false;
        }
        catch (java.security.AccessControlException e)
        {
	    FRLogger.error("sorry, no write permission", e);
            return false;
        }
        boolean save_ok = board_panel.board_handling.save_design_file(object_stream);
        if (!save_ok)
        {
            return false;
        }
        try
        {
            object_stream.flush();
            output_stream.close();
        }
        catch (java.io.IOException e)
        {
	    FRLogger.error("unable to close output file", e);
            return false;
        }
        return true;
    }
    
    /** The panel with the graphical representation of the board. */
    final BoardPanel board_panel;

    private final TestLevel test_level;
    
    private final boolean confirm_cancel;
    private final BoardObservers  board_observers;
    
    private final eu.mihosoft.freerouting.datastructures.IdNoGenerator item_id_no_generator;
    
    DesignFile design_file = null;
    
}

