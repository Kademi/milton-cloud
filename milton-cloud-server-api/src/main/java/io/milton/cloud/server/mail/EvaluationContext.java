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

import java.util.HashMap;
import java.util.Map;

/**
 * This is just to hold the source script, and the compiled version of that script.
 * 
 * We dont know what the compiled form might be, so just an object.
 *
 * @author brad
 */
public class EvaluationContext {
    private final String script;
    private Object compiledScript;
    private Map<String,Object> attributes = new HashMap<>();

    public EvaluationContext(String script) {
        this.script = script;
    }

    public String getScript() {
        return script;
    }      

    public Object getCompiledScript() {
        return compiledScript;
    }

    public void setCompiledScript(Object compiledScript) {
        this.compiledScript = compiledScript;
    }

    
    
    public Map<String, Object> getAttributes() {
        return attributes;
    }       
}
