package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ca.discotek.sinktank.AsmUtil;
import ca.discotek.sinktank.Method;
import ca.discotek.sinktank.ProjectData;
import ca.discotek.sinktank.Util;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class SinkParameterSelectorDialog extends JDialog {

    JButton okayButton;
    JButton cancelButton;
    
    JComboBox<String> box;
    boolean isCanceled = true;
    
    public SinkParameterSelectorDialog(JFrame parent, Method method) {
        super(parent, "Sink Parameter Selector", true);
        buildGui(method);
    }
    
    void buildGui(Method method) {
        setLayout(new BorderLayout());
        
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JLabel label = new JLabel("<html>Select the sink parameter for method " + Util.escapeHtml(method.toString()) + ":</html>");
        label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(label, BorderLayout.CENTER);
        
        String parameterNames[] = ProjectData.getParameterNames(method);
        List<String> list = new ArrayList<String>();
        String desc = ProjectData.getMethodDesc(method.descId);
        ca.discotek.rebundled.org.objectweb.asm.Type methodType = ca.discotek.rebundled.org.objectweb.asm.Type.getMethodType(desc);
        ca.discotek.rebundled.org.objectweb.asm.Type argTypes[] = methodType.getArgumentTypes();
        
        String model[] = new String[argTypes.length];
        for (int i=0; i<model.length; i++) {
            model[i] = AsmUtil.toString(argTypes[i]) + " " + (parameterNames == null ? "<???>" : parameterNames[i]);
        }
        
        box = new JComboBox<String>(model);
        JPanel boxPanel = new JPanel();
        boxPanel.add(box);
        panel.add(boxPanel, BorderLayout.SOUTH);
        
        add(panel, BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }
    
    JPanel buildButtonPanel() {
        JPanel panel = new JPanel();
        
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2,2,2,2);
        
        panel.add(new JLabel(), gbc);
        
        gbc.gridx++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;

        okayButton = new JButton("Okay");
        okayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isCanceled = false;
                SinkParameterSelectorDialog.this.dispose();
                SinkParameterSelectorDialog.this.setVisible(false);
            }
        });
        panel.add(okayButton, gbc);
        
        gbc.gridx++;
        
        cancelButton = new JButton("Cancel");
        cancelButton.setEnabled(true);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isCanceled = true;
                SinkParameterSelectorDialog.this.dispose();
                SinkParameterSelectorDialog.this.setVisible(false);
            }
        });
        panel.add(cancelButton, gbc);
        
        return panel;
    }
    
    public int getSelectedIndex() {
        return box.getSelectedIndex();
    }
    
    public static boolean setSinkParameterIndex(JFrame parent, Method methods[]) {
        for (int i=0; i<methods.length; i++) {
            if (!setSinkParameterIndex(parent, methods[i]))
                return false;
        }
        
        return true;
    }
    
    public static boolean setSinkParameterIndex(JFrame parent, Method method) {
        SinkParameterSelectorDialog d = new SinkParameterSelectorDialog(parent, method);
        d.pack();
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
        if (!d.isCanceled)
            method.addVulnerableParameterIndex(d.getSelectedIndex());
        return d.isCanceled;
    }
}
