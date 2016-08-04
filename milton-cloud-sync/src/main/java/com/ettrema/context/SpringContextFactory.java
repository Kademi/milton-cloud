package com.ettrema.context;

import java.io.File;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class SpringContextFactory implements Factory<ApplicationContext>{

    private static final Logger log = LoggerFactory.getLogger(CtxRootFolderFactory.class);

    private String appContext;
    private ApplicationContext springContext;
    Class[] classes = new Class[]{ApplicationContext.class};

    @Override
    public Class[] keyClasses() {
        return classes;
    }

    @Override
    public String[] keyIds() {
        return null;
    }

    @Override
    public Registration<ApplicationContext> insert(RootContext context, Context requestContext) {
        Registration<ApplicationContext> reg = context.put(springContext, this);
        return reg;
    }

    @Override
    public void init(RootContext context) {
        log.debug("app context is configured at: " + appContext);
        File fXml;
        if( appContext.startsWith("/") || appContext.contains(":")) {
            log.debug("app context path looks like an absolute path");
            fXml = new File(appContext);
        } else {
            log.debug("app context path looks relative, so resolving from rootFolder");
            File root = context.get("rootFolder");
            fXml = new File(root,appContext);
        }
        log.debug("absolute path: " + fXml.getAbsolutePath());
        springContext = new FileSystemXmlApplicationContext(appContext);
    }

    @Override
    public void destroy() {
        springContext = null;
    }

    @Override
    public void onRemove(ApplicationContext item) {
        
    }

}
