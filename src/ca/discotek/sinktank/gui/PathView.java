package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import ca.discotek.sinktank.Method;
import ca.discotek.sinktank.MethodNodeSelectionListener;
import ca.discotek.sinktank.Path;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class PathView extends JPanel {

    DefaultMutableTreeNode rootNode;
    DefaultTreeModel model;
    JTree tree;
    
    MethodNodeSelectionListener listener;
    
    Map<Method, List<Method>> originMap;
    Map<Method, List<String>> methodAnnotationMap;
    Map<Method, List<String>> methodParameterAnnotationMap;
    
    public PathView(Map<Method, List<Method>> originMap, Map<Method, List<String>> methodAnnotationMap, Map<Method, List<String>> methodParameterAnnotationMap) {
        this.originMap = originMap;
        this.methodAnnotationMap = methodAnnotationMap;
        this.methodParameterAnnotationMap = methodParameterAnnotationMap;
        rootNode = new DefaultMutableTreeNode("root");
        buildGui();
    }
    
    void buildGui() {
        setLayout(new BorderLayout());
        model = new DefaultTreeModel(rootNode);
        tree = new JTree(model);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(false);
        add(new JScrollPane(tree), BorderLayout.CENTER);
        
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                TreePath path = e.getPath();
                if (path != null) {
                    Object o = path.getLastPathComponent();
                    if (o instanceof MethodNode) {
                        MethodNode m = (MethodNode) e.getPath().getLastPathComponent();
                        listener.methodSelected(m.method);
                    }
                }
            }
        });
        
        tree.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                String text = null;
                TreePath path = tree.getPathForLocation(e.getX(),  e.getY());
                if (path != null) {
                    Object o = path.getLastPathComponent();
                    if (o instanceof MethodNode) {
                        MethodNode node = (MethodNode) o;
                        List<Method> originList = originMap.get(node.method);
                        if (originList != null) {
                            Iterator<Method> it = originList.listIterator();
                            StringBuilder buffer = new StringBuilder();
                            buffer.append("<html>");
                            buffer.append("This method is included because it is a descendent of the following method(s):");
                            buffer.append("<ul>");
                            while (it.hasNext()) {
                                buffer.append("<li>" + it.next() + "</li>");
                            }
                            buffer.append("</ul>");
                            
                            List<String> list = methodAnnotationMap.get(node.method);
                            if (list != null) {
                                buffer.append("It has the following method annotation(s):");
                                buffer.append("<ul>");
                                Iterator<String> annotationIt = list.listIterator();
                                while (annotationIt.hasNext()) {
                                    buffer.append("<li>" + annotationIt.next() + "</li>");
                                }
                                buffer.append("</ul>");
                            }
                            
                            list = methodParameterAnnotationMap.get(node.method);
                            if (list != null) {
                                buffer.append("It has the following method parameter annotation(s):");
                                buffer.append("<ul>");
                                Iterator<String> annotationIt = list.listIterator();
                                while (annotationIt.hasNext()) {
                                    buffer.append("<li>" + annotationIt.next() + "</li>");
                                }
                                buffer.append("</ul>");
                            }
                            
                            buffer.append("</html>");
                            
                            text = buffer.toString();
                        }
                    }
                }
                
                setToolTipText(text);
            }
        });
    }

    public void setClassNodeSelectionListener(MethodNodeSelectionListener l) {
        this.listener = l;
    }
    
    public void addPath(Path path) {
        DefaultMutableTreeNode parentNode = rootNode;
        for (int i=path.methods.length-1; i>=0; i--)
            parentNode = addMethod(path.methods[i], parentNode);
    }
    
    DefaultMutableTreeNode addMethod(Method method, DefaultMutableTreeNode parentNode) {
        int count = parentNode.getChildCount();
        MethodNode childNode;
        for (int i=0; i<count; i++) {
            childNode = (MethodNode) parentNode.getChildAt(i);
            if (childNode.method == method)
                return childNode;
        }

        childNode = new MethodNode(method, MethodTreeType.Fqn);
        TreeUtil.insertNode(childNode, parentNode, model);
        
        TreeUtil.expand(parentNode, tree);
        TreeUtil.expand(childNode, tree);
        
        return childNode;
    }
}
