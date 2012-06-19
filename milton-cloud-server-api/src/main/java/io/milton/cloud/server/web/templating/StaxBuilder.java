/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.web.templating;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMFactory;
import org.jdom.Namespace;
import org.jdom.UncheckedJDOMFactory;

/**
 * Builds a JDOM {@link org.jdom.Document org.jdom.Document} using a
 * {@link javax.xml.stream.XMLStreamReader}.
 *
 * @version $Revision: $, $Date: 6-02-15 15:21:25 -0 (Wed, 15 Feb 6) $
 * @author Tatu Saloranta
 * @author Bradley S. Huffman
 * @author Benson I. Margulies, mods for CXF to allow reading a portion of a
 * stream.
 */
public class StaxBuilder {

    /**
     * Map that contains conversion from textual attribute types StAX uses, to
     * int values JDOM uses.
     */
    private static final Map<String, Integer> ATTR_TYPES = new HashMap<String, Integer>(32);

    static {
        ATTR_TYPES.put("CDATA", new Integer(Attribute.CDATA_TYPE));
        ATTR_TYPES.put("cdata", new Integer(Attribute.CDATA_TYPE));
        ATTR_TYPES.put("ID", new Integer(Attribute.ID_TYPE));
        ATTR_TYPES.put("id", new Integer(Attribute.ID_TYPE));
        ATTR_TYPES.put("IDREF", new Integer(Attribute.IDREF_TYPE));
        ATTR_TYPES.put("idref", new Integer(Attribute.IDREF_TYPE));
        ATTR_TYPES.put("IDREFS", new Integer(Attribute.IDREFS_TYPE));
        ATTR_TYPES.put("idrefs", new Integer(Attribute.IDREFS_TYPE));
        ATTR_TYPES.put("ENTITY", new Integer(Attribute.ENTITY_TYPE));
        ATTR_TYPES.put("entity", new Integer(Attribute.ENTITY_TYPE));
        ATTR_TYPES.put("ENTITIES", new Integer(Attribute.ENTITIES_TYPE));
        ATTR_TYPES.put("entities", new Integer(Attribute.ENTITIES_TYPE));
        ATTR_TYPES.put("NMTOKEN", new Integer(Attribute.NMTOKEN_TYPE));
        ATTR_TYPES.put("nmtoken", new Integer(Attribute.NMTOKEN_TYPE));
        ATTR_TYPES.put("NMTOKENS", new Integer(Attribute.NMTOKENS_TYPE));
        ATTR_TYPES.put("nmtokens", new Integer(Attribute.NMTOKENS_TYPE));
        ATTR_TYPES.put("NOTATION", new Integer(Attribute.NOTATION_TYPE));
        ATTR_TYPES.put("notation", new Integer(Attribute.NOTATION_TYPE));
        ATTR_TYPES.put("ENUMERATED", new Integer(Attribute.ENUMERATED_TYPE));
        ATTR_TYPES.put("enumerated", new Integer(Attribute.ENUMERATED_TYPE));
    }
    /**
     * Whether ignorable white space should be ignored, ie not added in the
     * resulting JDOM tree. If true, it will be ignored; if false, it will be
     * added in the tree. Default value if false.
     */
    protected boolean cfgIgnoreWS;
    /**
     * The factory for creating new JDOM objects
     */
    private JDOMFactory factory;
    private Map additionalNamespaces;
    // This is set to 'true' when we are reading the middle of a stream,
    // and need to stop at the end of the element we start.
    private boolean isReadingMidStream;

    /**
     * Default constructor.
     */
    public StaxBuilder() {
    }

    public StaxBuilder(Map namespaces) {
        this.additionalNamespaces = namespaces;
    }

    public Map getAdditionalNamespaces() {
        return additionalNamespaces;
    }

    public void setAdditionalNamespaces(Map additionalNamespaces) {
        this.additionalNamespaces = additionalNamespaces;
    }

    /*
     * This sets a custom JDOMFactory for the builder. Use this to build the
     * tree with your own subclasses of the JDOM classes. @param factory
     * <code>JDOMFactory</code> to use
     */
    public void setFactory(JDOMFactory f) {
        factory = f;
    }

    public void setIgnoreWhitespace(boolean state) {
        cfgIgnoreWS = state;
    }

    /**
     * Returns the current {@link org.jdom.JDOMFactory} in use, if one has been
     * previously set with {@link #setFactory}, otherwise null.
     *
     * @return the factory builder will use
     */
    public JDOMFactory getFactory() {
        return factory;
    }

    /**
     * This will build a JDOM tree given a StAX stream reader. This API
     * explicitly supports building mid-stream.
     *
     * @param r Stream reader from which input is read.
     * @return
     * <code>Document</code> - JDOM document object.
     * @throws XMLStreamException If the reader threw such exception (to
     * indicate a parsing or I/O problem)
     */
    public Document build(XMLStreamReader r) throws XMLStreamException {
        isReadingMidStream = true;
        return buildInternal(r);
    }

    private Document buildInternal(XMLStreamReader r) throws XMLStreamException {
        /*
         * Should we do sanity checking to see that r is positioned at beginning
         * in the non-mid-stream case?
         */
        JDOMFactory f = factory;
        if (f == null) {
            f = new UncheckedJDOMFactory();
        }
        Document doc = f.document(null);
        buildTree(f, r, doc);
        return doc;
    }

    /**
     * This takes a
     * <code>XMLStreamReader</code> and builds up a JDOM tree. Recursion has
     * been eliminated by using local stack of open elements; this improves
     * performance somewhat (classic recursion-by-iteration-and-explicit stack
     * transformation)
     *
     * @param node
     * <code>Code</node> to examine.
     * @param doc JDOM
     * <code>Document</code> being built.
     */
    @SuppressWarnings("fallthrough")
    private void buildTree(JDOMFactory f, XMLStreamReader r, Document doc) throws XMLStreamException {
        Element current = null; // At top level
        int event = r.getEventType();

        // if we're at the start then we need to do a next
        if (event == -1) {
            event = r.next();
        }

        while (true) {
            boolean noadd = false;
            Content child = null;

            switch (event) {
                case XMLStreamConstants.CDATA:
                    child = f.cdata(r.getText());
                    break;

                case XMLStreamConstants.SPACE:
                    if (cfgIgnoreWS) {
                        noadd = true;
                        break;
                    }
                // fall through

                case XMLStreamConstants.CHARACTERS:
                    /*
                     * Small complication: although (ignorable) white space is
                     * allowed in prolog/epilog, and StAX may report such event,
                     * JDOM barfs if trying to add it. Thus, let's just ignore
                     * all textual stuff outside the tree:
                     */
                    if (current == null) {
                        noadd = true;
                        break;
                    }
                    child = f.text(r.getText());
                    break;

                case XMLStreamConstants.COMMENT:
                    child = f.comment(r.getText());

                    break;

                case XMLStreamConstants.END_DOCUMENT:
                    return;

                case XMLStreamConstants.END_ELEMENT:
                    /**
                     * If current.getParentElement() previously returned null
                     * and we get this event again we shouldn't bail out with a
                     * NullPointerException
                     */
                    if (current != null) {
                        current = current.getParentElement();
                    }
                    noadd = true;
                    if (isReadingMidStream && current == null) {
                        return;
                    }
                    break;

                case XMLStreamConstants.ENTITY_DECLARATION:

                case XMLStreamConstants.NOTATION_DECLARATION:
                    /*
                     * Shouldn't really get these, but maybe some stream readers
                     * do provide the info. If so, better ignore it -- DTD event
                     * should have most/all we need.
                     */
                    noadd = true;
                    break;

                case XMLStreamConstants.ENTITY_REFERENCE:
                    child = f.entityRef(r.getLocalName());
                    break;

                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    child = f.processingInstruction(r.getPITarget(), r.getPIData());
                    break;

                case XMLStreamConstants.START_ELEMENT: {
                    // Ok, need to add a new element and simulate recursion
                    Element newElem = null;
                    String nsURI = r.getNamespaceURI();
                    String elemPrefix = r.getPrefix(); // needed for special
                    // handling of elem's
                    // namespace
                    String ln = r.getLocalName();

                    if (nsURI == null || nsURI.length() == 0) {
                        if (elemPrefix == null || elemPrefix.length() == 0) {
                            newElem = f.element(ln);
                        } else {
                            /*
                             * Happens when a prefix is bound to the default
                             * (empty) namespace...
                             */
                            newElem = f.element(ln, elemPrefix, "");
                        }
                    } else {
                        newElem = f.element(ln, elemPrefix, nsURI);
                    }

                    /*
                     * Let's add element right away (probably have to do it to
                     * bind attribute namespaces, too)
                     */
                    if (current == null) { // at root
                        doc.setRootElement(newElem);
                        if (additionalNamespaces != null) {
                            for (Iterator iter = additionalNamespaces.keySet().iterator(); iter.hasNext();) {
                                String prefix = (String) iter.next();
                                String uri = (String) additionalNamespaces.get(prefix);

                                newElem.addNamespaceDeclaration(Namespace.getNamespace(prefix, uri));
                            }
                        }
                    } else {
                        f.addContent(current, newElem);
                    }

                    // Any declared namespaces?
                    int i;
                    int len;
                    for (i = 0, len = r.getNamespaceCount(); i < len; ++i) {
                        String prefix = r.getNamespacePrefix(i);
                        Namespace ns = Namespace.getNamespace(prefix, r.getNamespaceURI(i));
                        // JDOM has special handling for element's "own" ns:
                        if (prefix != null && prefix.equals(elemPrefix)) {
                            // already set by when it was constructed...
                        } else {
                            f.addNamespaceDeclaration(newElem, ns);
                        }
                    }

                    // And then the attributes:
                    for (i = 0, len = r.getAttributeCount(); i < len; ++i) {
                        String prefix = r.getAttributePrefix(i);
                        Namespace ns;

                        if (prefix == null || prefix.length() == 0) {
                            // Attribute not in any namespace
                            ns = Namespace.NO_NAMESPACE;
                        } else {
                            ns = newElem.getNamespace(prefix);

                        }
                        Attribute attr = f.attribute(r.getAttributeLocalName(i), r.getAttributeValue(i),
                                resolveAttrType(r.getAttributeType(i)), ns);
                        f.setAttribute(newElem, attr);
                    }
                    // And then 'push' new element...
                    current = newElem;

                    // Already added the element, can continue
                    noadd = true;
                    break;
                }
                case XMLStreamConstants.START_DOCUMENT:
                /*
                 * This should only be received at the beginning of document...
                 * so, should we indicate the problem or not?
                 */
                /*
                 * For now, let it pass: maybe some (broken) readers pass that
                 * info as first event in beginning of doc?
                 */

                case XMLStreamConstants.DTD:
                /*
                 * !!! Note: StAX does not expose enough information about
                 * doctype declaration (specifically, public and system id!);
                 * should (re-)parse information... not yet implemented
                 */
                // TBI
                // continue main_loop;
                // Should never get these, from a stream reader:
                     /*
                 * (commented out entries are just FYI; default catches them
                 * all)
                 */

                case XMLStreamConstants.ATTRIBUTE:
                case XMLStreamConstants.NAMESPACE:
                default:
                    /*
                     * throw new XMLStreamException("Unrecognized iterator event
                     * type: " + r.getEventType() + "; should not receive such
                     * types (broken stream reader?)");
                     */
                    break;
            }

            if (!noadd && child != null) {
                if (current == null) {
                    f.addContent(doc, child);
                } else {
                    f.addContent(current, child);
                }
            }

            if (r.hasNext()) {
                event = r.next();
            } else {
                break;
            }
        }
    }

    private static int resolveAttrType(String typeStr) {
        if (typeStr != null && typeStr.length() > 0) {
            Integer i = ATTR_TYPES.get(typeStr);
            if (i != null) {
                return i.intValue();
            }
        }
        return Attribute.UNDECLARED_TYPE;
    }
}
