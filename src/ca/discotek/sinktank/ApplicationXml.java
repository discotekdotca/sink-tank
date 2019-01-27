package ca.discotek.sinktank;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class ApplicationXml extends DefaultHandler {

    public static final String NEW_LINE = System.getProperty("line.separator");

    StringBuilder charBuffer = new StringBuilder();
    
    boolean inModule = false;
    boolean inEjb = false;
    boolean inWeb = false;
    boolean inWebUri = false;
    
    List<String> ejbJarList = new ArrayList<String>();
    List<String> warList = new ArrayList<String>();
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        
        Iterator<String> it = ejbJarList.listIterator();
        
        if (it.hasNext()) {
            buffer.append("EJB Jars: ");
            buffer.append(NEW_LINE);
        }
        
        while (it.hasNext()) {
            buffer.append("\t" + it.next());
            buffer.append(NEW_LINE);
        }
        
        buffer.append(NEW_LINE);
        buffer.append(NEW_LINE);
        
        it = warList.listIterator();
        
        if (it.hasNext()) {
            buffer.append("Wars: ");
            buffer.append(NEW_LINE);
        }
        
        while (it.hasNext()) {
            buffer.append("\t" + it.next());
            buffer.append(NEW_LINE);
        }
        
        return buffer.toString();
    }
    
    public void parse(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(is, this);
    }
    
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        return new InputSource(new ByteArrayInputStream(new byte[0]));
    }
    
    public void characters(char[] ch, int start, int length) {
        charBuffer.append(ch, start, length);
    }
    
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        charBuffer.setLength(0);
        
        if (qName.equals("module"))
            inModule = true;
        else if (qName.equals("ejb")) {
            inEjb = true;
        }
        else if (inModule && qName.equals("web")) {
            inWeb = true;
        }
        else if (inModule && inWeb && qName.equals("web-uri")) {
            inWebUri = true;
        }
    }
    
    public void endElement(String uri, String localName, String qName) {
        if (inModule && inEjb)
            ejbJarList.add(charBuffer.toString().trim());
        else if (inModule && inWeb && inWebUri)
            warList.add(charBuffer.toString().trim());
            
        
        if (inModule && qName.equals("module"))
            inModule = false;
        else if (inModule && qName.equals("ejb"))
            inEjb = false;
        else if (inModule && qName.equals("web"))
            inWeb = false;
        else if (inModule && inWeb && qName.equals("web-uri"))
            inWebUri = false;
    }
    
    public void error(SAXParseException e) {
        e.printStackTrace();
    }
    
    public void fatalError(SAXParseException e) {
        e.printStackTrace();
    }
    
    public void warning(SAXParseException e) {
        e.printStackTrace();
    }
    
    public String[] getEjbJars() {
        return (String[]) ejbJarList.toArray(new String[ejbJarList.size()]);
    }
    
    public String[] getWars() {
        return (String[]) warList.toArray(new String[warList.size()]);
    }
}
