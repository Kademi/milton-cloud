package com.bradmcevoy.utils;

import com.bradmcevoy.io.StreamToStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    private static void close( Reader reader ) {
        if( reader == null ) return ;
        try {
            reader.close();
        } catch(Exception e) {
            
        }
    }

    public static void copy(File source, File dest) {
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            int i = is.read();
            while (i >= 0) {
                os.write(i);
                i = is.read();
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            close(is);
            close(os);
        }
    }

    public static ByteArrayOutputStream readIn(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        StreamToStream.readTo(is, os, true, true);
        return os;
    }

    public static String readResource(Class cl, String res) throws IOException {
        InputStream in = cl.getResourceAsStream(res);
        ByteArrayOutputStream out = readIn(in);
        return out.toString();
    }

    public static void close(InputStream in) {
        try {
            in.close();
        } catch (IOException ex) {
            log.warn("Exception closing input stream", ex);
        }
    }

    public static InputStream openFile(File file) throws FileNotFoundException {
        FileInputStream fin = null;
        BufferedInputStream br = null;
        fin = new FileInputStream(file);
        br = new BufferedInputStream(fin);
        return br;
    }

    public static OutputStream openFileForWrite(File file) throws FileNotFoundException {
        FileOutputStream fout = new FileOutputStream(file);
        BufferedOutputStream bout = new BufferedOutputStream(fout);
        return bout;
    }

    public static Object useFile(File file, StreamReader exec) throws FileNotFoundException, Exception {
        InputStream in = null;
        try {
            in = openFile(file);
            return exec.use(in);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception ex) {
            throw ex;
        } finally {
            close(in);
        }
    }

    public static Object useFile(File file, StreamWriter exec) throws FileNotFoundException, Exception {
        OutputStream out = null;
        try {
            out = openFileForWrite(file);
            return exec.use(out);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception ex) {
            throw ex;
        } finally {
            close(out);
        }
    }

    public static String readFile(File file) throws FileNotFoundException {
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(file);
            br = new BufferedReader(fr);
            StringBuffer sb = new StringBuffer();
            String s = null;
            while ((s = br.readLine()) != null) {
                sb.append(s);
                sb.append("\n");
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            close(br);
            close(fr);
        }
    }

    public static String read(InputStream in) {
        try {
            BufferedInputStream bin = new BufferedInputStream(in);
            int s;
            byte[] buf = new byte[1024];
            StringBuffer sb = new StringBuffer();
            while ((s = bin.read()) > -1) {
                char ch = (char) s;
                sb.append(ch);
            }
            return sb.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static File resolveRelativePath(File start, String path) {
        String[] arr = path.split("/");
        File f = start;
        for (String s : arr) {
            if (s.equals("..")) {
                f = f.getParentFile();
            } else {
                f = new File(f, s);
            }
        }
        return f;
    }

    public static String getExtension(File file) {
        return getExtension(file.getName());
    }

    public static String getExtension(String nm) {
        if (nm.indexOf(".") >= 0) {
            String[] arr = nm.split("[.]");
            return arr[arr.length - 1];
        } else {
            return null;
        }
    }

    public static String stripExtension(String nm) {
        if (nm.indexOf(".") >= 0) {
            String[] arr = nm.split("[.]");
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < arr.length - 1; i++) {
                if (i != 0) {
                    sb.append(".");
                }
                sb.append(arr[i]);
            }
            return sb.toString();
        } else {
            return null;
        }
    }

    public static String preprendExtension(String filename, String newExt) {
        String ext = getExtension(filename);
        filename = stripExtension(filename);
        filename = filename + "." + newExt + "." + ext;
        return filename;
    }

    public static String incrementFileName(String name, boolean isFirst) {
        String mainName = stripExtension(name);
        String ext = getExtension(name);
        int count;
        if (isFirst) {
            count = 1;
        } else {
            int pos = mainName.lastIndexOf("(");
            if (pos > 0) {
                String sNum = mainName.substring(pos + 1, mainName.length() - 1);
                count = Integer.parseInt(sNum) + 1;
                mainName = mainName.substring(0, pos);
            } else {
                count = 1;
            }
        }
        return mainName + "(" + count + ")." + ext;
    }

    /**
     * replace spaces with underscores
     * 
     * @param s
     * @return
     */
    public static String sanitiseName(String s) {
        s = s.replaceAll("[ ]", "_");
        return s;
    }

    public static void close( OutputStream os ) {
        try {
            if( os == null ) {
                return;
            }
            os.close();
        } catch( Exception ex ) {

        }
    }
}
