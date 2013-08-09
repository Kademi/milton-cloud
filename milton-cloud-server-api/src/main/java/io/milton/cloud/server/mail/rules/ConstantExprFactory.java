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

import io.milton.cloud.process.ConstantExpression;
import io.milton.cloud.process.Expression;
import io.milton.cloud.server.mail.ExpressionFactory;
import java.util.Map;
import org.dom4j.Element;

/**
 *
 * @author brad
 */
public class ConstantExprFactory implements ExpressionFactory {

    public ConstantExprFactory() {
    }
    
    
    @Override
    public Expression create(Element el, Map<String, ExpressionFactory> map) {
        String s = el.getStringValue();
        String type = el.attributeValue("type");
        Object c;
        switch (type) {
            case "Integer":
                c = Integer.parseInt(s);
                break;
            case "Long":
                c = Long.parseLong(s);
                break;
            case "Float":
                c = Float.parseFloat(s);
                break;
            case "Double":
                c = Double.parseDouble(s);
                break;
            default:
                c = s;
        }
        return new ConstantExpression(c);
    }

    @Override
    public String getElementName() {
        return "const";
    }
}
