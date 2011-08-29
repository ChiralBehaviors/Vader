/** 
 * (C) Copyright 2011 Hal Hildebrand, all rights reserved.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.hellblazer.process;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Hal Hildebrand
 * 
 */
public interface ManagedProcess extends Serializable {

    void addCommand(String command);

    /**
     * @return a clone of the receiver. The clone will have a new ID
     */
    ManagedProcess clone();

    /**
     * Configure the receiver from the process' configuration.
     * 
     * @param process
     */
    ManagedProcess configureFrom(ManagedProcess process);

    /**
     * Stop the process and delete the home directory
     * 
     * @throws CannotStopProcessException
     *             - if the process cannot be stopped
     * @throws IOException
     *             - if there is an issue deleting the directory
     */
    void destroy() throws CannotStopProcessException, IOException;

    @Override
    boolean equals(Object other);

    /**
     * @return the List representing the command which the proces executes
     */
    List<String> getCommand();

    /**
     * @return the File which represents the home directory of the process
     *         execution
     */
    File getDirectory();

    /**
     * @return the Map which represents the environment the processs executes
     */
    Map<String, String> getEnvironment();

    /**
     * Returns the exit value for the subprocess.
     * 
     * @return the exit value of the subprocess represented by this
     *         <code>ManagedProcess</code> object. by convention, the value
     *         <code>0</code> indicates normal termination.
     * @exception IllegalThreadStateException
     *                if the subprocess represented by this <code>Process</code>
     *                object has not yet terminated.
     */
    public int getExitValue();

    /**
     * @return the UUID that uniquely identifies this process
     */
    UUID getId();

    /**
     * @return the host OS pid for this process
     */
    int getPid();

    /**
     * @return the InputStream of the process' STD ERR stream (i.e. file handle
     *         2) The stream obtains data piped from the error output stream of
     *         the process.
     * @exception IllegalThreadStateException
     *                if the subprocess represented by this
     *                <code>ManagedProcess</code> object has not yet started.
     */
    InputStream getStdErr();

    /**
     * @return the OutputStream of the process' STD IN stream Output to the
     *         stream is piped into the standard input stream of the process
     * @exception IllegalThreadStateException
     *                if the subprocess represented by this
     *                <code>ManagedProcess</code> object has not yet started.
     */
    OutputStream getStdIn();

    /**
     * @return the InputStream of the process' STD OUT stream (i.e. file handle
     *         1) The stream obtains data piped from the output stream of the
     *         process.
     * @exception IllegalThreadStateException
     *                if the subprocess represented by this
     *                <code>ManagedProcess</code> object has not yet started.
     */
    InputStream getStdOut();

    @Override
    int hashCode();

    /**
     * @return true if the process is active
     */
    boolean isActive();

    /**
     * @return true if the other ManagedProcess is identically configured as the
     *         receiver.
     */
    boolean isSameConfiguration(ManagedProcess other);

    /**
     * Restart the receiver.
     * 
     * @throws IOException
     *             if there is an error in starting up
     * @throws CannotStopProcessException
     *             if there is an error stopping the process
     */
    void restart() throws IOException, CannotStopProcessException;

    /**
     * 
     * Restart the receiver.
     * 
     * @param waitForSeconds
     *            - seconds to wait for the process to stop
     * @throws IOException
     *             if there is an error in starting up
     * @throws CannotStopProcessException
     *             if there is an error stopping the process
     */
    void restart(int waitForSeconds) throws IOException;

    void setCommand(List<String> commands);

    void setCommand(String[] commands);

    void setDirectory(File directory);

    void setDirectory(String directory);

    void setEnvironment(Map<String, String> environment);

    /**
     * Start the process
     * 
     * @throws IOException
     *             - if anything goes awry during startup
     */
    void start() throws IOException;

    /**
     * Stop the process
     * 
     * @throws CannotStopProcessException
     *             - if the process cannot be stopped
     */
    void stop() throws CannotStopProcessException;

    /**
     * Stop the process
     * 
     * @param waitForSeconds
     *            - seconds to wait for the process to stop
     * @throws CannotStopProcessException
     *             - if the process cannot be stopped
     */
    void stop(int waitForSeconds) throws CannotStopProcessException;

    /**
     * causes the current thread to wait, if necessary, until the process
     * represented by this <code>Process</code> object has terminated. This
     * method returns immediately if the subprocess has already terminated. If
     * the subprocess has not yet terminated, the calling thread will be blocked
     * until the subprocess exits.
     * 
     * @return the exit value of the process. By convention, <code>0</code>
     *         indicates normal termination.
     * @exception InterruptedException
     *                if the current thread is {@link Thread#interrupt()
     *                interrupted} by another thread while it is waiting, then
     *                the wait is ended and an {@link InterruptedException} is
     *                thrown.
     */
    public int waitFor() throws InterruptedException;
}
