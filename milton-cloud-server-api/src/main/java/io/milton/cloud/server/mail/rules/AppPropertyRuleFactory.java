/*
 * Copyright 2013 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

import io.milton.cloud.process.Expression;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.PropertyProviderApplication;
import io.milton.cloud.server.mail.ExpressionFactory;
import java.util.List;
import java.util.Map;
import org.dom4j.Element;

/**
 *
 * @author brad
 */
public class AppPropertyRuleFactory implements ExpressionFactory {

    private final ApplicationManager applicationManager;

    public AppPropertyRuleFactory(ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    @Override
    public AppPropertyExpression create(Element el, Map<String, ExpressionFactory> map) {
        String appId = el.attributeValue("app-id");
        String propName = el.attributeValue("prop-name");
        Application app = applicationManager.get(appId);
        if (app != null) {
            if (app instanceof PropertyProviderApplication) {
                PropertyProviderApplication papp = (PropertyProviderApplication) app;
                PropertyProviderApplication.ApplicationProperty expr = papp.getProperty(propName);
                if (expr != null) {
                    Expression child = findChild(el, map);
                    return new AppPropertyExpression(expr, child);
                } else {
                    throw new RuntimeException("Couldnt find property: " + propName + " in app " + appId);
                }
            } else {
                throw new RuntimeException("App " + appId + "  is not a compatiable app");
            }
        } else {
            throw new RuntimeException("Not an application: " + appId);
        }
    }

    @Override
    public String getElementName() {
        return "app-prop";
    }

    private Expression findChild(Element parentElement, Map<String, ExpressionFactory> map) {
        List elements = parentElement.elements();
        if (elements != null) {
            for (Object o : elements) {
                if( o instanceof Element ) {
                    Element el = (Element) o;
                    ExpressionFactory fac = map.get(el.getName());
                    Expression rule = fac.create(el, map);
                    return rule;
                }
            }
        }
        return null;
    }
}
