package io.milton.context;

public interface Factory<T> extends RemovalCallback<T> {
    public Class[] keyClasses();
    public String[] keyIds();    
    public Registration<T> insert(RootContext context, Context requestContext);    
    public void init( RootContext context);
    void destroy();
}
