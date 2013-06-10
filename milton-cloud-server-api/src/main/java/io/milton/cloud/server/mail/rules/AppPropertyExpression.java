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
import io.milton.cloud.process.ProcessContext;
import io.milton.context.Registration;

/**
 * Wraps a ApplicationProperty which will be the value of this expression, unless
 * there is also a child. If there is a child it will be evaulated with the 
 * parent value in context
 *
 * @author brad
 */
public class AppPropertyExpression implements Expression {

    private final Expression parent;
    private final AppPropertyExpression child;

    public AppPropertyExpression(Expression parent, AppPropertyExpression child) {
        this.parent = parent;
        this.child = child;
    }
    
    
    @Override
    public Object eval(ProcessContext context) {
        Object parentVal = parent.eval(context);
        if (parentVal != null) {
            if (child != null) {
                Registration<Object> reg = context.put(parentVal);
                try {
                    Object v = child.eval(context);
                    return v;
                } finally {
                    reg.remove();
                }
            }
        }
        return parentVal;
    }
}
