package com.bradmcevoy.version;

/**
 * Represents a means of accessing blocks of data for a file.
 *
 * @author brad
 */
public interface BlockStore {
    Block firstBlock();
    Block nextBlock(Block block);
}
