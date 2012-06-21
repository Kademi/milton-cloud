/*
 * Copyright 2012 McEvoy Software Ltd.
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
package io.milton.cloud.server.web;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.property.PropertySource;
import io.milton.resource.Resource;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

/**
 *
 * @author brad
 */
public class ParameterisedResourcePropertySource implements PropertySource {

    private String nsUri = "miltonx";

    @Override
    public Object getProperty(QName name, Resource r) throws NotAuthorizedException {
        if (r instanceof ParameterisedResource) {
            ParameterisedResource pr = (ParameterisedResource) r;
            try {
                return pr.getParam(name.getLocalPart());
            } catch (BadRequestException ex) {
                throw new RuntimeException(name.getLocalPart(), ex);
            }
        } else {
            throw new RuntimeException("Unsupported type: " + r);
        }

    }

    @Override
    public void setProperty(QName name, Object value, Resource r) throws PropertySetException, NotAuthorizedException {
        System.out.println("setProperty: " + name);
        if (r instanceof ParameterisedResource) {
            ParameterisedResource pr = (ParameterisedResource) r;
            try {
                String sValue = null;
                if( value != null ) {
                    sValue = value.toString();
                }
                pr.setParam(name.getLocalPart(), sValue);
            } catch (BadRequestException ex) {
                throw new RuntimeException(name.getLocalPart(), ex);
            }
        } else {
            throw new RuntimeException("Unsupported type: " + r);
        }
    }

    @Override
    public PropertyMetaData getPropertyMetaData(QName name, Resource r) {
        System.out.println("getPropertyMetaData: " + name);
        if (nsUri.equals(name.getNamespaceURI())) {
            if (r instanceof ParameterisedResource) {
                System.out.println(" --- yes");
                return new PropertyMetaData(PropertyAccessibility.WRITABLE, String.class);
            }
        }
        return PropertyMetaData.UNKNOWN;
    }

    @Override
    public void clearProperty(QName name, Resource r) throws PropertySetException, NotAuthorizedException {
        setProperty(name, null, r);
    }

    @Override
    public List<QName> getAllPropertyNames(Resource r) throws NotAuthorizedException, BadRequestException{
        if (r instanceof ParameterisedResource) {
            ParameterisedResource pr = (ParameterisedResource) r;
            List<QName> qnames = new ArrayList<>();
            List<String> list = pr.getParamNames();
            if( list != null ) {
                for( String s : list ) {
                    QName qname = new QName(nsUri, s);
                    qnames.add(qname);
                }
            }
            return qnames;
        } else {
            throw new RuntimeException("Unsupported type: " + r);
        }
    }
}
