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
    
    
    public enum SeekUnit {
        SECS,
        PERC
    }
    
    final String inputType; // general type, eg "video" or "image"
    final String type; // eg flv, png
    final int height;
    final int width;
    final boolean cropToFit;
    final Integer seekAmount;
    final SeekUnit seekUnit;
    private String[] converterArgs;
    private final String name;
    
    /**
     * For formats where height and width are not relevant, such as m3u8
     * 
     * @param inputType
     * @param type 
     */
    public FormatSpec(String inputType, String type) {
        this.inputType = inputType;
        this.type = type;
        height = -1;
        width = -1;
        cropToFit = false;
        this.seekAmount = null;
        this.seekUnit = null;
        name = buildName();
    }
    
    public FormatSpec(String inputType, String type, int width, int height, boolean cropToFit, String ... converterArgs) {
        this.inputType = inputType;
        this.type = type;
        this.height = height;
        this.width = width;
        this.cropToFit = cropToFit;
        this.converterArgs = converterArgs;
        this.seekAmount = null;
        this.seekUnit = null;        
        name = buildName();
    }

    public FormatSpec(String inputType, String type, int width, int height, boolean cropToFit, SeekUnit seekUnit, Integer seekAmount, String ... converterArgs) {
        this.inputType = inputType;
        this.type = type;
        this.height = height;
        this.width = width;
        this.cropToFit = cropToFit;
        this.converterArgs = converterArgs;
        this.seekAmount = seekAmount;
        this.seekUnit = seekUnit;
        name = buildName();
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

    public Integer getSeekAmount() {
        return seekAmount;
    }

    public SeekUnit getSeekUnit() {
        return seekUnit;
    }
    
    
    
        
    /**
     * Combines type, width and height, eg
     *
     * 100-150.png
     *
     * @return
     */
    public final String buildName() {
        StringBuilder sb = new StringBuilder();
        sb.append(width+"").append("-");
        sb.append(height+"");
        if( seekUnit != null && seekAmount != null ) {
            sb.append("-").append(seekAmount.toString()).append(seekUnit);
        }
        sb.append(".").append(type);        
        return sb.toString();
    }

    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return getName();
    }
    
    
    
}
