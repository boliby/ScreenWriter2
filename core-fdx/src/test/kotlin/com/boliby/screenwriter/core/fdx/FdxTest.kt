package com.boliby.screenwriter.core.fdx

import com.boliby.screenwriter.core.fountain.FountainParser
import com.boliby.screenwriter.core.layout.PageTemplate
import com.boliby.screenwriter.core.model.Block
import com.boliby.screenwriter.core.model.ElementType.ACTION
import com.boliby.screenwriter.core.model.ElementType.CENTERED
import com.boliby.screenwriter.core.model.ElementType.CHARACTER
import com.boliby.screenwriter.core.model.ElementType.DIALOGUE
import com.boliby.screenwriter.core.model.ElementType.PAGE_BREAK
import com.boliby.screenwriter.core.model.ElementType.SCENE_HEADING
import com.boliby.screenwriter.core.model.Script
import com.boliby.screenwriter.core.model.Span
import com.boliby.screenwriter.core.model.Style
import com.boliby.screenwriter.testing.Samples
import org.w3c.dom.Element
import org.w3c.dom.Node
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FdxTest {
    private val sample = """
        <?xml version="1.0" encoding="UTF-8" standalone="no" ?>
        <FinalDraft DocumentType="Script" Template="No" Version="1">
          <Content>
            <Paragraph Number="1" Type="Scene Heading">
              <SceneProperties Length="1/8" Page="1" Title=""/>
              <Text>INT. HOUSE - DAY</Text>
            </Paragraph>
            <Paragraph Type="Action" Mood="calm">
              <Text>She reads </Text>
              <Text Font="Courier" Style="Bold+Underline">the letter</Text>
              <Text>.</Text>
            </Paragraph>
            <Paragraph Type="General">
              <DualDialogue>
                <Paragraph Type="Character"><Text>ANN</Text></Paragraph>
                <Paragraph Type="Dialogue"><Text>Yes.</Text></Paragraph>
                <Paragraph Type="Character"><Text>BOB</Text></Paragraph>
                <Paragraph Type="Dialogue"><Text>No.</Text></Paragraph>
              </DualDialogue>
            </Paragraph>
            <Paragraph Alignment="Center" StartsNewPage="Yes" Type="Action">
              <Text>THE END</Text>
            </Paragraph>
          </Content>
          <ElementSettings Type="Action">
            <ParagraphSpec Alignment="Left" FirstIndent="0.00" LeftIndent="1.25" RightIndent="7.25" SpaceBefore="12" Spacing="1"/>
          </ElementSettings>
          <SmartType><Characters><Character>ANN</Character></Characters></SmartType>
          <Unknown Keep="me"/>
        </FinalDraft>
    """.trimIndent()

    private fun Script.summary() = blocks.map { listOf(it.type, it.text, it.spans, it.dual, it.sceneNumber) }

    @Test
    fun readsElementsStylesAndDualDialogue() {
        val script = FdxReader.read(sample)
        assertEquals(
            listOf(
                listOf(SCENE_HEADING, "INT. HOUSE - DAY", emptyList<Span>(), false, "1"),
                listOf(ACTION, "She reads the letter.", listOf(Span(10, 20, Style.BOLD), Span(10, 20, Style.UNDERLINE)), false, null),
                listOf(CHARACTER, "ANN", emptyList<Span>(), false, null),
                listOf(DIALOGUE, "Yes.", emptyList<Span>(), false, null),
                listOf(CHARACTER, "BOB", emptyList<Span>(), true, null),
                listOf(DIALOGUE, "No.", emptyList<Span>(), false, null),
                listOf(PAGE_BREAK, "", emptyList<Span>(), false, null),
                listOf(CENTERED, "THE END", emptyList<Span>(), false, null),
            ),
            script.summary(),
        )
    }

    @Test
    fun unchangedScriptWritesBackTheSameXml() {
        val original = Xml.parse(sample).documentElement
        val written = Xml.parse(FdxWriter.write(FdxReader.read(sample))).documentElement
        assertTrue(sameXml(original, written), FdxWriter.write(FdxReader.read(sample)))
    }

    @Test
    fun editedParagraphKeepsWhatTheAppDoesNotUse() {
        val script = FdxReader.read(sample)
        val edited = script.copy(blocks = script.blocks.map { if (it.sceneNumber == "1") it.copy(text = "EXT. YARD - NIGHT") else it })
        val written = Xml.parse(FdxWriter.write(edited)).documentElement
        val heading = written.child("Content")!!.children("Paragraph").first()
        assertEquals("1", heading.getAttribute("Number"))
        assertEquals("1/8", heading.child("SceneProperties")?.getAttribute("Length"))
        assertEquals("EXT. YARD - NIGHT", heading.children("Text").joinToString("") { it.textContent })
        assertEquals("me", written.child("Unknown")?.getAttribute("Keep"))
    }

    @Test
    fun editedStylesAreWrittenAsRuns() {
        val script = FdxReader.read(sample)
        val action = script.blocks[1].copy(text = "Big news.", spans = listOf(Span(0, 3, Style.ITALIC)))
        val written = Xml.parse(FdxWriter.write(script.copy(blocks = listOf(action)))).documentElement
        val runs = written.child("Content")!!.child("Paragraph")!!.children("Text")
        assertEquals(listOf("Big" to "Italic", " news." to ""), runs.map { it.textContent to it.getAttribute("Style") })
        assertEquals("calm", written.child("Content")!!.child("Paragraph")!!.getAttribute("Mood"))
    }

    @Test
    fun fountainScriptsConvertBothWays() {
        val fountain = FountainParser.parse(
            "Title: Test\n\nINT. HOUSE - DAY #4#\n\nShe *really* waits.\n\nANN\nYes.\n\nBOB ^\nNo.\n\n===\n\n> THE END <",
        )
        val back = FdxReader.read(FdxWriter.write(fountain))
        assertEquals(fountain.summary(), back.summary())
    }

    @Test
    fun templateComesFromElementSettings() {
        val template = FdxTemplate.from(FdxReader.read(sample))!!
        assertEquals(1.25, template.format(ACTION).left)
        assertEquals(61, template.format(ACTION).maxChars)
        assertEquals(1.25, template.format(CENTERED).left)
    }

    @Test
    fun refusesExternalEntities() {
        val hostile = """<?xml version="1.0"?><!DOCTYPE x [<!ENTITY e SYSTEM "file:///etc/passwd">]><FinalDraft><Content/></FinalDraft>"""
        assertFailsWith<Exception> { FdxReader.read(hostile) }
    }

    @Test
    fun samplesRoundTrip() {
        for (name in listOf("Big-Fish", "Brick-&-Steel", "The-Last-Birthday-Card")) {
            val file = Samples.find("$name.fdx") ?: return
            val original = Xml.parse(file.readText()).documentElement
            val script = file.inputStream().use(FdxReader::read)
            val written = FdxWriter.write(script)
            assertTrue(sameXml(original, Xml.parse(written).documentElement), "$name changed on round trip")
            assertEquals(script.summary(), FdxReader.read(written).summary(), name)
            assertEquals(FdxTemplate.from(script), FdxTemplate.from(FdxReader.read(written)), name)
        }
    }

    // Structural equality: same elements, attributes, and text, ignoring the
    // whitespace between elements.
    private fun sameXml(a: Element, b: Element): Boolean {
        if (a.tagName != b.tagName) return false
        val attrs = { e: Element -> (0 until e.attributes.length).associate { e.attributes.item(it).nodeName to e.attributes.item(it).nodeValue } }
        if (attrs(a) != attrs(b)) return false
        val kids = { e: Element ->
            generateSequence(e.firstChild) { it.nextSibling }
                .filter { it.nodeType != Node.TEXT_NODE || it.textContent.isNotBlank() || e.tagName == "Text" }
                .toList()
        }
        val ka = kids(a)
        val kb = kids(b)
        if (ka.size != kb.size) return false
        return ka.zip(kb).all { (x, y) ->
            when {
                x is Element && y is Element -> sameXml(x, y)
                x.nodeType == y.nodeType -> x.textContent == y.textContent
                else -> false
            }
        }
    }
}

/**
 * Writes `build/fdx-check/fdx-check.fdx` from a short script that uses every
 * element, for checking by hand that Final Draft or Fade In opens it (Phase 1C).
 */
class FdxExportCheck {
    @Test
    fun writeCheckFile() {
        val source = javaClass.getResource("/fdx-check.fountain")!!.readText()
        val out = java.io.File("build/fdx-check").apply { mkdirs() }.resolve("fdx-check.fdx")
        out.writeText(FdxWriter.write(FountainParser.parse(source)))
        val back = FdxReader.read(out.readText())
        assertTrue(back.blocks.any { it.dual } && back.blocks.any { it.type == PAGE_BREAK })
    }
}
