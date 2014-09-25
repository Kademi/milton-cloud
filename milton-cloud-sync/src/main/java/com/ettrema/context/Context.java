package com.ettrema.context;


import java.util.HashMap;

/** Basic context functionality. Use either a RootContext or RequestContext
 */
public abstract class Context implements Contextual { 
    
    HashMap<Class,Registration> itemByClass = new HashMap<Class,Registration>();
    HashMap<String,Registration> itemByName = new HashMap<String,Registration>();
    
    /** If creating, the item is inserted into the given context
     *
     *  Should only be used when a child is referring to a parent, so that
     *  the 
     */
    abstract Registration getOrCreateRegistration(Class c, Context context);
    

    /** If creating, the item is inserted into the given context
     */
    abstract Registration getOrCreateRegistration(String id, Context context);
        
    /** Used by subclasses
     */
    protected Context() {
    }
    
    public int numItemsById() {
        return itemByName.size();
    }
    
    public <T> T get(String id) {
        Registration<T> reg = getRegistration(id);
        return get( reg );
    }
    
    
    public <T> T get(Class<T> c) { 
        Registration<T> reg = getRegistration(c);
        return get( reg );
    }
    
    private <T> T get(Registration<T> reg) { 
        if( reg == null ) return null;
        return  reg.item;
    }
    
    public int getInt(String id) {
        Object o = get(id);
        if( o == null ) {
            throw new IllegalArgumentException("No config value found for: " + id + ". Should be an integer");
        } else {
            if( o instanceof String ) {
                String s = (String) o;
                return Integer.parseInt(s);
            } else {
                if( o instanceof Integer ) {
                    Integer ii = (Integer) o;
                    return ii;
                } else {
                    String s = o.toString();
                    return Integer.parseInt(s);
                }
            }
        }               
    }
    
    public boolean getBoolean(String id) {
        Object o = get(id);
        if( o == null ) {
            throw new IllegalArgumentException("No config value found for: " + id + ". Should be a boolean");
        } else {
            if( o instanceof String ) {
                String s = (String) o;
                return Boolean.parseBoolean(s);
            } else {
                if( o instanceof Integer ) {
                    Integer ii = (Integer) o;
                    return ii==1;
                } else {
                    String s = o.toString();
                    return Boolean.parseBoolean(s);
                }
            }
        }               

    }
    
    <T> Registration<T> getRegistration(Class<T> c) {
        Registration<T> reg = itemByClass.get(c);
        return reg;
    }
        
    <T> Registration<T> getRegistration(String id) {
        Registration<T> o = itemByName.get(id);
        return o;
    }    
    
    /** Place o into context, keying by the given id
     */
    public <T> Registration<T> put( String id, T o ) {
        return put( id, o, null );
    }

    
    /** Place the given object into context, keying only by its class
     */
    public <T> Registration<T> put( T o ) {
        return put( o, null );
    }
    
    public <T> Registration<T> put( T o, RemovalCallback f ) {
        if( o == null ) throw new NullPointerException("o is null");
        Registration<T> reg = new Registration<T>(o,f,this);
        register( o.getClass(), o, reg );
        return reg;
    }
    
    
    /** Put the given object into context, keying only the given id
     */
    public <T> Registration<T> put( String id, T o, Factory f ) {
        Registration<T> reg = new Registration<T>(o,f,this);
        reg.addKey(id);
        itemByName.put(id,reg);
        return reg;
    }
    
    private void register(Class c, Object o, Registration reg ) {
        if( c == null ) return ;
        if( c == Object.class ) return ;        
        itemByClass.put(c,reg);
        reg.addKey(c);
        for( Class i : c.getInterfaces() ) {
            if( !reg.contains(i) ) {
                register( i, o, reg );
            }
        }
        register( c.getSuperclass(), o, reg );
    }            
    
    protected void debug(Object o) {
        System.out.println(o + " - " + this.getClass().getName());
    }

    public void tearDown() {
        itemByClass = null;
        itemByName = null;        
    }
}
