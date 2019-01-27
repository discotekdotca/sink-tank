package ca.discotek.sinktank.gui;

import java.util.concurrent.Callable;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public interface JobListener {
    public void start(Callable<?> job, int start, int end, String text);
    public void update(Callable<?> job, int index);
    public void finished(Callable<?> job);
}