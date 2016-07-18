package com.bradmcevoy.utils;

import java.util.ArrayList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ElementList extends ArrayList<Element> {
    
    public ElementList(NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if( n instanceof Element ) {
                add( (Element)n );
            }
        }
    }    
}
