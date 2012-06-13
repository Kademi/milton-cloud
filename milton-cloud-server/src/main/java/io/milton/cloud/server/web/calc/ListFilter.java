package io.milton.cloud.server.web.calc;

import io.milton.resource.Resource;
import io.milton.cloud.server.web.ResourceList;
import io.milton.cloud.server.web.SpliffyResource;

/**
 *
 * @author brad
 */
public class ListFilter implements Accumulator {

    ResourceList dest = new ResourceList();

    @Override
    public void accumulate( SpliffyResource r, Object o ) {
        if( o instanceof Boolean ) {
            Boolean b = (Boolean) o;
            if( b.booleanValue() ) {
                dest.add( r );
            }
        }
    }
}
