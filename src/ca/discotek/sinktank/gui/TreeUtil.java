package ca.discotek.sinktank.gui;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class TreeUtil {

    public synchronized static void insertNode(DefaultMutableTreeNode newChild, DefaultMutableTreeNode parent, DefaultTreeModel model) {
        int count = parent.getChildCount();
        if (count == 0) 
            model.insertNodeInto(newChild, parent, 0);
        else {
            String text = newChild.getUserObject().toString();
            DefaultMutableTreeNode child;
            boolean inserted = false;
            for (int i=0; i<count; i++) {
                child = (DefaultMutableTreeNode) parent.getChildAt(i);
                if (text.compareToIgnoreCase(child.getUserObject().toString()) < 0) {
                    model.insertNodeInto(newChild, parent, i);
                    inserted = true;
                    break;
                }   
            }
            
            if (!inserted)
                model.insertNodeInto(newChild, parent, count);
        }
    }
    
    public static void expandAll(final JTree tree) {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) model.getRoot();
        expand(rootNode, tree, true);
    }
    
    public static void expand(DefaultMutableTreeNode node, JTree tree) {
        expand(node, tree, false);
    }
    
    public static void expand(DefaultMutableTreeNode node, JTree tree, boolean recurse) {
        tree.expandPath(new TreePath(node.getPath()));
        if (recurse) {
            int count = node.getChildCount();
            for (int i=0; i<count; i++)
                expand( (DefaultMutableTreeNode) node.getChildAt(i), tree, true);
        }
    }
    
    public static void removeAllChildren(DefaultMutableTreeNode node, DefaultTreeModel model) {
        int count = node.getChildCount();
        for (int i=0; i<count; i++)
            model.removeNodeFromParent( (MutableTreeNode) node.getChildAt(0)); 
    }
    
    public static void removeChildlessParents(DefaultMutableTreeNode node, DefaultTreeModel model, boolean removeRoot) {
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
        if (parentNode == null && !removeRoot)
            return;

        int childCount = node.getChildCount();
        
        if (childCount == 0) {
            if (parentNode != null) {
                model.removeNodeFromParent(node);
                if (parentNode != null) { // i.e. not root, which we don't ever want to remove
                    removeChildlessParents(parentNode, model, removeRoot);
                }
            }
        }
    }
}
