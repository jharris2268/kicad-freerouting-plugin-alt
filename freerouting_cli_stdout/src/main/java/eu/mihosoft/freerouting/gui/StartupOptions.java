/*
 *
 *   Copyright (C) 2014 Andrey Belomutskiy
 *
 *   Copyright (C) 2021 Erich S. Heinzle
 *   Website http://www.repo.hu/projects/freerouting_cli/
 *
 */

package eu.mihosoft.freerouting.gui;

import eu.mihosoft.freerouting.logger.FRLogger;

/**
 * Andrey Belomutskiy
 * 6/28/2014
 */
public class StartupOptions {
    boolean single_design_option = false;
    boolean test_version_option = false;
    boolean session_file_option = false;
    boolean pre_route_fanout = false;
    String design_input_filename = null;
    String design_output_filename = null;
    String design_rules_filename = null;
    String design_input_directory_name = null;
    int max_autoroute_passes = 99999;
    int max_postroute_passes = 0;
    int trace_level = 0;
    boolean use_message_server = false;
    boolean check_continue=false;
    
    
    private StartupOptions() {
    }

    public static StartupOptions parse(String[] p_args) {
        StartupOptions result = new StartupOptions();
        result.process(p_args);
        return result;
    }

    private void process(String[] p_args) {
        for (int i = 0; i < p_args.length; ++i) {
            try {
                if (p_args[i].startsWith("-de")) {
                    // the design file is provided
                    if (p_args.length > i + 1 && !p_args[i + 1].startsWith("-")) {
                        single_design_option = true;
                        design_input_filename = p_args[i + 1];
                    }
                } else if (p_args[i].startsWith("-di")) {
                    // the design directory is provided
                    if (p_args.length > i + 1 && !p_args[i + 1].startsWith("-")) {
                        design_input_directory_name = p_args[i + 1];
                    }
                } else if (p_args[i].startsWith("-do")) {
                    if (p_args.length > i + 1 && !p_args[i + 1].startsWith("-")) {
                        design_output_filename = p_args[i + 1];
                    }
                } else if (p_args[i].startsWith("-dr")) {
                    if (p_args.length > i + 1 && !p_args[i + 1].startsWith("-")) {
                        design_rules_filename = p_args[i + 1];
                    }
                } else if (p_args[i].startsWith("-ap")) {
                    if (p_args.length > i + 1 && !p_args[i + 1].startsWith("-")) {
                        max_autoroute_passes = Integer.decode(p_args[i + 1]);
                        if (max_autoroute_passes < 2) {
                            FRLogger.warn("Maximum autoroute passes should be greater than 1");
                        }	    
                    }
                } else if (p_args[i].startsWith("-pp")) {
                    if (p_args.length > i + 1 && !p_args[i + 1].startsWith("-")) {
                        max_postroute_passes = Integer.decode(p_args[i + 1]);
                    }
                } else if (p_args[i].startsWith("-fo")) {
                    pre_route_fanout = true;
                } else if (p_args[i].startsWith("-s")) {
                    session_file_option = true;
                } else if (p_args[i].startsWith("-test")) {
                    test_version_option = true;
                } else if (p_args[i].startsWith("-trace")) {
                    if (p_args.length > i + 1 && !p_args[i + 1].startsWith("-")) {
                        trace_level = Integer.decode(p_args[i + 1]);
                    }
                } else if (p_args[i].startsWith("-ms")) {
                    use_message_server=true;
                    FRLogger.setUseMessageServer(true);
                
                } else if (p_args[i].startsWith("-cc")) {
                    check_continue=true;
                }
            }
            catch (Exception e)
            {
                FRLogger.error("There was a problem parsing the '"+p_args[i]+"' parameter", e);
            }
        }
        if (trace_level == 0) {
            FRLogger.setTraceLevelNone();
        } else if (trace_level == 1) {
            FRLogger.setTraceLevelCritical();
        } else if (trace_level > 1) {
            FRLogger.setTraceLevelAll();
        }
    }

    public String testVersion()
    {
	if (test_version_option) {
            return "testing version: on";
        } 
        return "testing version: off";
    }

    public boolean isTestVersion() {
        return test_version_option;
    }

    public String getDesignDir() {
        return design_input_directory_name;
    }
}
