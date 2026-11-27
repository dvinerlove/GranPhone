package com.bald.uriah.baldphone.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GenerationGuardTest {
    @Test
    public void staleTokensAreRejected() {
        final GenerationGuard guard = new GenerationGuard();
        final int first = guard.next();
        final int second = guard.next();
        assertFalse(guard.isCurrent(first));
        assertTrue(guard.isCurrent(second));
    }

    @Test
    public void invalidateDropsInflightToken() {
        final GenerationGuard guard = new GenerationGuard();
        final int token = guard.next();
        guard.invalidate();
        assertFalse(guard.isCurrent(token));
        assertTrue(guard.isCurrent(guard.next()));
    }
}
