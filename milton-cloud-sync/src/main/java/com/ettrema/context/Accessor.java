package com.ettrema.context;

public interface Accessor extends Dependent{
    public void preExecute( Context context );
    public void postExecute( Context context );
}
