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
package io.milton.cloud.server.web.templating;

import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.web.RootFolder;
import io.milton.common.Path;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.resource.Resource;
import io.milton.vfs.db.Profile;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author brad
 */
public class MenuItem implements Comparable<MenuItem> {
    
    public static void setActiveId(String id) {
        Set<String> set = getActiveMenuIdSet();
        set.add(id);
    }
    
    public static void setActiveIds(String ... ids) {
        Set<String> set = getActiveMenuIdSet();
        for( String id : ids ) {
            set.add(id);
        }
    }
    
    public static boolean isActiveId(String id) {
        return getActiveMenuIdSet().contains(id);
    }
    
    public static Set<String> getActiveMenuIdSet() {
        Request req = HttpManager.request();
        if( req == null ) {
            return new HashSet<>();
        }
        Set<String> set = (Set<String>) HttpManager.request().getAttributes().get("activeMenuIds");
        if( set == null ) {
            set = new HashSet<>();
            HttpManager.request().getAttributes().put("activeMenuIds", set);
        }
        return set;
    }
    
    private final String id;
    private String text;
    private String href;
    private int ordering = 100;
    private MenuItemList items;
    
    private final ApplicationManager applicationManager;
    private final Resource r;
    private final Profile user;
    private RootFolder rootFolder;

    public MenuItem(String id, ApplicationManager applicationManager, Resource r, Profile user, RootFolder rootFolder) {
        this.id = id;
        this.applicationManager = applicationManager;
        this.r = r;
        this.user = user;
        this.rootFolder = rootFolder;
    }

    /**
     * Build a sub-list containing only active menu items
     * 
     * @return 
     */
    public MenuItemList getActiveItems() {
        MenuItemList list = new MenuItemList();
        for( MenuItem mi : getItems() ) {
            if( mi.isActive()) {
                list.add(mi);
            }
        }
        return list;
    }
    
    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    public int getOrdering() {
        return ordering;
    }

    public void setOrdering(int ordering) {
        this.ordering = ordering;
    }
    
    

    /**
     * @return the href
     */
    public String getHref() {
        if( href == null ) {
            // get from children
            if( !getItems().isEmpty()) {
                href = getItems().get(0).getHref();
            }
        } else if( href.length() == 0 ) {
            return "/";
        }
        return href;
    }

    /**
     * @param href the href to set
     */
    public void setHref(String href) {
        this.href = href;
    }

    /**
     * Applications indicate current active id's by callign MenuItem.setActiveId
     * when the resource is located.
     * 
     * @return 
     */
    public boolean isActive() {
        return isActiveId(id);
    }

 
    

    /**
     * @return the items
     */
    public MenuItemList getItems() {
        if( items == null ) {
            items = new MenuItemList();
            applicationManager.appendMenu(this);
            Collections.sort(items);
        }
        return items;
    }

    public ApplicationManager getApplicationManager() {
        return applicationManager;
    }

    public MenuItem add(String id) {
        MenuItem m = new MenuItem(id, applicationManager, r, user, rootFolder);
        items.add(m);
        return m;
    }

    public MenuItem getOrCreate(String id, String text) {
        return getOrCreate(id, text, (String)null);
    }
    
    
    public MenuItem getOrCreate(String id, String text, Path p) {
        return getOrCreate(id, text, p.toString());
    }
    
    public MenuItem getOrCreate(String id, String text, String href) {
        for( MenuItem i : getItems()  ) {
            if( i.getId().equals(id)) {
                return i;
            }
        }
        MenuItem i = add(id);
        i.setHref(href);
        i.setText(text);
        return i;
    }
    
    public MenuItem add(OrganisationFolder parentOrg, String resourceName, String text ) {
        Path p = parentOrg.getPath().child(resourceName);
        return add(p, "menu" + resourceName, text);
    }
    
    public MenuItem add(Path path, String id, String text ) {
        MenuItem mi = add(id);
        mi.setHref(path.toString());
        mi.setText(text);        
        return mi;
    }

    public Resource getResource() {
        return r;
    }

    public RootFolder getRootFolder() {
        return rootFolder;
    }

    public Profile getUser() {
        return user;
    }

    @Override
    public int compareTo(MenuItem o) {
        if( o == null ) {
            return 1;
        } else {
            if( o.ordering == ordering) {
                return id.compareTo(o.id);
            } else {
                return Integer.compare(ordering, o.ordering);
            }
        }
    }

    
    
    
    
}
