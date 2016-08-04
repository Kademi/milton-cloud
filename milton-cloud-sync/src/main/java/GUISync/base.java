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

    login login = new login();

    

    public void run() {
        login.setVisible(true);
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

            JOptionPane.showMessageDialog(null, "KSync tool already running !");

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
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
            //will set the JProgressbar's orange color to new color
            UIManager.put("ProgressBar.background", Color.orange);
            UIManager.put("ProgressBar.foreground", Color.blue);
            UIManager.put("ProgressBar.selectionBackground", Color.red);
            UIManager.put("ProgressBar.selectionForeground", Color.green);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }

        base b = new base();
        b.run();
    }

}
