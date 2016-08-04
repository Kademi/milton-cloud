package org.spliffy.sync.app;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.JOptionPane;
import org.openide.windows.WindowManager;

/**
 *
 * @author brad
 */
public class WindowController {

    private final Desktop desktop;
    private final String url;
    
    public WindowController(String url) {
        // Before more Desktop API is used, first check
        // whether the API is supported by this particular
        // virtual machine (VM) on this particular host.
        if( Desktop.isDesktopSupported() ) {
            desktop = Desktop.getDesktop();
        } else {
            desktop = null;
        }
        this.url = url;
    }    

    public void hideMain() {
        WindowManager.getDefault().getMainWindow().setVisible(false);
    }

    public void showMain() {
        WindowManager.getDefault().getMainWindow().setVisible(true);
    }

    public void openMediaLounge() {
        if( desktop.isSupported( Desktop.Action.BROWSE ) ) {
            URI uri = null;
            try {
                uri = new URI( url );
            } catch( URISyntaxException use ) {
                showError( "Sorry, I can't open this web address: " + url );
                return;
            }
            try {
                desktop.browse( uri );
            } catch( IOException ex ) {
                showError( "Can't open: " + url );
            }
        } else {
            showError( "Can't open: " + url );
        }
    }

    private void showError( String err ) {
        JOptionPane.showMessageDialog(null, err, "Error opening browser", JOptionPane.ERROR_MESSAGE );
    }    
}
