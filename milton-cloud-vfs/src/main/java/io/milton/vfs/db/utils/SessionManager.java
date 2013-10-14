package io.milton.vfs.db.utils;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

/**
 *
 * @author brad
 */
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    private static ThreadLocal<Session> tlSession = new ThreadLocal<>();

    public static Session session() {
        //return SessionFactoryUtils.getSession(sessionFactory, false);
        return tlSession.get();
    }

    public static Transaction beginTx() {
        Session s = session();
        if (s == null) {
            throw new NullPointerException("There is no session bound to the current thread");
        }
        return s.beginTransaction();
    }

    public static void commit(Transaction tx) {
        Session s = session();
        s.flush();
        tx.commit();
    }
    private static SessionFactory sessionFactory;
    private boolean checkConnectionOnOpen = true;

    public SessionManager(SessionFactory sf) {
        sessionFactory = sf;
    }

    public Session open() {
        Session session = SessionFactoryUtils.getSession(sessionFactory, true);
        tlSession.set(session);

        if (checkConnectionOnOpen) {
            // We're having some difficult to diagnose connection issues at the moment.
            // To experiment lets test the connection when opened, if it fails do a rollback
            session.doWork(new Work() {
                @Override
                public void execute(Connection connection) throws SQLException {
                    CallableStatement stmt = null;
                    ResultSet rs = null;
                    try {
                        stmt = connection.prepareCall("select * from repository where id = 1");
                        rs = stmt.executeQuery();
                    } catch (Exception e) {
                        log.warn("CONNECTION TEST FAILED", e);
                        connection.rollback(); // attempt to rollback to avoid 'tx aborted' error
                    } finally {
                        if (rs != null) {
                            rs.close();
                        }
                        if (stmt != null) {
                            stmt.close();
                        }
                    }
                }
            });
        }

        return session;
    }

    public void close() {
        Session s = tlSession.get();
        if (s != null) {
            SessionFactoryUtils.closeSession(s);
        }
        tlSession.remove();
    }

    public org.hibernate.Cache getCache() {
        return sessionFactory.getCache();
    }

    public boolean isCheckConnectionOnOpen() {
        return checkConnectionOnOpen;
    }

    public void setCheckConnectionOnOpen(boolean checkConnectionOnOpen) {
        this.checkConnectionOnOpen = checkConnectionOnOpen;
    }
}
