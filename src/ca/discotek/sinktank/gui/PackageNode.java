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

public class PackageNode extends DefaultMutableTreeNode {

    public final String fqn;
    
    public PackageNode(String name, String fqn) {
        super(name);
        this.fqn = fqn;
    }
}
