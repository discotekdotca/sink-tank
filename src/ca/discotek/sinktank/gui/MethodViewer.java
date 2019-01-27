package ca.discotek.sinktank.gui;

import javax.swing.tree.TreePath;

import ca.discotek.sinktank.Method;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class MethodViewer extends AbstractNodeViewer {
    
    public MethodViewer(MethodTreeType type) {
        this(null, type);
    }
    
    public MethodViewer(Method methods[], MethodTreeType type) {
        super(type);
        buildGui();
        
        if (methods != null)
            addMethods(methods);
    }
    
    public void addMethods(final Method methods[]) {
        for (int i=0; i<methods.length; i++) {
            addMethod(methods[i]);
        }
    }
    
    public void expandAll() {
        TreeUtil.expandAll(tree);
    }
}
