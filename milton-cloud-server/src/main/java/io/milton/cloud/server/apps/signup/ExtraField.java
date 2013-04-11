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
package io.milton.cloud.server.apps.signup;

import io.milton.cloud.server.web.templating.Formatter;
import java.util.ArrayList;
import java.util.List;

import static io.milton.context.RequestContext._;

/**
 *
 * @author brad
 */
public class ExtraField {

    public static ExtraField parse(String name, String options) {
        ExtraField f = new ExtraField();
        f.name = name;
        if (options != null) {
            String[] arr = options.split(";");
            for (String s : arr) {
                s = s.trim();
                if( s.equalsIgnoreCase("required")) {
                    f.required = true;
                } else if( s.startsWith("text")) {
                    String[] arrText = s.split("=");
                    if( arrText.length > 1) {
                        f.text = arrText[1];
                    }
                } else if( s.startsWith("options")) {
                    f.options = new ArrayList<>();
                    s = s.replace("options", "");
                    s = s.replace("(", "");
                    s = s.replace(")", "");
                    s = s.trim();
                    for( String opt : s.split(",")) {
                        f.options.add(opt);
                    }
                }
            }
        }
        if( f.text == null ) {
            f.text = name;
        }
        return f;
    }
    private String name;
    private boolean required;
    private String text;
    private List<String> options;

    public String getName() {
        return name;
    }

    public String getText() {
        return text;
    }

    
    public List<String> getOptions() {
        return options;
    }

    public boolean isRequired() {
        return required;
    }
    
    public String getHtml() {
        Formatter formatter = _(Formatter.class);
        StringBuilder sb = new StringBuilder();
        if( options != null && !options.isEmpty()) {
            sb.append("<select");
            sb.append("name='").append(name).append("'");
            if( required) {
                sb.append(" class='required'");
            }
            sb.append(">");
            for( String opt : options) {
                sb.append( formatter.option(opt, opt, null) );
            }
            sb.append("</select>");
        } else {
            sb.append("<input");
            sb.append(" name='").append(name).append("'");
            if( required) {
                sb.append(" class='required'");
            }
            sb.append(" type='text'");
            sb.append("/>");
        }
        return sb.toString();
    }
}
