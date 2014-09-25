package com.bradmcevoy.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkingInputStream extends InputStream{
    private static final Logger log = LoggerFactory.getLogger(ChunkingInputStream.class);
    final ChunkStore store;
    
    private int chunkNum;
    private int pos = -1;
    private byte[] currentChunk;
    
    public ChunkingInputStream(ChunkStore store) {
        this.store = store;
        this.currentChunk = store.getChunk(chunkNum++);
    }

    @Override
    public int read() throws IOException {
        if( currentChunk == null ) return -1;
        pos++;
        if( pos >= currentChunk.length ) { // if first, or have exhausted current, get next chunk
            currentChunk = store.getChunk(chunkNum++);
            if( currentChunk == null ) return -1;   // EOF. no more data
            pos = 0;
        }        
        int i = currentChunk[pos] & 0xff;
        return i;
    }
    
    public void writeTo(OutputStream out) throws IOException {
        long t = System.currentTimeMillis();
        boolean done = (currentChunk == null || currentChunk.length==0);
        while( !done ) {
            out.write(currentChunk);
            log.debug("chunk: " + chunkNum + " - " + currentChunk.length);
            
            currentChunk = store.getChunk(chunkNum++);
            done = (currentChunk == null || currentChunk.length==0);            
        }
        t = System.currentTimeMillis() - t;
        log.debug("transmission time: " + t + "ms");
    }    
//    public synchronized int read(byte b[], int off, int len) {
//        log.debug("read-" + off + " - " + len);
//        log.debug(" chunk: " + chunkNum + "  size:" + currentChunk.length);
//	if (b == null) {
//	    throw new NullPointerException();
//	} else if (off < 0 || len < 0 || len > b.length - off) {
//	    throw new IndexOutOfBoundsException();
//	}
//        
//        int nextLen = 0;
//        int thisLen = len;
//        if( thisLen+off > currentChunk.length-1 ) {
//            nextLen = off + thisLen - currentChunk.length - 1;
//            thisLen = currentChunk.length - off;
//            log.debug("   nextLen: " + nextLen);
//            log.debug("   thisLen: " + thisLen);
//        }
//        log.debug("arraycopy " + currentChunk.length + "," + pos + "," + b.length + "," + off + "," + thisLen);
//	System.arraycopy(currentChunk, pos, b, off, thisLen);
//	pos += thisLen;
//        int nextBytes = 0;
//        if( nextLen > 0 ) {
//            nextBytes = read(b,off+thisLen,nextLen);
//        }
//	return len;
//    }    


}
