/** (C) Copyright 2011-2014 Chiral Behaviors, All Rights Reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
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

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

/**
 * @author Hal Hildebrand
 * 
 */
public interface ManagedProcess extends Serializable {

    /**
     * Returns the exit value for the subprocess.
     * 
     * @return the exit value of the subprocess represented by this
     *         <code>ManagedProcess</code> object. by convention, the value
     *         <code>0</code> indicates normal termination. If null, there is no
     *         return value yet.
     * @exception IllegalThreadStateException
     *                if the subprocess represented by this <code>Process</code>
     *                object has not yet terminated.
     */
    public Integer getExitValue();

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
     * @return the UUID that uniquely identifies this process
     */
    UUID getId();

    /**
     * @return the host OS pid for this process
     */
    Integer getPid();

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
     * Retrieve the tail of the Stderr stream on demand.
     *
     * @param numLines
     *            The number of lines to retrieve from the tail of the StdErr
     *            stream. Max = 4000
     * @return numLines worth of output from the tail of the StdErr stream.
     * @throws IOException
     */
    String getStdErrTail(int numLines) throws IOException;

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

    /**
     * Retrieve the tail of the Stdout stream on demand.
     *
     * @param numLines
     *            The number of lines to retrieve from the tail of the StdOut
     *            stream. Max = 4000
     * @return numLines worth of output from the tail of the StdOut stream.
     * @throws IOException
     */
    String getStdOutTail(int numLines) throws IOException;

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

    Tailer tailStdErr(TailerListener listener);

    /**
     * Add a listener to tail the STDERR stream
     * 
     * @param listener
     */
    Tailer tailStdErr(TailerListener listener, long delayMillis, boolean end,
                      boolean reOpen, int bufSize);

    Tailer tailStdOut(TailerListener listener);

    /**
     * Add a listener to tail the STDOUT stream
     * 
     * @param listener
     */
    Tailer tailStdOut(TailerListener listener, long delayMillis, boolean end,
                      boolean reOpen, int bufSize);

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
    int waitFor() throws InterruptedException;
}
