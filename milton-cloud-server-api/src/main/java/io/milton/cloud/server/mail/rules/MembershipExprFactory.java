/*
 */
package io.milton.cloud.server.mail.rules;

import io.milton.cloud.process.Expression;
import io.milton.cloud.server.mail.ExpressionFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.dom4j.Element;

/**
 * Filter out elements of a list which do not match the bean property expression
 *
 * @author brad
 */
public class MembershipExprFactory implements ExpressionFactory {

    public MembershipExprFactory() {
    }


    @Override
    public Expression create(Element el, Map<String, ExpressionFactory> map) {
        String groupNames = el.attributeValue("group");
        List<String> list = new ArrayList<>();
        for( String s : groupNames.split(" ")) {
            list.add(s);
        }
        List childElements = el.elements();
        if( childElements == null || childElements.isEmpty()) {
            return new MembershipExpression(list, null);
        } else {
            Element elChild = (Element) childElements.get(0);
            Expression expr = map.get(elChild.getName()).create(elChild, map);
            return new MembershipExpression(list, expr);
        }
    }

    @Override
    public String getElementName() {
        return "membership";
    }


}
