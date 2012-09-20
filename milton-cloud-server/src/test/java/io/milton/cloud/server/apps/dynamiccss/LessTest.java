/*
 * Copyright 2012 McEvoy Software Ltd.
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
package io.milton.cloud.server.apps.dynamiccss;

import com.asual.lesscss.LessEngine;
import com.asual.lesscss.LessException;
import org.junit.Before;
import org.junit.Test;

/**
 * Just runs LESS to see how it works
 *
 * @author brad
 */
public class LessTest {
    
    LessEngine engine = new LessEngine();
        
    @Test
    public void test_simple() throws LessException {
        long tm = System.currentTimeMillis();
        
        String text = engine.compile("div { width: 1 + 1 }");
        System.out.println("text: " + text);
        org.junit.Assert.assertTrue(text.contains("2"));
        System.out.println("completed in: " + (System.currentTimeMillis()-tm) + "ms");
    }
    
    @Test
    public void test_vars() throws LessException {
        long tm = System.currentTimeMillis();
        LessEngine engine = new LessEngine();
        String css = "@nice-blue: #5B83AD;\n" +
                        "@light-blue: @nice-blue + #111;\n" +
                        "#header { color: @light-blue; }";
        String text = engine.compile(css);
        System.out.println("text: " + text);        
        org.junit.Assert.assertTrue(text.contains("color: #6c94be;"));
        System.out.println("completed in: " + (System.currentTimeMillis()-tm) + "ms");        
    }
}
