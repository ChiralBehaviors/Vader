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
package com.hellblazer.process.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

import com.hellblazer.process.CannotStopProcessException;
import com.hellblazer.process.ManagedProcess;
import com.hellblazer.utils.Utils;

/**
 * @author Hal Hildebrand
 * 
 */
abstract public class AbstractManagedProcess implements ManagedProcess,
        Cloneable {
    public static final String  CONTROL_DIR_PREFIX           = ".control-";
    public static final int     DEFAULT_KILL_TIMEOUT_SECONDS = 10;
    public static final int     DEFAULT_PAUSE_MILLIS         = 500;
    public static final int     MAX_TAIL_BUFFER_LINES        = 4000;
    private static final Logger log                          = Logger.getLogger(AbstractManagedProcess.class.getCanonicalName());

    private static final long   serialVersionUID             = 1L;

    public static UUID getIdFrom(File homeDirectory) {
        if (!homeDirectory.exists() || !homeDirectory.isDirectory()) {
            return null;
        }
        File[] contents = homeDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(CONTROL_DIR_PREFIX);
            }
        });
        if (contents == null || contents.length == 0) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("Home directory does not exist or does not contain a valid control directory: "
                         + homeDirectory.getAbsolutePath());
            }
            return null;
        }
        if (contents.length > 1) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("Home directory contains more than a single control directory: "
                         + homeDirectory.getAbsolutePath());
            }
            return null;
        }
        String uuidString = contents[0].getName().substring(CONTROL_DIR_PREFIX.length());
        if (uuidString.length() == 0) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("Home directory does not contain a valid control directory: "
                         + homeDirectory.getAbsolutePath());
            }
            return null;
        }
        return UUID.fromString(uuidString);
    }

    public static void initializeDirectory(File directory) throws IOException {
        remove(directory);
        if (!directory.mkdirs()) {
            throw new IOException("Cannot create directory: " + directory);
        }
    }

    public static void remove(File directory) throws IOException {
        Utils.clean(directory);
        if (directory.exists() && !directory.delete()) {
            throw new IOException(String.format("Cannot delete %s",
                                                directory.getAbsolutePath()));
        }
    }

    protected List<String>        command    = new ArrayList<String>();

    protected File                controlDirectory;

    protected File                directory;

    protected Map<String, String> environment;
    protected final UUID          id;
    protected volatile boolean    terminated = false;

    public AbstractManagedProcess() {
        this(UUID.randomUUID());
    }

    public AbstractManagedProcess(UUID id) {
        this.id = id;
    }

    abstract public void acquireFromHome(File homeDirectory);

    @Override
    public void addCommand(String piece) {
        if (command == null) {
            command = new ArrayList<String>();
        }
        command.add(piece);
    }

    @Override
    public AbstractManagedProcess clone() {
        AbstractManagedProcess clone;
        try {
            clone = getClass().newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException("cannot create instance", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                                            "cannot create instance due to access restrictions",
                                            e);
        }
        clone.command = command;
        clone.directory = directory;
        if (environment != null) {
            clone.environment = new HashMap<String, String>();
            clone.environment.putAll(environment);
        }
        return clone;
    }

    @Override
    public ManagedProcess configureFrom(ManagedProcess process) {
        command = process.getCommand();
        environment = process.getEnvironment();
        directory = process.getDirectory();
        return this;
    }

    @Override
    public synchronized void destroy() throws IOException {
        stop();
        remove(directory);

    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractManagedProcess other = (AbstractManagedProcess) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public List<String> getCommand() {
        return command;
    }

    @Override
    public File getDirectory() {
        return directory;
    }

    @Override
    public Map<String, String> getEnvironment() {
        return environment;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public InputStream getStdErr() {
        try {
            return new FileInputStream(getStdErrFile());
        } catch (FileNotFoundException e) {
            throw new IllegalThreadStateException(
                                                  "Process has not been started");
        }
    }

    @Override
    public String getStdErrTail(int numLines) throws IOException {
        if (!getStdErrFile().exists()) {
            throw new IllegalThreadStateException(
                                                  "Process has not been started or has already exited");
        }
        List<String> lines = new ArrayList<>();
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(
                                                                          getStdErrFile())) {
            int linesRead = 0;
            String line;
            while (((line = reader.readLine()) != null)
                   && (linesRead++ < numLines)) {
                lines.add(line);
            }
        }
        Collections.reverse(lines);
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line);
            builder.append('\n');
        }
        return builder.toString();
    }

    @Override
    public OutputStream getStdIn() {
        try {
            return new FileOutputStream(getStdInFile(), true);
        } catch (FileNotFoundException e) {
            throw new IllegalThreadStateException(
                                                  "Process has not been started or has already exited");
        }
    }

    @Override
    public InputStream getStdOut() {
        try {
            return new FileInputStream(getStdOutFile());
        } catch (FileNotFoundException e) {
            throw new IllegalThreadStateException(
                                                  "Process has not been started");
        }
    }

    @Override
    public String getStdOutTail(int numLines) throws IOException {
        if (!getStdOutFile().exists()) {
            throw new IllegalThreadStateException(
                                                  "Process has not been started or has already exited");
        }
        List<String> lines = new ArrayList<>();
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(
                                                                          getStdOutFile())) {
            int linesRead = 0;
            String line;
            while (((line = reader.readLine()) != null)
                   && (linesRead++ < numLines)) {
                lines.add(line);
            }
        }
        Collections.reverse(lines);
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line);
            builder.append('\n');
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (id == null ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean isSameConfiguration(ManagedProcess other) {
        if (command == null) {
            if (other.getCommand() != null) {
                return false;
            }
        } else if (!command.equals(other.getCommand())) {
            return false;
        }
        if (directory == null) {
            if (other.getDirectory() != null) {
                return false;
            }
        } else if (!directory.equals(other.getDirectory())) {
            return false;
        }
        if (environment == null) {
            if (other.getEnvironment() != null) {
                return false;
            }
        } else if (!environment.equals(other.getEnvironment())) {
            return false;
        }
        return true;
    }

    @Override
    public synchronized void restart() throws IOException {
        restart(DEFAULT_KILL_TIMEOUT_SECONDS);
    }

    @Override
    public synchronized void restart(int waitForSeconds) throws IOException {
        stop();
        start();
    }

    @Override
    public void setCommand(List<String> command) {
        if (command == null) {
            command = new ArrayList<String>();
        }
        this.command = command;
    }

    @Override
    public void setCommand(String[] command) {
        if (command == null) {
            command = new String[0];
        }
        ArrayList<String> commandList = new ArrayList<String>();
        for (String part : command) {
            commandList.add(part);
        }
        setCommand(commandList);
    }

    @Override
    public void setDirectory(File directory) {
        this.directory = directory;
    }

    @Override
    public void setDirectory(String directory) {
        if (directory == null) {
            setDirectory((File) null);
        }
        setDirectory(new File(directory));
    }

    @Override
    public void setEnvironment(Map<String, String> environment) {
        if (environment == null) {
            environment = new HashMap<String, String>();
        }
        this.environment = environment;
    }

    @Override
    public synchronized void start() throws IOException {

        if (isActive()) {
            return;
        }
        terminated = false;

        if (command == null || command.size() == 0) {
            command = new ArrayList<String>();
            return;
        }

        if (directory == null) {
            throw new IllegalStateException(
                                            "Process home directory must not be null");
        }

        initializeDirectory(new File(directory, CONTROL_DIR_PREFIX + id));

        // Create initial STD IN file
        File stdInFile = getStdInFile();
        FileOutputStream stdIn = new FileOutputStream(stdInFile);
        stdIn.close();

        if (log.isLoggable(Level.FINE)) {
            log.fine("[" + id + "] executing: " + command + " dir: "
                     + directory + " env: " + environment);
        }

        execute();

        // On Windows platforms, the stdout and stderr files might not be
        // established yet, so poll
        int counter = 0;
        while (!getStdErrFile().exists() || !getStdOutFile().exists()) {

            try {
                Thread.sleep(10);

                if (counter++ > 150) {
                    throw new IOException("Process did not start up correctly");
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    @Override
    public synchronized void stop() throws CannotStopProcessException {
        stop(DEFAULT_KILL_TIMEOUT_SECONDS);
    }

    /* (non-Javadoc)
     * @see com.hellblazer.process.ManagedProcess#tailStdErr(org.apache.commons.io.input.TailerListener)
     */
    @Override
    public Tailer tailStdErr(TailerListener listener) {
        return Tailer.create(getStdErrFile(), listener);
    }

    /* (non-Javadoc)
     * @see com.hellblazer.process.ManagedProcess#tailStdErr(org.apache.commons.io.input.TailerListener)
     */
    @Override
    public Tailer tailStdErr(TailerListener listener, long delayMillis,
                             boolean end, boolean reOpen, int bufSize) {
        return Tailer.create(getStdErrFile(), listener, delayMillis, end,
                             reOpen, bufSize);
    }

    /* (non-Javadoc)
     * @see com.hellblazer.process.ManagedProcess#tailStdOut(org.apache.commons.io.input.TailerListener)
     */
    @Override
    public Tailer tailStdOut(TailerListener listener) {
        return Tailer.create(getStdOutFile(), listener);
    }

    /* (non-Javadoc)
     * @see com.hellblazer.process.ManagedProcess#tailStdOut(org.apache.commons.io.input.TailerListener)
     */
    @Override
    public Tailer tailStdOut(TailerListener listener, long delayMillis,
                             boolean end, boolean reOpen, int bufSize) {
        return Tailer.create(getStdOutFile(), listener, delayMillis, end,
                             reOpen, bufSize);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        String name = getClass().getCanonicalName();
        name = name.substring(name.lastIndexOf('.') + 1);
        buf.append(name);
        buf.append("{").append(getId()).append("} ");
        buf.append(" home dir: ");
        buf.append(directory);
        buf.append(" pid: ");
        buf.append(getPid());
        return buf.toString();
    }

    /**
     * Execute the command of the receiver process. Control will not return
     * until the command list execution has finished.
     * 
     * Default is to simply execute the command list of the receiver.
     * 
     * @throws IOException
     *             if anything goes wrong during the execution
     */

    protected void execute() throws IOException {
        primitiveExecute(command);
    }

    protected String getControlDirectoryFileName() {
        return CONTROL_DIR_PREFIX + id;
    }

    protected File getStdErrFile() {
        return new File(directory, getStdErrFileName());
    }

    protected String getStdErrFileName() {
        return inControlDirectory("std.err");
    }

    protected File getStdInFile() {
        return new File(directory, getStdInFileName());
    }

    protected String getStdInFileName() {
        return inControlDirectory("std.in");
    }

    protected File getStdOutFile() {
        return new File(directory, getStdOutFileName());
    }

    protected String getStdOutFileName() {
        return inControlDirectory("std.out");
    }

    protected String inControlDirectory(String fileName) {
        return getControlDirectoryFileName() + File.separatorChar + fileName;
    }

    /**
     * The actual execution process. Control will not return until the command
     * list execution has finished.
     * 
     * @param commands
     *            - the command list to execute
     * 
     * @throws IOException
     *             - if anything goes wrong during the execution.
     */
    protected void primitiveExecute(List<String> commands) throws IOException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(directory);
        if (environment != null) {
            builder.environment().putAll(environment);
        }
        builder.command(commands);
        builder.redirectErrorStream(true); // combine OUT and ERR into one
        // stream
        Process p = builder.start();
        final BufferedReader shellReader = new BufferedReader(
                                                              new InputStreamReader(
                                                                                    p.getInputStream()));
        Runnable reader = new Runnable() {
            @Override
            public void run() {
                String line;
                try {
                    line = shellReader.readLine();
                } catch (IOException e) {
                    if (!"Stream closed".equals(e.getMessage())
                        && !e.getMessage().contains("Bad file descriptor")) {
                        log.log(Level.SEVERE, "Failed reading process output",
                                e);
                    }
                    return;
                }
                while (line != null) {
                    if (log.isLoggable(Level.FINE) && line != null) {
                        log.fine("[" + id + "] " + line);
                    }
                    try {
                        line = shellReader.readLine();
                    } catch (IOException e) {
                        if (!"Stream closed".equals(e.getMessage())) {
                            log.log(Level.SEVERE,
                                    "Failed reading process output", e);
                        }
                        return;
                    }
                }
            }
        };

        Thread readerThread = new Thread(reader, "Process reader for: "
                                                 + getCommand());
        readerThread.setDaemon(true);
        readerThread.start();

        try {
            p.waitFor();
        } catch (InterruptedException e) {
            return;
        } finally {
            readerThread.interrupt();
            p.destroy();
        }
    }
}
