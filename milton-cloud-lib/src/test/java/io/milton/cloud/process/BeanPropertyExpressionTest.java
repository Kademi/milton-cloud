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
package io.milton.cloud.process;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author brad
 */
public class BeanPropertyExpressionTest {

    public BeanPropertyExpressionTest() {
    }

    @Test
    public void testSomeMethod() {
        BeanPropertyExpression expr = new BeanPropertyExpression("attributes.customer.addresses[1].addressLine1");
        ProcessContext context = new ProcessContext();
        Customer c = new Customer();
        c.setAddresses(new Address[2]);
        c.getAddresses()[0] = new Address("a line1", "a line2");
        c.getAddresses()[1] = new Address("b line1", "b line2");
        context.addAttribute("customer", c);
        String s = (String) expr.eval(context);
        assertEquals("b line1", s);
    }

    public class Customer {
        private Address[] addresses;

        public Address[] getAddresses() {
            return addresses;
        }

        public void setAddresses(Address[] addresses) {
            this.addresses = addresses;
        }
        
        
    }

    public class Address {

        private String addressLine1;
        private String addressLine2;

        public Address() {
        }

        public Address(String addressLine1, String addressLine2) {
            this.addressLine1 = addressLine1;
            this.addressLine2 = addressLine2;
        }
        
        
        public String getAddressLine1() {
            return addressLine1;
        }

        public void setAddressLine1(String addressLine1) {
            this.addressLine1 = addressLine1;
        }

        public String getAddressLine2() {
            return addressLine2;
        }

        public void setAddressLine2(String addressLine2) {
            this.addressLine2 = addressLine2;
        }
        
        
    }
}