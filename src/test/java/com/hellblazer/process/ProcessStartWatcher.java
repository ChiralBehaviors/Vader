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
