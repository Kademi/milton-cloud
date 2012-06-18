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
package io.milton.vfs;

import io.milton.vfs.data.HashCalc;
import io.milton.cloud.common.Triplet;
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
    public void testCalcHash_NoItems() {
        ArrayList<Triplet> list = new ArrayList<>();
        long hash = hashCalc.calcHash(list); 
        assertEquals(1, hash);
    }
    
    @Test
    public void testCalcHash_OneItem() {
        ArrayList<Triplet> list = new ArrayList<>();
        Triplet t = new Triplet();
        t.setName("a");
        t.setType("f");
        t.setHash(1);
        list.add(t);
        long hash = hashCalc.calcHash(list);
        System.out.println("hash: " + hash);
        assertEquals(95814007, hash);
    }
    
}
