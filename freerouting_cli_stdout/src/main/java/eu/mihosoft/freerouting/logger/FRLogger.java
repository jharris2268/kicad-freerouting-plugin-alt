/*
 *   Copyright (C) 2014  Alfons Wirtz
 *   website www.freerouting.net
 *
 *   Copyright (C) 2017 Michael Hoffer <info@michaelhoffer.de>
 *   Website www.freerouting.mihosoft.eu
 *
 *   Copyright (C) 2021 Erich S. Heinzle
 *   Website http://www.repo.hu/projects/freerouting_cli/
 */

package eu.mihosoft.freerouting.logger;

//import eu.mihosoft.freerouting.FreeRouting;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

import eu.mihosoft.freerouting.logger.MessageServer;


public class FRLogger {
    //private static Logger logger = LogManager.getLogger(FreeRouting.class);
    
    public static boolean use_message_server = false;
    
    private static DecimalFormat performanceFormat = new DecimalFormat("0.00");

    private static HashMap<Integer, Instant> perfData = new HashMap<Integer, Instant>();

    private static TraceLevel trace_level = TraceLevel.NO_DEBUGGING_OUTPUT;

    public static void send_message(String type, String msg) {
        //System.out.println(type+": "+msg);
        if (use_message_server) {
        
            try {
                MessageServer.getInstance().send_message(type,msg, type=="plop" || type=="error");
            } catch (Exception ex) {
                System.err.println("MessageServer failed "+ex);
                System.err.println(type+": "+msg);
                System.exit(1);
            }
        } else {
            System.out.println("--FRCLI--"+type.toUpperCase()+"--"+msg);
        }
    }
            


    public static void traceEntry(String perfId)
    {
        perfData.put(perfId.hashCode(), java.time.Instant.now());
    }

    public static void traceExit(String perfId)
    {
        traceExit(perfId, null);
    }

    public static void traceExit(String perfId, Object result)
    {
        long timeElapsed = Duration.between(perfData.get(perfId.hashCode()), java.time.Instant.now()).toMillis();

        perfData.remove(perfId.hashCode());
        if (timeElapsed < 0) {
            timeElapsed = 0;
        }
        //System.out.println("--FRCLI--TRACE--Method '" + perfId.replace("{}", result != null ? result.toString() : "(null)") + "' was performed in " + performanceFormat.format(timeElapsed/1000.0) + " seconds.");
        send_message("trace", "Method '" + perfId.replace("{}", result != null ? result.toString() : "(null)") + "' was performed in " + performanceFormat.format(timeElapsed/1000.0) + " seconds.");
    }

    public static void plop(String msg)
    {
        //System.out.println("--FRCLI--PLOP--" + msg);
        send_message("plop",msg);
    }
    public static void info(String msg)
    {
        //System.out.println("--FRCLI--INFO--" + msg);
        send_message("info",msg);
    }

    public static void warn(String msg)
    {
        //System.out.println("--FRCLI--WARN--" + msg);
        send_message("warn",msg);
    }

    public static void debug(String msg)
    {
        if (trace_level != TraceLevel.NO_DEBUGGING_OUTPUT)
        {
            //System.out.println("--FRCLI--DEBUG--" + msg);
            send_message("debug",msg);
        }
    }

    public static void validate(String msg)
    {
        if (trace_level != TraceLevel.NO_DEBUGGING_OUTPUT)
        {
            //System.out.println("--FRCLI--VALIDATE--" + msg);
            send_message("validate",msg);
        }
    }

    public static void progress(String msg, int count, int total)
    {
        //System.out.println("--FRCLI--PROGRESS--" + msg + ": " + count + "/" + total);
        send_message("progress",msg+": "+count + "/" + total);
    }
    public static void error(String msg, Throwable t)
    {
        if (t == null)
        {
            //System.out.println("--FRCLI--ERROR--" + msg);
            send_message("error",msg);
        } else
        {
            //System.out.println("--FRCLI--ERROR--" + msg + " : " + t);
            send_message("error",msg+" : " + t);
        }
    }

    public static void setTraceLevelNone()
    {
        trace_level = TraceLevel.NO_DEBUGGING_OUTPUT;
        info("FRLogger verbosity: NO_DEBUGGING_OUTPUT");
    }
    
    public static void setTraceLevelCritical()
    {
        trace_level = TraceLevel.CRITICAL_DEBUGGING_OUTPUT;
        info("FRLogger verbosity: CRITICAL_DEBUGGING_OUTPUT");
    }

    public static void setTraceLevelAll()
    {
        trace_level = TraceLevel.ALL_DEBUGGING_OUTPUT;
        info("FRLogger verbosity: ALL_DEBUGGING_OUTPUT");
    }

    public static boolean allDebuggingOutput()
    {
        return trace_level == TraceLevel.ALL_DEBUGGING_OUTPUT;
    }

    private static enum TraceLevel
    {
        NO_DEBUGGING_OUTPUT, CRITICAL_DEBUGGING_OUTPUT, ALL_DEBUGGING_OUTPUT
    }

    public static void setUseMessageServer(boolean _use_message_server)
    {
        use_message_server = _use_message_server;
        plop("use_message_server = "+use_message_server);
    }
    
    
}
