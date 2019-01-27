package ca.discotek.sinktank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class ResultsConsolidator extends TaintAdapter {

    List<Path> sourceLists = new ArrayList<Path>();
    List<Path> sinkLists = new ArrayList<Path>();

    List<Path> sourceSinkSourceList = new ArrayList<Path>();
    List<Path> sourceSinkSinkList = new ArrayList<Path>();
    
    boolean contains(Path containerPath, Path path) {
        int index = containerPath.indexOf(path.methods[0]);
        if (index < 0)
            return false;
        else {
            int length = path.methods.length;
            
            if (index + length > containerPath.methods.length)
                return false;
            
            for (int i=1; i<length; i++) {
                if (!containerPath.methods[index + i].equals(path.methods[i]))
                    return false;
            }
            
            return true;
        }
    }

    void updateLists(List<Path> lists, Path list) {
        Path listArray[] = lists.toArray(new Path[lists.size()]);
        for (int i=0; i<listArray.length; i++) {
            if (contains(listArray[i], list))
                lists.remove(listArray[i]);
        }
        
        lists.add(list);
    }
    
    public synchronized void foundSource(List<Method> list) {
        updateLists(sourceLists, new Path(list.toArray(new Method[list.size()])));
    }
    
    public synchronized void foundSink(List<Method> list) {
        updateLists(sinkLists, new Path(list.toArray(new Method[list.size()])));
    }
    
    public synchronized void foundSourceSink(List<Method> sourceList, List<Method> sinkList) {
        Path sourcePath = new Path(sourceList.toArray(new Method[sourceList.size()]));
        Path sinkPath = new Path(sinkList.toArray(new Method[sinkList.size()]));
        
        Path sourceArray[] = sourceSinkSourceList.toArray(new Path[sourceSinkSourceList.size()]);
        Path sinkArray[] = sourceSinkSinkList.toArray(new Path[sourceSinkSinkList.size()]);
        
        boolean exists = false;
        for (int i=0; i<sourceArray.length; i++) {
            exists = false;
            if (contains(sourcePath, sourceArray[i]) && sourcePath.methods.length > sourceArray[i].methods.length)
                sourceArray[i] = sourcePath;
            
            if (contains(sinkPath, sinkArray[i]) && sinkPath.methods.length > sinkArray[i].methods.length)
                sinkArray[i] = sinkPath;
            
            if (!exists && contains(sourcePath, sourceArray[i]) && contains(sinkPath, sinkArray[i]))
                exists = true;
        }
        
        if (!exists) {
            sourceSinkSourceList.add(sourcePath);
            sourceSinkSinkList.add(sinkPath);
        }
    }
    
    public Path[] getSourcePaths(boolean sort) {
        Path paths[] = sourceLists.toArray(new Path[sourceLists.size()]);
        if (sort) {
            Arrays.sort(paths, new Comparator<Path>() {
                public int compare(Path o1, Path o2) {
                    return o1.methods[o1.methods.length-1].toString().compareToIgnoreCase(o2.methods[o2.methods.length-1].toString());
                }
            });
        }
        return paths;
    }
    
    public Path[] getSinkPaths(boolean sort) {
        Path paths[] = sinkLists.toArray(new Path[sinkLists.size()]);
        if (sort) {
            Arrays.sort(paths, new Comparator<Path>() {
                public int compare(Path o1, Path o2) {
                    return o1.methods[o1.methods.length-1].toString().compareToIgnoreCase(o2.methods[o2.methods.length-1].toString());
                }
            });
        }
        return paths;
    }
    
    public Path[][] getSourceSinkPaths(boolean sort, final boolean sortBySink) {
        Path sourcePaths[] = sourceSinkSourceList.toArray(new Path[sourceSinkSourceList.size()]);
        Path sinkPaths[] = sourceSinkSinkList.toArray(new Path[sourceSinkSinkList.size()]);
        
        Path paths[][] = new Path[sourcePaths.length][2];
        for (int i=0; i<paths.length; i++) {
            paths[i][0] = sourcePaths[i];
            paths[i][1] = sinkPaths[i];
        }
        
        if (sort)
            Arrays.sort(paths, new Comparator<Path[]>() {
                public int compare(Path[] o1, Path[] o2) {
                    return sortBySink ?
                        o1[1].methods[o1[1].methods.length-1].toString().compareTo(o2[1].methods[o2[1].methods.length-1].toString()) :
                        o1[0].methods[0].toString().compareTo(o2[0].methods[0].toString());
                }
            });
        return paths;
    }
}
