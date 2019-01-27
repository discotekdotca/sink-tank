package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import ca.discotek.sinktank.AsmUtil;
import ca.discotek.sinktank.MethodAnnotationDescriptor;
import ca.discotek.sinktank.ParameterAnnotationDescriptor;
import ca.discotek.sinktank.ProjectData;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class AnnotationViewer extends JPanel {

    DefaultMutableTreeNode methodRootNode;
    DefaultTreeModel methodModel;
    JTree methodTree;
    
    DefaultMutableTreeNode parameterRootNode;
    DefaultTreeModel parameterModel;
    JTree parameterTree;
    

    Map<String, AnnotationTreeNode> baseAnnotationMethodMap = new LinkedHashMap<String, AnnotationTreeNode>();
    Map<String, MethodAnnotationDescriptorNode> realAnnotationMethodMap = new LinkedHashMap<String, MethodAnnotationDescriptorNode>();
    Map<String, AnnotationTreeNode> baseAnnotationParameterMap = new LinkedHashMap<String, AnnotationTreeNode>();
    Map<String, ParameterAnnotationDescriptorNode> realAnnotationParameterMap = new LinkedHashMap<String, ParameterAnnotationDescriptorNode>();
    
    Set<String> disabledMethodAnnotationSet = new HashSet<String>();
    Set<String> disabledMethodParameterAnnotationSet = new HashSet<String>();
    
    class DisableTreeCellRenderer extends DefaultTreeCellRenderer {
        
        Set<String> disabledAnnotationSet;
        
        public DisableTreeCellRenderer(Set<String> disabledAnnotationSet) {
            this.disabledAnnotationSet = disabledAnnotationSet;
        }
        
        public Component getTreeCellRendererComponent(JTree tree,
                Object value,
                boolean selected,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {
            
            Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            String valueText = value == null ? null : value.toString();
            c.setEnabled(!disabledAnnotationSet.contains(valueText.substring(1)));
            return c;
        }
    }
    
    
    
    public void reset() {
        TreeUtil.removeAllChildren(methodRootNode, methodModel);
        baseAnnotationMethodMap.clear();
        realAnnotationMethodMap.clear();
        TreeUtil.removeAllChildren(parameterRootNode, parameterModel);
        baseAnnotationParameterMap.clear();
        realAnnotationParameterMap.clear();
    }
    
    public AnnotationViewer() {
        methodRootNode = new DefaultMutableTreeNode("Method Root");
        methodModel = new DefaultTreeModel(methodRootNode);
        
        parameterRootNode = new DefaultMutableTreeNode("Parameter Root");
        parameterModel = new DefaultTreeModel(parameterRootNode);
        
        buildGui();
    }
    
    void buildGui() {
        setLayout(new BorderLayout());
        
        JTabbedPane tabber = new JTabbedPane();
        add(tabber, BorderLayout.CENTER);
        
        methodTree = new JTree(methodModel);
        methodTree.setCellRenderer(new DisableTreeCellRenderer(disabledMethodAnnotationSet));
        methodTree.setShowsRootHandles(true);
        methodTree.setRootVisible(false);
        tabber.addTab("Method", new JScrollPane(methodTree));
        
        parameterTree = new JTree(parameterModel);
        parameterTree.setCellRenderer(new DisableTreeCellRenderer(disabledMethodParameterAnnotationSet));
        parameterTree.setShowsRootHandles(true);
        parameterTree.setRootVisible(false);
        tabber.addTab("Parameter", new JScrollPane(parameterTree));
    }
    
    public String[] getMethodAnnotations() {
        List<String> list = new ArrayList<String>();
        int count = methodRootNode.getChildCount();
        for (int i=0; i<count; i++) {
            list.add( ( (AnnotationTreeNode) methodRootNode.getChildAt(i)).text );
        }
        
        return list.toArray(new String[list.size()]);
    }
    
    public String[] getSelectedMethodAnnotations() {
        List<String> list = new ArrayList<String>();
        TreePath paths[] = methodTree.getSelectionPaths();
        if (paths != null) {
            Object o;
            for (int i=0; i<paths.length; i++) {
                if (paths[i] != null) {
                    o = paths[i].getLastPathComponent();
                    if (o instanceof AnnotationTreeNode) {
                        list.add( ((AnnotationTreeNode) o).text );
                    }
                }
            }
        }
        
        return list.toArray(new String[list.size()]);
    }
    
    public String[] getParameterAnnotations() {
        List<String> list = new ArrayList<String>();
        int count = parameterRootNode.getChildCount();
        for (int i=0; i<count; i++) {
            list.add( ( (AnnotationTreeNode) parameterRootNode.getChildAt(i)).text );
        }
        
        return list.toArray(new String[list.size()]);
    }
    
    public String[] getSelectedParameterAnnotations() {
        List<String> list = new ArrayList<String>();
        TreePath paths[] = parameterTree.getSelectionPaths();
        if (paths != null) {
            Object o;
            for (int i=0; i<paths.length; i++) {
                if (paths[i] != null) {
                    o = paths[i].getLastPathComponent();
                    if (o instanceof AnnotationTreeNode) {
                        list.add( ((AnnotationTreeNode) o).text );
                    }
                }
            }
        }
        
        return list.toArray(new String[list.size()]);
    }
    
    public void addMethodAnnotationPopupMouseListener(MouseListener l) {
        methodTree.addMouseListener(l);
    }
    
    public void addParameterAnnotationPopupMouseListener(MouseListener l) {
        parameterTree.addMouseListener(l);
    }
    
    public AnnotationTreeNode addMethodAnnotation(String annotation) {
        AnnotationTreeNode treeNode = new AnnotationTreeNode(annotation);
        TreeUtil.insertNode(treeNode, methodRootNode, methodModel);

        if (!methodTree.isExpanded(new TreePath(methodRootNode.getPath()))) {
            TreeUtil.expand(methodRootNode, methodTree);
        }
        TreeUtil.expand(treeNode, methodTree);
        
        return treeNode;
    }
    
    public void removeMethodAnnotation(String annotation) {
        int count = methodRootNode.getChildCount();
        for (int i=0; i<count; i++) {
            AnnotationTreeNode node = (AnnotationTreeNode) methodRootNode.getChildAt(i);
            if (node.text.equals(annotation)) {
                methodModel.removeNodeFromParent(node);
                break;
            }
        }
    }
    
    public void removeMethodParameterAnnotation(String annotation) {
        int count = parameterRootNode.getChildCount();
        for (int i=0; i<count; i++) {
            AnnotationTreeNode node = (AnnotationTreeNode) parameterRootNode.getChildAt(i);
            if (node.text.equals(annotation)) {
                parameterModel.removeNodeFromParent(node);
                break;
            }
        }
    }
    
    
    public void resetConfiguration() {
        disabledMethodAnnotationSet.clear();
        disabledMethodParameterAnnotationSet.clear();
    }
    
    public void toggleMethodAnnotationEnabled(String annotation) {
        if (!disabledMethodAnnotationSet.remove(annotation))
            disabledMethodAnnotationSet.add(annotation);
        methodTree.repaint();
    }
    
    public void toggleParameterAnnotationEnabled(String annotation) {
        if (!disabledMethodParameterAnnotationSet.remove(annotation))
            disabledMethodParameterAnnotationSet.add(annotation);
        methodTree.repaint();
    }
     
    public void addMethodAnnotation(MethodAnnotationDescriptor descriptor) {
        String rootAnnotation = AsmUtil.toTypeString(descriptor.node.desc);
        AnnotationTreeNode treeNode = baseAnnotationMethodMap.get(rootAnnotation);
        if (treeNode == null) {
            treeNode = addMethodAnnotation(rootAnnotation);
            baseAnnotationMethodMap.put(rootAnnotation, treeNode);
        }
        
        MethodAnnotationDescriptorNode node = new MethodAnnotationDescriptorNode(descriptor);
        String realName = node.getUserObject().toString();
        MethodAnnotationDescriptorNode existingNode = realAnnotationMethodMap.get(realName);
        if (existingNode == null) {
            TreeUtil.insertNode(node, treeNode, methodModel);
            TreeUtil.expand(node, methodTree);
            realAnnotationMethodMap.put(realName, node);
        }
        else
            node = existingNode;
        
        
        MethodNode methodNode = new MethodNode(descriptor.method, MethodTreeType.Fqn);
        TreeUtil.insertNode(methodNode, node, methodModel);
        TreeUtil.expand(methodNode, methodTree);
    }
    
    public void addMethodAnnotations(MethodAnnotationDescriptor descriptors[]) {
        for (int i=0; i<descriptors.length; i++)
            addMethodAnnotation(descriptors[i]);
        
        TreeUtil.expand(methodRootNode, methodTree);
    }
    
    MethodNode getMethodNode(MethodAnnotationDescriptorNode node, String childName) {
        int count = node.getChildCount();
        MethodNode methodNode;
        for (int i=0; i<count; i++) {
            methodNode = (MethodNode) node.getChildAt(i);
            if (methodNode.getUserObject().toString().equals(childName))
                return methodNode;
        }
        
        return null;
    }
    
    ParameterNamesMethodNode getParameterNamesMethodNode(ParameterAnnotationDescriptorNode node, String childName) {
        int count = node.getChildCount();
        ParameterNamesMethodNode parametersNode;
        for (int i=0; i<count; i++) {
            parametersNode = (ParameterNamesMethodNode) node.getChildAt(i);
            if (parametersNode.getUserObject().toString().equals(childName))
                return parametersNode;
        }
        
        return null;
    }
    
    public void removeMethodAnnotation(MethodAnnotationDescriptor descriptor) {
        String name = AsmUtil.toAnnotationString(descriptor.node);
        MethodAnnotationDescriptorNode node = realAnnotationMethodMap.get(name);
        MethodNode child = getMethodNode(node, descriptor.method.toString());
        methodModel.removeNodeFromParent(child);

        TreeUtil.removeChildlessParents(node, methodModel, true);
    }
    
    public AnnotationTreeNode addParameterAnnotation(String annotation) {
        AnnotationTreeNode treeNode = new AnnotationTreeNode(annotation);
        TreeUtil.insertNode(treeNode, parameterRootNode, parameterModel);
//        TreeUtil.expandAll(parameterTree);
        
        if (!parameterTree.isExpanded(new TreePath(parameterRootNode.getPath()))) {
            TreeUtil.expand(parameterRootNode, parameterTree);
        }
        TreeUtil.expand(treeNode, parameterTree);
        
        return treeNode;
    }
    
    public void addParameterAnnotation(ParameterAnnotationDescriptor descriptor) {
        String rootAnnotation = AsmUtil.toTypeString(descriptor.node.desc);
        AnnotationTreeNode treeNode = baseAnnotationParameterMap.get(rootAnnotation);
        if (treeNode == null) {
            treeNode = addParameterAnnotation(rootAnnotation);
            baseAnnotationParameterMap.put(rootAnnotation, treeNode);
        }

        ParameterAnnotationDescriptorNode node = new ParameterAnnotationDescriptorNode(descriptor);
        String realName = node.getUserObject().toString();
        ParameterAnnotationDescriptorNode existingNode = realAnnotationParameterMap.get(realName);
        if (existingNode == null) {
            TreeUtil.insertNode(node, treeNode, parameterModel);
            TreeUtil.expand(node, parameterTree);
            realAnnotationParameterMap.put(realName, node);
        }
        else
            node = existingNode;
        
        ParameterNamesMethodNode parameterNode = new ParameterNamesMethodNode(descriptor.method, descriptor.parameterIndex);
        TreeUtil.insertNode(parameterNode, node, parameterModel);
        TreeUtil.expand(parameterNode, parameterTree);
    }
    
    public void addParameterAnnotations(ParameterAnnotationDescriptor descriptors[]) {
        for (int i=0; i<descriptors.length; i++)
            addParameterAnnotation(descriptors[i]);
        
        TreeUtil.expand(parameterRootNode, parameterTree);
    }
    
    public void removeParameterAnnotation(ParameterAnnotationDescriptor descriptor) {
        String className = ProjectData.getClassName(descriptor.method.classId);
        String method = ProjectData.getMethodName(descriptor.method.memberId);
        String desc = ProjectData.getMethodDesc(descriptor.method.descId);
        String parameterNames[] = ProjectData.getParameterNames(descriptor.method.classId, descriptor.method.memberId, descriptor.method.descId);
        String methodText = AsmUtil.toMethodSignatureWithParameterNames(className, method, desc, parameterNames, descriptor.parameterIndex);
        String annotation = AsmUtil.toAnnotationString(descriptor.node);
        ParameterAnnotationDescriptorNode node = realAnnotationParameterMap.get(annotation);
        ParameterNamesMethodNode child = getParameterNamesMethodNode(node, "<html>" + methodText + "</html>");
        parameterModel.removeNodeFromParent(child);

        TreeUtil.removeChildlessParents(node, parameterModel, true);
    }
    
    class AnnotationTreeNode extends DefaultMutableTreeNode {
        
        public final String text;
        
        public AnnotationTreeNode(String text) {
            super('@' + text);
            this.text = text;
        }
    }
    
    class MethodAnnotationDescriptorNode extends DefaultMutableTreeNode {
        
        MethodAnnotationDescriptor mad;
        
        public MethodAnnotationDescriptorNode(MethodAnnotationDescriptor mad) {
            this.mad = mad;
            String annotationText = AsmUtil.toAnnotationString(mad.node);
            setUserObject(annotationText);
        }
    }
    
    class ParameterAnnotationDescriptorNode extends DefaultMutableTreeNode {
        
        ParameterAnnotationDescriptor pad;
        
        public ParameterAnnotationDescriptorNode(ParameterAnnotationDescriptor pad) {
            this.pad = pad;
            String annotationText = AsmUtil.toAnnotationString(pad.node);
            setUserObject(annotationText);
        }
    }
}
