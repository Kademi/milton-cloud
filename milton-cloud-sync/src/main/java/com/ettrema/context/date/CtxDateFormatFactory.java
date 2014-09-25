package com.ettrema.context.date;

import com.ettrema.context.Context;
import com.ettrema.context.Factory;
import com.ettrema.context.Registration;
import com.ettrema.context.RootContext;
import java.text.DateFormat;

public class CtxDateFormatFactory implements Factory<DateFormat> {
    
    public static Class[] classes = {DateFormat.class};
    
    public CtxDateFormatFactory() {
    }

    public Class[] keyClasses() {
        return classes;
    }

    public String[] keyIds() {
        return null;
    }

    public Registration<DateFormat> insert(RootContext context, Context requestContext) {
        DateFormat d = DateFormat.getDateInstance(DateFormat.SHORT);
        Registration<DateFormat> reg = requestContext.put(d,this);
        return reg;
    }

    public void init(RootContext context) {
    }

    public void destroy() {
    }

    public void onRemove(DateFormat item) {
    }
    
}
