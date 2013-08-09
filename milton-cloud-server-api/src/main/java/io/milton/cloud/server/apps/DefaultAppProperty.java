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
package io.milton.cloud.server.apps;

import io.milton.cloud.process.ProcessContext;
import io.milton.cloud.server.apps.PropertyProviderApplication.ApplicationProperty;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;

/**
 *
 * @author brad
 */
public class DefaultAppProperty implements PropertyProviderApplication.ApplicationProperty{

    public static void add(Map<String,PropertyProviderApplication.ApplicationProperty> mapOfProperties, String name, String desc, Class c) {
        DefaultAppProperty p = new DefaultAppProperty(name, desc, c);
        add(mapOfProperties, p);
        
    }
    
    public static void add(Map<String,PropertyProviderApplication.ApplicationProperty> mapOfProperties, ApplicationProperty prop) {
        mapOfProperties.put(prop.getName(), prop);
    }
    
    private String name;
    private String description;
    private Class sourceClass;

    public DefaultAppProperty(String name, String description, Class sourceClass) {
        this.name = name;
        this.description = description;
        this.sourceClass = sourceClass;
    }



    public DefaultAppProperty() {
    }
        
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class getSourceClass() {
        return sourceClass;
    }

    public void setSourceClass(Class sourceClass) {
        this.sourceClass = sourceClass;
    }

    
    
    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Object eval(ProcessContext context) {
        Object val = null;
        try {
            Class c = getSourceClass();
            if( context == null ) {
                throw new RuntimeException("No context");
            }
            if( c == null ) {
                throw new RuntimeException("no class");
            }
            Object source = context.get(c);
            if( source == null ) {
                throw new RuntimeException("Source not found in context: " + c);
            }
            val = PropertyUtils.getProperty(source, getName());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new RuntimeException(getName(), ex);
        }
        return val;
    }

    
}
