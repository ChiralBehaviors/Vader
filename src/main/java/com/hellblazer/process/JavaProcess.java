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
import java.net.ConnectException;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.security.auth.Subject;

/**
 * @author Hal Hildebrand
 * 
 */
public interface JavaProcess extends ManagedProcess {

    void addArgument(String argument);

    void addArguments(String[] arguments);

    void addVmOption(String vmOption);

    void addVmOptions(String[] vmOptions);

    /**
     * @return the List of arguments to the Java program
     */
    List<String> getArguments();

    File getJarFile();

    String getJavaClass();

    /**
     * @return the File which points to the Java executable
     */
    File getJavaExecutable();

    JMXConnector getLocalJmxConnector() throws ConnectException,
                                       NoLocalJmxConnectionException;

    MBeanServerConnection getLocalMBeanServerConnection() throws IOException,
                                                         NoLocalJmxConnectionException;

    MBeanServerConnection getLocalMBeanServerConnection(Subject delegationSubject)
                                                                                  throws ConnectException,
                                                                                  NoLocalJmxConnectionException;

    /**
     * @return the List of arguments to the Java virtual machine
     */
    List<String> getVmOptions();

    void setArguments(List<String> arguments);

    void setArguments(String[] arguments);

    void setJarFile(File jarFile);

    void setJarFile(String jarFile);

    void setJavaClass(String javaClass);

    void setJavaExecutable(File javaExecutable);

    void setJavaExecutable(String javaExecutable);

    void setVmOptions(List<String> vmOptions);

    void setVmOptions(String[] vmOptions);
}