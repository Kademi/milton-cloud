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
package io.milton.cloud.server.web;

import io.milton.cloud.server.web.ExtraField;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author brad
 */
public class ExtraFieldTest {
    
    public ExtraFieldTest() {
    }

    @Test
    public void testSomeMethod() {
        ExtraField f = ExtraField.parse("suburb", "required;text=Your suburb ");
        assertEquals("suburb", f.getName());
        assertEquals("Your suburb", f.getText());
        assertTrue(f.isRequired());
        assertTrue(f.getOptions() == null);
    }
}