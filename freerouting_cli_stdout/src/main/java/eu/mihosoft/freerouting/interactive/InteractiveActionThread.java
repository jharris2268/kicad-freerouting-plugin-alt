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
 * InteractiveActionThread.java
 *
 * Created on 2. Maerz 2006, 07:23
 *
 */
package eu.mihosoft.freerouting.interactive;

import eu.mihosoft.freerouting.logger.FRLogger;

import java.util.ArrayList;
import java.util.List;
import java.lang.ref.WeakReference;

/**
 * Used for running an interactive action in a separate thread,
 * that can be stopped by the user.
 *
 * @author Alfons Wirtz
 */
public abstract class InteractiveActionThread extends Thread implements eu.mihosoft.freerouting.datastructures.Stoppable
{
    protected List<ThreadActionListener> listeners = new ArrayList<ThreadActionListener>();

    public void addListener(ThreadActionListener toAdd) {
        listeners.add(toAdd);
    }

    public static InteractiveActionThread get_autoroute_instance(BoardHandling p_board_handling)
    {
        FRLogger.plop("InteractiveActionThread get_autoroute_instance");
        return new AutorouteThread(p_board_handling);
    }

    public static InteractiveActionThread get_batch_autorouter_instance(BoardHandling p_board_handling)
    {
        FRLogger.plop("InteractiveActionThread get_batch_autorouter_instance");
        
        return new BatchAutorouterThread(p_board_handling);
    }

    public static InteractiveActionThread get_fanout_instance(BoardHandling p_board_handling)
    {
        FRLogger.plop("InteractiveActionThread get_fanout_instance");
        return new FanoutThread(p_board_handling);
    }

    public static InteractiveActionThread get_pull_tight_instance(BoardHandling p_board_handling)
    {
        FRLogger.plop("InteractiveActionThread get_pull_tight_instance");
        return new PullTightThread(p_board_handling);
    }

    /** Creates a new instance of InteractiveActionThread */
    protected InteractiveActionThread(BoardHandling p_board_handling)
    {
        this.hdlg = p_board_handling;
        FRLogger.set_interactive_action_thread(this);
    }

    protected abstract void thread_action();

    public void run()
    {
        thread_action();
//        hdlg.repaint();
    }

    public synchronized void request_stop()
    {
        stop_requested = true;
    }

    public synchronized boolean is_stop_requested()
    {
        return stop_requested;
    }

    private boolean stop_requested = false;
    public final BoardHandling hdlg;

    private static class AutorouteThread extends InteractiveActionThread
    {

        private AutorouteThread(BoardHandling p_board_handling)
        {
            super(p_board_handling);
            
        }

        protected void thread_action()
        {
           // if (!(hdlg.interactive_state instanceof SelectedItemState))
           // {
           //     return;
           // }
      //      InteractiveState return_state = ((SelectedItemState) hdlg.interactive_state).autoroute(this);
  //          hdlg.set_interactive_state(return_state);
        }
    }

    private static class FanoutThread extends InteractiveActionThread
    {

        private FanoutThread(BoardHandling p_board_handling)
        {
            super(p_board_handling);
        }

        protected void thread_action()
        {
        //    if (!(hdlg.interactive_state instanceof SelectedItemState))
          //  {
            //    return;
           // }
    //        InteractiveState return_state = ((SelectedItemState) hdlg.interactive_state).fanout(this);
    //        hdlg.set_interactive_state(return_state);
        }
    }

    private static class PullTightThread extends InteractiveActionThread
    {

        private PullTightThread(BoardHandling p_board_handling)
        {
            super(p_board_handling);
        }

        protected void thread_action()
        {
//            if (!(hdlg.interactive_state instanceof SelectedItemState))
  //          {
    //            return;
      //      }
         //   InteractiveState return_state = ((SelectedItemState) hdlg.interactive_state).pull_tight(this);
        //    hdlg.set_interactive_state(return_state);
        }
    }

}

