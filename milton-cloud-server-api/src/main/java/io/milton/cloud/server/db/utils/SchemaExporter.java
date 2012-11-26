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
package io.milton.cloud.server.db.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

/**
 *
 * @author brad
 */
public class SchemaExporter {

    private static enum Dialect {

        ORACLE("org.hibernate.dialect.Oracle10gDialect"),
        POSTGRES("org.hibernate.dialect.PostgreSQLDialect"),
        MYSQL("org.hibernate.dialect.MySQLDialect"),
        HSQL("org.hibernate.dialect.HSQLDialect");
        private String dialectClass;

        private Dialect(String dialectClass) {
            this.dialectClass = dialectClass;
        }

        public String getDialectClass() {
            return dialectClass;
        }
    }
    private final List<String> packageNames;
    private final File outputDir;

    public SchemaExporter(List<String> packageNames, File outputDir) {
        this.outputDir = outputDir;
        this.packageNames = packageNames;
    }

    /**
     * Method that actually creates the file.
     *
     * @param dbDialect to use
     */
    public void generate() throws Exception {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        AnnotationConfiguration cfg;
        cfg = new AnnotationConfiguration();
        cfg.setProperty("hibernate.hbm2ddl.auto", "create");
        cfg.setNamingStrategy(new org.hibernate.cfg.ImprovedNamingStrategy());

        for (String packageName : packageNames) {
            for (Class<Object> clazz : getClasses(packageName)) {
                cfg.addAnnotatedClass(clazz);
            }
        }
        List<File> outFiles = new ArrayList<>();
        for (Dialect d : Dialect.values()) {
            cfg.setProperty("hibernate.dialect", d.getDialectClass());
            SchemaExport export = new SchemaExport(cfg);
            export.setDelimiter(";");
            File fOut = new File(outputDir, d.name().toLowerCase() + ".sql");
            export.setOutputFile(fOut.getAbsolutePath());
            export.execute(true, false, false, false);
            outFiles.add(fOut);
        }
        System.out.println("**********************************");
        for (File f : outFiles) {
            System.out.println("   exported: " + f.getAbsolutePath());
        }
        System.out.println("**********************************");
    }

    /**
     * Utility method used to fetch Class list based on a package name.
     *
     * @param packageName (should be the package containing your annotated
     * beans.
     */
    private List<Class> getClasses(String packageName) throws Exception {
        return getClassNamesFromPackage(packageName);
    }

    public File getOutputDir() {
        return outputDir;
    }

    public static List<Class> getClassNamesFromPackage(String packageName) throws IOException, ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL;
        ArrayList<Class> classes = new ArrayList<>();

        String packagePath = packageName.replace(".", "/");
        packageURL = classLoader.getResource(packagePath);

        ClassLoader cld = Thread.currentThread().getContextClassLoader();

        if (packageURL.getProtocol().equals("jar")) {
            String jarFileName;
            JarFile jf;
            Enumeration<JarEntry> jarEntries;
            String entryName;

            // build jar file name, then loop through zipped entries
            jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
            jarFileName = jarFileName.substring(5, jarFileName.indexOf("!"));
            System.out.println(">" + jarFileName);
            jf = new JarFile(jarFileName);
            jarEntries = jf.entries();
            while (jarEntries.hasMoreElements()) {
                entryName = jarEntries.nextElement().getName();
                if (entryName.startsWith(packagePath) && entryName.length() > packagePath.length() + 5) {
                    if (entryName.endsWith(".class")) {
                        System.out.println("entryName: " + entryName);
                        //entryName = entryName.substring(packageName.length()+1, entryName.lastIndexOf('.'));
                        String className = entryName.replace("/", ".");
                        className = className.substring(0, className.length() - 6);
                        System.out.println("classname: " + className);
                        Class c = cld.loadClass(className);
                        classes.add(c);
                    }
                }
            }

            // loop through files in classpath
        } else {
            File directory = new File(packageURL.getFile());
            String[] files = directory.list();
            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith(".class")) {
                    classes.add(Class.forName(packageName + '.' + files[i].substring(0, files[i].length() - 6)));
                }
            }            
        }
        return classes;
    }
}
