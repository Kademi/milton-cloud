
package io.milton.cloud.process;

import java.io.Serializable;

public abstract class AbstractRule implements Rule, Serializable {
    
    private static final long serialVersionUID = 1L;

    public boolean toBool(Object o) {
        if (o == null) {
            return false;
        } else if (o instanceof Boolean) {
            return (Boolean) o;
        } else if (o instanceof Integer) {
            Integer i = (Integer) o;
            return i.intValue() == 0;
        } else if (o instanceof String) {
            String s = (String) o;
            s = s.toLowerCase();
            return s.equals("true") || s.equals("yes");
        } else {
            throw new RuntimeException("Unsupported boolean type: " + o.getClass());
        }

    }

}
