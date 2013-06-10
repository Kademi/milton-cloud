/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.mail.rules;

import io.milton.cloud.process.AndRule;
import io.milton.cloud.process.Expression;
import io.milton.cloud.process.Rule;
import io.milton.cloud.server.mail.ExpressionFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.dom4j.Element;

/**
 *
 * @author brad
 */
public class AndRuleFactory implements ExpressionFactory {

    public AndRuleFactory() {
    }

    
    
    @Override
    public Rule create(Element el, Map<String, ExpressionFactory> map) {
        List<Rule> rules = new ArrayList<>();
        for (Object oChild : el.elements()) {
            Element elChild = (Element) oChild;
            Expression expr = map.get(elChild.getName()).create(elChild, map);
            if (expr instanceof Rule) {
                rules.add((Rule) expr);
            } else {
                throw new RuntimeException("Not a Rule: " + expr);                            
            }
        }
        return new AndRule(rules);
    }

    @Override
    public String getElementName() {
        return "and";
    }
}
