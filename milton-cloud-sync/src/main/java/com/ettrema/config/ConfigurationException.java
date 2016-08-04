package com.ettrema.config;


public class ConfigurationException extends Exception {
    private static final long serialVersionUID = 1L;
    
    public ConfigurationException(String msg) {
        super(msg);
    }
    
    public ConfigurationException(String context,  Exception cause) {
        super(context,cause);
    }
    
}
