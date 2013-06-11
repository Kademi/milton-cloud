/*
 */
package io.milton.cloud.server.apps.user.rules;

import io.milton.cloud.process.ProcessContext;
import io.milton.cloud.server.apps.PropertyProviderApplication.ApplicationProperty;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.util.Date;
import java.util.List;

/**
 *
 * @author brad
 */
public class ListSizeApplicationProperty implements ApplicationProperty<Integer> {

    @Override
    public String getName() {
        return "listSize";
    }

    @Override
    public String getDescription() {
        return "The number of items in a list";
    }

    @Override
    public Class getSourceClass() {
        return List.class;
    }

    @Override
    public Integer eval(ProcessContext context) {
        List arg = context.get(List.class);
        if( arg == null ) {
            return 0;
        }
        return arg.size();
    }
}
