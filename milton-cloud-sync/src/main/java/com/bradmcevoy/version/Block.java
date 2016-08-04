package com.bradmcevoy.version;

/**
 * Represents a block of data in a versioned file's block map
 *
 * @author brad
 */
public interface Block {
    long getBlockSize();

    int getChecksum();
}
