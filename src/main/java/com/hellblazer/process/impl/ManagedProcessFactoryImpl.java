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

    /* (non-Javadoc)
     * @see com.hellblazer.process.ManagedProcessFactory#createJavaProcess()
     */
    @Override
    public JavaProcess createJavaProcess() {
        return new JavaProcessImpl(create());
    }

}
