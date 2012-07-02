package io.milton.cloud.server.scratch;


import io.milton.cloud.server.apps.ApplicationManager;
import io.milton.cloud.server.web.templating.HtmlTemplater;
import io.milton.sync.SyncCommand;
import io.milton.vfs.db.Branch;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext; 


/**
 *
 * @author brad
 */
public class Scratch {

    static Server server;

    public static void main(String[] args) throws Exception {
        sendStop();
        
        String extraApps = ""; //toCsv(FuseDataCreatorApp.class, LearningRegistrationApp.class, LearningContentApp.class, LearningAdminApp.class);
        System.out.println("extra apps: " + extraApps);
        System.setProperty(ApplicationManager.EXTRA_APPS_SYS_PROP_NAME, extraApps);
        File root = new File("src/main/resources");
        System.setProperty(HtmlTemplater.ROOTS_SYS_PROP_NAME, root.getAbsolutePath() );
        System.out.println("set template roots: " + root.getAbsolutePath() );

        System.setProperty("extra.web.resources.location", root.getParentFile().getAbsolutePath());

        server = new Server(8080);
        server.setStopAtShutdown(true);
        WebAppContext context = new WebAppContext();
        //File f = new File();
        context.setDescriptor("src/test/resources/web.xml");
        context.setResourceBase("src/main/webapp");
        context.setContextPath("/");

        context.setParentLoaderPriority(true);
//        context.setExtraClasspath("E:/proj/spliffy/spliffy-server/src/main/resources/,E:/proj/spliffy/spliffy-server/src/test/resources/, src/test/resources/");

        server.setHandler(context);

        server.start();

        File contentDir = new File("src/main/3dn-content");
        File coursesDir = new File("src/main/courses");
        System.out.println("Beginning monitor of: " + contentDir.getAbsolutePath());
        File dbFile = new File("target/sync-db");
        List<SyncCommand.SyncJob> jobs = Arrays.asList(
                // content for 3dn 
//                new SyncCommand.SyncJob(contentDir, "http://127.0.0.1:8080/organisations/3dn/idhealth.localhost/" + Branch.TRUNK + "/", "admin", "password8", true)
//                ,                
//                new SyncCommand.SyncJob(new File(coursesDir, "pmh1"), "http://127.0.0.1:8080/organisations/3dn/programs/prof/pmh1/" + Branch.TRUNK + "/", "admin", "password8", true),
//                new SyncCommand.SyncJob(new File(coursesDir, "pmh2"), "http://127.0.0.1:8080/organisations/3dn/programs/prof/pmh2/" + Branch.TRUNK + "/", "admin", "password8", true),
//                new SyncCommand.SyncJob(new File(coursesDir, "cmh1"), "http://127.0.0.1:8080/organisations/3dn/programs/carer/cmh1/" + Branch.TRUNK + "/", "admin", "password8", true),
//                new SyncCommand.SyncJob(new File(coursesDir, "cmh2"), "http://127.0.0.1:8080/organisations/3dn/programs/carer/pmh2/" + Branch.TRUNK + "/", "admin", "password8", true)
        );
        SyncCommand.start(dbFile, jobs);
        
        
        System.out.println("... monitor started");
        Thread stopThread = new StopThread();
        stopThread.start();
        server.join();
    }

    private static String toCsv(Class... classes) {
        StringBuilder sb = new StringBuilder();
        for (Class c : classes) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(c.getName());
        }
        return sb.toString();
    }

    public static void sendStop() throws Exception {
        try {
            System.out.println("sendStop...");
            Socket s = new Socket(InetAddress.getByName("127.0.0.1"), 8079);
            OutputStream out = s.getOutputStream();
            System.out.println("*** sending jetty stop request");
            out.write(("\r\n").getBytes());
            out.flush();
            s.close();
            System.out.println("send stop completed");
        } catch (IOException iOException) {
            System.out.println("send stop failed, presumably server is not running");
        }
    }

    private static class StopThread extends Thread {

        private ServerSocket socket;

        public StopThread() {
            setDaemon(true);
            setName("StopMonitor");
            try {
                socket = new ServerSocket(8079, 1, InetAddress.getByName("127.0.0.1"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            System.out.println("*** running jetty 'stop' thread");
            Socket accept;
            try {
                accept = socket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(accept.getInputStream()));
                reader.readLine();
                System.out.println("*** stopping jetty embedded server");
                server.stop();
                accept.close();
                socket.close();
                Thread.sleep(200);
                System.exit(1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
