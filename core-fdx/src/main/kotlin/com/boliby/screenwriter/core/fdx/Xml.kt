package com.boliby.screenwriter.core.fdx

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.InputStream
import java.io.StringReader
import java.io.StringWriter
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/** DOM helpers that work on both the JVM and Android. */
internal object Xml {
    // Files come from anywhere, so DTDs and external entities are refused.
    private val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
        isExpandEntityReferences = false
        runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
    }

    fun parse(input: InputStream): Document = synchronized(factory) { factory.newDocumentBuilder() }.parse(input)

    fun parse(text: String): Document =
        synchronized(factory) { factory.newDocumentBuilder() }.parse(InputSource(StringReader(text)))

    fun serialize(node: Node, declaration: Boolean = false): String {
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, if (declaration) "no" else "yes")
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        }
        return StringWriter().also { transformer.transform(DOMSource(node), StreamResult(it)) }.toString()
    }
}

internal fun Element.children(name: String? = null): List<Element> {
    val result = mutableListOf<Element>()
    var node = firstChild
    while (node != null) {
        if (node is Element && (name == null || node.tagName == name)) result += node
        node = node.nextSibling
    }
    return result
}

internal fun Element.child(name: String): Element? = children(name).firstOrNull()

internal fun Element.attr(name: String): String? = getAttribute(name).takeIf { hasAttribute(name) }
