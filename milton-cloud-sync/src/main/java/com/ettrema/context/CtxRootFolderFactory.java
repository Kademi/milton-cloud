package com.ettrema.context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CtxRootFolderFactory implements Factory<File> {
    private static final Logger log = LoggerFactory.getLogger(CtxRootFolderFactory.class);
    private File file;    
    private static String[] keys = {"rootFolder"};
    
    public static final String KEY_ROOT_FOLDER = "rootFolder";
    
    public CtxRootFolderFactory() {
        log.debug("Created: CtxRootFolderFactory");
    }

    public Class[] keyClasses() {
        return null;
    }

    public String[] keyIds() {
        return keys;
    }

    public Registration<File> insert(RootContext context, Context requestContext) {        
        Registration<File> reg = (Registration<File>)context.put(file,this);
        return reg;
    }

    public void init(RootContext context) {
        log.debug("CtxRootFolderFactory.init");
        File configFile = (File)context.get("configFile");
        if( configFile == null ) throw new RuntimeException("Could not find config file in context. Should have been added earlier");
        log.debug( "configFile: " + configFile.getAbsolutePath());
        file = configFile.getParentFile();
        context.put("rootFolder",file);
        File propsFile = new File(file, "environment.properties");
        loadEnvironmentProperties(context,propsFile);            
    }

    private void loadEnvironmentProperties(RootContext context, final File propsFile) throws RuntimeException {
        log.debug("loadEnvironmentProperties");
        if( !propsFile.exists() ) {
            log.warn("properties file does not exist: " + propsFile.getAbsolutePath());
            return ;
        }
        final Properties props = new Properties();
        FileInputStream fin = null;
        try {            
            fin = new FileInputStream(propsFile);
            props.load(fin);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }     finally {
            close(fin);
        }
        for( String s : props.stringPropertyNames() ) {
            String v = props.getProperty(s);
//            debug("Registering property: " + s + " = " + v );
            context.put(s, v);
        }
    }

    private void close(InputStream in) {
        if( in == null ) return ;
        try {
            in.close();
        } catch (IOException ex) {
            log.warn("exception closing stream",ex);
        }
    }
           
    
    public void destroy() {
    }

    public void onRemove(File item) {
    }
    
}
