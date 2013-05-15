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

/**
 *
 * @author brad
 */
public class Dimension {

    /**
     * Parse something like 1280x720
     * 
     * @param s
     * @return 
     */
    static Dimension parse(String s) {
        int i = s.indexOf("x");
        if( i < 0 ) {
            return null;            
        }
        int w = Integer.parseInt(s.substring(0, i));
        int h = Integer.parseInt(s.substring(i+1));
        return new Dimension(h, w);
    }
    
    private int height;
    private int width;

    public Dimension(int height, int width) {
        this.height = height;
        this.width = width;
    }
    
    
    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }
    

    @Override
    public String toString() {
        return width + "x" + height;
    }
    
    
    
}
