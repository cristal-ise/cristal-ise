/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.kernel.utils;

import java.io.PrintStream;
import java.sql.Timestamp;

import org.cristalise.kernel.process.AbstractMain;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.ScriptConsole;
import org.cristalise.kernel.utils.server.SimpleTCPIPServer;

import lombok.extern.slf4j.Slf4j;

/**
 * Old fashioned Logger utility class designed before the well know logging frameworks of java were available. Internally uses SLF4J
 * api.
 *
 * <pre>
 * - message string should always contain the class name and the method name: Logger.msg(1,"ItemFact::createDir() - LifeCycle DB created");
 * - use meaningful abbreviation and also use the dash to separate the 'header' from the message!
 * - each method should start with this 'method signature' debug: Logger.msg(1,"ItemFact::createDir() - path:" + path);
 * </pre>
 * @deprecated Replaced by @SLF4J lombok annotations
 */
@Slf4j
@Deprecated
public class Logger {
    /**
     * logging level 0 (only error & warning) => no logging ; 9 => maximum logging add ten to output time before each message
     */
    private static int                 mLogLevel = 0;
    static protected SimpleTCPIPServer mConsole  = null;

    /**
     * Prints the log message to the configured list of log streams. Uses SLF4J api and map the logLevel like this:
     * <pre>
     * if logLevel <= 2 : INFO
     * if logLevel >= 8 : TRACE
     * else : DEBUG
     * </pre>
     *
     * @param message - the string to write to the log. It can also use String.format() syntax
     * @param msgLogLevel  - log level of this message. If the current log level was set less that this number,
     *                       the log message will not be displayed
     * @param args - Arguments referenced by the format specifiers in the message string
     */
    static private void printMessage(String message, int msgLogLevel, Object...args) {
        if (msgLogLevel <= 2)      log.info (replaceMsgPlaceholders(message), args);
        else if (msgLogLevel >= 8) log.trace(replaceMsgPlaceholders(message), args);
        else                       log.debug(replaceMsgPlaceholders(message), args);
    }

    /**
     * Check whether the given logLevel would produce a log entry or not
     *
     * @param logLevel the level to be checked
     * @return true of the logLevel is smaller then or equal to the configured level
     */
    static public boolean doLog(int logLevel) {
        if (logLevel > 9) logLevel -= 10;
        return mLogLevel >= logLevel;
    }

    /**
     * Use this only for temporary messages while developing/debugging. When the code is stable, change calls to debug to
     * message/warning/error with an appropriate log level. Is is marked deprecated to highlight stray calls. This makes it easier to manage
     * debug calls in the source.
     *
     * @param msg - the string to write to the console, or log file if specified in cmd line
     * @deprecated use debug method with level parameter
     */
    @Deprecated
    static public void debug(String msg) {
        log.debug(msg);
    }

    /**
     * Report information that will be useful for debugging. Uses SLF4J string substitution syntax.
     *
     * @param level - log level of this message. If the current log level was set less that this number,
     *                the log message will not be displayed
     * @param msg - the string to write to the log. It can also use String.format() syntax
     * @param args - Arguments referenced by the format specifiers in the msg string
     */
    static public void debug(int level, String msg, Object...args) {
        log.debug(replaceMsgPlaceholders(msg), args);
    }

    /**
     * Report information that will be useful for debugging. Uses SLF4J string substitution syntax.
     *
     * @param level - log level of this message. If the current log level was set less that this number,
     *                the log message will not be displayed
     * @param msg - the string to write to the log. It can also use String.format() syntax
     * @param args - Arguments referenced by the format specifiers in the msg string
     */
    static public void msg(int level, String msg, Object...args) {
        printMessage(msg, level, args);
    }

    /**
     * Report information that is important to log all the time (uses log level 0). Uses SLF4J string substitution syntax.
     *
     * @param msg - the string to write to the log. It can also use String.format() syntax
     * @param args - Arguments referenced by the format specifiers in the msg string
     */
    static public void msg(String msg, Object...args) {
        printMessage(msg, 0, args);
    }

    /**
     * Report error (uses log level 0). Uses SLF4J string substitution syntax.
     *
     * @param msg - the string to write to the log. It can also use String.format() syntax
     * @param args - Arguments referenced by the format specifiers in the msg string
     */
    static public void error(String msg, Object...args) {
        log.error(replaceMsgPlaceholders(msg), args);
    }

    private static String replaceMsgPlaceholders(String msg) {
        if (msg.contains("%s")) msg = msg.replaceAll("%s", "{}");
        if (msg.contains("%d")) msg = msg.replaceAll("%d", "{}");

        return msg;
    }

    /**
     * Report exception
     *
     * @param ex the Throwable to be logged
     */
    static public void error(Throwable ex) {
        log.error("", ex);
    }

    /**
     * Report warning (uses log level 0). Uses SLF4J string substitution syntax.
     *
     * @param msg - the string to write to the log. It can also use String.format() syntax
     * @param args - Arguments referenced by the format specifiers in the msg string
     */
    static public void warning(String msg, Object...args) {
        log.warn(replaceMsgPlaceholders(msg), args);
    }

    /**
     * Report FATAL error and call the shutdown hook of the process. Uses SLF4J string substitution syntax.
     *
     * @param msg - the string to write to the log. It can also use String.format() syntax
     * @param args - Arguments referenced by the format specifiers in the msg string
     */
    static public void die(String msg, Object...args) {
        log.error(replaceMsgPlaceholders(msg), args);
        AbstractMain.shutdown(1);
    }

    /**
     * Add a new log stream
     *
     * @param console the PrintStream to be used as console
     * @param logLevel the log level to be used for this stream
     */
    public static void addLogStream(PrintStream console, int logLevel) {
        log.info("***********************************************************");
        log.info("  CRISTAL log started at level " + logLevel + " @" + new Timestamp(System.currentTimeMillis()).toString());
        log.info("***********************************************************");

        log.warn("LogStreams are unsupported");

        mLogLevel = logLevel > 9 ? logLevel - 10 : logLevel;
    }

    /**
     * Remove exeisting log stream
     *
     * @param console the PrintStream to be used as console
     */
    public static void removeLogStream(PrintStream console) {
        log.warn("LogStreams are unsupported");
    }

    static public int initConsole(String id) {
        int port = Gateway.getProperties().getInt(id + ".Console.port", 0);

        if (port == 0) Logger.msg("No port defined for " + id + " console. Using any port.");

        mConsole = new SimpleTCPIPServer(port, ScriptConsole.class, 5);
        mConsole.startListening();
        Gateway.getProperties().setProperty(id + ".Console.port", String.valueOf(mConsole.getPort()));
        return mConsole.getPort();
    }

    static public int getConsolePort() {
        if (mConsole != null) return mConsole.getPort();
        else                  return -1;
    }

    static public void closeConsole() {
        if (mConsole != null) {
            mConsole.stopListening();
            mConsole = null;
        }
    }

    public static void removeAll() {
        mLogLevel = 0;
    }
}
