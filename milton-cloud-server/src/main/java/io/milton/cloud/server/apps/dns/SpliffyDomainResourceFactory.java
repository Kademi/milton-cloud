package io.milton.cloud.server.apps.dns;

import io.milton.dns.Name;
import io.milton.dns.TextParseException;
import io.milton.dns.resource.ADomainResourceRecord;
import io.milton.dns.resource.DomainResource;
import io.milton.dns.resource.DomainResourceFactory;
import io.milton.dns.resource.DomainResourceRecord;
import io.milton.dns.resource.MXDomainResourceRecord;
import io.milton.dns.resource.NSDomainResourceRecord;
import io.milton.dns.resource.NonAuthoritativeException;
import io.milton.dns.resource.SOADomainResourceRecord;
import io.milton.dns.utils.Utils;
import io.milton.vfs.db.Website;
import io.milton.vfs.db.utils.SessionManager;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.*;

import org.apache.log4j.Logger;
import org.hibernate.Session;

/**
 *
 * @author Nick
 *
 */
public class SpliffyDomainResourceFactory implements DomainResourceFactory {

    private static Logger log = Logger.getLogger(SpliffyDomainResourceFactory.class.getName());
    private String primaryDomain;
    private Inet4Address ipv4;
    private Inet6Address ipv6;
    private List<String> nsNames = new ArrayList<>();
    private List<String> mxNames = new ArrayList<>();
    private long defaultTtl = 7200;
    private String defaultMx;
    private SessionManager sessionManager;

    /**
     *
     * @param nsNames
     * @param mcIps
     * @param defaultMx
     * @param defaultTtl
     * @param soaData
     * @param sessionManager
     */
    public SpliffyDomainResourceFactory(SessionManager sessionManager) {
        if (sessionManager == null) {
            throw new RuntimeException(">:o");
        }
        this.sessionManager = sessionManager;

    }

    @Override
    public DomainResource getDomainResource(String domainName) throws NonAuthoritativeException {

        log.info("Finding Website: " + domainName);

        int firstDot = domainName.indexOf(".");
        if (firstDot == -1 || firstDot == domainName.length() - 1) {
            throw new NonAuthoritativeException(domainName);
        }

        Session session = null;
        Website w = null;
        try {
            session = sessionManager.open();
            w = Website.findByDomainNameDirect(domainName, session);

            if (w == null) {
                log.info("Website is null");
                if (this.isAuthorityFor(domainName, session)) {
                    return null;
                }
                throw new NonAuthoritativeException(domainName);
            }

            log.info("Website was found");
            DomainResourceImpl dr;
            if (isZoneName(domainName, session)) {
                dr = new ZoneDomainResourceImpl(domainName);
                addSOARecord(dr, domainName);
                addNSRecords(dr, domainName);
            } else {
                dr = new DomainResourceImpl(domainName);
            }
            addIPRecords(dr, domainName);
            String mxName = w.getMailServer();
            if (mxName == null) {
                mxName = defaultMx;
            }
            if (mxName != null) {
                addMXRecord(dr, domainName, mxName);
            }
            log.info("records: " + dr.getRecords().size());
            return dr;

        } catch (TextParseException e) {
            //shouldn't happen
            throw new NonAuthoritativeException(domainName);
        } catch (RuntimeException e) {
            throw e;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public boolean isAuthorityFor(String domainName, Session session) {
        /* No delegation support yet */
        Name name;
        try {
            name = Utils.stringToName(domainName); // TODO: might be cleaner to just split on .
        } catch (TextParseException ex) {
            throw new RuntimeException(ex);
        }
        for (int tlabels = name.labels(); tlabels > 0; tlabels--) {

            Name tname = new Name(name, name.labels() - tlabels);
            Website w = Website.findByDomainNameDirect(Utils.nameToString(tname), session);
            if (w != null) {
                return true;
            }
        }
        return false;
    }

    public boolean isZoneName(String domainName, Session session) throws TextParseException {

        /* No delegation support yet */
        Name name = Utils.stringToName(domainName);
        Name parent = new Name(name, 1);
        Website w = Website.findByDomainNameDirect(Utils.nameToString(parent), session);
        if (w == null) {
            return true;
        }
        return false;

    }

    public String getDefaultMx() {
        return defaultMx;
    }

    public void setDefaultMx(String defaultMx) {
        this.defaultMx = defaultMx;
    }

    private void addSOARecord(DomainResourceImpl dr, String name) {
        dr.addRecord(new SOARecord(name));
    }

    private void addIPRecords(DomainResourceImpl dr, String name) {
        if (ipv4 != null) {
            dr.addRecord(new ARecord(ipv4, name));
        }
        if (ipv6 != null) {
            dr.addRecord(new ARecord(ipv6, name));
        }
    }

    private void addMXRecord(DomainResourceImpl dr, String domainName, String mailServer) {
        dr.addRecord(new MXRecord(domainName, 10, mailServer));
    }

    private void addNSRecords(DomainResourceImpl dr, String domainName) {
        if (nsNames != null) {
            for (int i = 0; i < nsNames.size(); i++) {
                String ns = nsNames.get(i);
                dr.addRecord(new NSRecord(domainName, ns));
            }
        }
    }

    public long getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(long defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    public Inet4Address getIpv4() {
        return ipv4;
    }

    public void setIpv4(Inet4Address ipv4) {
        this.ipv4 = ipv4;
    }

    public Inet6Address getIpv6() {
        return ipv6;
    }

    public void setIpv6(Inet6Address ipv6) {
        this.ipv6 = ipv6;
    }

    public List<String> getMxNames() {
        return mxNames;
    }

    public void setMxNames(List<String> mxNames) {
        this.mxNames = mxNames;
    }

    public List<String> getNsNames() {
        return nsNames;
    }

    public void setNsNames(List<String> nsNames) {
        this.nsNames = nsNames;
    }

    public String getPrimaryDomain() {
        return primaryDomain;
    }

    public void setPrimaryDomain(String primaryDomain) {
        this.primaryDomain = primaryDomain;
    }

    public class AbstractRecord implements DomainResourceRecord {

        private final String name;

        public AbstractRecord(String name) {
            if (name == null) {
                throw new RuntimeException("No null names, please");
            }
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getTtl() {
            return defaultTtl;
        }
    }

    public class ARecord extends AbstractRecord implements ADomainResourceRecord {

        private final InetAddress address;

        public ARecord(InetAddress address, String name) {
            super(name);
            this.address = address;
        }

        @Override
        public InetAddress getAddress() {
            return address;
        }
    }

    public class NSRecord extends AbstractRecord implements NSDomainResourceRecord {

        private final String target;

        public NSRecord(String name, String target) {
            super(name);
            this.target = target;
        }

        @Override
        public String getTarget() {
            return target;
        }
    }

    public class MXRecord extends AbstractRecord implements MXDomainResourceRecord {

        private final int ordinal;
        private final String target;

        public MXRecord(String name, int ordinal, String target) {
            super(name);
            this.ordinal = ordinal;
            if (target == null) {
                throw new RuntimeException("Target name is null");
            }
            this.target = target;
        }

        @Override
        public int getPriority() {
            return ordinal * 10;
        }

        @Override
        public String getTarget() {
            return target;
        }
    }

    public class SOARecord extends AbstractRecord implements SOADomainResourceRecord {

        public SOARecord(String name) {
            super(name);
        }

        @Override
        public String getHost() {
            return "www." + primaryDomain;
        }

        @Override
        public String getAdminEmail() {
            return "admin@" + primaryDomain;
        }

        @Override
        public long getZoneSerialNumber() {
            return 1;
        }

        @Override
        public long getRefresh() {
            return 3600;
        }

        @Override
        public long getRetry() {
            return 600;
        }

        @Override
        public long getExpire() {
            return 86400;
        }

        @Override
        public long getMinimum() {
            return 3600;
        }
    }
}
