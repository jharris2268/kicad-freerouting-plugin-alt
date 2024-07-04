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
 * MainApplication.java
 *
 * Created on 19. Oktober 2002, 17:58
 *
 */
package eu.mihosoft.freerouting.gui;

import eu.mihosoft.freerouting.board.TestLevel;
import eu.mihosoft.freerouting.constants.Constants;
import eu.mihosoft.freerouting.interactive.InteractiveActionThread;
import eu.mihosoft.freerouting.interactive.ThreadActionListener;
import eu.mihosoft.freerouting.logger.FRLogger;
import eu.mihosoft.freerouting.logger.MessageServer;

import eu.mihosoft.freerouting.interactive.BoardHandling;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONStringer;

/**
 *
 * Main application for creating frames with new or existing board designs.
 *
 * @author Alfons Wirtz
 */
public class MainApplication
{
    /**
     * Main function of the Application
     * @param args
     */
    public static void main(String[] args)
    {
        FRLogger.traceEntry("MainApplication.main()");
    
        StartupOptions startupOptions = StartupOptions.parse(args);
        
        int ret = run_main(startupOptions);
        
        if (FRLogger.use_message_server) {
            FRLogger.plop(MessageServer.stats());
        }
        FRLogger.traceExit("MainApplication.main()");
        if (FRLogger.use_message_server) {
            try {
                MessageServer.getInstance().send_finished();
            } catch (IOException ex) {
                FRLogger.plop("MessageServer.getInstance().send_finished() failed: "+ex);
            }
        }
        System.exit(ret);
        
    }
    
    private static int run_main(StartupOptions startupOptions) {        

        FRLogger.info(startupOptions.testVersion());

        FRLogger.info("freerouting_cli application starting...");

        Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());

        if (startupOptions.single_design_option || true)
        {
            BoardFrame.Option board_option;
            if (startupOptions.session_file_option) {
                board_option = BoardFrame.Option.SESSION_FILE;
            } else {
                board_option = BoardFrame.Option.SINGLE_FRAME;
            }
            
            DesignFile design_file = null;
            if (startupOptions.design_input_filename != null) {
                FRLogger.info("Opening '"+startupOptions.design_input_filename+"'...");
                design_file = new DesignFile(new java.io.File(startupOptions.design_input_filename));
                if (design_file == null) {
                    FRLogger.warn("File with name " +  startupOptions.design_input_filename + " not found");
                    return 1;
                }
            } else if (startupOptions.design_input_stdin) {
                try {
                    BufferedReader input_stream = new BufferedReader(new InputStreamReader(System.in));
                    
                    String design_file_text="";
                    String line;
                    try {
                        while ((line = input_stream.readLine()) != null) {
                            design_file_text += line;
                        }
                    } catch (IOException e) {
                        FRLogger.warn("reading from stdin failed  "+e);
                    } finally {
                        if (input_stream!=null) {
                            input_stream.close();
                        }
                    }
                    FRLogger.plop("read "+design_file_text.length()+" bytes from stdin");
                
                    design_file = new DesignFile("autoroute.dsn", design_file_text);
                } catch (Exception e) {
                    FRLogger.warn("design file input from stdin failed  "+e);
                    return 1;
                }
            } else {
                if (!startupOptions.use_message_server) {
                    FRLogger.warn("no input file specified");
                    return 1;
                }
                try {
                    FRLogger.info("requesting design_file_text");
                    JSONStringer request_obj = MessageServer.getInstance().start_message("request");
                    request_obj.key("request_type").value("design_file_text");
                    request_obj.key("wait_reply").value(true);
                    request_obj.endObject();
                    
                    JSONObject reply_obj = MessageServer.getInstance().send_json_expect_json_reply(request_obj);
                    if (!reply_obj.has("design_file_text")) {
                        FRLogger.warn("no design file text received");
                        return 1;
                    }
                    
                    String design_file_text = reply_obj.getString("design_file_text");
                    String file_name = reply_obj.getString("file_name");
                    design_file = new DesignFile(file_name, design_file_text);
                } catch (Exception e) {
                    FRLogger.warn("design file request failed "+e);
                }
                
            }
            FRLogger.info("Loading design " + design_file.get_name());
            
            final BoardFrame new_frame =
                        create_board_frame(design_file, board_option,
                                startupOptions.test_version_option, 
                                startupOptions.design_rules_filename);
            
       	    if (new_frame == null) {
                FRLogger.warn("Couldn't create window frame");
                
                return 1;
            }
            new_frame.board_panel.board_handling.settings.check_continue = startupOptions.check_continue;
            
            new_frame.board_panel.board_handling.settings.autoroute_settings.set_stop_pass_no(startupOptions.max_autoroute_passes);

            if (startupOptions.pre_route_fanout) {
                new_frame.board_panel.board_handling.settings.autoroute_settings.set_with_fanout(true);
            }

            new_frame.board_panel.board_handling.settings.autoroute_settings.set_with_postroute(startupOptions.max_postroute_passes);

            if (startupOptions.max_autoroute_passes <= 99999)
            {
                InteractiveActionThread thread;
                thread = new_frame.board_panel.board_handling.start_batch_autorouter();
                thread.addListener(new ThreadActionListener() {
                    @Override
                    public void autorouterStarted() {
                        FRLogger.plop("autorouterStarted");
                    }

                    @Override
                    public void autorouterAborted() {
                        FRLogger.plop("autorouterAborted");
                        ExportBoardToFile(startupOptions.design_output_filename);
                    }

                    @Override
                    public void autorouterFinished() {
                        FRLogger.plop("autorouterFinished");
                        ExportBoardToFile(startupOptions.design_output_filename);
                    }

                    private void ExportBoardToFile(String filename) {
                        if ((filename != null)
                                && ((filename.toLowerCase().endsWith(".dsn"))
                                || (filename.toLowerCase().endsWith(".ses"))
                                || (filename.toLowerCase().endsWith(".scr")))) {

                            FRLogger.info("Saving '" + filename + "'...");
                            try {
                                String filename_only = new File(filename).getName();
                                String design_name = filename_only.substring(0, filename_only.length() - 4);

                                java.io.OutputStream output_stream = new java.io.FileOutputStream(filename);

                                if (filename.toLowerCase().endsWith(".dsn")) {
                                    new_frame.board_panel.board_handling.export_to_dsn_file(output_stream, design_name, false);
                                } else if (filename.toLowerCase().endsWith(".ses")) {
                                    new_frame.board_panel.board_handling.export_specctra_session_file(design_name, output_stream);
                                } else if (filename.toLowerCase().endsWith(".scr")) {
					// ditched script export
                                }

                                //Runtime.getRuntime().exit(0);
                            } catch (Exception e) {
                                FRLogger.error("Couldn't export board to file", e);
                            }
                        } else {
                            FRLogger.warn("Couldn't export board to '" + filename + "'.");
                        }
                    }
                });
                try {
                    FRLogger.plop("call thread join");
                    thread.join();
                    FRLogger.plop("thread join finished");
                } catch (InterruptedException ex) {
                    FRLogger.plop("BatchAutorouterThread exception: "+ex);
                }
            }
        }
        return 0;
        
    }

    /**
     * Creates new form MainApplication
     * It takes the directory of the board designs as optional argument.
     * @param startupOptions
     */
    public MainApplication(StartupOptions startupOptions)
    {
        this.design_dir_name = startupOptions.getDesignDir();
        this.is_test_version = startupOptions.isTestVersion();
    }



    /**
     * Creates a new board frame containing the data of the input design file.
     * Returns null, if an error occurred.
     */
    static private BoardFrame create_board_frame(DesignFile p_design_file, 
            BoardFrame.Option p_option, boolean p_is_test_version, String p_design_rules_file)
    {

        java.io.InputStream input_stream = p_design_file.get_input_stream();
        if (input_stream == null)
        {
	    FRLogger.error("Error: null design file input", null);
            return null;
        }

        TestLevel test_level;
        if (p_is_test_version)
        {
            test_level = TestLevel.TEST_VERSION;
        }
        else
        {
            test_level = TestLevel.RELEASE_VERSION;
        }
        BoardFrame new_frame = new BoardFrame(p_design_file, p_option, test_level, !p_is_test_version);
	///boolean read_ok = new_frame.read(input_stream, p_design_file.is_created_from_text_file(), p_message_field);
        FRLogger.plop("new_frame created..");
    
    
        boolean read_ok = new_frame.read(input_stream, p_design_file.is_created_from_text_file());
        FRLogger.plop("read_ok="+read_ok);
        if (!read_ok)
        {
            return null;
        }
        if (p_design_file.is_created_from_text_file())
        {
            // Read the file  with the saved rules, if it is existing.

            String file_name = p_design_file.get_name();
            String[] name_parts = file_name.split("\\.");

            String design_name = name_parts[0];

            String parent_folder_name = null;
            String rules_file_name = null;
            String confirm_import_rules_message = null;
            if (p_design_rules_file == null) {
                parent_folder_name = p_design_file.get_parent();
                rules_file_name = design_name + ".rules";
                confirm_import_rules_message = "Please confirm importing stored rules\nBeware that rules imported from the host system may be overwritte";
            } else {
                rules_file_name = p_design_rules_file;
            }

            DesignFile.read_rules_file(design_name, parent_folder_name, rules_file_name,
                    new_frame.board_panel.board_handling, p_option == BoardFrame.Option.WEBSTART,
                    confirm_import_rules_message);
        }
        return new_frame;
    }

    /** The list of open board frames */
    private final java.util.Collection<BoardFrame> board_frames 
            = new java.util.LinkedList<>();
    private String design_dir_name = null;
    private final boolean is_test_version;

    static final String WEB_FILE_BASE_NAME = "http://www.freerouting.mihosoft.eu";

    static final String VERSION_NUMBER_STRING = 
        "v" + Constants.FREEROUTING_VERSION
            + " (build-date: "
            + Constants.FREEROUTING_BUILD_DATE +")";
}
