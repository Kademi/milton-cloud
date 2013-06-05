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
package io.milton.cloud.process;

import java.lang.reflect.InvocationTargetException;
import org.apache.commons.beanutils.PropertyUtils;

/**
 *
 * @author brad
 */
public class BeanPropertyExpression implements Expression {

    private String beanPath;

    public BeanPropertyExpression(String beanPath) {
        this.beanPath = beanPath;
    }

    @Override
    public Object eval(ProcessContext context) {
        Object val = null;
        try {
            val = PropertyUtils.getProperty(context, beanPath);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new RuntimeException(beanPath, ex);
        }
        return val;
    }

    public String getBeanPath() {
        return beanPath;
    }
}
