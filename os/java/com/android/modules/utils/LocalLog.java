/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.modules.utils;

import android.os.SystemClock;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Class gathers and caches logs and dumps when {@link #reverseDump()} method or {@link #dump()}
 * method is invoked. Copied from {@see android.util.LocalLog}, users of that class should
 * migrate to using this class.
 */
public final class LocalLog {

    private final Deque<String> mLog;
    private final int mMaxLines;

    /**
     * {@code true} to use log timestamps expressed in local date/time, {@code false} to use log
     * timestamped expressed with the elapsed realtime clock and UTC system clock. {@code false} is
     * useful when logging behavior that modifies device time zone or system clock.
     */
    private final boolean mUseLocalTimestamps;

    public LocalLog(int maxLines) {
        this(maxLines, true /* useLocalTimestamps */);
    }

    public LocalLog(int maxLines, boolean useLocalTimestamps) {
        mMaxLines = Math.max(0, maxLines);
        mLog = new ArrayDeque<>(mMaxLines);
        mUseLocalTimestamps = useLocalTimestamps;
    }

    public void log(String msg) {
        if (mMaxLines <= 0) {
            return;
        }
        final String logLine;
        if (mUseLocalTimestamps) {
            logLine = String.format("%s - %s", LocalDateTime.now(), msg);
        } else {
            logLine =
                    String.format(
                            "%s / %s - %s", SystemClock.elapsedRealtime(), Instant.now(), msg);
        }
        append(logLine);
    }

    private synchronized void append(String logLine) {
        while (mLog.size() >= mMaxLines) {
            mLog.remove();
        }
        mLog.add(logLine);
    }

    public synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        dump(pw);
    }

    public synchronized void dump(PrintWriter pw) {
        dump("", pw);
    }

    /**
     * Dumps the content of local log to print writer with each log entry predeced with indent
     *
     * @param indent indent that precedes each log entry
     * @param pw printer writer to write into
     */
    public synchronized void dump(String indent, PrintWriter pw) {
        Iterator<String> itr = mLog.iterator();
        while (itr.hasNext()) {
            pw.printf("%s%s\n", indent, itr.next());
        }
    }

    public synchronized void reverseDump(FileDescriptor fd, PrintWriter pw, String[] args) {
        reverseDump(pw);
    }

    public synchronized void reverseDump(PrintWriter pw) {
        Iterator<String> itr = mLog.descendingIterator();
        while (itr.hasNext()) {
            pw.println(itr.next());
        }
    }

    public static class ReadOnlyLocalLog {
        private final LocalLog mLog;

        ReadOnlyLocalLog(LocalLog log) {
            mLog = log;
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            mLog.dump(pw);
        }

        public void dump(PrintWriter pw) {
            mLog.dump(pw);
        }

        public void reverseDump(FileDescriptor fd, PrintWriter pw, String[] args) {
            mLog.reverseDump(pw);
        }

        public void reverseDump(PrintWriter pw) {
            mLog.reverseDump(pw);
        }
    }

    public ReadOnlyLocalLog readOnlyLocalLog() {
        return new ReadOnlyLocalLog(this);
    }
}
