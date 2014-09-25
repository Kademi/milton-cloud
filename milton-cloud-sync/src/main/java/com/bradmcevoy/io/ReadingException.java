package com.bradmcevoy.io;

import java.io.IOException;

public class ReadingException extends IOException{
    public ReadingException(IOException cause) {
        super(cause);
    }
}
