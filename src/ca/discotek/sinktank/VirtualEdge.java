package ca.discotek.sinktank;

import ca.discotek.sinktank.dijkstra.Edge;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class VirtualEdge extends Edge {
    
    public VirtualEdge(Method source, Method destination) {
        super(source, destination);
    }

}
