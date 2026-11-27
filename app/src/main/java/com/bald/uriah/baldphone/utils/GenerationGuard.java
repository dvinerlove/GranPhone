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

/**
 * Stale-result guard for async loads: each load takes a token from {@link #next()}
 * and applies its result only while {@link #isCurrent(int)} holds. Bump via
 * {@link #invalidate()} when the owner dies so in-flight results are dropped.
 * Main-thread only — not synchronized.
 */
public final class GenerationGuard {
    private int generation;

    public int next() {
        return ++generation;
    }

    public boolean isCurrent(int token) {
        return token == generation;
    }

    public void invalidate() {
        ++generation;
    }
}
