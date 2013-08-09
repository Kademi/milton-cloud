/*
 */
package io.milton.cloud.server.mail.rules;

import io.milton.cloud.process.ContainsRule;
import io.milton.cloud.process.Expression;
import io.milton.cloud.server.mail.ExpressionFactory;
import java.util.Map;
import org.dom4j.Element;

/**
 * 
 *
 * @author brad
 */
public class ContainsRuleFactory implements ExpressionFactory {

    public ContainsRuleFactory() {
    }


    @Override
    public Expression create(Element el, Map<String, ExpressionFactory> map) {
        String propName = el.attributeValue("property");
        String val = el.attributeValue("value");
            return new ContainsRule(propName, val);
    }

    @Override
    public String getElementName() {
        return "contains";
    }


}
