package com.ettrema.config;

import com.ettrema.context.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogConfigurator {
    private static final Logger log = LoggerFactory.getLogger( CatalogConfigurator.class );
    
    public RootContext load( FactoryCatalog c, File configFile, List<Object> initialContents ) {
        log.info("loading config at: " + configFile.getAbsolutePath());
        loadConfig(c,configFile);
        RootContext context = new RootContext(c, initialContents);
        return context;                
    }
    
    public RootContext load( FactoryCatalog c, File configFile ) {
        log.info("loading config at: " + configFile.getAbsolutePath());
        loadConfig(c,configFile);
        RootContext context = new RootContext(c);        
        return context;        
    }
    
    public void loadConfig( FactoryCatalog c, File configFile ) {
        if( !configFile.exists() ) throw new IllegalArgumentException("Config file does not exist: " + configFile.getAbsolutePath());
        try {
            c.setConfigFile(configFile);
            Document doc = loadXml(configFile);
            String parent = doc.getRootElement().getAttributeValue("parent");
            if( parent != null && parent.length() > 0 ) {
                File file = resolveRelativePath(configFile.getParentFile(),parent);
                load( c, file );
            }
            loadKeys(doc, c);
            loadFactories(doc, c);
        } catch (ConfigurationException ex) {
            log.error("Exception loading config file: " + configFile,ex);
            throw new RuntimeException(configFile.getAbsolutePath() + ". See logs for original exception",ex);
        }        
    }
    
    protected File resolveRelativePath(File start, String path) {
        String[] arr = path.split("/");
        File f = start;
        for( String s : arr ) {
            if( s.equals("..") ) {
                f = f.getParentFile();
            } else {
                f = new File(f,s);
            }
        }
        return f;
    }

    private Document getDomDocument(File configFile) throws FileNotFoundException, JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        return builder.build(configFile);
    }

    static Map<String, String> loadArgs(Element el) {
        Map<String, String> map = new HashMap<String, String>();
        for( Object oElChild : el.getChildren("arg")) {
            Element elChild = (Element) oElChild;
            String name = elChild.getAttributeValue("name");
            String val = elChild.getValue();
            map.put(name, val);
        }
        return map;
    }

    private void loadFactories(final Document doc, final FactoryCatalog c) throws ConfigurationException  {
        String pack = doc.getRootElement().getAttributeValue("package");
        for( Object oEl : doc.getRootElement().getChildren("factory") ) {
            Element el = (Element) oEl;
            String className = el.getAttributeValue("class");
            Map<String,String> args = loadArgs(el);
            log.info("loading factory: " + className);
            if( className.startsWith(".") ) className = pack + className;
            Class clazz = loadClass(className);
            String config = el.getAttributeValue("config");
            Factory factory = loadFactory(config, clazz, args);
            c.addFactory(factory);
        }
    }

    private Factory loadFactory(final String config, final Class clazz,Map<String,String> args) throws ConfigurationException {
        Factory factory;
        try {
            if( isPresent(config) ) {
                Constructor con = loadConstructor(clazz);
                factory = (Factory)con.newInstance(config);
            } else {
                factory = (Factory)clazz.newInstance();
            }
            for( Map.Entry<String,String> entry : args.entrySet()  ) {
                String name = entry.getKey();
                String value = entry.getValue();
                BeanUtils.setProperty(factory, name, value);
            }
            return factory;
        } catch (IllegalArgumentException ex) {
            throw new ConfigurationException("class:" + clazz.getName() + " config:" + config,ex);
        } catch (ConfigurationException ex) {
            throw new ConfigurationException("class:" + clazz.getName() + " config:" + config,ex);
        } catch (InvocationTargetException ex) {
            throw new ConfigurationException("class:" + clazz.getName() + " config:" + config,ex);
        } catch (IllegalAccessException ex) {
            throw new ConfigurationException("class:" + clazz.getName() + " config:" + config,ex);
        } catch (InstantiationException ex) {
            throw new ConfigurationException("class:" + clazz.getName() + " config:" + config,ex);
        }
    }

    private Constructor loadConstructor(final Class clazz) throws ConfigurationException{
        try {
            Constructor con = clazz.getConstructor(String.class);
            return con;
        } catch (SecurityException ex) {
            throw new ConfigurationException(clazz.getName(),ex);
        } catch (NoSuchMethodException ex) {
            throw new ConfigurationException(clazz.getName(),ex);
        }
    }

    private Class loadClass(final String className) throws ConfigurationException {
        try {
            Class clazz = Class.forName(className);
            return clazz;
        } catch (ClassNotFoundException ex) {
            throw new ConfigurationException(className,ex);
        }
    }

    private void loadKeys(final Document doc, final FactoryCatalog c) throws ConfigurationException {        
        int i = 0;
        for( Object oEl : doc.getRootElement().getChildren("key") ) {
            Element el = (Element) oEl;
            i++;
            String name = el.getAttributeValue("name");
            log.info("loading key: " + name);
            if( !isPresent(name) ) throw new ConfigurationException("No name supplied. Ordinal:" + i);
            String value = el.getAttributeValue("value");
            if( !isPresent(value) ) throw new ConfigurationException("No value supplied for key:" + name + ". Ordinal:" + i);
            c.addKey(name,value);
        }
    }

    private Document loadXml(final File configFile) throws ConfigurationException {
        try {
            Document doc = getDomDocument(configFile);
            return doc;
        } catch (JDOMException ex) {
            throw new RuntimeException(configFile.getAbsolutePath(), ex);
        } catch (FileNotFoundException ex) {
            throw new ConfigurationException(configFile.getAbsolutePath(),ex);        
        } catch (IOException ex) {
            throw new RuntimeException(configFile.getAbsolutePath(), ex);
        }
    }
    
    protected boolean isPresent(String s) {
        return (s!=null && s.length()>0);
    }
}
