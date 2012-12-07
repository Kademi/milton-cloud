/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.web.templating;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean2;
import org.apache.commons.beanutils.converters.DateTimeConverter;

/**
 *
 * @author brad
 */
public class DataBinder {

    private final BeanUtilsBean bub;
    //private String[] dateFormats = {"dd/MM/yy", "dd/MM/yyyy", "dd/MM/yyyy HH:mm", "dd/MM/yy HH:mm"};
    private String[] dateFormats = {"dd/MM/yyyy"};

    public DataBinder() {
        ConvertUtilsBean2 convertUtilsBean = new ConvertUtilsBean2();
        DateTimeConverter dtConverter = new NullSafeDateTimeConverter();
        dtConverter.setPatterns(dateFormats);
        convertUtilsBean.register(dtConverter, Date.class);

        bub = new BeanUtilsBean(convertUtilsBean);
    }

    public void populate(Object bean, Map properties) throws IllegalAccessException, InvocationTargetException {
        // need to cater for check boxes which send no value if not set
        // we use a convention that every check has a hidden input with name=name_checkbox
        // see Formatter.checkbox for details
        List keys = new ArrayList(properties.keySet());
        for(Object key : keys) {
            String k = key.toString();
            if( k.endsWith("_checkbox")) {
                String propName = k.replace(Formatter.CHECKBOX_SUFFIX, "");
                if( !properties.containsKey(propName)) {
                    properties.put(propName, "");
                }
            }
        }
        bub.populate(bean, properties);
    }

    public class NullSafeDateTimeConverter extends DateTimeConverter {

        @Override
        protected Object handleMissing(Class type) {
            return null;
        }

        @Override
        protected Class getDefaultType() {
            return Date.class;
        }
    }
}
