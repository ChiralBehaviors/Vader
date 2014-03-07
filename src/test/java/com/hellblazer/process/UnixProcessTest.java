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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;

import com.hellblazer.process.impl.AbstractManagedProcess;
import com.hellblazer.process.impl.JavaProcessImpl;
import com.hellblazer.process.impl.UnixProcess;
import com.hellblazer.utils.Utils;

/**
 * @author Hal Hildebrand
 * 
 */
public class UnixProcessTest extends ProcessTest {
    protected static final String LS_DIR   = "test-dirs/ls-test";

    protected static final String TEST_DIR = "test-dirs/unix-test";
    File                          testDir;

    protected void copyTestClassFile() throws Exception {
        String classFileName = HelloWorld.class.getCanonicalName().replace('.',
                                                                           '/')
                               + ".class";
        URL classFile = getClass().getResource("/" + classFileName);
        assertNotNull(classFile);
        File copiedFile = new File(testDir, classFileName);
        assertTrue(copiedFile.getParentFile().mkdirs());
        FileOutputStream out = new FileOutputStream(copiedFile);
        InputStream in = classFile.openStream();
        byte[] buffer = new byte[1024];
        for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.close();
    }

    @Override
    protected void setUp() {
        Utils.initializeDirectory(TEST_DIR);
        testDir = new File(TEST_DIR);
    }

    public void testAcquire() throws Exception {
        assertNotNull("Java executable exists", ProcessTest.javaBin);
        Utils.initializeDirectory(testDir);
        copyTestClassFile();
        JavaProcess process = new JavaProcessImpl(new UnixProcess());
        int sleepTime = 60;
        process.setArguments(new String[] { "-sleep", String.valueOf(sleepTime) });
        process.setJavaClass(HelloWorld.class.getCanonicalName());
        process.setDirectory(testDir);
        process.setJavaExecutable(ProcessTest.javaBin);
        process.start();
        try {
            UUID id = AbstractManagedProcess.getIdFrom(testDir);
            assertNotNull("acquired process id from home directory", id);
            UnixProcess acquiredProcess = new UnixProcess(id);
            acquiredProcess.acquireFromHome(testDir);
            assertTrue("Process is active", process.isActive());
            assertTrue("acquired process reports process as active",
                       acquiredProcess.isActive());
            acquiredProcess.stop();
            assertTrue("Process not active", !process.isActive());
        } finally {
            process.stop();
        }
    }

    public void testSmoke() throws Exception {
        ManagedProcess process = new UnixProcess();
        process.setDirectory(testDir);
        process.setCommand(new String[] { "echo", "foo" });
        process.start();
        assertEquals("process exited normally", 0, process.waitFor());
        assertFalse("process is not active", process.isActive());
    }

    public void testStdOut() throws Exception {
        String[] names = { "bar", "baz", "foo" };

        Utils.initializeDirectory(LS_DIR);
        File lsDir = new File(LS_DIR);
        StringBuffer expected = new StringBuffer();
        int i = 0;
        for (String name : names) {
            FileOutputStream fos = new FileOutputStream(new File(lsDir, name));
            fos.write("Test bytes".getBytes());
            fos.close();
            expected.append(name);
            i++;
            if (i < names.length) {
                expected.append(", ");
            }
        }

        ManagedProcess process = new UnixProcess();
        process.setDirectory(testDir);
        process.setCommand(new String[] { "ls", lsDir.getAbsolutePath() });
        process.start();
        assertEquals("process exited normally", 0, process.waitFor());
        assertTrue("process is not active", !process.isActive());

        BufferedReader stdOut = new BufferedReader(
                                                   new InputStreamReader(
                                                                         process.getStdOut()));
        StringBuffer rslt = new StringBuffer();
        String line = stdOut.readLine();
        while (line != null) {
            rslt.append(line);
            line = stdOut.readLine();
            if (line != null) {
                rslt.append(", ");
            }
        }

        assertEquals("Expected ls result", expected.toString(), rslt.toString());
    }

}
