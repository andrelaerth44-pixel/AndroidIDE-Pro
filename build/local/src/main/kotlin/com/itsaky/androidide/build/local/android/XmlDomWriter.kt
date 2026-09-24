/*
 *  This file is part of AndroidIDE Pro.
 *
 *  AndroidIDE Pro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The DOM serialization approach is compatible with the ART-safe pattern used by CodeAssist.
 */

package com.itsaky.androidide.build.local.android

import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * ART-safe DOM-to-XML serializer.
 *
 * It avoids javax.xml.transform so the resource merge path does not depend on a desktop JAXP transformer
 * implementation that may be unavailable on Android.
 */
internal object XmlDomWriter {
  fun toXml(node: Node, xmlDeclaration: Boolean = true): String {
    val out = StringBuilder()
    if (xmlDeclaration) out.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
    when (node) {
      is Document -> node.documentElement?.let { write(it, out) }
      else -> write(node, out)
    }
    return out.toString()
  }

  private fun write(node: Node, out: StringBuilder) {
    when (node.nodeType) {
      Node.ELEMENT_NODE -> writeElement(node as Element, out)
      Node.TEXT_NODE -> out.append(escapeText(node.nodeValue.orEmpty()))
      Node.CDATA_SECTION_NODE -> out.append("<![CDATA[").append(node.nodeValue.orEmpty()).append("]]>" )
      Node.COMMENT_NODE -> out.append("<!--").append(node.nodeValue.orEmpty()).append("-->")
      Node.ENTITY_REFERENCE_NODE -> out.append('&').append(node.nodeName).append(';')
      Node.PROCESSING_INSTRUCTION_NODE -> out.append("<?").append(node.nodeName).append(' ').append(node.nodeValue.orEmpty()).append("?>")
    }
  }

  private fun writeElement(element: Element, out: StringBuilder) {
    out.append('<').append(element.tagName)
    val attributes = element.attributes
    for (i in 0 until attributes.length) {
      val attr = attributes.item(i) as Attr
      out.append(' ').append(attr.name).append("=\"").append(escapeAttribute(attr.value)).append('"')
    }
    val children = element.childNodes
    if (children.length == 0) {
      out.append("/>")
      return
    }
    out.append('>')
    for (i in 0 until children.length) write(children.item(i), out)
    out.append("</").append(element.tagName).append('>')
  }

  private fun escapeText(value: String): String = buildString(value.length) {
    value.forEach {
      when (it) {
        '&' -> append("&amp;")
        '<' -> append("&lt;")
        '>' -> append("&gt;")
        else -> append(it)
      }
    }
  }

  private fun escapeAttribute(value: String): String = buildString(value.length) {
    value.forEach {
      when (it) {
        '&' -> append("&amp;")
        '<' -> append("&lt;")
        '>' -> append("&gt;")
        '"' -> append("&quot;")
        '\n' -> append("&#10;")
        '\r' -> append("&#13;")
        '\t' -> append("&#9;")
        else -> append(it)
      }
    }
  }
}
