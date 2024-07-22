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
 * DesignFile.java
 *
 * Created on 25. Oktober 2006, 07:48
 *
 */
package eu.mihosoft.freerouting.gui;

import eu.mihosoft.freerouting.designforms.specctra.RulesFile;
import eu.mihosoft.freerouting.logger.FRLogger;

/**
 *  File functionality with security restrictions used, when the application is opened with Java Webstart
 *
 * @author Alfons Wirtz
 */
public class DesignFile
{

    public static final String[] all_file_extensions = {"bin", "dsn"};
    public static final String[] text_file_extensions = {"dsn"};
    public static final String binary_file_extension = "bin";

/*
    public static DesignFile get_instance(String p_design_file_name, boolean p_is_webstart)
    {
        if (p_design_file_name == null)
        {
            return null;
        }
        DesignFile result = new DesignFile(new java.io.File(p_design_file_name));//, null);
        return result;
    }
*/
    /**
     * Creates a new instance of DesignFile.
     * If p_is_webstart, the application was opened with Java Web Start.
     */
    public DesignFile(java.io.File p_design_file)
    {
        //this.file_chooser = p_file_chooser;
        this.input_file = p_design_file;
        this.output_file = p_design_file;
        if (p_design_file != null)
        {
            String file_name = p_design_file.getName();
            String[] name_parts = file_name.split("\\.");
            if (name_parts[name_parts.length - 1].compareToIgnoreCase(binary_file_extension) != 0)
            {
                String binfile_name = name_parts[0] + "." + binary_file_extension;
                this.output_file = new java.io.File(p_design_file.getParent(), binfile_name);
            }
        }
    }
    
    public DesignFile(String file_name, String p_design_text) {
        this.file_name = file_name;
        this.design_file_text = p_design_text;
    }
    


    /**
     * Gets an InputStream from the file. Returns null, if the algorithm failed.
     */
    public java.io.InputStream get_input_stream()
    {
        if (this.input_file==null) {
            
            if (this.design_file_text == null) {
                return null;
            }
            
            //return new java.io.StringBufferInputStream(this.design_file_text);
            try {
                byte[] bytes = this.design_file_text.getBytes("UTF-8");
                FRLogger.info("creating input stream with "+bytes.length+" bytes");
                return new java.io.ByteArrayInputStream(bytes);
            } catch (Exception e) {
                FRLogger.error(e.getLocalizedMessage(), e);
                return null;
            }
                
            
        }
            
        
        java.io.InputStream result;
        try {
           return new java.io.FileInputStream(this.input_file);
        } catch (Exception e) {
            FRLogger.error(e.getLocalizedMessage(), e);
        }
        return null;
    }

    /**
     * Gets the file name as a String. Returns null on failure.
     */
    public String get_name() {
        if (this.input_file != null)
        {
            return this.input_file.getName();
        }
        return this.file_name;
        
    }

    /**
     * Writes a Specctra Session File to update the design file in the host system.
     * Returns false, if the write failed
     */
    /*public boolean write_specctra_session_file(BoardFrame p_board_frame)
    {
        String design_file_name = this.get_name();
        String[] file_name_parts = design_file_name.split("\\.", 2);
        String design_name = file_name_parts[0];

        {
            String output_file_name = design_name + ".ses";
            FRLogger.info("Saving '"+output_file_name+"'...");
            java.io.File curr_output_file = new java.io.File(get_parent(), output_file_name);
            java.io.OutputStream output_stream;
            try
            {
                output_stream = new java.io.FileOutputStream(curr_output_file);
            } catch (Exception e)
            {
                output_stream = null;
            }

            if (p_board_frame.board_panel.board_handling.export_specctra_session_file(design_file_name, output_stream))
            {
                FRLogger.info("session file " + output_file_name + " written in Specctra format");
            }
            else
            {
                FRLogger.info("writing session file " + output_file_name + " failed");
                return false;
            }
        }
        if (true)
        {
            return write_rules_file(design_name, p_board_frame.board_panel.board_handling);
        }
        return true;
    }*/

    /**
     * Saves the board rule to file, so that they can be reused later on.
     */
    /*private boolean write_rules_file(String p_design_name, eu.mihosoft.freerouting.interactive.BoardHandling p_board_handling)
    {
        String rules_file_name = p_design_name + RULES_FILE_EXTENSION;
        java.io.OutputStream output_stream;

        FRLogger.info("Saving '"+rules_file_name+"'...");

        java.io.File rules_file = new java.io.File(this.get_parent(), rules_file_name);
        try
        {
            output_stream = new java.io.FileOutputStream(rules_file);
        } catch (java.io.IOException e)
        {
            FRLogger.error("unable to create rules file", e);
            return false;
        }

        RulesFile.write(p_board_handling, output_stream, p_design_name);
        return true;
    }*/

    public static boolean read_rules_file(String p_design_name, String p_parent_name, String rules_file_name,
                                          eu.mihosoft.freerouting.interactive.BoardHandling p_board_handling, boolean p_is_web_start, String p_confirm_message)
    {

        boolean result = true;
        boolean dsn_file_generated_by_host = p_board_handling.get_routing_board().communication.specctra_parser_info.dsn_file_generated_by_host;

        {
            try
            {
                java.io.File rules_file = p_parent_name == null ? new java.io.File(rules_file_name) : new java.io.File(p_parent_name, rules_file_name);
                FRLogger.info("Opening '"+rules_file_name+"'...");
                java.io.InputStream input_stream = new java.io.FileInputStream(rules_file);
                if (input_stream != null && dsn_file_generated_by_host)
                {
                    result = RulesFile.read(input_stream, p_design_name, p_board_handling);
                }
                else
                {
                    result = false;
                }
            } catch (java.io.FileNotFoundException e)
            {
                FRLogger.error("File '"+rules_file_name+"' was not found.", null);
                result = false;
            }
        }
        return result;
    }

    /**
     * Gets the binary file for saving or null, if the design file is not available
     * because the application is run with Java Web Start.
     */
    public java.io.File get_output_file()
    {
        return this.output_file;
    }

    public java.io.File get_input_file()
    {
        return this.input_file;
    }

    public String get_parent()
    {
        if (input_file != null)
        {
            return input_file.getParent();
        }
        return null;
    }

    public java.io.File get_parent_file()
    {
        if (input_file != null)
        {
            return input_file.getParentFile();
        }
        return null;
    }

    public boolean is_created_from_text_file()
    {
        if (design_file_text!=null) { return true; }
        return this.input_file != this.output_file;
    }

    /** Used, if the application is run without Java Web Start. */
    private java.io.File output_file;
    private java.io.File input_file;
    private static final String RULES_FILE_EXTENSION = ".rules";
    
    private String file_name;
    private String design_file_text;
    
}
