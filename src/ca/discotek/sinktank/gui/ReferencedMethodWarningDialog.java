package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ca.discotek.sinktank.Method;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class ReferencedMethodWarningDialog extends JDialog {

    static final String TEXT = 
        "<html>The following methods were referenced, but not defined in the deployment files. Consequently, "
        + "the results of path analysis will not be exhaustive. Additionally, any JSPs that reference undefined methods, "
        + "cannot be compiled and analyzed. To resolve this issue, add the library dependencies as deployment units (including &lt;jre-home&gt;/lib/rt.jar).</html>";
    
    MethodViewer methodViewer;
    
    boolean canceled = true;
    
    public ReferencedMethodWarningDialog(JFrame parent, Set<Method> methodSet) {
        super(parent, "Undefined Method(s) Warning", true);
        buildGui();

        methodViewer.addMethods(methodSet.toArray(new Method[methodSet.size()]));
    }
    
    void buildGui() {
        setLayout(new BorderLayout());

        JLabel label = new JLabel(TEXT);
        label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        add(label, BorderLayout.NORTH);
        
        methodViewer = new MethodViewer(MethodTreeType.MethodParameters);
        add(methodViewer, BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }
    
    JPanel buildButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);
        
        panel.add(new JLabel(), gbc);
        
        gbc.gridx++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;

        JButton okayButton = new JButton("Okay");
        okayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canceled = false;
                setVisible(false);
                dispose();
            }
        });
        panel.add(okayButton, gbc);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canceled = true;
                setVisible(false);
                dispose();
            }
        });
        gbc.gridx++;
        panel.add(cancelButton, gbc);
        
        return panel;
    }
    
    public boolean getCanceled() {
        return canceled;
    }
}
