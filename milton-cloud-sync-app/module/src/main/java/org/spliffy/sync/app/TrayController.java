package org.spliffy.sync.app;


import io.milton.common.LogUtils;
import io.milton.event.EventListener;
import io.milton.event.EventManager;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.openide.LifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spliffy.sync.SpliffySync;
import org.spliffy.sync.event.DownloadSyncEvent;
import org.spliffy.sync.event.FinishedSyncEvent;
import org.spliffy.sync.event.ScanEvent;
import org.spliffy.sync.event.UploadSyncEvent;

/**
 *
 * @author brad
 */
public class TrayController {

    private static final Logger log = LoggerFactory.getLogger(TrayController.class);

    private final WindowController windowController;
    private final SpliffySync spliffySync;
    
    private final TrayIcon trayIcon;
    private final Image trayIconIdle;
    private final Image trayIconUploading;
    private final Image trayIconScanning;
    private final Image trayIconOffline;
    private MenuItem openItem;
    private MenuItem openWebItem;
    private CheckboxMenuItem paused;    
    private MenuItem exitItem;
    private Image current;

    public TrayController(EventManager eventManager, WindowController windowController, SpliffySync spliffySync) {
        this.windowController = windowController;
        this.spliffySync = spliffySync;
        TrayControllerEventListener tcel = new TrayControllerEventListener();
        eventManager.registerEventListener(tcel, UploadSyncEvent.class);
        eventManager.registerEventListener(tcel, DownloadSyncEvent.class);
        eventManager.registerEventListener(tcel, FinishedSyncEvent.class);

        trayIconIdle = createImage("/org/spliffy/sync/app/logo16x16.png", "idle");
        trayIconUploading = createImage("/org/spliffy/sync/app/upload16x16.png", "idle");
        trayIconScanning = createImage("/org/spliffy/sync/app/scanning16x16.png", "idle");
        trayIconOffline = createImage("/org/spliffy/sync/app/offline16x16.png", "idle");

        trayIcon = new TrayIcon(trayIconIdle);
        trayIcon.setImageAutoSize(true);

    }

    public boolean show() {
        log.trace("show");
        if (!SystemTray.isSupported()) {
            log.trace("tray is not supported");
            return false;
        } else {
            final PopupMenu popup = new PopupMenu();


            final SystemTray tray = SystemTray.getSystemTray();

            // Create a pop-up menu components
            openItem = new MenuItem("Open");
            openWebItem = new MenuItem("Go to the web site");
            paused = new CheckboxMenuItem("Pause");
            exitItem = new MenuItem("Exit");
            setFont(openItem, paused, exitItem, openWebItem);

            //Add components to pop-up menu
            popup.add(openItem);
            popup.add(openWebItem);
            popup.addSeparator();
            popup.add(paused);
            popup.addSeparator();
            popup.add(exitItem);

            trayIcon.setPopupMenu(popup);

            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                log.error("couldnt add system tray", e);
                return false;
            }

            trayIcon.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    windowController.showMain();
                }
            });

            trayIcon.addMouseListener(new MouseListener() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    windowController.showMain();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                }
            });

            openItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    windowController.showMain();
                }
            });

            openWebItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    windowController.openMediaLounge();
                }
            });

            paused.addItemListener(new ItemListener() {

                @Override
                public void itemStateChanged(ItemEvent e) {
                    log.debug(" paused : " + paused.getState());
                    spliffySync.setPaused(paused.getState());
                }
            });

            exitItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    LifecycleManager.getDefault().exit();
                }
            });

            return true;
        }
    }

    private void setFont(MenuItem... menuItems) {
        Font font = Font.decode("Segoe UI-Plain-11");
        for (MenuItem item : menuItems) {
            item.setFont(font);
        }
    }

    protected static Image createImage(String path, String description) {
        URL imageURL = TrayController.class.getResource(path);

        if (imageURL == null) {
            throw new RuntimeException("Could not load image: " + path + " using class: " + TrayController.class);
        } else {
            System.out.println("Loaded image: " + imageURL);
            return (new ImageIcon(imageURL, description)).getImage();
        }
    }


    
    private void setOffline() {
        if (trayIcon.getImage() == trayIconOffline) {
            return;
        }
        setIcon(trayIconOffline);
        trayIcon.setToolTip("Unable to connect to the server");
    }

    private void setUploading() {
        if (trayIcon.getImage() == trayIconUploading) {
            return;
        }
        setIcon(trayIconUploading);
        trayIcon.setToolTip("Uploading file(s) to the server");
    }

    private void setDownloading() {
        if (trayIcon.getImage() == trayIconUploading) {
            return;
        }
        setIcon(trayIconUploading);
    }

    private void setIdle() {
        if (trayIcon.getImage() == trayIconIdle) {
            return;
        }
        setIcon(trayIconIdle);
        trayIcon.setToolTip("Not uploading or scanning");
    }


    private void setScanning() {
        if (trayIcon.getImage() == trayIconScanning) {
            return;
        }
        setIcon(trayIconScanning);
        trayIcon.setToolTip("Scanning for new and updated files...");
    }

    private void setIcon(final Image image) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                LogUtils.trace(log, "TrayController: setImage", image);
                trayIcon.setImage(image);
            }
        });
    }

    private class TrayControllerEventListener implements EventListener {

        @Override
        public void onEvent(io.milton.event.Event e) {
            if( e instanceof UploadSyncEvent ) {
                setUploading();
            } else if( e instanceof DownloadSyncEvent ) {
                setDownloading();
            } else if( e instanceof ScanEvent) {
                setScanning();
            } else if( e instanceof FinishedSyncEvent) {
                setIdle();
            }
        }
    }
}
