package com.boliby.screenwriter.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ElementTypeTest {
    @Test
    fun uppercaseElements() {
        val uppercase = ElementType.entries.filter { it.displaysUppercase }.toSet()
        assertEquals(
            setOf(
                ElementType.SCENE_HEADING,
                ElementType.CHARACTER,
                ElementType.TRANSITION,
                ElementType.SHOT,
            ),
            uppercase,
        )
    }
}
