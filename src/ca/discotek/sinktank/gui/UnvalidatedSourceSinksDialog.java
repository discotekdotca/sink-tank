package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import ca.discotek.sinktank.AsmUtil;
import ca.discotek.sinktank.Method;
import ca.discotek.sinktank.ProjectData;
import ca.discotek.sinktank.swing.ButtonPanel;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class UnvalidatedSourceSinksDialog extends JDialog {
    
    boolean canceled = true;
    
    public UnvalidatedSourceSinksDialog(JFrame parent, Set<Method> unvalidatedSourceSet, Set<Method> unvalidatedSinkSet) {
        super(parent, true);
        buildGui(parent, unvalidatedSourceSet, unvalidatedSinkSet);
    }
    
    void buildGui(JFrame parent, Set<Method> unvalidatedSourceSet, Set<Method> unvalidatedSinkSet) {
        setLayout(new BorderLayout());
        
        JLabel label = new JLabel("<html>The following source and/or sink definitions do not exist within the specified deployments. This could be normal or could indicate "
                + "you need to add other deployments or libraries (perhaps rt.jar) to the deployments or you have an invalid source/sink definition.</html>");
        
        add(label, BorderLayout.NORTH);
        
        if (unvalidatedSourceSet.size() > 0 && unvalidatedSinkSet.size() > 0) {
            setTitle("Unvalidated Source/Sink Definitions");
            JTabbedPane tabber = new JTabbedPane();
            tabber.add("Sources", buildUnvalidatedDefintionsPanel(unvalidatedSourceSet));
            tabber.add("Sinks", buildUnvalidatedDefintionsPanel(unvalidatedSinkSet));
            
            add(tabber, BorderLayout.CENTER);
        }
        else {
            if (unvalidatedSourceSet.size() > 0)  {
                setTitle("Unvalidated Source Definitions");
                add(buildUnvalidatedDefintionsPanel(unvalidatedSourceSet), BorderLayout.CENTER);
            }
            else {
                setTitle("Unvalidated Sink Definitions");
                add(buildUnvalidatedDefintionsPanel(unvalidatedSinkSet), BorderLayout.CENTER);
            }
        }
        
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }
    
    JPanel buildUnvalidatedDefintionsPanel(Set<Method> methodSet) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        
        StringBuilder buffer = new StringBuilder();
        buffer.append("<html>");
        
        if (methodSet.size() > 0) {
            String className, methodName, desc;
            
            List<String> list = new ArrayList<String>();
            Iterator<Method> it = methodSet.iterator();
            Method method;
            while (it.hasNext()) {
                method = it.next();
                className = ProjectData.getClassName(method.classId);
                methodName = ProjectData.getMethodName(method.memberId);
                desc = ProjectData.getMethodDesc(method.descId);
                list.add(AsmUtil.toMethodSignature(className, methodName, desc, true, true, true));
            }
            
            Collections.sort(list);
            
            buffer.append("<h2>Methods</h2>");
            buffer.append("<ul>");
            Iterator<String> nameIt = list.listIterator();
            while (nameIt.hasNext()) {
                buffer.append("<li>");
                buffer.append(nameIt.next());
                buffer.append("</li>");
            }
            buffer.append("</ul>");
        }
        
        buffer.append("</html>");
        
        panel.add(new JScrollPane(new JLabel(buffer.toString())), BorderLayout.CENTER);
        
        return panel;
    }
    
    JPanel buildButtonPanel() {
        ButtonPanel panel = new ButtonPanel();
        JButton button = new JButton("Okay");
        panel.addButton(button);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canceled = false;
                setVisible(false);
                dispose();
            }
        });
        
        button = new JButton("Cancel");
        panel.addButton(button);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canceled = true;
                setVisible(false);
                dispose();
            }
        });
        return panel;
    }
    
    public boolean getCanceled() {
        return canceled;
    }
    
}
