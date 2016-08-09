/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package GUISync;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 *
 * @author ibraheem
 */
public class base {

    MainPage main = new MainPage();

    public void run() {
        main.setVisible(true);
    }

    public static File file;
    /*     */    public static FileChannel filechannel;
    /*     */    public static FileLock fileLock;

    public static void main(String args[]) {

        try {
            file = new File("Key");

            if (file.exists()) {
                file.delete();
            }

            filechannel = new RandomAccessFile(file, "rw").getChannel();
            fileLock = filechannel.tryLock();

            if (fileLock == null) {
                filechannel.close();
                throw new RuntimeException("errr");
            }
            Thread shut = new Thread(new Runnable() {

                @Override
                public void run() {

                }

            });

            Runtime.getRuntime().addShutdownHook(shut);

            protect();
        } catch (IOException | RuntimeException e) {
            System.out.println(e.getMessage());

            System.exit(0);

        }
    }

    public static void unlock() {
        try {
            if (fileLock != null) {
                fileLock.release();
                filechannel.close();
                file.delete();
            }
        } catch (Exception e) {
        }
    }

    //will prevent copy
    private static void protect() {
        base b = new base();
        b.run();
    }

}
