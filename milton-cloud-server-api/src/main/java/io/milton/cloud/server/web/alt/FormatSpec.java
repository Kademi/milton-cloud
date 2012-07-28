/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.web.alt;

/**
 *
 * @author brad
 */
public class FormatSpec {
    final String inputType; // general type, eg "video" or "image"
    final String type; // eg flv, png
    final int height;
    final int width;
    private String[] converterArgs;

    public FormatSpec(String inputType, String type, int width, int height, String ... converterArgs) {
        this.inputType = inputType;
        this.type = type;
        this.height = height;
        this.width = width;
        this.converterArgs = converterArgs;
    }

    public int getHeight() {
        return height;
    }

    public String getOutputType() {
        return type;
    }

    public int getWidth() {
        return width;
    }

    public String getInputType() {
        return inputType;
    }

    public String[] getConverterArgs() {
        return converterArgs;
    }

    public void setConverterArgs(String[] converterArgs) {
        this.converterArgs = converterArgs;
    }
    
    
    
        
    /**
     * Combines type, width and height, eg
     *
     * 100-150.png
     *
     * @return
     */
    public String getName() {
        return width + "-" + height + "." + type;
    }

    @Override
    public String toString() {
        return getName();
    }
    
    
    
}
