package io.milton.cloud.server.apps.dns;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import io.milton.cloud.server.apps.AppConfig;
import io.milton.cloud.server.apps.Application;
import io.milton.cloud.server.apps.LifecycleApplication;
import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.web.SpliffyResourceFactory;
import io.milton.dns.server.JNameServer;
import io.milton.dns.utils.Utils;
import io.milton.vfs.db.Branch;
import io.milton.vfs.db.Organisation;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;
import java.net.Inet4Address;
import java.util.Arrays;
import org.apache.log4j.Logger;

public class NameServerApp implements Application, LifecycleApplication {

    private static Logger log = Logger.getLogger(NameServerApp.class);
    static final String LISTEN = "Listen_on_Socket_Addresses";
    static final String MC_IP = "A_Record";
    static final String NAMES = "Namserver_Domain_Names";
    static final String MXNAME = "Default_MX_Server_Name";
    static final String TTL = "Default_Record_TTL";
    static final String HOST = "Primary_Namserver";
    static final String ADMIN = "NS_Admin_Email";
    static final String MINIMUM = "Max_Negative_Query_Cache";
    JNameServer ns;

    @Override
    public String getInstanceId() {
        return "nameserver";
    }

    @Override
    public void init(SpliffyResourceFactory resourceFactory, AppConfig config) throws Exception {
        String listenOn = config.get(LISTEN);
        String serverIp = config.get(MC_IP);
        String names = config.get(NAMES);
        String mx = config.get(MXNAME);
        log.info("init: listenon=" + listenOn);

        List<InetSocketAddress> listenOn_ = new ArrayList<>();
        Inet4Address ip;

        String[] ss = listenOn.split(",");
        for (String s : ss) {
            int port = 53;
            if (s.indexOf(":") != -1) {
                String[] s2 = s.split(":");
                s = s2[0];
                port = Integer.parseInt(s2[1]);
            }
            InetAddress addr = InetAddress.getByName(s);
            log.info("listening on " + addr.getHostAddress());
            listenOn_.add(new InetSocketAddress(addr, port));
        }
        InetSocketAddress[] listen = listenOn_.toArray(new InetSocketAddress[listenOn_.size()]);
        ip = (Inet4Address) InetAddress.getByName(serverIp.trim());

        SessionManager sm = resourceFactory.getSessionManager();

        System.out.println("creating sdrf");
        SpliffyDomainResourceFactory sdrf = new SpliffyDomainResourceFactory(sm);
        sdrf.setIpv4(ip);

        ss = names.split(",");
        List<String> nameServers = new ArrayList<>();
        for (String s : ss) {
            s = s.trim();
            nameServers.add(s);
        }
        sdrf.setNsNames(nameServers);

        if (mx != null && mx.length() > 0) {
            sdrf.setDefaultMx(mx);
        }
        System.out.println("default MX: " + sdrf.getDefaultMx());

        String primaryDomain = config.getContext().get(CurrentRootFolderService.class).getPrimaryDomain();
        sdrf.setPrimaryDomain(primaryDomain);

        log.info("atarting server");
        ns = new JNameServer(sdrf, listen);
        ns.start();


        System.out.println("done init");

    }

    @Override
    public void shutDown() {
        if (ns != null) {
            ns.stop();
        }
    }

    @Override
    public void initDefaultProperties(AppConfig config) {
        try {
            config.add(LISTEN, "0.0.0.0:53");
            InetAddress addr = Utils.probeIp();
            if (addr != null) {
                config.add(MC_IP, addr.getHostAddress());
            } else {
                config.add(MC_IP, "127.0.0.1");
            }

            config.add(NAMES, "ns1.localhost, ns2.localhost");
            config.add(MXNAME, "mx1.localhost");
        } catch (Throwable e) {
            log.error("Exception starting DNS app");
        }
    }

    @Override
    public String getTitle(Organisation organisation, Branch websiteBranch) {
        return "DNS Nameserver";
    }

    @Override
    public String getSummary(Organisation organisation, Branch websiteBranch) {
        // TODO Auto-generated method stub
        return NAMES + ": A comma separated list of n ";
    }
}
