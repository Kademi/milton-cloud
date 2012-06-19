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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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

    private String packageName;
    
    private File outputDir;
    
    public SchemaExporter(String packageName)  {
        this.packageName = packageName;
    }

    /**
     * Method that actually creates the file.
     *
     * @param dbDialect to use
     */
    public void generate() throws Exception {
        if( !outputDir.exists()) {
            outputDir.mkdirs();
        }
        AnnotationConfiguration cfg;
        cfg = new AnnotationConfiguration();
        cfg.setProperty("hibernate.hbm2ddl.auto", "create");
        cfg.setNamingStrategy(new org.hibernate.cfg.ImprovedNamingStrategy());

        for (Class<Object> clazz : getClasses(packageName)) {
            cfg.addAnnotatedClass(clazz);
        }
        for (Dialect d : Dialect.values()) {
            cfg.setProperty("hibernate.dialect", d.getDialectClass());
            SchemaExport export = new SchemaExport(cfg);
            export.setDelimiter(";");
            File fOut = new File(outputDir, d.name().toLowerCase() + ".sql");
            export.setOutputFile(fOut.getAbsolutePath());
            export.execute(true, false, false, false);
        }
    }

    /**
     * Utility method used to fetch Class list based on a package name.
     *
     * @param packageName (should be the package containing your annotated
     * beans.
     */
    private List<Class> getClasses(String packageName) throws Exception {
        List<Class> classes = new ArrayList<>();
        File directory = null;
        try {
            ClassLoader cld = Thread.currentThread().getContextClassLoader();
            if (cld == null) {
                throw new ClassNotFoundException("Can't get class loader.");
            }
            String path = packageName.replace('.', '/');
            URL resource = cld.getResource(path);
            if (resource == null) {
                throw new ClassNotFoundException("No resource for " + path);
            }
            String sFile = resource.getFile();
            sFile = sFile.replace("test-classes", "classes");
            directory = new File(sFile);
            
        } catch (NullPointerException x) {
            throw new ClassNotFoundException(packageName + " (" + directory
                    + ") does not appear to be a valid package");
        }

        if (directory.exists()) {
            String[] files = directory.list();
            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith(".class")) {
                    classes.add(Class.forName(packageName + '.' + files[i].substring(0, files[i].length() - 6)));
                }
            }
        } else {
            throw new ClassNotFoundException(packageName + " is not a valid package");
        }
        System.out.println("getClasses: " + packageName + " -> " + directory.getAbsolutePath() + " = " + classes.size());
        return classes;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }
    
    
}
