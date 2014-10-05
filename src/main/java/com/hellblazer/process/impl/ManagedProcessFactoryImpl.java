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

import java.io.File;
import java.util.UUID;

import com.hellblazer.process.JavaProcess;
import com.hellblazer.process.ManagedProcess;
import com.hellblazer.process.ManagedProcessFactory;

/**
 * @author Hal Hildebrand
 * 
 */
public class ManagedProcessFactoryImpl implements ManagedProcessFactory {
    protected static String         operatingSystem    = System.getProperty("os.name").toLowerCase();
    protected static final String[] supportedPlatforms = { "mac", "linux",
            "unix", "solaris"                         };

    public static boolean isPlatformSupported() {
        for (String platform : supportedPlatforms) {
            if (operatingSystem.contains(platform)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ManagedProcess acquireFrom(File homeDirectory) {
        UUID id = AbstractManagedProcess.getIdFrom(homeDirectory);
        if (id == null) {
            throw new IllegalStateException(
                                            "Cannot obtain process control information from home directory: "
                                                    + homeDirectory.getAbsolutePath());
        }
        AbstractManagedProcess process = create(id);
        process.acquireFromHome(homeDirectory);
        return process;
    }

    @Override
    public ManagedProcess create() {
        return create(UUID.randomUUID());
    }

    /* (non-Javadoc)
     * @see com.hellblazer.process.ManagedProcessFactory#createJavaProcess()
     */
    @Override
    public JavaProcess createJavaProcess() {
        return new JavaProcessImpl(create());
    }

    protected AbstractManagedProcess create(UUID id) {
        if (operatingSystem.contains("mac")) {
            return new UnixProcess(id);
        } else if (operatingSystem.contains("linux")) {
            return new UnixProcess(id);
        } else if (operatingSystem.contains("unix")) {
            return new UnixProcess(id);
        } else if (operatingSystem.contains("solaris")) {
            return new UnixProcess(id);
        } else {
            throw new IllegalStateException("Unimplemented OS platform: "
                                            + operatingSystem);
        }
    }

}
