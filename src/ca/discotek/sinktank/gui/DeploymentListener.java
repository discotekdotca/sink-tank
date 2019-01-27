package ca.discotek.sinktank.gui;

import java.io.File;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public interface DeploymentListener {

    public void deploymentAdded(File file);
    public void deploymentRemoved(File file);
}
