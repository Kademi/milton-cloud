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
package io.milton.cloud.server.web.alt;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author brad
 */
public class ProportionTest {

    public ProportionTest() {
    }

    @Test
    public void testSomeMethod() {
        Proportion p = new Proportion(650, 366, 640, 360);
        System.out.println("w=" + p.getConstrainedWidth());
        System.out.println("h=" + p.getContrainedHeight());
    }
    
    @Test
    public void testToEven1() {
        int i = Proportion.toEvenInt(2.0);
        assertEquals(2, i);
    }
    
    @Test
    public void testToEven2() {
        int i = Proportion.toEvenInt(2.1);
        assertEquals(2, i);
    }

    @Test
    public void testToEven3() {
        int i = Proportion.toEvenInt(2.9);
        assertEquals(2, i);
    }

    @Test
    public void testToEven4() {
        int i = Proportion.toEvenInt(3.0);
        assertEquals(2, i);
    }

    @Test
    public void testToEven5() {
        int i = Proportion.toEvenInt(3.9);
        assertEquals(4, i);
    }

    
}