package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

public class SourceSinkView extends JPanel {

    Map<Method, MethodNode> sinkRootMap = new HashMap<Method, MethodNode>();
    
    DefaultMutableTreeNode rootNode;
    DefaultTreeModel model;
    JTree tree;
    
    MethodNodeSelectionListener listener;
    
    final Map<Method, List<Method>> sourceOriginMap;
    final Map<Method, List<Method>> sinkOriginMap;
    
    final Map<Method, List<String>> sourceMethodAnnotationMap;
    final Map<Method, List<String>> sinkMethodAnnotationMap;
    final Map<Method, List<String>> sourceMethodParameterAnnotationMap;
    final Map<Method, List<String>> sinkMethodParameterAnnotationMap;
    
    public SourceSinkView(Map<Method, List<Method>> sourceOriginMap, Map<Method, List<Method>> sinkOriginMap,
            Map<Method, List<String>> sourceMethodAnnotationMap, Map<Method, List<String>> sinkMethodAnnotationMap,
            Map<Method, List<String>> sourceMethodParameterAnnotationMap, Map<Method, List<String>> sinkMethodParameterAnnotationMap) {
        
        this.sourceOriginMap = sourceOriginMap;
        this.sinkOriginMap = sinkOriginMap;
        
        this.sourceMethodAnnotationMap = sourceMethodAnnotationMap;
        this.sinkMethodAnnotationMap = sinkMethodAnnotationMap;
        this.sourceMethodParameterAnnotationMap = sourceMethodParameterAnnotationMap;
        this.sinkMethodParameterAnnotationMap = sinkMethodParameterAnnotationMap;
        
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
                        
                        StringBuilder buffer = new StringBuilder();

                        buffer.append("<html>");

                        List<Method> sourceOriginList = sourceOriginMap.get(node.method);
                        List<Method> sinkOriginList = sinkOriginMap.get(node.method);

                        boolean found = sourceOriginList != null || sinkOriginList != null;
                        
                        if (found) {
                            buffer.append("<ul>");
                            buffer.append("This method is included because it is a descendent of the following sink/source method(s):");
                        }
                        if (sourceOriginList != null) {
                            Iterator<Method> it = sourceOriginList.listIterator();
                            while (it.hasNext()) {
                                buffer.append("<li>" + it.next() + " [source]</li>");
                            }
                        }
                        
                        if (sinkOriginList != null) {
                            found = true;
                            Iterator<Method> it = sinkOriginList.listIterator();
                            while (it.hasNext()) {
                                buffer.append("<li>" + it.next() + " [sink]</li>");
                            }
                        }

                        if (found)
                            buffer.append("</ul>");

                        List<String> list = sourceMethodAnnotationMap.get(node.method);
                        Set<String> set = new HashSet<>();

                        if (list != null) {
                            found = true;
                            buffer.append("This method has the following <b>source</b> method annotation(s):");
                            buffer.append("<ul>");
                            Iterator<String> annotationIt = list.listIterator();
                            String annotation;
                            set.clear();
                            while (annotationIt.hasNext()) {
                                annotation = annotationIt.next();
                                if (!set.contains(annotation)) {
                                    buffer.append("<li>" + annotation + "</li>");
                                    set.add(annotation);
                                }
                            }
                            buffer.append("</ul>");
                        }
                        
                        list = sourceMethodParameterAnnotationMap.get(node.method);
                        if (list != null) {
                            found = true;
                            buffer.append("This method has the following <b>source</b> method parameter annotation(s):");
                            buffer.append("<ul>");
                            Iterator<String> annotationIt = list.listIterator();
                            String annotation;
                            set.clear();
                            while (annotationIt.hasNext()) {
                                annotation = annotationIt.next();
                                if (!set.contains(annotation)) {
                                    buffer.append("<li>" + annotation + "</li>");
                                    set.add(annotation);
                                }
                            }
                            buffer.append("</ul>");
                        }
                        
                        list = sinkMethodAnnotationMap.get(node.method);
                        if (list != null) {
                            found = true;
                            buffer.append("This method has the following <b>sink</b> method annotation(s):");
                            buffer.append("<ul>");
                            Iterator<String> annotationIt = list.listIterator();
                            String annotation;
                            set.clear();
                            while (annotationIt.hasNext()) {
                                annotation = annotationIt.next();
                                if (!set.contains(annotation)) {
                                    buffer.append("<li>" + annotation + "</li>");
                                    set.add(annotation);
                                }
                            }
                            buffer.append("</ul>");
                        }
                        
                        list = sinkMethodParameterAnnotationMap.get(node.method);
                        if (list != null) {
                            found = true;
                            buffer.append("This method has the following <b>sink</b> method parameter annotation(s):");
                            buffer.append("<ul>");
                            Iterator<String> annotationIt = list.listIterator();
                            String annotation;
                            set.clear();
                            while (annotationIt.hasNext()) {
                                annotation = annotationIt.next();
                                if (!set.contains(annotation)) {
                                    buffer.append("<li>" + annotation + "</li>");
                                    set.add(annotation);
                                }
                            }
                            buffer.append("</ul>");
                        }
                        
                        
                        
                        if (found) {
                            buffer.append("</html>");
                            text = buffer.toString();
                        }
                    }
                }
                
                tree.setToolTipText(text);
            }
        });
    }

    public void setMethodNodeSelectionListener(MethodNodeSelectionListener l) {
        this.listener = l;
    }

    public void addSourceSink(Path sourcePath, Path sinkPath) {
        Method sinkMethod = sinkPath.methods[sinkPath.methods.length-1];
        MethodNode sinkRootNode = sinkRootMap.get(sinkMethod);
        boolean sinkRootNodeIsNull = sinkRootNode == null;
        if (sinkRootNodeIsNull) {
            sinkRootNode = new MethodNode(sinkMethod, MethodTreeType.Fqn);
            sinkRootNode.setUserObject("<html><b>" + Util.escapeHtml(sinkRootNode.method.toString()) + "<b></html>");
            sinkRootMap.put(sinkMethod, sinkRootNode);
            TreeUtil.insertNode(sinkRootNode, rootNode, model);
        }
        
        DefaultMutableTreeNode parentNode = sinkRootNode;
        MethodNode methodNode = null;
        String text;
        
        MethodNode commonNode = null;
        if (!sinkRootNodeIsNull) {
            int count = sinkRootNode.getChildCount();
            for (int i=0; i<count; i++) {
                commonNode = (MethodNode) sinkRootNode.getChildAt(i);
                if (commonNode.method.toString().equals(sourcePath.methods[0].toString()))
                    break;
                else
                    commonNode = null;
            }
        }
        
        boolean commonNodeExists = commonNode != null;
        if (commonNode == null) {
            commonNode = new MethodNode(sourcePath.methods[0], MethodTreeType.Fqn);
            TreeUtil.insertNode(commonNode, parentNode, model);
            parentNode = commonNode;
        }
        
        if (sourcePath.methods.length > 1) {
            DefaultMutableTreeNode sourceNode;
            if (commonNodeExists)
                sourceNode = (DefaultMutableTreeNode) commonNode.getFirstChild();
            else {
                sourceNode = new DefaultMutableTreeNode("[Sources]");
                model.insertNodeInto(sourceNode, parentNode, parentNode.getChildCount());
            }
            parentNode = sourceNode;
        }
        
        for (int i=0; i<sourcePath.methods.length; i++) {
            if (i == 0)
                continue;
 
            methodNode = getMethodChild(sourcePath.methods[i], parentNode);
            if (methodNode == null) {
                methodNode = new MethodNode(sourcePath.methods[i], MethodTreeType.Fqn);
                text = i<sourcePath.methods.length-1 ? sourcePath.methods[i].toString() : "<html><font color=\"red\">" + Util.escapeHtml(sourcePath.methods[i].toString()) + "</font></html>";
                methodNode.setUserObject(text);
                TreeUtil.insertNode(methodNode, parentNode, model);
            }
            
            parentNode = methodNode;
        }

        if (sinkPath.methods.length > 1) {
            DefaultMutableTreeNode sinkNode;
            if (commonNodeExists)
                sinkNode = ((DefaultMutableTreeNode) commonNode.getFirstChild()).getNextSibling();
            else {
                sinkNode = new DefaultMutableTreeNode("[Sinks]");
                model.insertNodeInto(sinkNode, commonNode, commonNode.getChildCount());
            }
            parentNode = sinkNode;
        }
        
        for (int i=0; i<sinkPath.methods.length; i++) {
            if (i == 0)
                continue;

            methodNode = getMethodChild(sinkPath.methods[i], parentNode);
            if (methodNode == null) {
                methodNode = new MethodNode(sinkPath.methods[i], MethodTreeType.Fqn);
                text = i<sinkPath.methods.length-1 ? sinkPath.methods[i].toString() : "<html><font color=\"red\">" + Util.escapeHtml(sinkPath.methods[i].toString()) + "</font></html>";
                methodNode.setUserObject(text);
                TreeUtil.insertNode(methodNode, parentNode, model);
            }
            
            parentNode = methodNode;
        }

        
        
        TreeUtil.expand(rootNode, tree, true);
    }
    
    MethodNode getMethodChild(Method method, DefaultMutableTreeNode parentNode) {
        int count = parentNode.getChildCount();
        for (int i=0; i<count; i++) {
            MethodNode node = (MethodNode) parentNode.getChildAt(i);
            if (node.method == method)
                return node;
        }
        
        return null;
    }
    
}
