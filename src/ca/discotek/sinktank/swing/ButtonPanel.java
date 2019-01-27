package ca.discotek.sinktank.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class ButtonPanel extends JPanel {

    GridBagConstraints gbc;
    
    public ButtonPanel() {
        this(new Insets(2,2,2,2));
    }
    
    public ButtonPanel(Insets insets) {
        setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = insets;
        
        add(new JLabel(), gbc);
        
        gbc.gridx++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
    }
    
    public void add(AbstractButton button) {
        addButton(button);
    }
    
    public void addButton(AbstractButton button) {
        add(button, gbc);
        gbc.gridx++;
    }
}
