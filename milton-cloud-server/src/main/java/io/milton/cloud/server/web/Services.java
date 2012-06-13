package io.milton.cloud.server.web;

import io.milton.cloud.server.web.templating.Templater;
import org.hashsplit4j.api.BlobStore;
import org.hashsplit4j.api.HashStore;
import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.manager.CurrentDateService;
import io.milton.cloud.server.manager.DefaultCurrentDateService;
import io.milton.cloud.server.manager.ResourceManager;
import io.milton.cloud.server.manager.ShareManager;
import io.milton.cloud.server.web.templating.Formatter;
import io.milton.cloud.server.web.templating.HtmlTemplateParser;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.cloud.server.web.templating.TextTemplater;

/**
 *
 * @author brad
 */
public class Services {

    private final HashStore hashStore;
    private final BlobStore blobStore;
    private final Templater htmlTemplater;
    private final Templater textTemplater;
    private final SpliffySecurityManager securityManager;
    private final ResourceManager resourceManager;
    private final ShareManager shareManager;
    private final ApplicationManager applicationManager;
    private HtmlTemplateParser templateParser;
    private final CurrentDateService currentDateService;

    public Services(HashStore hashStore, BlobStore blobStore, SpliffySecurityManager securityManager, ResourceManager resourceManager, ShareManager shareManager, ApplicationManager applicationManager) {
        this.hashStore = hashStore;
        this.blobStore = blobStore;        
        this.securityManager = securityManager;
        this.resourceManager = resourceManager;
        this.shareManager = shareManager;
        this.applicationManager = applicationManager;
        templateParser = new HtmlTemplateParser();
        this.textTemplater = new TextTemplater(securityManager);
        currentDateService = new DefaultCurrentDateService(); // todo: make pluggable to support testing
        this.htmlTemplater = new HtmlTemplater(applicationManager, new Formatter(currentDateService), securityManager);
    }

    
    public BlobStore getBlobStore() {
        return blobStore;
    }

    public HashStore getHashStore() {
        return hashStore;
    }

    public Templater getHtmlTemplater() {
        return htmlTemplater;
    }

    public SpliffySecurityManager getSecurityManager() {
        return securityManager;
    }


    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public ShareManager getShareManager() {
        return shareManager;
    }

    public HtmlTemplateParser getTemplateParser() {
        return templateParser;
    }

    public ApplicationManager getApplicationManager() {
        return applicationManager;
    }

    public Templater getTextTemplater() {
        return textTemplater;
    }

    public CurrentDateService getCurrentDateService() {
        return currentDateService;
    }
    
}
