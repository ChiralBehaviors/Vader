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
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.input.ReversedLinesFileReader;

/**
 * Created by saptarshi.roy on 9/29/14.
 */
public class CachedFileTailer {

    private List<String> lines;

    private final int maxLines;
    private final File file;
    private final Timer expirationTimer;
    private volatile boolean bufferExpired = true;
    private final ReadWriteLock lock;

    public CachedFileTailer(File targetFile, TimeUnit freshnessInternalTimeUnit, long freshnessInterval, int maxLines) {

        this.maxLines = maxLines;
        this.file = targetFile;
        this.lines = new ArrayList<>(maxLines);
        this.lock = new ReentrantReadWriteLock();

        this.expirationTimer = new Timer();

        long periodInMS = TimeUnit.MILLISECONDS.convert(freshnessInterval, freshnessInternalTimeUnit);
        expirationTimer.scheduleAtFixedRate(new CacheExpirationTimer(), periodInMS, periodInMS);
    }

    private class CacheExpirationTimer extends TimerTask {
        @Override
        public void run() {
            bufferExpired = true;
        }
    }

    public List<String> getTailLines(int numLines) throws IOException {

        if (numLines < 1) {
            return Collections.EMPTY_LIST;
        }

        if (numLines > maxLines) {
            numLines = maxLines;
        }

        // Only 1 thread should ever reload the expired buffer
        if (bufferExpired && lock.writeLock().tryLock()) {
            try {
                loadLines();
                bufferExpired = false;
            } finally {
                lock.writeLock().unlock();
            }
        }

        List<String> subBuffer = new ArrayList<>(numLines);

        // We want arbitrary numbers of threads to be able to read the buffer concurrently.
        try {
            lock.readLock().lock();

            int startPos = lines.size() - numLines;
            if (startPos < 0) {
                startPos = 0;
            }

            for (int i = startPos; i < lines.size(); i++) {
                subBuffer.add(lines.get(i));
            }
        } finally {
            lock.readLock().unlock();
        }

        return subBuffer;
    }


    private void loadLines() throws IOException {
        lines.clear();
        ReversedLinesFileReader reader = new ReversedLinesFileReader(file);
        int linesRead = 0;
        String line;
        while ( ((line = reader.readLine()) != null) && (linesRead++ < maxLines)) {
            lines.add(line);
        }

        Collections.reverse(lines);
    }
}
