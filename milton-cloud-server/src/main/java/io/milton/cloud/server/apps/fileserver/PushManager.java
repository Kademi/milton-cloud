/*
 * Copyright 2012 McEvoy Software Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.apps.fileserver;

import io.milton.cloud.server.manager.CurrentRootFolderService;
import io.milton.cloud.server.sync.push.AuthenticateMessage;
import io.milton.cloud.server.sync.push.FilesChangedMessage;
import io.milton.cloud.server.apps.fileserver.TcpChannelHub.Client;
import io.milton.cloud.server.web.ContentResource;
import io.milton.cloud.server.web.RootFolder;
import io.milton.cloud.server.web.SpliffySecurityManager;
import io.milton.context.Context;
import io.milton.context.Executable2;
import io.milton.context.RootContext;
import io.milton.event.DeleteEvent;
import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.event.EventManager;
import io.milton.event.MoveEvent;
import io.milton.event.PutEvent;
import io.milton.event.ResourceEvent;
import io.milton.vfs.db.BaseEntity;
import io.milton.vfs.db.Profile;
import io.milton.vfs.db.utils.SessionManager;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author brad
 */
public class PushManager implements EventListener {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PushManager.class);
    private final EventManager eventManager;
    private final TcpChannelHub hub;
    private final SpliffySecurityManager securityManager;
    private final CurrentRootFolderService currentRootFolderService;
    private final Map<Long, Client> authenticatedClients = new ConcurrentHashMap<>(); // key is the profile id
    private final SessionManager sessionManager;
    private final RootContext rootContext;

    public PushManager(int port, EventManager eventManager, SpliffySecurityManager securityManager, CurrentRootFolderService currentRootFolderService, SessionManager sessionManager, RootContext rootContext) {
        if (sessionManager == null) {
            throw new RuntimeException("sessionManager cannot be null");
        }
        this.eventManager = eventManager;
        this.rootContext = rootContext;
        hub = new TcpChannelHub(port, new ClientConnectionClientListener());
        this.securityManager = securityManager;
        this.currentRootFolderService = currentRootFolderService;
        this.sessionManager = sessionManager;
    }

    public void start() {
        hub.start();
        eventManager.registerEventListener(this, PutEvent.class);
        eventManager.registerEventListener(this, DeleteEvent.class);
        eventManager.registerEventListener(this, MoveEvent.class);
    }

    public void stop() {
        hub.stop();
    }

    @Override
    public void onEvent(Event e) {
        log.info("onEvent: " + e);
        if (e instanceof ResourceEvent) {
            ResourceEvent re = (ResourceEvent) e;
            if (re.getResource() instanceof ContentResource) {
                ContentResource cr = (ContentResource) re.getResource();
                if (cr.getBranch() != null) {
                    BaseEntity owner = cr.getBranch().getRepository().getBaseEntity();
                    notifyClients(owner);
                }
            }
        }
    }

    private void notifyClients(BaseEntity owner) {
        log.info("notifyClients: " + owner.getId() + " connected clients: " + authenticatedClients.size());
        if (owner instanceof Profile) {
            Profile p = (Profile) owner;
            Client c = authenticatedClients.get(p.getId());
            if (c != null) {
                FilesChangedMessage message = new FilesChangedMessage();
                c.send(message);
            }

        }
    }

    public class ClientConnectionClientListener implements ClientListener {

        @Override
        public void handleNotification(final Client client, final Serializable msg) {
            if (msg instanceof AuthenticateMessage) {
                rootContext.execute(new Executable2() {
                    @Override
                    public void execute(Context context) {
                        sessionManager.open();
                        try {
                            log.info("handleNotification: authenticate");
                            AuthenticateMessage auth = (AuthenticateMessage) msg;
                            RootFolder rootFolder = currentRootFolderService.getRootFolder(auth.getWebsite());
                            if (rootFolder == null) {
                                log.warn("No such host: " + auth.getWebsite());
                                respondNoSuchHost(client, auth.getWebsite());
                            } else {
                                Profile p = securityManager.authenticate(rootFolder.getOrganisation(), auth.getUsername(), auth.getPassword());
                                if (p == null) {
                                    log.warn("Login failed: " + auth.getWebsite() + " user=" + auth.getUsername() + " pwd:" + auth.getPassword());
                                    respondLoginFailed(client, auth.getUsername(), rootFolder.getOrganisation().getName());
                                } else {
                                    log.info("handleNotification: auth ok: " + auth.getUsername());
                                    authenticatedClients.put(p.getId(), client);
                                }
                            }
                        } finally {
                            sessionManager.close();
                        }

                    }
                });
            }
        }

        private void respondNoSuchHost(Client client, String host) {
            client.send("No such host: " + host);
        }

        private void respondLoginFailed(Client client, String username, String host) {
            client.send("Login failed with user: " + username + " to host: " + host);
        }
    }
}
