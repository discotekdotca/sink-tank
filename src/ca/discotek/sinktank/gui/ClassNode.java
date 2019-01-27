package ca.discotek.sinktank.gui;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class ClassNode extends DefaultMutableTreeNode {

    public final String fqn;
    public final String name;
    
    public ClassNode(String fqn, String name) {
        super(name);
        this.fqn = fqn;
        this.name = name;
    }
}
