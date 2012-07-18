package io.milton.cloud.server.scratch;


import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 * @author brad
 */
public class Scratch {

    

    public static void main(String[] args) throws Exception {
        String fuseHome = "/home/brad/proj/fuse-admin";
        
        URL[] urls = {new URL("file://" + fuseHome + "/fuse-app/target/classes/"), new URL("file://" + fuseHome + "/fuse-war/target/classes/")};
        URLClassLoader c = new URLClassLoader(urls);
        
        Object t = c.loadClass("com.fuselms.apps.autoload.Autoloader");
        System.out.println("t: " + t);
        
        Object runner = c.loadClass("com.fuselms.scratch.ScratchRunner").newInstance();
        Method m = runner.getClass().getMethod("start", String.class);
        m.invoke(runner, fuseHome);
    }
    
}
