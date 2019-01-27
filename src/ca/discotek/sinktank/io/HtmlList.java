package ca.discotek.sinktank.io;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class HtmlList {

    public final String name;
    
    public final List<Object> list = new ArrayList<Object>();
    
    public HtmlList() {
        this("");
    }
    
    public HtmlList(String name) {
        this.name = name;
    }
    
    public void addItem(Object item) {
        list.add(item);
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        
        buffer.append(name + "<ul>");
        for (Object o : list) {
            buffer.append("<li>");
            buffer.append(o == null ? "null" : o.toString());
            buffer.append("</li>");
        }
        buffer.append("</ul>");
        
        return buffer.toString();
    }
}
