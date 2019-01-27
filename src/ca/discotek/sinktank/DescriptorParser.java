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

public class DescriptorParser {
    
    List<String> sourceMethodAnnotationList = new ArrayList<>();
    List<String> sourceParameterAnnotationList = new ArrayList<>();

    List<String> sinkMethodAnnotationList = new ArrayList<>();
    List<String> sinkParameterAnnotationList = new ArrayList<>();

    List<Method> sourceMethodList = new ArrayList<Method>();
    List<Method> sinkMethodList = new ArrayList<Method>();
    
    StringBuilder charBuffer = new StringBuilder();
    
    String owner;
    String name;
    List<String> parameterTypeList = new ArrayList<String>();
    String returnType;
    List<Integer> vulnerableParameterIndexList = new ArrayList<Integer>();
    
    boolean isSourceParser = false;
    
    List<TaintListener> listenerList = new ArrayList<TaintListener>();
    
    public void addListener(TaintListener l) {
        listenerList.add(l);
    }
    
    public void fireDefinedSourceMethod(Method md) {
        Iterator<TaintListener> it = listenerList.listIterator();
        while (it.hasNext())
            it.next().definedSourceMethod(md);
    }
    
    public void fireDefinedSinkMethod(Method smd) {
        Iterator<TaintListener> it = listenerList.listIterator();
        while (it.hasNext())
            it.next().definedSinkMethod(smd);
    }
    
    public void fireDefinedSourceAnnotation(String annotation, boolean isMethod) {
        Iterator<TaintListener> it = listenerList.listIterator();
        while (it.hasNext())
            it.next().definedSourceAnnotation(annotation, isMethod);
    }
    
    public void fireDefinedSinkAnnotation(String annotation, boolean isMethod) {
        Iterator<TaintListener> it = listenerList.listIterator();
        while (it.hasNext())
            it.next().definedSinkAnnotation(annotation, isMethod);
    }
    
    
    public Method[] getSourceMethods() {
        return sourceMethodList.toArray(new Method[sourceMethodList.size()]);
    }
    
    public Method[] getSinkMethods() {
        return sinkMethodList.toArray(new Method[sinkMethodList.size()]);
    }
    
    public String[] getSourceMethodAnnotations() {
        return sourceMethodAnnotationList.toArray(new String[sourceMethodAnnotationList.size()]);
    }
    
    public String[] getSinkMethodAnnotations() {
        return sinkMethodAnnotationList.toArray(new String[sinkMethodAnnotationList.size()]);
    }
    
    public String[] getSourceParameterAnnotations() {
        return sourceParameterAnnotationList.toArray(new String[sourceParameterAnnotationList.size()]);
    }
    
    public String[] getSinkParameterAnnotations() {
        return sinkParameterAnnotationList.toArray(new String[sinkParameterAnnotationList.size()]);
    }
    
    public void parseMethodSources(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        isSourceParser = true;
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(is, new MethodDescriptorParser());
    }
    
    public void parseMethodSinks(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        isSourceParser = false;
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(is, new MethodDescriptorParser());
    }
    
    public void parseAnnotations(InputStream is, boolean isSource, boolean isMethod) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(is, new AnnotationDescriptorParser(isSource, isMethod));
    }

    
    class MethodDescriptorParser extends DefaultHandler {
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            return new InputSource(new ByteArrayInputStream(new byte[0]));
        }
        
        public void characters(char[] ch, int start, int length) {
            charBuffer.append(ch, start, length);
        }
        
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            charBuffer.setLength(0);
            
            if (qName.equals("method")) {
                owner = attributes.getValue("owner");
                name = attributes.getValue("name");
                if (name == null) {
                    String value = attributes.getValue("constructor");
                    boolean isConstructor = Boolean.parseBoolean(value);
                    if (isConstructor)
                        name = "<init>";
                    else
                        throw new Error("Invalid sink configuration. Found method element without either a valid name or constructor attribute for " + owner);
                }
                    
                
                ProjectData.addClassName(owner);
                ProjectData.addMethodName(name);
            }
        }
        
        public void endElement(String uri, String localName, String qName) {
            if (qName.equals("method")) {
               
                String desc;
                if (isSourceParser) {
                    desc = AsmUtil.toInternalMethodSignature(returnType, parameterTypeList.toArray(new String[parameterTypeList.size()]));
                    ProjectData.addMethodDesc(desc);
                    Method md = new Method(
                            owner, 
                            name, 
                            desc);
                    NodeManager.addMethod(md); 
                    sourceMethodList.add(md);
                    fireDefinedSourceMethod(md);
                }
                else {
                    desc = AsmUtil.toInternalMethodSignature(returnType, parameterTypeList.toArray(new String[parameterTypeList.size()]));
                    ProjectData.addMethodDesc(desc);

                    Method smd = 
                        new Method(
                                owner, 
                                name, 
                                desc);

                    Iterator<Integer> it = vulnerableParameterIndexList.listIterator();
                    while (it.hasNext())
                        smd.addVulnerableParameterIndex(it.next());
                    sinkMethodList.add(smd);
                    NodeManager.addMethod(smd);
                    fireDefinedSinkMethod(smd);
                }
                
                owner = null;
                name = null;
                parameterTypeList.clear();
                returnType = null;
                vulnerableParameterIndexList.clear();
            }
            else if (qName.equals("parameter-type")) {
                parameterTypeList.add(charBuffer.toString());
            }
            else if (qName.equals("return-type")) {
                returnType = charBuffer.toString();
            }
            else if (!isSourceParser && qName.equals("vulnerable-parameter-index")) {
                vulnerableParameterIndexList.add(Integer.parseInt(charBuffer.toString()));
            }
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

    }
    
    class AnnotationDescriptorParser extends DefaultHandler {
        
        boolean isSource;
        boolean isMethod;
        
        AnnotationDescriptorParser(boolean isSource, boolean isMethod) {
            this.isSource = isSource;
            this.isMethod = isMethod;
        }
        
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            return new InputSource(new ByteArrayInputStream(new byte[0]));
        }
        
        public void characters(char[] ch, int start, int length) {
            charBuffer.append(ch, start, length);
        }
        
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            charBuffer.setLength(0);
        }
        
        public void endElement(String uri, String localName, String qName) {
            if (qName.equals("annotation")) {
                String annotation = charBuffer.toString().trim();
                if (isSource) {
                    sourceMethodAnnotationList.add(annotation);
                    fireDefinedSourceAnnotation(annotation, isMethod);
                }
                else {
                    sinkMethodAnnotationList.add(annotation);
                    fireDefinedSinkAnnotation(annotation, isMethod);
                }
            }
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
    }
    
    
    public boolean isSinkMethod(int classId, int methodId, int descId) {
        Iterator<Method> it = sinkMethodList.iterator();
        Method smd;
        while (it.hasNext()) {
            smd = it.next();
            if (smd.classId == classId && smd.memberId == methodId && smd.descId == descId)
                return true;
        }
        return false;
    }
    
    public boolean isSourceMethod(int classId, int methodId, int descId) {
        Iterator<Method> it = sourceMethodList.iterator();
        Method md;
        while (it.hasNext()) {
            md = it.next();
            if (md.classId == classId && md.memberId == methodId && md.descId == descId)
                return true;
        }
        return false;
    }
}
