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
package io.milton.cloud.server.apps.website;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author brad
 */
public class LessParameterParserTest {
    
    LessParameterParser parser;

    @Before
    public void setup() {
        parser = new LessParameterParser();
    }
    
    @Test
    public void testParse() throws UnsupportedEncodingException, IOException {
        String s = "@text2: #464646;\n" +
                    "@hero1: #0d7ea5;";
        ByteArrayInputStream in = new ByteArrayInputStream(s.getBytes("UTF-8"));
        Map<String,String> map = new HashMap<>();
        parser.findParams(in, map);
        assertEquals(2, map.size());
        String hero1Val = map.get("hero1");
        assertEquals("#0d7ea5", hero1Val);
        String text2Val = map.get("text2");
        assertEquals("#464646", text2Val);
    }
}
