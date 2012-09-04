/*
 * Copyright 2012 McEvoy Software Ltd.
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
package io.milton.sync.event;

import io.milton.event.Event;

/**
 *
 * @author brad
 */
public class TransferProgressEvent implements Event {

    private final long bytesRead;
    private final Long totalBytes;
    private final String fileName;

    public TransferProgressEvent(long bytesRead, Long totalBytes, String fileName) {
        this.bytesRead = bytesRead;
        this.totalBytes = totalBytes;
        this.fileName = fileName;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public String getFileName() {
        return fileName;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public int getPercent() {
        if (totalBytes > 0) {
            return (int)(bytesRead * 100 / totalBytes);
        } else {
            return 0;
        }
    }
}
