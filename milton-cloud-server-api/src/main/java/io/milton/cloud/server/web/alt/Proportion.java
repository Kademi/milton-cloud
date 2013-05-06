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
 * Eg: Given an image of these dimensions 2592h x 3888w, and lets say we
 * want that to fit into a bounding box of 52x52..
 *
 * First we determine which is the contrained dimension. In this case the
 * target ratio (width/height) is 1, and the actual ration is 0.666
 *
 * Because the actualRatio is less then targetRation we need to constrain
 * the width, ie we will set the width to 52 and allow the height to be
 * whatever maintains the actual ration = 52 x 0.666 = 34.6
 *
 * Because avconv/ffmpeg will only scale on the smaller of the dimensions,
 * we must tell it to scale height to 34.6, and allow it to adjust the width
 *
 */
public class Proportion {
    
    public static int toEvenInt(double d) {
        return (int) toEven(d);
    }
    
    public static long toEven(double d) {
        long i = Math.round(d);
        if( i % 2 == 0) {
            return i;
        } else {
            return i -1;
        }
    }
    
    double targetRatio;
    double actualRatio;
    double maxWidth;
    double maxHeight;
    double origHeight;
    double origWidth;

    public Proportion(double width, double height, double maxWidth, double maxHeight) {
        targetRatio = maxWidth / maxHeight;
        actualRatio = width / height;
        origHeight = height;
        origWidth = width;
        this.maxHeight = maxHeight;
        this.maxWidth = maxWidth;
    }

    public boolean scaleByHeight() {
        return actualRatio > targetRatio;
    }

    public double getMaxHeight() {
        return maxHeight;
    }

    public double getMaxWidth() {
        return maxWidth;
    }
        
    public boolean scaleByWidth() {
        return !scaleByHeight();
    }

    public int getConstrainedWidth() {
        if (scaleByHeight()) {
            return toEvenInt(maxWidth);
        } else {
            return toEvenInt(actualRatio * maxHeight);
        }
    }

    public int getContrainedHeight() {
        if (scaleByWidth()) {
            return toEvenInt(maxHeight);
        } else {
            return toEvenInt(maxWidth / actualRatio);
        }
    }

    @Override
    public String toString() {
        return "orig " + origHeight + "h x" + origWidth + "w -scaleByHeight=" + scaleByHeight() + " -->> " + getContrainedHeight() + "h x " + getConstrainedWidth() + "w";
    }
    
    
}
