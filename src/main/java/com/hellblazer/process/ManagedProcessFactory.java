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

import java.io.File;

/**
 * @author Hal Hildebrand
 * 
 */
public interface ManagedProcessFactory {
    /**
     * Create a new instance of a ManagedProcess appropriate for the current
     * runtime platform. The returned process is initialized with the control
     * information stashed in the supplied home directory
     * 
     * @param homeDirectory
     * @return
     */
    ManagedProcess acquireFrom(File homeDirectory);

    /**
     * Create a new instance of a ManagedProcess appropriate for the current
     * runtime platform
     */
    ManagedProcess create();

    /**
     * Create a new instance of a JavaProcess appropriate for the current
     * runtime platform
     */
    JavaProcess createJavaProcess();
}
