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

import io.milton.cloud.process.AndRule;
import io.milton.cloud.process.ComparisonRule;
import io.milton.cloud.process.OrRule;
import io.milton.cloud.process.Rule;
import io.milton.cloud.process.TrueRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author brad
 */
public class XmlScriptParserTest {
    
    public XmlScriptParserTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of parse method, of class XmlScriptParser.
     */
    @Test
    public void testParse_TrueRule() {
        String xml = "<true />";
        EvaluationContext context = new EvaluationContext(xml);
        XmlScriptParser instance = new XmlScriptParser();
        instance.parse(context);
        Object oRule = context.getCompiledScript();
        assertNotNull(oRule);
        Rule r = (Rule) oRule;
        System.out.println("r=" + r);
        assertTrue(r instanceof TrueRule);
    }
    
    @Test
    public void testParse_And() {
        String xml = "<and><true/><true/></and>";
        EvaluationContext context = new EvaluationContext(xml);
        XmlScriptParser instance = new XmlScriptParser();
        instance.parse(context);
        Object oRule = context.getCompiledScript();
        assertNotNull(oRule);
        Rule r = (Rule) oRule;
        assertTrue(r instanceof AndRule);
    }    
    
    @Test
    public void testParse_Or() {
        String xml = "<or><true/><true/></or>";
        EvaluationContext context = new EvaluationContext(xml);
        XmlScriptParser instance = new XmlScriptParser();
        instance.parse(context);
        Object oRule = context.getCompiledScript();
        assertNotNull(oRule);
        Rule r = (Rule) oRule;
        assertEquals(OrRule.class, r.getClass());
    }  
    
    @Test
    public void testParse_Combo() {
        String xml = "<comparison operator='EQUALS'><const type='Integer'>7</const><const type='Integer'>7</const></comparison>";
        EvaluationContext context = new EvaluationContext(xml);
        XmlScriptParser instance = new XmlScriptParser();
        instance.parse(context);
        Object oRule = context.getCompiledScript();
        assertNotNull(oRule);
        Rule r = (Rule) oRule;
        assertEquals(ComparisonRule.class, r.getClass());
    }     
}