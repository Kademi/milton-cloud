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
package io.milton.cloud.common;

import io.milton.cloud.common.HashCalc;
import io.milton.cloud.common.Triplet;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author brad
 */
public class HashCalcTest {

    HashCalc hashCalc;


    @Before
    public void setup() {
        hashCalc = new HashCalc();
    }

    /**
     * Test of calcHash method, of class HashCalc.
     */
    @Test
    public void testCalcHash_NoItems() throws IOException {
        ArrayList<Triplet> list = new ArrayList<>();
        String hash = hashCalc.calcHash(list); 
        assertEquals("be1bdec0aa74b4dcb079943e70528096cca985f8", hash);
    }
    
    @Test
    public void testCalcHash_OneItem() throws IOException {
        ArrayList<Triplet> list = new ArrayList<>();
        Triplet t = new Triplet();
        t.setName("a");
        t.setType("f");
        t.setHash("1");
        list.add(t);
        String hash = hashCalc.calcHash(list);
        System.out.println("hash: " + hash);
        assertEquals("27f807e686f714a769b6bdd52f848446e8565b59", hash);
    }
    
}
