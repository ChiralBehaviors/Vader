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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;

import com.hellblazer.utils.Condition;
import com.hellblazer.utils.Utils;

/**
 * ProcessTestHelper
 *
 * @author saptarshi.roy
 */
public class ProcessStartWatcher {

    private List<String> lines = new CopyOnWriteArrayList<>();
    private final Tailer tailer;

    public ProcessStartWatcher(ManagedProcess process) {

        TailerListener listener = new TailerListenerAdapter() {
            @Override
            public void handle(String line) {
                lines.add(line);
            }
        };

        tailer = process.tailStdOut(listener);
    }

    public boolean waitForSuccessfulStartup() {

        Condition condition = new Condition() {
            @Override
            public boolean isTrue() {
                boolean foundStartupString = ((lines.size() > 0) && (lines.get(0).equals(HelloWorld.STARTUP_MSG)));
                return (foundStartupString);
            }
        };

        // Poll every half second, up to a max of 5 seconds
        boolean found = false;
        try {
            found = Utils.waitForCondition(5000, 100, condition);
        } finally {
            tailer.stop();
        }

        return found;
    }
}
