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
package io.milton.cloud.process;

import java.util.List;

/**
 *
 * @author brad
 */
public class AndRule implements Rule {

    private List<Rule> rules;

    public AndRule(List<Rule> rules) {
        this.rules = rules;
    }
    
    
    
    @Override
    public Boolean eval(ProcessContext context) {
        for( Rule r : rules ) {
            boolean result = r.eval(context);
            if( !result ) {
                return false;
            }
        }
        return true;
    }
    
}
