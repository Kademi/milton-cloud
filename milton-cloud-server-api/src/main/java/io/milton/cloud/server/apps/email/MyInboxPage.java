/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.email;

import io.milton.cloud.server.web.CommonCollectionResource;
import io.milton.cloud.server.web.TemplatedHtmlPage;
import java.util.Map;


/**
 *
 * @author brad
 */
public class MyInboxPage extends TemplatedHtmlPage {

    public MyInboxPage(String name, CommonCollectionResource parent) {
        super(name, parent, "email/myInbox", "My inbox");
    }

    @Override
    protected Map<String, Object> buildModel(Map<String, String> params) {
        Map<String, Object> map = super.buildModel(params);
        return map;
    }
    

    
}
