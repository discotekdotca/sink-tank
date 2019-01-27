package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JProgressBar;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class IndeterminateProgressDialog extends JDialog {

    JProgressBar bar;
    JFrame f;
    
    public IndeterminateProgressDialog(JFrame f) {
        super(f, "Progress", false);
        this.f = f;
        buildGui();
    }
    
    void buildGui() {
        bar = new JProgressBar();
        
        bar.setStringPainted(true);
        bar.setString("Processing...");
        
        setLayout(new BorderLayout() );
        add(bar, BorderLayout.CENTER);
        bar.setPreferredSize(new Dimension(400, 100));
        
        f.getGlassPane().addMouseListener(new MouseListener() {
            public void mouseReleased(MouseEvent e) { e.consume(); }
            public void mousePressed(MouseEvent e) { e.consume(); }
            public void mouseExited(MouseEvent e) { e.consume(); }
            public void mouseEntered(MouseEvent e) { e.consume(); }
            public void mouseClicked(MouseEvent e) { e.consume(); }
        });
    }
    
    public void start() {
        f.getGlassPane().setVisible(true);
        bar.setIndeterminate(true);
    }
    
    public void stop() {
        f.getGlassPane().setVisible(false);
        bar.setIndeterminate(false);
    }
    
    public void setVisible(boolean visible) {
        if (!visible)
            stop();
        super.setVisible(visible);
    }
}
