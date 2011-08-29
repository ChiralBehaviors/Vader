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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;

import sun.management.ConnectorAddressLink;

import com.hellblazer.process.CannotStopProcessException;
import com.hellblazer.process.JavaProcess;
import com.hellblazer.process.ManagedProcess;
import com.hellblazer.process.NoLocalJmxConnectionException;

/**
 * @author Hal Hildebrand
 * 
 */
public class JavaProcessImpl implements JavaProcess, Cloneable {
    private static final long        serialVersionUID = 1L;
    protected List<String>           arguments;
    protected File                   jarFile;
    protected String                 javaClass;
    protected File                   javaExecutable;
    protected transient JMXConnector jmxc;
    protected ManagedProcess         process;
    protected List<String>           vmOptions;

    public JavaProcessImpl(ManagedProcess process) {
        assert process != null;
        this.process = process;
    }

    /* (non-Javadoc)
     * @see com.hellblazer.process.JavaProcess#addArgument(java.lang.String)
     */
    @Override
    public void addArgument(String argument) {
        if (arguments == null) {
            arguments = new ArrayList<String>();
        }
        arguments.add(argument);
    }

    /* (non-Javadoc)
     * @see com.hellblazer.process.JavaProcess#addArguments(java.lang.String[])
     */
    @Override
    public void addArguments(String[] args) {
        if (arguments == null) {
            arguments = new ArrayList<String>();
        }
        for (String arg : args) {
            arguments.add(arg);
        }
    }

    @Override
    public void addCommand(String command) {
        throw new UnsupportedOperationException(
                                                "Cannot set the command of the process directly");
    }

    /* (non-Javadoc)
     * @see com.hellblazer.process.JavaProcess#addVmOption(java.lang.String)
     */
    @Override
    public void addVmOption(String vmOption) {
        if (vmOptions == null) {
            vmOptions = new ArrayList<String>();
        }
        vmOptions.add(vmOption);
    }

    /* (non-Javadoc)
     * @see com.hellblazer.process.JavaProcess#addVmOptions(java.lang.String[])
     */
    @Override
    public void addVmOptions(String[] vmOpts) {
        if (vmOptions == null) {
            vmOptions = new ArrayList<String>();
        }
        for (String opt : vmOpts) {
            vmOptions.add(opt);
        }
    }

    @Override
    public JavaProcess clone() {
        JavaProcessImpl clone;
        try {
            clone = (JavaProcessImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Clone not supported", e);
        }
        clone.process = process.clone();
        if (arguments != null) {
            clone.arguments = new ArrayList<String>(arguments);
        }
        if (vmOptions != null) {
            clone.vmOptions = new ArrayList<String>(vmOptions);
        }
        clone.jarFile = jarFile;
        clone.javaClass = javaClass;
        return clone;
    }

    @Override
    public JavaProcess configureFrom(ManagedProcess p) {
        if (!(p instanceof JavaProcess)) {
            throw new UnsupportedOperationException(
                                                    "Can only configure from an instance of JavaProcess");
        }
        JavaProcess javaProcess = (JavaProcess) p;
        javaExecutable = javaProcess.getJavaExecutable();
        arguments = javaProcess.getArguments();
        vmOptions = javaProcess.getVmOptions();
        javaClass = javaProcess.getJavaClass();
        jarFile = javaProcess.getJarFile();
        process.setDirectory(javaProcess.getDirectory());
        process.setEnvironment(javaProcess.getEnvironment());
        return this;
    }

    @Override
    public void destroy() throws CannotStopProcessException, IOException {
        process.destroy();
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
        final JavaProcessImpl other = (JavaProcessImpl) obj;
        if (process == null) {
            if (other.process != null) {
                return false;
            }
        } else if (!process.equals(other.process)) {
            return false;
        }
        return true;
    }

    /**
     * @return the List of arguments to the Java program
     */
    @Override
    public List<String> getArguments() {
        return new ArrayList<String>(arguments);
    }

    /**
     * @return the List which represents the command to execute this Java
     *         process
     */
    @Override
    public List<String> getCommand() {
        ArrayList<String> command = new ArrayList<String>();
        if (javaExecutable != null) {
            command.add(javaExecutable.getAbsolutePath());
        }
        if (vmOptions != null) {
            command.addAll(vmOptions);
        }
        command.addAll(getExecution());
        if (arguments != null) {
            command.addAll(arguments);
        }
        return command;
    }

    /**
     * @return the home directory of the process execution
     */
    @Override
    public File getDirectory() {
        return process.getDirectory();
    }

    @Override
    public Map<String, String> getEnvironment() {
        return process.getEnvironment();
    }

    /**
     * @return the List which represents the arguments to the VM invocation to
     *         run the Java program
     */
    protected List<String> getExecution() {
        assert !(jarFile != null && javaClass != null); // can't execute jar and java class simultaneously
        ArrayList<String> execution = new ArrayList<String>();
        if (jarFile != null) {
            execution.add("-jar");
            execution.add(jarFile.getAbsolutePath());
        } else {
            execution.add(javaClass);
        }
        return execution;
    }

    @Override
    public int getExitValue() {
        return process.getExitValue();
    }

    /**
     * Answer the unique id of this java process
     */
    @Override
    public UUID getId() {
        return process.getId();
    }

    @Override
    public File getJarFile() {
        return jarFile;
    }

    @Override
    public String getJavaClass() {
        return javaClass;
    }

    /**
     * @return the File which points to the Java executable
     */
    @Override
    public File getJavaExecutable() {
        return javaExecutable;
    }

    /**
     * @throws ConnectException
     */
    @Override
    public JMXConnector getLocalJmxConnector() throws ConnectException,
                                              NoLocalJmxConnectionException {
        if (jmxc != null) {
            return jmxc;
        }

        if (!process.isActive()) {
            throw new ConnectException(
                                       "Cannot establish local JMX connection as process is not active: "
                                               + this);
        }

        String address;
        try {
            address = ConnectorAddressLink.importFrom(process.getPid());
        } catch (IOException e) {
            ConnectException cex = new ConnectException(
                                                        "Cannot obtain local JMX connector address of: "
                                                                + this);
            cex.initCause(e);
            throw cex;
        }

        if (address == null) {
            throw new NoLocalJmxConnectionException(
                                                    "Local JMX connector address does not exist for: "
                                                            + this);
        }

        JMXServiceURL jmxUrl;
        try {
            jmxUrl = new JMXServiceURL(address);
        } catch (MalformedURLException e) {
            ConnectException cex = new ConnectException(
                                                        "Invalid local JMX URL for "
                                                                + this + " : "
                                                                + address);
            cex.initCause(e);
            throw cex;
        }

        try {
            jmxc = JMXConnectorFactory.connect(jmxUrl);
        } catch (java.rmi.ConnectException e) {
            if (e.getMessage().startsWith("Connection refused")) {
                throw new NoLocalJmxConnectionException(
                                                        "Local JMX connector address does not exist for: "
                                                                + this);
            }
            ConnectException cex = new ConnectException(
                                                        "Underlying RMI communications exception");
            cex.initCause(e);
            throw cex;
        } catch (IOException e) {
            ConnectException cex = new ConnectException(
                                                        "Cannot establish local JMX connection to: "
                                                                + this);
            cex.initCause(e);
            throw cex;
        }

        try {
            jmxc.connect();
        } catch (IOException e) {
            ConnectException cex = new ConnectException(
                                                        "Cannot establish local JMX connection to: "
                                                                + this);
            cex.initCause(e);
            throw cex;
        }

        return jmxc;
    }

    @Override
    public MBeanServerConnection getLocalMBeanServerConnection()
                                                                throws ConnectException,
                                                                NoLocalJmxConnectionException {
        JMXConnector connector = getLocalJmxConnector();

        try {
            return connector.getMBeanServerConnection();
        } catch (IOException e) {
            ConnectException cex = new ConnectException(
                                                        "Cannot establish local JMX connection to: "
                                                                + this);
            cex.initCause(e);
            throw cex;
        }
    }

    @Override
    public MBeanServerConnection getLocalMBeanServerConnection(Subject delegationSubject)
                                                                                         throws ConnectException,
                                                                                         NoLocalJmxConnectionException {
        JMXConnector connector = getLocalJmxConnector();

        try {
            return connector.getMBeanServerConnection(delegationSubject);
        } catch (IOException e) {
            ConnectException cex = new ConnectException(
                                                        "Cannot establish local JMX connection to: "
                                                                + this);
            cex.initCause(e);
            throw cex;
        }
    }

    @Override
    public int getPid() {
        return process.getPid();
    }

    @Override
    public InputStream getStdErr() {
        return process.getStdErr();
    }

    @Override
    public OutputStream getStdIn() {
        return process.getStdIn();
    }

    @Override
    public InputStream getStdOut() {
        return process.getStdOut();
    }

    /**
     * @return the List of arguments to the Java virtual machine
     */
    @Override
    public List<String> getVmOptions() {
        return new ArrayList<String>(vmOptions);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (process == null ? 0 : process.hashCode());
        return result;
    }

    /**
     * @return true if the Java process is active
     */
    @Override
    public boolean isActive() {
        return process.isActive();
    }

    @Override
    public boolean isSameConfiguration(ManagedProcess otherProcess) {
        if (!(otherProcess instanceof JavaProcess)) {
            return false;
        }

        JavaProcess other = (JavaProcess) otherProcess;
        if (arguments == null) {
            if (other.getArguments() != null) {
                return false;
            }
        } else if (!arguments.equals(other.getArguments())) {
            return false;
        }
        if (jarFile == null) {
            if (other.getJarFile() != null) {
                return false;
            }
        } else if (!jarFile.equals(other.getJarFile())) {
            return false;
        }
        if (javaClass == null) {
            if (other.getJavaClass() != null) {
                return false;
            }
        } else if (!javaClass.equals(other.getJavaClass())) {
            return false;
        }
        if (javaExecutable == null) {
            if (other.getJavaExecutable() != null) {
                return false;
            }
        } else if (!javaExecutable.equals(other.getJavaExecutable())) {
            return false;
        }
        if (vmOptions == null) {
            if (other.getVmOptions() != null) {
                return false;
            }
        } else if (!vmOptions.equals(other.getVmOptions())) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see com.hellblazer.process.ManagedProcess#restart()
     */
    @Override
    public void restart() throws IOException {
        process.restart();
    }

    /* (non-Javadoc)
     * @see com.hellblazer.process.ManagedProcess#restart(int)
     */
    @Override
    public void restart(int waitForSeconds) throws IOException {
        process.restart(waitForSeconds);
    }

    @Override
    public void setArguments(List<String> arguments) {
        if (arguments == null) {
            arguments = new ArrayList<String>();
        }
        this.arguments = new ArrayList<String>(arguments);
    }

    @Override
    public void setArguments(String[] arguments) {
        if (arguments == null) {
            arguments = new String[] {};
        }
        ArrayList<String> args = new ArrayList<String>();
        for (String argument : arguments) {
            args.add(argument);
        }
        setArguments(args);
    }

    @Override
    public void setCommand(List<String> commands) {
        throw new UnsupportedOperationException(
                                                "Cannot set the command of the process directly");
    }

    @Override
    public void setCommand(String[] commands) {
        throw new UnsupportedOperationException(
                                                "Cannot set the command of the process directly");
    }

    @Override
    public void setDirectory(File directory) {
        process.setDirectory(directory);
    }

    @Override
    public void setDirectory(String directory) {
        process.setDirectory(directory);
    }

    @Override
    public void setEnvironment(Map<String, String> environment) {
        process.setEnvironment(environment);
    }

    @Override
    public void setJarFile(File jarFile) {
        if (jarFile != null) {
            javaClass = null; // exclusive or
        }
        this.jarFile = jarFile;
    }

    @Override
    public void setJarFile(String jarFile) {
        if (jarFile == null) {
            this.jarFile = null;
            return;
        }
        setJarFile(new File(jarFile));
    }

    @Override
    public void setJavaClass(String javaClass) {
        if (javaClass != null) {
            jarFile = null; // exclusive or
        }
        this.javaClass = javaClass;
    }

    @Override
    public void setJavaExecutable(File javaExecutable) {
        this.javaExecutable = javaExecutable;
    }

    @Override
    public void setJavaExecutable(String javaExecutable) {
        if (javaExecutable == null) {
            this.javaExecutable = null;
            return;
        }
        setJavaExecutable(new File(javaExecutable));
    }

    @Override
    public void setVmOptions(List<String> vmOptions) {
        if (vmOptions == null) {
            vmOptions = new ArrayList<String>();
        }
        this.vmOptions = new ArrayList<String>(vmOptions);
    }

    @Override
    public void setVmOptions(String[] vmOptions) {
        if (vmOptions == null) {
            vmOptions = new String[] {};
        }
        ArrayList<String> options = new ArrayList<String>();
        for (String option : vmOptions) {
            options.add(option);
        }
        setVmOptions(options);
    }

    /**
     * Start the Java process
     * 
     * @throws IOException
     *             - if anything goes awry in starting up the process
     */
    @Override
    public synchronized void start() throws IOException {
        assert javaExecutable != null;
        if (vmOptions == null) {
            vmOptions = new ArrayList<String>();
        }
        if (arguments == null) {
            arguments = new ArrayList<String>();
        }
        process.setCommand(getCommand());
        process.start();
    }

    /**
     * Stop the execution of the Java process.
     * 
     * @throws CannotStopProcessException
     */
    @Override
    public synchronized void stop() throws CannotStopProcessException {
        process.stop();
    }

    @Override
    public synchronized void stop(int waitForSeconds)
                                                     throws CannotStopProcessException {
        process.stop(waitForSeconds);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("JavaProcess");
        buf.append("{").append(getId()).append("} ");
        buf.append(" home dir: ");
        buf.append(getDirectory());
        buf.append(" pid: ");
        buf.append(getPid());
        return buf.toString();
    }

    @Override
    public int waitFor() throws InterruptedException {
        return process.waitFor();
    }

}
