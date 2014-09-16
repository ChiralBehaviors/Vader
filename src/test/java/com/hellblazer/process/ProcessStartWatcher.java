/*
 * Copyright (c) 2013, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforcea.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
