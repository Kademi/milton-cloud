package io.milton.sync;

import static io.milton.sync.SwingConflictResolver.ConflictChoice.LOCAL;
import static io.milton.sync.SwingConflictResolver.ConflictChoice.NOTHING;
import static io.milton.sync.SwingConflictResolver.ConflictChoice.REMOTE;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import static javax.swing.SwingConstants.LEFT;

/**
 *
 * @author brad
 */
public class SwingConflictResolver {

    public static void main(String[] args) throws InterruptedException {
        SwingConflictResolver r = new SwingConflictResolver();
        ConflictChoice choice = r.showConflictResolver("Please resolve conflicts Please resolve conflictsPlease resolve conflictsPlease resolve conflictsPlease resolve conflictsPlease resolve conflicts", 3);
        System.out.println("choice: " + choice + " - " + r.rememberSecs);
    }

    public enum ConflictChoice {

        LOCAL,
        REMOTE,
        NOTHING
    }

    public ConflictChoice choice = null;
    public Integer rememberSecs;

    public SwingConflictResolver() {
    }

    public ConflictChoice showConflictResolver(String message, Integer defaultSecs) {

        JFrame f1 = new JFrame("Conflict resolver");
        f1.setBounds(32, 32, 400, 200);

        JButton btnLocal = new JButton("Local");
        btnLocal.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton btnRemote = new JButton("Remote");
        btnRemote.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton btnDoNothing = new JButton("Do nothing");
        btnDoNothing.setAlignmentX(Component.CENTER_ALIGNMENT);

        final JDialog d2 = new JDialog(f1, "", Dialog.ModalityType.DOCUMENT_MODAL);

        btnLocal.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                choice = LOCAL;
                d2.setVisible(false);
            }
        });

        btnRemote.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                choice = REMOTE;
                d2.setVisible(false);
            }
        });

        btnDoNothing.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                choice = NOTHING;
                d2.setVisible(false);
            }
        });

        final JTextField txtSeconds = new JTextField();
        txtSeconds.setMaximumSize(new Dimension(200, 20));
        txtSeconds.setAlignmentX(Component.LEFT_ALIGNMENT);
        if( defaultSecs != null ) {
            txtSeconds.setText(defaultSecs.toString());
        }

        d2.setBounds(132, 132, 400, 200);
        Container cp2 = d2.getContentPane();
        JPanel outerPanel = new JPanel();
        outerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        cp2.add(outerPanel, BorderLayout.CENTER);
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));

        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));

        outerPanel.add(messagePanel);


        JTextArea lblMessage = new JTextArea(message);
        lblMessage.setLineWrap(true);
        lblMessage.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        lblMessage.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagePanel.add(lblMessage);
        messagePanel.add(Box.createHorizontalStrut(5));
        messagePanel.add(Box.createVerticalStrut(15) );
        messagePanel.add(Box.createVerticalGlue());

        messagePanel.add(new JLabel("Remember choice for seconds:", LEFT));
        messagePanel.add(txtSeconds);
        messagePanel.add(Box.createVerticalStrut(5));

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));

        buttonsPanel.add(btnLocal);
        buttonsPanel.add(Box.createVerticalStrut(5));
        buttonsPanel.add(btnRemote);
        buttonsPanel.add(Box.createVerticalStrut(5));
        buttonsPanel.add(btnDoNothing);

        outerPanel.add(buttonsPanel);
        d2.setVisible(true);

        String s = txtSeconds.getText();
        try {
            Integer i = Integer.parseInt(s);
            if (i != null && i >= 0) {
                rememberSecs = i;
            }
        } catch (NumberFormatException ex) {
        }

        System.out.println("done - " + rememberSecs);

        return choice;
    }

    public Integer getRememberSecs() {
        return rememberSecs;
    }

}
