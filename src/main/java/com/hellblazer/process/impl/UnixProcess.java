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
package com.hellblazer.process.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hellblazer.process.CannotStopProcessException;

/**
 * @author Hal Hildebrand
 * 
 */
public class UnixProcess extends AbstractManagedProcess {
    private static String[]     activeStates     = new String[] { "U", "I",
            "R", "S"                            };
    private static final Logger log              = Logger.getLogger(UnixProcess.class.getCanonicalName());
    private static final long   serialVersionUID = 1L;

    protected int               exitValue        = -1;
    protected int               pid              = -1;
    protected int               wrapperPid       = -1;

    public UnixProcess() {
        super();
    }

    public UnixProcess(UUID id) {
        super(id);
    }

    @Override
    public void acquireFromHome(File homeDirectory) {
        setDirectory(homeDirectory);
        try {
            wrapperPid = readPid(getWrapperPidFile());
            pid = readPid(getPidFile());
        } catch (IllegalStateException e) {
            // process not started
        }
    }

    @Override
    protected void execute() throws IOException {
        writeScript();
        List<String> scriptCmnds = new ArrayList<String>();
        scriptCmnds.add("/bin/sh");
        scriptCmnds.add(getScriptFile().getAbsolutePath());
        primitiveExecute(scriptCmnds);
    }

    /**
     * @return the array of strings which represent active process state
     */
    protected String[] getActiveStates() {
        return activeStates;
    }

    @Override
    public int getExitValue() {
        if (pid == -1 || terminated) {
            return exitValue;
        }
        File exitValueFile = getExitValueFile();

        if (log.isLoggable(Level.FINE)) {
            log.fine("looking for exit value file: "
                     + exitValueFile.getAbsolutePath());
        }
        FileInputStream is = null;
        try {
            is = new FileInputStream(exitValueFile);
        } catch (FileNotFoundException e1) {
            throw new IllegalThreadStateException("Process has not terminated");
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = null;
        try {
            line = reader.readLine();
            exitValue = Integer.parseInt(line);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Unable to parse exit value: {"
                                            + line + "} of process [" + id
                                            + "]");
        } catch (IOException e) {
            throw new IllegalStateException(
                                            "Unable to retrieve exit value of process ["
                                                    + id + "]");
        }
        try {
            reader.close();
        } catch (IOException e) {
            // ignore
        }
        return exitValue;
    }

    protected File getExitValueFile() {
        return new File(directory, getExitValueFileName());
    }

    protected String getExitValueFileName() {
        return inControlDirectory("exit.value");
    }

    @Override
    public int getPid() {
        return pid;
    }

    protected File getPidFile() {
        return new File(directory, getPidFileName());
    }

    protected String getPidFileName() {
        return inControlDirectory("pid");
    }

    /**
     * @return
     */
    protected String getProcessStatus(int thePid) {
        if (thePid == -1) {
            return null;
        }
        ProcessBuilder ps = new ProcessBuilder();
        ps.command(new String[] { "ps", "-o", "state", "-p",
                String.valueOf(thePid) });
        ps.redirectErrorStream(true);

        if (log.isLoggable(Level.FINEST)) {
            log.finest("requesting process status: " + ps.command());
        }
        Process psProc;
        try {
            psProc = ps.start();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start ps -o state-p "
                                            + thePid, e);
        }

        BufferedReader reader = new BufferedReader(
                                                   new InputStreamReader(
                                                                         psProc.getInputStream()));

        int status;
        try {
            status = psProc.waitFor();
        } catch (InterruptedException e) {
            return "";
        }

        String line;
        try {
            line = reader.readLine();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to parse status for pid="
                                            + thePid, e);
        }

        if (status == 1) {
            return null; // process does not exist
        }

        if (status != 0) {
            log.severe("Retrieval of status for pid=" + thePid
                       + " failed with status code " + status);
            logStatusErrorOutput(reader, line);
            throw new IllegalStateException("Retrieval of status for pid="
                                            + thePid
                                            + " failed with status code "
                                            + status);
        }

        if (line == null) {
            throw new IllegalStateException("Unable to parse status for pid="
                                            + thePid + ", no output!");
        }

        if ("STAT".equals(line)) {
            log.fine("ignoring 'STAT' header");
            try {
                line = reader.readLine();
            } catch (IOException e) {
                throw new IllegalStateException(
                                                "Unable to parse status for pid="
                                                        + thePid, e);
            }
        }
        if (log.isLoggable(Level.FINEST)) {
            log.finest("pid=" + pid + " status: " + line);
        }
        return line;
    }

    protected File getScriptFile() {
        return new File(directory, getScriptFileName());
    }

    protected String getScriptFileName() {
        return inControlDirectory("run.sh");
    }

    protected File getWrapperPidFile() {
        return new File(directory, getWrapperPidFileName());
    }

    protected String getWrapperPidFileName() {
        return inControlDirectory("wrapper.pid");
    }

    @Override
    public boolean isActive() {
        return isActive(pid);
    }

    public boolean isActive(int thePid) {
        if (thePid == -1) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("inactive, pid == -1 for " + this);
            }
            return false;
        }
        String status = getProcessStatus(thePid);
        if (status == null) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("inactive, no status for " + this);
            }
            return false;
        }
        for (String active : getActiveStates()) {
            if (status.startsWith(active)) {
                return true;
            }
        }
        if (log.isLoggable(Level.FINE)) {
            log.fine("inactive, status is " + status + " for " + this);
        }
        return false;
    }

    /**
     * @return true if the process is well and truly dead
     */
    protected boolean isDead() {
        if (terminated || getProcessStatus(pid) == null) {
            terminated = true;
        }
        return terminated;
    }

    protected boolean isProcessDead(int thePid) {
        String line = getProcessStatus(thePid);

        if (line == null) {
            return true;
        }

        return false;
    }

    protected void kill() {
        if (pid == -1) {
            return;
        }
        ProcessBuilder kill = new ProcessBuilder();
        kill.command(new String[] { "kill", String.valueOf(pid) });
        kill.redirectErrorStream(true);
        Process killProc;
        try {
            killProc = kill.start();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start kill pid=" + pid,
                                            e);
        }

        try {
            killProc.waitFor();
        } catch (InterruptedException e) {
            return;
        }
    }

    protected void kill(int signal) {
        if (pid == -1) {
            return;
        }
        ProcessBuilder kill = new ProcessBuilder();
        kill.command(new String[] { "kill", "-" + signal, String.valueOf(pid) });
        kill.redirectErrorStream(true);
        Process killProc;
        try {
            killProc = kill.start();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start kill -" + signal
                                            + " -p" + pid, e);
        }

        try {
            killProc.waitFor();
        } catch (InterruptedException e) {
            return;
        }
    }

    /**
     * @param reader
     * @param line
     */
    private void logStatusErrorOutput(BufferedReader reader, String line) {
        if (log.isLoggable(Level.FINE)) {
            StringBuffer out = new StringBuffer(60);
            String errLine = line;
            while (errLine != null) {
                out.append(errLine);
                out.append("\n");
                try {
                    errLine = reader.readLine();
                } catch (IOException e) {
                    out.append("*** Unable to retrieve further output: "
                               + e.getMessage());
                    break;
                }
            }
            log.fine("ps error output: \n" + out.toString());
        }
    }

    protected int readPid(File pidFile) {
        for (int i = 0; i < 1000; i++) {
            if (pidFile.exists() && pidFile.length() > 0) {
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                return -1;
            }
        }
        if (!pidFile.exists()) {
            throw new IllegalStateException("Required PID file is missing! <"
                                            + pidFile + ">");
        }
        try {
            BufferedReader pidStream = new BufferedReader(
                                                          new InputStreamReader(
                                                                                new FileInputStream(
                                                                                                    pidFile)));
            String pidNum = pidStream.readLine();
            if (pidNum == null) {
                throw new IllegalStateException("pid is empty <" + pidFile
                                                + ">");
            }
            int thePid = Integer.parseInt(pidNum);
            pidStream.close();
            return thePid;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read PID file <"
                                            + pidFile + ">");
        }
    }

    @Override
    public synchronized void start() throws IOException {
        if (isActive()) {
            return;
        }
        super.start();
        wrapperPid = readPid(getWrapperPidFile());
        pid = readPid(getPidFile());
        if (log.isLoggable(Level.FINE)) {
            log.fine("started [" + id + "] pid=" + pid);
        }
    }

    /* (non-Javadoc)
     * @see com.hellblazer.process.ManagedProcess#stop()
     */
    @Override
    public synchronized void stop(int waitForSeconds)
                                                     throws CannotStopProcessException {
        if (isDead()) {
            return;
        }
        if (log.isLoggable(Level.FINE)) {
            log.fine("stopping: " + this);
        }

        // Be nice about it.
        kill();

        long target = System.currentTimeMillis() + waitForSeconds * 1000;
        while (System.currentTimeMillis() < target && !isDead()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return;
            }
        }

        if (!isDead()) {
            log.info("Cannot kill:   PID=" + pid + " " + command
                     + " resorting to kill -9");
            // Okay, then.  Terminate with extreme prejudice
            kill(9);
        }

        if (!isDead()) {
            throw new CannotStopProcessException("Cannot stop process.  PID="
                                                 + pid + " " + command);
        }

    }

    @Override
    public int waitFor() throws InterruptedException {
        if (terminated) {
            return getExitValue();
        }
        waitFor(wrapperPid);
        return getExitValue();
    }

    protected void waitFor(int thePid) throws InterruptedException {
        while (isActive(thePid)) {
            Thread.sleep(10);
        }
        for (int i = 0; i < 10; i++) {
            if (!getExitValueFile().exists()) {
                Thread.sleep(10);
            }
        }
    }

    /**
     * Output the scripts which will actually run the process in the background,
     * capturing outptut streams, PID and exit value.
     * 
     * Script is of the form:
     * 
     * #!/bin/sh exec 1> .control-905eda8c-e0cf-40c7-9169-fbe16c601ae7/std.out
     * exec 2> .control-905eda8c-e0cf-40c7-9169-fbe16c601ae7/std.err (nohup
     * {quoted command} < {ctrl-dir}/std.in & x=$!; echo $x > {ctrl-dir}/pid;
     * wait $x; echo $? > {ctrl-dir}/exit.value)& echo $! >
     * {ctrl-dir}/wrapper.pid
     * 
     */
    protected void writeScript() throws IOException {
        PrintWriter script = new PrintWriter(
                                             new OutputStreamWriter(
                                                                    new FileOutputStream(
                                                                                         getScriptFile())));

        script.println("#!/bin/sh");

        script.append("exec 1> ");
        script.append(getStdOutFileName());
        script.println();

        script.append("exec 2> ");
        script.append(getStdErrFileName());
        script.println();

        script.append('(');
        script.append("nohup ");
        for (String part : command) {
            script.append('"').append(part).append('"');
            script.append(' ');
        }
        script.append(" < ");
        script.append(getStdInFileName());
        script.append(" & x=$!; echo $x > ");
        script.append(getPidFileName());
        script.append("; wait $x; echo $? > ");
        script.append(getExitValueFileName());
        script.println(")&");

        script.append("echo $! > ");
        script.println(getWrapperPidFileName());

        script.flush();
        script.close();
    }

}
