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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import sun.management.ConnectorAddressLink;
import sun.rmi.server.UnicastServerRef;
import sun.rmi.server.UnicastServerRef2;

import com.sun.jmx.remote.internal.RMIExporter;

/**
 * @author Hal Hildebrand
 * 
 */
@SuppressWarnings("restriction")
public class HelloWorld implements RMIExporter {

    public static final String STARTUP_MSG = "HelloWorld startup successful";

    static void bindJmx() throws Exception {
        JMXConnectorServer server;
        // Ensure cryptographically strong random number generater used
        // to choose the object number - see java.rmi.server.ObjID
        System.setProperty("java.rmi.server.randomIDs", "true");
        // Ensure that the rmi server socket binds to the localhost, rather than the translated IP address
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");

        // This RMI server should not keep the VM alive
        Map<String, RMIExporter> env = new HashMap<String, RMIExporter>();
        env.put(RMIExporter.EXPORTER_ATTRIBUTE, new HelloWorld());
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        JMXServiceURL url = new JMXServiceURL("rmi", "127.0.0.1", 11645);
        server = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
        server.start();
        ConnectorAddressLink.export(server.getAddress().toString());
    }

    public static void main(String[] argv) throws Exception {

        System.out.println(STARTUP_MSG);

        if (argv[0].equals("-echo")) {
            for (int i = 1; i < argv.length; i++) {
                System.out.println(argv[i]);
                System.err.println(argv[i]);
            }
        } else if (argv[0].equals("-jmx")) {
            bindJmx();
            Thread.sleep(Integer.parseInt(argv[1]));
            System.out.println("finished");
        } else if (argv[0].equals("-sleep")) {
            Thread.sleep(Integer.parseInt(argv[1]));
            System.out.println("finished");
        } else if (argv[0].equals("-errno")) {
            System.exit(Integer.parseInt(argv[1]));
        } else if (argv[0].equals("-readln")) {
            BufferedReader reader = new BufferedReader(
                                                       new InputStreamReader(
                                                                             System.in));
            String line = null;
            int i = 0;
            while (line == null && i++ < 100) {
                Thread.sleep(100);
                line = reader.readLine();
            }
            if (line == null) {
                System.exit(1);
            } else {
                System.out.println(line);
            }
        } else {
            System.err.println("Unknown option: " + argv[0]);
            System.exit(-1);
        }
    }

    Remote firstExported;

    /**
     * <p>
     * Prevents our RMI server objects from keeping the JVM alive.
     * </p>
     * 
     * <p>
     * We use a private interface in Sun's JMX Remote API implementation that
     * allows us to specify how to export RMI objects. We do so using
     * UnicastServerRef, a class in Sun's RMI implementation. This is all
     * non-portable, of course, so this is only valid because we are inside
     * Sun's JRE.
     * </p>
     * 
     * <p>
     * Objects are exported using
     * {@link UnicastServerRef#exportObject(Remote, Object, boolean)}. The
     * boolean parameter is called <code>permanent</code> and means both that
     * the object is not eligible for Distributed Garbage Collection, and that
     * its continued existence will not prevent the JVM from exiting. It is the
     * latter semantics we want (we already have the former because of the way
     * the JMX Remote API works). Hence the somewhat misleading name of this
     * class.
     * </p>
     */

    @Override
    public Remote exportObject(Remote obj, int port,
                               RMIClientSocketFactory csf,
                               RMIServerSocketFactory ssf)
                                                          throws RemoteException {

        synchronized (this) {
            if (firstExported == null) {
                firstExported = obj;
            }
        }

        final UnicastServerRef ref;
        if (csf == null && ssf == null) {
            ref = new UnicastServerRef(port);
        } else {
            ref = new UnicastServerRef2(port, csf, ssf);
        }
        return ref.exportObject(obj, null, true);
    }

    // Nothing special to be done for this case
    @Override
    public boolean unexportObject(Remote obj, boolean force)
                                                            throws NoSuchObjectException {
        return UnicastRemoteObject.unexportObject(obj, force);
    }

}
