package ca.discotek.sinktank.dijkstra;

import ca.discotek.sinktank.Method;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class Edge {

    public Method source;
    public Method destination;
    
    boolean flipped = false;
    
    public Edge(Method source, Method destination) {
        this.source = source;
        this.destination = destination;
    }
    
    public void flip() {
        flipped = true;
        Method v = source;
        source = destination;
        destination = v;
    }
    
    public void restore() {
        if (flipped) {
            flipped = false;
            Method v = source;
            source = destination;
            destination = v;
        }
    }
    
    public String toString() {
        return source + " -> " + destination;
    }
    
    public int hashCode() {
        // need to add something such that source->destination != destination->source,
        // hence the "+ source.hashCode() / 2)" part
        return (source.hashCode() + source.hashCode() / 2) + destination.hashCode();
    }
    
    public boolean equals(Object o) {
        if (o instanceof Edge) {
            Edge edge = (Edge) o;
            return edge.source.equals(source) && edge.destination.equals(destination);
        }
        else
            return false;
    }
}
