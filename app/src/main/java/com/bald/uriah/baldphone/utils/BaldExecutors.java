/*
 * Copyright 2019 Uriah Shaul Mandel
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

package com.bald.uriah.baldphone.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Shared app executors. {@link #db()} serializes Room writes and DB-heavy work;
 * {@link #io()} is a small pool for provider I/O (PackageManager, contacts).
 */
public final class BaldExecutors {
    private BaldExecutors() {
    }

    private static final class Holder {
        private static final Executor DB = Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "bald-db");
            thread.setDaemon(true);
            return thread;
        });
        private static final Executor IO = Executors.newCachedThreadPool(runnable -> {
            final Thread thread = new Thread(runnable, "bald-io");
            thread.setDaemon(true);
            return thread;
        });
        private static final Handler MAIN = new Handler(Looper.getMainLooper());
    }

    /** Single thread: Room writes and work that must run serialized. */
    public static Executor db() {
        return Holder.DB;
    }

    /** Pool for provider I/O that may run in parallel. */
    public static Executor io() {
        return Holder.IO;
    }

    public static void postToMain(Runnable runnable) {
        Holder.MAIN.post(runnable);
    }
}
