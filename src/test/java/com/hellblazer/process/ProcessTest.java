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
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;


/**
 * 
 * @author hhildebrand
 * 
 */
abstract public class ProcessTest extends TestCase {
    static File javaBin = getJavaExecutable();

    private static File getJavaExecutable() {
        String pathSeparator = System.getProperty("file.separator");
        String javaBin = System.getProperty("java.home");
        if (javaBin.endsWith(pathSeparator)) {
            javaBin += "bin";
        } else {
            javaBin += pathSeparator + "bin";
        }
        javaBin += pathSeparator;
        String os = System.getProperty("os.name").toLowerCase();
        return new File(os.indexOf("win") >= 0 ? javaBin + "java.exe"
                                              : javaBin + "java");
    }

    protected void setupJavaClasspath(JavaProcess process) {

        // Force the Java classpath to be local directory. Otherwise,
        // in certain environments we might inherit an existing classpath, which
        // breaks the test. For example, this might happen when running the test
        // within an IDE or CI server.

        Map<String,String> env = new HashMap<>();
        env.put("CLASSPATH", "./");
        process.setEnvironment(env);
    }
}
