package io.milton.cloud.server.web;

import io.milton.cloud.server.apps.orgs.OrganisationFolder;
import io.milton.cloud.server.apps.website.ManageWebsiteFolder;
import io.milton.cloud.server.apps.website.ManageWebsitesFolder;
import io.milton.http.Cookie;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import java.util.List;

/**
 * Used for redirecting to content resources
 *
 *
 * @author brad
 */
public abstract class ContentRedirectorPage extends TemplatedHtmlPage {

    public static void select(BranchFolder f) {
        selectItem(f);
        if (f.getParent() instanceof ManageWebsiteFolder) {
            ManageWebsiteFolder w = (ManageWebsiteFolder) f.getParent();
            select(w);
        }
    }

    public static void select(ManageWebsiteFolder f) {
        selectItem(f);
    }

    /**
     * Select the given resource as the current child of its parent. This will
     * write a cookie associated with the parent with the child's name
     *
     * @param r
     */
    public static void selectItem(CommonCollectionResource r) {
        Response resp = HttpManager.response();
        if (resp == null) {
            return;
        }
        if (r.getParent() != null) {
            String cookieName = cookieName(r.getParent());
            String cookieValue = r.getName();
            resp.setCookie(cookieName, cookieValue);
        }
    }

    /**
     * The name of the cookie to store the current child of the given collection
     * in
     *
     * @param w
     * @return
     */
    public static String cookieName(CommonCollectionResource w) {
        String s = "current_" + w.getPath().toString("_");
        return s;
    }
    private Request _request;

    public ContentRedirectorPage(String name, CommonCollectionResource parent, String template, String title) {
        super(name, parent, template, title);
    }

    protected Request request() {
        if (_request == null) {
            _request = HttpManager.request();
        }
        return _request;
    }

    /**
     * Return the name of the current child, if any, for the given parent item
     *
     * @param parentItem
     * @return
     */
    protected String cookie(CommonCollectionResource parentItem) {
        String cookieName = cookieName(parentItem);
        
        Cookie c = request().getCookie(cookieName);
        if (c == null) {
            return null;
        }
        return c.getValue();
    }

    protected <T extends CommonResource> T getSelected(CommonCollectionResource parentItem, List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        String childName = cookie(parentItem);
        if (childName != null) {
            for (T r : list) {
                if (childName.equals(r.getName())) {
                    return r;
                }
            }
        }
        return list.get(0);
    }

    protected BranchFolder getBranch() throws NotAuthorizedException, BadRequestException {
        BranchFolder branchFolder = (BranchFolder) this.closest("branch");
        if (branchFolder != null) {
            return branchFolder;
        }
        ManageWebsiteFolder websiteFolder = getWebsite();
        if (websiteFolder == null) {
            return null;
        }
        List<BranchFolder> list = websiteFolder.getBranchFolders();
        if (list == null || list.isEmpty()) {
            return null;
        }
        String currentBranchName = cookie(websiteFolder);
        if (currentBranchName != null) {
            for (BranchFolder r : list) {
                if (currentBranchName.equals(r.getName())) {
                    return r;
                }
            }
        }
        return websiteFolder.getLive();
    }

    protected ManageWebsiteFolder getWebsite() throws NotAuthorizedException, BadRequestException {
        ManageWebsiteFolder websiteFolder = (ManageWebsiteFolder) this.closest("website");
        if (websiteFolder != null) {
            return websiteFolder;
        }
        OrganisationFolder orgFolder = getOrganisationFolder();
        ManageWebsitesFolder websitesFolder = (ManageWebsitesFolder) orgFolder.child("websites");
        List<ManageWebsiteFolder> websites = websitesFolder.getWebsiteFolders();
        ManageWebsiteFolder w = getSelected(websitesFolder, websites);
        return w;
    }

    protected OrganisationFolder getOrganisationFolder() {
        OrganisationFolder orgFolder = WebUtils.findParentOrg(this);
        return orgFolder;
    }
}
