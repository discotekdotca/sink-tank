package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

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

public class NameSpaceSelectorDialog extends JDialog {

    DefaultMutableTreeNode rootNode;
    DefaultTreeModel model;
    JTree tree;
    
    boolean canceled = true;
    
    JButton okayButton;
    
    public NameSpaceSelectorDialog(JFrame frame, Method methods[]) {
        super(frame, "Application Namespace Selection", true);
        buildGui(getNamespaces(methods));
    }
    
    private String[] getNamespaces(Method methods[]) {
        
        String className;
        String packageName;
        int index;
        Set<String> nameSet = new HashSet<String>();
        for (int i=0; i<methods.length; i++) {
            className = ProjectData.getClassName(methods[i].classId);
            index = className.lastIndexOf('.');
            packageName = index < 0 ? "<empty>" : className.substring(0, index);
            nameSet.add(packageName);
        }
        
        return nameSet.toArray(new String[nameSet.size()]);
    }
    
    void buildGui(String namespaces[]) {
        setLayout(new BorderLayout());
        
        rootNode = new DefaultMutableTreeNode("root");
        model = new DefaultTreeModel(rootNode);
        tree = new JTree(model);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        
        Map<String, PackageNode> map = new HashMap<>();
        PackageNode node;
        String chunks[];
        StringBuilder buffer = new StringBuilder();
        String fqn;
        DefaultMutableTreeNode parentNode;
        for (int i=0; i<namespaces.length; i++) {
            parentNode = rootNode;
            buffer.setLength(0);
            chunks = namespaces[i].split("\\.");
            for (int j=0; j<chunks.length; j++) {
                if (j>0)
                    buffer.append('.');
                buffer.append(chunks[j]);
                fqn = buffer.toString();
                node = map.get(fqn);
                if (node == null) {
                    node = new PackageNode(chunks[j], fqn) {
                        public boolean isLeaf() {
                            return false;
                        }
                    };
                    map.put(fqn, node);
                    TreeUtil.insertNode(node, parentNode, model);
                }
                
                parentNode = node;
            }
        }

        tree.expandPath(new TreePath(rootNode.getPath()));

        int count = tree.getRowCount();
        tree.addSelectionInterval(0, count-1);
        
        add(new JScrollPane(tree), BorderLayout.CENTER);
        
        add(buildButtonPanel(), BorderLayout.SOUTH);
        
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                TreePath paths[] = tree.getSelectionPaths();
                boolean enabled = paths != null && paths.length > 0;
                okayButton.setEnabled(enabled);
            }
        });
    }
    
    JPanel buildButtonPanel() {
        ButtonPanel panel = new ButtonPanel();
        JButton button = okayButton = new JButton("Okay");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canceled = false;
                dispose();
                setVisible(false);
            }
        });
        panel.addButton(button);
        
        button = new JButton("Cancel");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canceled = true;
                dispose();
                setVisible(false);
            }
        });
        panel.addButton(button);
        return panel;
    }
    
    public boolean getCanceled() {
        return canceled;
    }
    
    public Map<Character, Set<String>> getNamespaces() {
        TreePath paths[] = tree.getSelectionPaths();
        Map<Character, Set<String>> map = new HashMap<Character, Set<String>>();

        Object o;
        String name;
        Character key;
        Set<String> set;
        for (int i=0; paths != null && i<paths.length; i++) {
             o = paths[i].getLastPathComponent();
             if (o instanceof PackageNode) {
                 name = ((PackageNode) o).fqn;
                 key = name.charAt(0);
                 set = map.get(key);
                 if (set == null) {
                     set = new HashSet<String>();
                     map.put(key, set);
                 }
                 set.add(name);
             }
        }

        
        return map;
    }
}
