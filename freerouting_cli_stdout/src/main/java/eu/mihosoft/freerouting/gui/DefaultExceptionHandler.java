/*
 *   Copyright (C) 2014 Andrey Belomutsky
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
public class DefaultExceptionHandler implements Thread.UncaughtExceptionHandler {
    public void uncaughtException(Thread t, Throwable e) {
        handleException(e);
    }

    public static void handleException(Throwable e) {
        // Here you should have a more robust, permanent record of problems
        FRLogger.error(e.getLocalizedMessage(), e);
    }

}
