package ca.discotek.sinktank.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ca.discotek.sinktank.Method;
import ca.discotek.sinktank.dijkstra.Edge;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class EdgeViewer extends AbstractNodeViewer {
    
    final MethodTreeType type;
    final boolean isOutgoing;
    
    public EdgeViewer(MethodTreeType type) {
        this(type, true);
    }
    
    public EdgeViewer(MethodTreeType type, boolean isOutgoing) {
        super(type);
        this.type = type;
        this.isOutgoing = isOutgoing;
    }
    
    public void addEdges(final Edge edges[]) {
        Map<MethodNode, List<MethodNode>> map = new HashMap<>();
        
        Method sourceMethod, destinationMethod;
        MethodNode node;
        for (int i=0; i<edges.length; i++) {
            if (isOutgoing) {
                sourceMethod = edges[i].source;
                destinationMethod = edges[i].destination;
            }
            else {
                sourceMethod = edges[i].destination;
                destinationMethod = edges[i].source;
            }
            
            node = addMethod(sourceMethod);
            MethodNode destinationNode = new MethodNode(destinationMethod, MethodTreeType.Fqn);
            List<MethodNode> list = map.get(node);
            if (list == null) {
                list = new ArrayList<>();
                map.put(node,  list);
            }
            list.add(destinationNode);
        }
        
        Map.Entry<MethodNode, List<MethodNode>> entries[] = map.entrySet().toArray(new Map.Entry[map.size()]);
        Arrays.sort(entries, new Comparator<Map.Entry<MethodNode, List<MethodNode>>>() {
            public int compare(Entry<MethodNode, List<MethodNode>> o1, Entry<MethodNode, List<MethodNode>> o2) {
                return o1.getKey().getUserObject().toString().compareTo(o2.getKey().getUserObject().toString());
            }
        });

        Iterator<MethodNode> it;
        MethodNode parentNode;
        for (int i=0; i<entries.length; i++) {
            Collections.sort(entries[i].getValue(), new Comparator<MethodNode>() {
                public int compare(MethodNode o1, MethodNode o2) {
                    return o1.getUserObject().toString().compareTo(o2.getUserObject().toString());
                }
            });
            
            it = entries[i].getValue().listIterator();
            while (it.hasNext()) {
                parentNode = it.next();
                model.insertNodeInto(parentNode, entries[i].getKey(), parentNode.getChildCount());
            }
        }

        
    }
    
    public void removeEdge(Edge edge) {
        removeMethod(isOutgoing ? edge.source : edge.destination);
    }
}
