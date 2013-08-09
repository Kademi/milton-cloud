/*
 */
package io.milton.cloud.server.mail.rules;

import io.milton.cloud.process.Expression;
import io.milton.cloud.process.ListFilterExpression;
import io.milton.cloud.server.mail.ExpressionFactory;
import java.util.List;
import java.util.Map;
import org.dom4j.Element;

/**
 * Filter out elements of a list which do not match the bean property expression
 *
 * @author brad
 */
public class ListFilterExprFactory implements ExpressionFactory {

    public ListFilterExprFactory() {
    }


    @Override
    public Expression create(Element el, Map<String, ExpressionFactory> map) {
        String propName = el.attributeValue("property");
        String val = el.attributeValue("value");
        List childElements = el.elements();
        if( childElements == null || childElements.isEmpty()) {
            return new ListFilterExpression(propName, val, null);
        } else {
            Element elChild = (Element) childElements.get(0);
            Expression expr = map.get(elChild.getName()).create(elChild, map);
            return new ListFilterExpression(propName, val, expr);
        }
    }

    @Override
    public String getElementName() {
        return "listFilter";
    }


}
