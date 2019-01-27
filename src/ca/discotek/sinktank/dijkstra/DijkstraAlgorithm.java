package ca.discotek.sinktank.dijkstra;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import ca.discotek.sinktank.Method;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class DijkstraAlgorithm {


    private Set<Method> settledNodes;
    private TreeSet<Method> unSettledNodes;
    private Map<Method, Method> predecessors;

    Map<Method, Set<Method>> neighborMap;
    
    public DijkstraAlgorithm(Map<Method, Set<Method>> neighborMap) {
        this.neighborMap = neighborMap;
    }

    public void execute(Method source) {
        settledNodes = new HashSet<Method>();
        unSettledNodes = new TreeSet<Method>();
        predecessors = new HashMap<Method, Method>();
        unSettledNodes.add(source);
        while (unSettledNodes.size() > 0) {
            Method node = unSettledNodes.first();
            settledNodes.add(node);
            unSettledNodes.remove(node);
            findMinimalDistances(node);
        }
    }

    private void findMinimalDistances(Method node) {
        Set<Method> adjacentNodes = getNeighbors(node);
        for (Method target : adjacentNodes) {
            predecessors.put(target, node);
            unSettledNodes.add(target);
        }

    }

    private int getDistance(Method node, Method target) {
        return 0;
    }

    private Set<Method> getNeighbors(Method node) {
        Set<Method> neighbors = neighborMap.get(node);
        Set<Method> clone = neighbors == null ? new HashSet<Method>() : new HashSet<Method>(neighbors);
        Iterator<Method> it = neighbors == null ? null : neighbors.iterator();
        Method m;
        if (it != null) {
            while (it.hasNext()) {
                m = it.next();
                if (settledNodes.contains(m)) // is settled
                    clone.remove(m);
            }
        }
        
        return clone;
    }


    /*
     * This method returns the path from the source to the selected target and
     * NULL if no path exists
     */
    public List<Method> getPath(Method target) {
        List<Method> path = new LinkedList<Method>();
        Method step = target;
        // check if a path exists
        if (predecessors.get(step) == null) {
            return null;
        }
        path.add(step);
        while (predecessors.get(step) != null) {
            step = predecessors.get(step);
            path.add(step);
        }
        // Put it into the correct order
        Collections.reverse(path);
        return path;
    }

}