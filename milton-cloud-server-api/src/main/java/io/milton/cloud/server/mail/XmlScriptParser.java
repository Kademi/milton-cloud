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
package io.milton.cloud.server.mail;

import io.milton.cloud.process.Expression;
import io.milton.cloud.process.Rule;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.mail.rules.AndRuleFactory;
import io.milton.cloud.server.mail.rules.AppPropertyRuleFactory;
import io.milton.cloud.server.mail.rules.ComparisonRuleFactory;
import io.milton.cloud.server.mail.rules.ConstantExprFactory;
import io.milton.cloud.server.mail.rules.ContainsRuleFactory;
import io.milton.cloud.server.mail.rules.ListFilterExprFactory;
import io.milton.cloud.server.mail.rules.MembershipExprFactory;
import io.milton.cloud.server.mail.rules.OrRuleFactory;
import io.milton.cloud.server.mail.rules.TrueRuleFactory;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 *
 * @author brad
 */
public class XmlScriptParser implements ScriptParser {

    private final Map<String, ExpressionFactory> map = new HashMap<>();

    public XmlScriptParser(ApplicationManager applicationManager) {
        this();
        add(new AppPropertyRuleFactory(applicationManager));
    }

    public XmlScriptParser(List<ExpressionFactory> extraFactories) {
        this();
        for (ExpressionFactory ef : extraFactories) {
            add(ef);
        }

    }

    public XmlScriptParser() {
        add(new TrueRuleFactory());
        add(new AndRuleFactory());
        add(new OrRuleFactory());
        add(new ConstantExprFactory());
        add(new ComparisonRuleFactory());
        add(new ListFilterExprFactory());
        add(new ContainsRuleFactory());
        add(new MembershipExprFactory());        
    }

    @Override
    public void parse(EvaluationContext context) {
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(new ByteArrayInputStream(context.getScript().getBytes("UTF-8")));
            Element root = document.getRootElement();
            ExpressionFactory fac = map.get(root.getName());
            Expression rule = fac.create(root, map);
            if (!(rule instanceof Rule)) {
                throw new RuntimeException("Expression is not a Rule: " + rule.getClass());
            }
            context.setCompiledScript(rule);
        } catch (UnsupportedEncodingException | DocumentException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void add(ExpressionFactory rf) {
        map.put(rf.getElementName(), rf);
    }
}
