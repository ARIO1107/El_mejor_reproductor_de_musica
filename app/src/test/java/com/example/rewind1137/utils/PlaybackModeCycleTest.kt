package com.example.rewind1137.utils

import com.example.rewind1137.viewmodel.PlaybackMode
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackModeCycleTest {

    @Test
    fun `next mode goes from sequential to shuffle`() {
        assertEquals(PlaybackMode.SHUFFLE, PlaybackModeCycle.next(PlaybackMode.SEQUENTIAL))
    }

    @Test
    fun `next mode goes from shuffle to repeat one`() {
        assertEquals(PlaybackMode.REPEAT_ONE, PlaybackModeCycle.next(PlaybackMode.SHUFFLE))
    }

    @Test
    fun `next mode wraps from repeat one to sequential`() {
        assertEquals(PlaybackMode.SEQUENTIAL, PlaybackModeCycle.next(PlaybackMode.REPEAT_ONE))
    }
}
