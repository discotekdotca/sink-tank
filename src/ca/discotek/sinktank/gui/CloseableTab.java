package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class CloseableTab extends JPanel {
    
    String id;
    String text;
    JTabbedPane tabber;
    JComponent component;
    

    public static interface CloseListener {
        public void tabClosed(String id);
    }
    
    CloseListener closeListener;
    
    public CloseableTab(String id, String text, JTabbedPane tabber, JComponent component, CloseableTab.CloseListener l) {
        this.id = id;
        this.text = text;
        this.tabber = tabber;
        this.component = component;
        this.closeListener = l;
        
        buildGui();
    }
    
    void buildGui() {
        setLayout(new BorderLayout());
        add(new JLabel(text), BorderLayout.CENTER);
        add(new CloseButton(), BorderLayout.EAST);
    }
    
    class CloseButton extends JLabel {
        
        public CloseButton() {
            super("X");
            setHorizontalAlignment(JLabel.RIGHT);
            Border separatorBorder = BorderFactory.createEmptyBorder(2, 4, 0, 0);
            
            Border innerBorder = BorderFactory.createEmptyBorder(0, 2, 0, 2);
            Border outerBorder = BorderFactory.createLineBorder(Color.gray);
            Border border = BorderFactory.createCompoundBorder(outerBorder, innerBorder);
            setBorder(BorderFactory.createCompoundBorder(separatorBorder, border));
            
            setToolTipText("Close Tab");
            
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    tabber.removeTabAt(tabber.indexOfComponent(component));
                    closeListener.tabClosed(id);
                }
            });

        }
    }
    
}

