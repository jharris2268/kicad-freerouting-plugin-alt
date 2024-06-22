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
 * BatchAutorouterThread.java
 *
 * Created on 25. April 2006, 07:58
 *
 */
package eu.mihosoft.freerouting.interactive;

import eu.mihosoft.freerouting.geometry.planar.FloatPoint;
import eu.mihosoft.freerouting.geometry.planar.FloatLine;

import eu.mihosoft.freerouting.board.Unit;

import eu.mihosoft.freerouting.autoroute.BatchAutorouter;
import eu.mihosoft.freerouting.autoroute.BatchFanout;
import eu.mihosoft.freerouting.autoroute.BatchOptRoute;
import eu.mihosoft.freerouting.logger.FRLogger;

/**
 * GUI interactive thread for the batch autorouter.
 *
 * @author Alfons Wirtz
 */
public class BatchAutorouterThread extends InteractiveActionThread
{

    /** Creates a new instance of BatchAutorouterThread */
    protected BatchAutorouterThread(BoardHandling p_board_handling)
    {
        super(p_board_handling);
        AutorouteSettings autoroute_settings = p_board_handling.get_settings().autoroute_settings;
        this.batch_autorouter = new BatchAutorouter(this, !autoroute_settings.get_with_fanout(), true, autoroute_settings.get_start_ripup_costs(), true);
        this.batch_opt_route = new BatchOptRoute(this);

    }

    protected void thread_action()
    {
        for (ThreadActionListener hl : this.listeners)
            hl.autorouterStarted();

        FRLogger.traceEntry("BatchAutorouterThread.thread_action()");

        try
        {
            String start_message = "Batch Autorouter running";
            FRLogger.info(start_message);
            if (hdlg.get_settings().autoroute_settings.get_with_fanout())
            {
                FRLogger.info("Fanout first: True");
                BatchFanout.fanout_board(this);
            }
            else
            {
                FRLogger.info("Fanout first: False");
            }
            boolean autoroute_completed = true;
            if (hdlg.get_settings().autoroute_settings.get_with_autoroute()) {
                FRLogger.info("Perform autoroute: True");
                autoroute_completed = batch_autorouter.autoroute_passes();
            } else {
                FRLogger.info("Perform autoroute: False");
            }
            hdlg.get_routing_board().finish_autoroute();
	    // this seems to add some time consuming post route optimisations
            if (autoroute_completed && hdlg.get_settings().autoroute_settings.get_with_postroute())
            {
                FRLogger.info("Perform postroute optimisation: True");
                String opt_message = "Batch Optimizer runnning";
                FRLogger.info(opt_message);
                this.batch_opt_route.optimize_board();
                String curr_message;
                if (this.is_stop_requested())
                {
                    curr_message = "interrupted";
                }
                else
                {
                    curr_message = "completed";
                }
                String end_message = "Postroute " + curr_message;
                FRLogger.info(end_message);
            }
            else
            {
        //        hdlg.screen_messages.clear();
                FRLogger.info("Perform postroute optimisation: False");
                String curr_message;
                if (this.is_stop_requested())
                {
                    curr_message = "interrupted";
                }
                else
                {
                    curr_message = "completed";
                }
                Integer incomplete_count = hdlg.get_ratsnest().incomplete_count();
                String end_message = "Autoroute " + curr_message + ", " + incomplete_count.toString() + " connections not found";
                FRLogger.info(end_message);
            }

            if (hdlg.get_routing_board().rules.get_trace_angle_restriction() == eu.mihosoft.freerouting.board.AngleRestriction.FORTYFIVE_DEGREE && hdlg.get_routing_board().get_test_level() != eu.mihosoft.freerouting.board.TestLevel.RELEASE_VERSION)
            {
                eu.mihosoft.freerouting.tests.Validate.multiple_of_45_degree("after eu.mihosoft.freerouting.autoroute: ", hdlg.get_routing_board());
            }
        } catch (Exception e)
        {
            FRLogger.error(e.getLocalizedMessage(),e);
        }

        FRLogger.traceExit("BatchAutorouterThread.thread_action()");

        for (ThreadActionListener hl : this.listeners)
        {
        //    if (this.is_stop_requested()) {
        //        hl.autorouterAborted();
        //    }
        //    else {
                hl.autorouterFinished();
        //    }
        }
    }

    private final BatchAutorouter batch_autorouter;
    private final BatchOptRoute batch_opt_route;
}
