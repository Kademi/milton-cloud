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

import io.milton.cloud.process.ComparisonRule;
import io.milton.cloud.process.Expression;
import io.milton.cloud.server.mail.ExpressionFactory;
import java.util.Map;
import org.dom4j.Element;

/**
 *
 * @author brad
 */
public class ComparisonRuleFactory implements ExpressionFactory{

    @Override
    public ComparisonRule create(Element el, Map<String,ExpressionFactory> map) {
        String sOperator = el.attributeValue("operator");
        ComparisonRule.Operator op = ComparisonRule.Operator.valueOf(sOperator);
        Element leftEl = (Element) el.elements().get(0);
        Element rightEl = (Element) el.elements().get(1);
        Expression left = map.get(leftEl.getName()).create(leftEl, map);
        Expression right = map.get(rightEl.getName()).create(rightEl, map);
        return new ComparisonRule(op, left, right);
    }

    @Override
    public String getElementName() {
        return "comparison";
    }
    
}
