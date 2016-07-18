package com.bradmcevoy.io;

import java.io.IOException;

public class WritingException extends IOException{
    public WritingException(IOException cause) {
        super(cause);
    }
}
