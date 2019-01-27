package ca.discotek.sinktank.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class ProgressSupport {

    List<JobListener> list = new ArrayList<JobListener>();
    
    public static ProgressSupport INSTANCE = new ProgressSupport();
    
    public static ProgressSupport getInstance() {
        return INSTANCE;
    }
    
    private ProgressSupport() {}
    
    public void addListener(JobListener l) {
        list.add(l);
    }
    
    public void removeListener(JobListener l) {
        list.remove(l);
    }
    
    public void fireStart(Callable<?> c, int start, int end, String text) {
        Iterator<JobListener> it = list.listIterator();
        while (it.hasNext())
            it.next().start(c, start, end, text);
    }
    
    public void fireUpdate(Callable<?> c, int index) {
        Iterator<JobListener> it = list.listIterator();
        while (it.hasNext())
            it.next().update(c, index);
    }
    
    public void fireFinished(Callable<?> c) {
        Iterator<JobListener> it = list.listIterator();
        while (it.hasNext())
            it.next().finished(c);
    }
    
}
