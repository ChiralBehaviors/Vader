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

import java.io.*;
import java.net.ConnectException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;

import com.hellblazer.process.impl.JavaProcessImpl;
import com.hellblazer.process.impl.ManagedProcessFactoryImpl;
import com.hellblazer.utils.Condition;
import com.hellblazer.utils.Utils;

/**
 * @author Hal Hildebrand
 * 
 */
public class JavaProcessTest extends ProcessTest {
    protected static final String TEST_DIR       = "test-dirs/java-process-test";
    protected static final String TEST_JAR       = "test.jar";
    protected File                testDir;
    MBeanServerConnection         connection;
    ManagedProcessFactoryImpl     processFactory = new ManagedProcessFactoryImpl();

    public void testClassExecution() throws Exception {
        copyTestClassFile();
        JavaProcess process = new JavaProcessImpl(processFactory.create());
        process.setArguments(new String[] { "-echo", "foo", "bar", "baz" });
        process.setJavaClass(HelloWorld.class.getCanonicalName());
        assertNull("No jar file set", process.getJarFile());

        try {
            launchProcess(process);

            assertEquals("Process exited normally", 0, process.waitFor());
            assertTrue("Process not active", !process.isActive());

            try (BufferedReader reader = new BufferedReader(
                                                            new InputStreamReader(
                                                                                  process.getStdErr()))) {
                validateExpectedEchoLines(reader);
            }

            try (BufferedReader reader = new BufferedReader(
                                                            new InputStreamReader(
                                                                                  process.getStdOut()))) {
                assertEquals(HelloWorld.STARTUP_MSG, reader.readLine());
                validateExpectedEchoLines(reader);
            }
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    public void testExitValue() throws Exception {
        copyTestClassFile();
        JavaProcess process = new JavaProcessImpl(processFactory.create());
        process.setArguments(new String[] { "-errno", "66" });
        process.setJavaClass(HelloWorld.class.getCanonicalName());
        process.setDirectory(testDir);
        process.setJavaExecutable(javaBin);

        try {
            launchProcess(process);
            assertEquals("Process exited abnormally", 66, process.waitFor());
            assertTrue("Process not active", !process.isActive());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    public void testJarExecution() throws Exception {
        copyTestJarFile();
        JavaProcess process = new JavaProcessImpl(processFactory.create());
        process.setArguments(new String[] { "-echo", "hello" });
        process.setJarFile(new File(testDir, TEST_JAR));
        assertNull("No java class set", process.getJavaClass());

        try {
            launchProcess(process);
            assertEquals("Process exited normally", 0, process.waitFor());
            assertTrue("Process not active", !process.isActive());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    public void testLocalMBeanServerConnection() throws Exception {
        copyTestClassFile();
        final JavaProcess process = new JavaProcessImpl(processFactory.create());
        int sleepTime = 60000;
        process.setArguments(new String[] { "-jmx", Integer.toString(sleepTime) });
        process.setJavaClass(HelloWorld.class.getCanonicalName());
        process.setDirectory(testDir);
        process.setJavaExecutable(javaBin);

        try {
            launchProcess(process);

            Condition condition = new Condition() {
                @Override
                public boolean isTrue() {
                    try {
                        connection = process.getLocalMBeanServerConnection();
                        return true;
                    } catch (ConnectException e) {
                        return false;
                    } catch (NoLocalJmxConnectionException e) {
                        return false;
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.flush();
                        System.err.flush();
                        System.out.println("error");
                        fail("error retrieving JMX connection: " + e);
                        return false;
                    }
                }

            };

            assertTrue("JMX connection established",
                       Utils.waitForCondition(60 * 1000, condition));

            Set<ObjectName> names = connection.queryNames(null, null);
            assertTrue(names.size() > 1);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        assertTrue("Process not active", !process.isActive());
    }

    public void testOnDemandInputOutputStreams() throws Exception {
        copyTestClassFile();
        JavaProcess process = new JavaProcessImpl(processFactory.create());
        process.setArguments(new String[] { "-loglines" });
        process.setJavaClass(HelloWorld.class.getCanonicalName());
        process.setDirectory(testDir);
        process.setJavaExecutable(javaBin);

        // Try it before launching process
        try {
            process.getStdOutTail(200);
            fail("Expected an illegal thread state exception because the process hasn't started yet");
        } catch (IllegalThreadStateException e) {
            System.out.println("Caught expected exception : " + e.getMessage());
        }

        try {
            process.getStdErrTail(200);
            fail("Expected an illegal thread state exception because the process hasn't started yet");
        } catch (IllegalThreadStateException e) {
            System.out.println("Caught expected exception : " + e.getMessage());
        }

        try {
            launchProcess(process);

            String stdout = process.getStdOutTail(200).trim();
            assertTrue(stdout.startsWith("Line #4800"));
            assertTrue(stdout.endsWith("Line #4999"));

            String stderr = process.getStdErrTail(200).trim();
            assertTrue(stderr.startsWith("Line #4800"));
            assertTrue(stderr.endsWith("Line #4999"));

        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    public void testTailStdInputOutputStreams() throws Exception {
        final List<String> lines = new CopyOnWriteArrayList<>();
        TailerListener listener = new TailerListenerAdapter() {
            @Override
            public void handle(String line) {
                lines.add(line);
            }
        };

        copyTestClassFile();
        JavaProcess process = new JavaProcessImpl(processFactory.create());
        String testLine = "hello";
        process.setArguments(new String[] { "-readln", testLine });
        process.setJavaClass(HelloWorld.class.getCanonicalName());
        process.setDirectory(testDir);
        process.setJavaExecutable(javaBin);

        Tailer tailer = null;
        try {
            launchProcess(process);
            tailer = process.tailStdOut(listener);

            try (PrintWriter writer = new PrintWriter(
                                                      new OutputStreamWriter(
                                                                             process.getStdIn()))) {
                writer.println(testLine);
                writer.flush();
            }

            assertEquals("Process exited normally", 0, process.waitFor());
            assertTrue("Process not active", !process.isActive());
            Utils.waitForCondition(1000, new Condition() {
                @Override
                public boolean isTrue() {
                    return lines.size() > 1;
                }
            });

            assertEquals(2, lines.size());
            assertEquals(testLine, lines.get(1));

            tailer.stop();
        } finally {
            if (tailer != null) {
                tailer.stop();
            }

            process.destroy();
        }
    }

    private void launchProcess(JavaProcess process) throws IOException {
        process.setDirectory(testDir);
        process.setJavaExecutable(javaBin);

        setupJavaClasspath(process);
        process.start();
        assertTrue("Expected successful process start",
                   new ProcessStartWatcher(process).waitForSuccessfulStartup());

        // Give the process a chance to do its thing before launching into
        // evaluating output
        try {
            System.out.println("Waiting for process before evaluating output...");
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void validateExpectedEchoLines(BufferedReader reader)
                                                                 throws IOException {
        String line;
        line = reader.readLine();
        assertEquals("foo", line);
        line = reader.readLine();
        assertEquals("bar", line);
        line = reader.readLine();
        assertEquals("baz", line);
        line = reader.readLine();
        assertNull(line);
    }

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

    protected void copyTestJarFile() throws Exception {
        String classFileName = HelloWorld.class.getCanonicalName().replace('.',
                                                                           '/')
                               + ".class";
        URL classFile = getClass().getResource("/" + classFileName);
        assertNotNull(classFile);

        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue("Manifest-Version", "1.0");
        attributes.putValue("Main-Class", HelloWorld.class.getCanonicalName());

        FileOutputStream fos = new FileOutputStream(new File(testDir, TEST_JAR));
        JarOutputStream jar = new JarOutputStream(fos, manifest);
        JarEntry entry = new JarEntry(classFileName);
        jar.putNextEntry(entry);
        InputStream in = classFile.openStream();
        byte[] buffer = new byte[1024];
        for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
            jar.write(buffer, 0, read);
        }
        in.close();
        jar.closeEntry();
        jar.close();
    }

    @Override
    protected void setUp() {
        System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
        System.setProperty("sun.net.client.defaultReadTimeout", "10000");
        System.setProperty("javax.net.debug", "all");
        Utils.initializeDirectory(TEST_DIR);
        testDir = new File(TEST_DIR);
    }
}
