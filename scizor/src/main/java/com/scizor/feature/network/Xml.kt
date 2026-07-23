package com.scizor.feature.network

import java.io.StringReader
import java.io.StringWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

/** XML/HTML detection + pretty-printing for body views, mirroring Scyther's XML handling. */
internal object Xml {

    fun looksLikeXml(contentType: String?, text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        if (contentType?.contains("xml", ignoreCase = true) == true) return true
        val t = text.trim()
        return t.startsWith("<?xml")
    }

    /** True for HTML documents (rendered as-is; HTML has no safe canonical re-indent). */
    fun looksLikeHtml(contentType: String?, text: String?): Boolean {
        if (contentType?.contains("html", ignoreCase = true) == true) return true
        val t = text?.trim()?.lowercase() ?: return false
        return t.startsWith("<!doctype html") || t.startsWith("<html")
    }

    fun pretty(text: String): String = runCatching {
        // Normalise inter-tag whitespace first so the transformer re-indents cleanly.
        val normalised = text.trim().replace(Regex(">\\s+<"), "><")
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
        }
        val out = StringWriter()
        transformer.transform(StreamSource(StringReader(normalised)), StreamResult(out))
        out.toString().trim()
    }.getOrDefault(text)
}
