package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import ca.discotek.rebundled.org.objectweb.asm.Type;
import ca.discotek.sinktank.AsmUtil;
import ca.discotek.sinktank.Method;
import ca.discotek.sinktank.ProjectData;
import ca.discotek.sinktank.UserConfiguration;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public abstract class AbstractNodeViewer extends JPanel {

    final protected MethodTreeType type;
    
    final protected DefaultMutableTreeNode rootNode;
    final protected DefaultTreeModel model;
    protected JTree tree;
    protected JViewport viewport;
    
    Map<String, PackageNode> packageNodeMap = new HashMap<String, PackageNode>();
    Map<String, ClassNode> classNodeMap = new HashMap<String, ClassNode>();
    Map<Method, MethodNode> methodNodeMap = new HashMap<Method, MethodNode>();
    
    Map<String, Set<String>> packageClassMap = new HashMap<String, Set<String>>();
    Map<String, Set<Method>> classMethodMap = new HashMap<String, Set<Method>>();
    
    Set<Method> disabledMethodSet = new HashSet<Method>();
    
    class DisableTreeCellRenderer extends DefaultTreeCellRenderer {
        
        public Component getTreeCellRendererComponent(JTree tree,
                Object value,
                boolean selected,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {
            
            Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            String valueText = value == null ? null : value.toString();
            boolean found = false;
            String name, params, vulnerableIndices;
            for (Method m : disabledMethodSet) {
                name = ProjectData.getMethodName(m.memberId);
                params = '(' +  AsmUtil.toParameterTypesString( ProjectData.getMethodDesc(m.descId) ) + ')';
                vulnerableIndices = UserConfiguration.getVulnerableParametersText(m, false);
                if ((name + params + (vulnerableIndices.length() == 0 ? "" : ' ' + vulnerableIndices)).equals(valueText)) {
                    found = true;
                    break;
                }
            }
            c.setEnabled(!found);
            return c;
        }
    }
    
    public AbstractNodeViewer(MethodTreeType type) {
        this.type = type;
        rootNode = new DefaultMutableTreeNode("root");
        model = new DefaultTreeModel(rootNode);
        buildGui();
    }
    
    void buildGui() {
        setLayout(new BorderLayout());
        tree = new JTree(model);
        tree.setCellRenderer(new DisableTreeCellRenderer());
        tree.setShowsRootHandles(true);
        tree.setRootVisible(false);
        JScrollPane scroller = new JScrollPane(tree);
        viewport = scroller.getViewport();
        add(scroller, BorderLayout.CENTER);
    }
    public void addPopupMouseListener(MouseListener l) {
        tree.addMouseListener(l);
    }
    
    public Method[] getMethods() {
        return methodNodeMap.keySet().toArray(new Method[methodNodeMap.size()]);
    }
    
    public Method[] getSelectedMethods() {
        List<Method> list = new ArrayList<Method>();
        TreePath paths[] = tree.getSelectionPaths();
        Object o;
        MethodNode node;
        for (int i=0; paths != null && i<paths.length; i++) {
            o = paths[i].getLastPathComponent();
            if (o instanceof MethodNode) {
                node = (MethodNode) o;
                list.add(node.method);
            }
        }
            
        return list.toArray(new Method[list.size()]);
    }
    
    public void reset() {
        TreeUtil.removeAllChildren(rootNode, model);
        
        packageNodeMap.clear();
        classNodeMap.clear();
        methodNodeMap.clear();
    }

    public MethodNode addMethod(Method method) {
        MethodNode methodNode = null;
        if (type == MethodTreeType.Fqn) {
            methodNode = new MethodNode(method, type);
            TreeUtil.insertNode(methodNode, rootNode, model);
        }
        else {
            String className = ProjectData.getClassName(method.classId);
            String names[] = className.split("\\.");
            StringBuilder buffer = new StringBuilder();
            ClassNode classNode = null;
            PackageNode packageNode = null;
            String name;
            DefaultMutableTreeNode parentNode = rootNode;
            
            for (int i=0; i<names.length; i++) {
                if (i>0)
                    buffer.append(".");
                buffer.append(names[i]);
                
                name = buffer.toString();
            
                if (i==names.length-1) {
                    if (type == MethodTreeType.ClassMethodParameters) {
                        methodNode = new MethodNode(method, type);
                    }
                    else {
                        classNode = classNodeMap.get(name);
                        if (classNode == null) {
                            classNode = new ClassNode(name, names[i]);
                            classNodeMap.put(name, classNode);
                            TreeUtil.insertNode(classNode, parentNode, model);
                        }

                        methodNode = methodNodeMap.get(method);
                        if (methodNode == null) {
                            methodNode = new MethodNode(method, type);
                            methodNodeMap.put(method,  methodNode);
                            TreeUtil.insertNode(methodNode, classNode, model);
                        }

                        if (type == MethodTreeType.Method) {
                            methodNode = new MethodNode(method, type);
                            Type methodType = Type.getMethodType(ProjectData.getMethodDesc(method.descId));
                            Type argTypes[] = methodType.getArgumentTypes();
                            ParameterNode parameterNode;
                            for (int j=0; j<argTypes.length; j++) {
                                parameterNode = new ParameterNode(method, j);
                                TreeUtil.insertNode(parameterNode, methodNode, model);
                            }
                        }
                    }
                }
                else {
                    packageNode = packageNodeMap.get(name);
                    if (packageNode == null) {
                        packageNode = new PackageNode(names[i], name);
                        packageNodeMap.put(name, packageNode);
                        TreeUtil.insertNode(packageNode, parentNode, model);
                    }
                    
                    tree.expandPath(new TreePath(parentNode.getPath()));
                    
                    parentNode = packageNode;
                }
            }
        }
        
        methodNodeMap.put(method, methodNode);
        
        return methodNode;
    }

    public void removeMethod(Method method) {
      MethodNode methodNode = methodNodeMap.remove(method);
      if (methodNode != null) {
          DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) methodNode.getParent();
          if (parentNode != null) {
              model.removeNodeFromParent(methodNode);
              removeChildlessParents(parentNode);
          }
      }
      
      disabledMethodSet.remove(method);
  }
    
    public void resetConfiguration() {
        disabledMethodSet.clear();
    }
    
    public void toggleMethodEnabled(Method method) {
        if (!disabledMethodSet.remove(method))
            disabledMethodSet.add(method);
    }
    
    void removeChildlessParents(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();

        int childCount = node.getChildCount();
        
        if (childCount == 0) {
            if (node instanceof ClassNode)
                classNodeMap.remove( ((ClassNode) node).fqn );
            else if (node instanceof PackageNode)
                packageNodeMap.remove( ((PackageNode) node).fqn );

            if (parentNode != null) {
                model.removeNodeFromParent(node);
                if (parentNode != null) { // i.e. not root, which we don't ever want to remove
                    removeChildlessParents(parentNode);
                }
            }
        }
    }
}
