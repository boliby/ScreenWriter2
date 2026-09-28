package com.boliby.screenwriter.testing

import java.io.File

/**
 * The fountain.io sample scripts, downloaded by `scripts/fetch-samples.sh`.
 * They're copyrighted and not in git, so tests that need them pass quietly when
 * they're missing, unless REQUIRE_SAMPLES is set, as it is in CI.
 */
object Samples {
    private val dir: File? = System.getProperty("screenwriter.samples")?.let(::File)

    fun find(name: String): File? {
        val file = dir?.resolve(name)?.takeIf { it.isFile }
        if (file == null && !System.getProperty("screenwriter.requireSamples").isNullOrEmpty()) {
            error("Sample $name is missing. Run scripts/fetch-samples.sh.")
        }
        return file
    }
}
