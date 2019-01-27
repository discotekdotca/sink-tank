package ca.discotek.sinktank;

import java.util.List;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public abstract class TaintAdapter implements TaintListener {
    public void definedSourceMethod(Method source ) {}
    public void definedSinkMethod(Method source) {}
    public void definedSourceAnnotation(String source, boolean isMethod) {}
    public void definedSinkAnnotation(String sink, boolean isMethod) {}
    
    public void foundSource(List<Method> list) {}
    public void foundSink(List<Method> list) {}
    public void foundSourceSink(List<Method> sourceList, List<Method> sinkList) {}
}
