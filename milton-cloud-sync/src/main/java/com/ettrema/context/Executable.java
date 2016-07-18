package com.ettrema.context;

public interface Executable<T> {
    public T execute( Context context );    
}
