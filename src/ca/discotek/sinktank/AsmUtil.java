package ca.discotek.sinktank;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.discotek.rebundled.org.objectweb.asm.ClassReader;
import ca.discotek.rebundled.org.objectweb.asm.Opcodes;
import ca.discotek.rebundled.org.objectweb.asm.Type;
import ca.discotek.rebundled.org.objectweb.asm.tree.AnnotationNode;
import ca.discotek.rebundled.org.objectweb.asm.tree.ClassNode;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class AsmUtil {
    
    public static String getClassName(InputStream is) throws IOException {
        ClassReader cr = new ClassReader(is);
        ClassNode node = new ClassNode();
        cr.accept(node, ClassReader.SKIP_CODE);
        return node.name;
    }
    
    public static String toInternalMethodSignature(String returnType, String parameterTypes[]) {
        StringBuilder buffer = new StringBuilder();
        
        buffer.append('(');
        for (int i=0; i<parameterTypes.length; i++)
            buffer.append(toInternalType(parameterTypes[i]));
        buffer.append(')');
        buffer.append(toInternalType(returnType));
        
        return buffer.toString();
    }

    static final Pattern ARRAY_PATTERN = Pattern.compile("\\[\\]");
    
    public static String toInternalType(String type) {
        StringBuilder buffer = new StringBuilder();
        
        Matcher matcher = ARRAY_PATTERN.matcher(type);
        int firstArray = -1;
        int start = 0;
        int length = type.length();
        while (matcher.find(start)) {
            start = matcher.start();
            if (firstArray < 0)
                firstArray = start;
            
            buffer.append('[');
            start += 2;
            if (start >= length)
                break;
        }

        String componentType = firstArray > 0 ? type.substring(0, firstArray) : type;
        
        String primitive = null;
        if (componentType.equals("boolean"))
            primitive = "Z";
        else if (componentType.equals("byte"))
            primitive = "B";
        else if (componentType.equals("char"))
            primitive = "C";
        else if (componentType.equals("double"))
            primitive = "D";
        else if (componentType.equals("float"))
            primitive = "F";
        else if (componentType.equals("int"))
            primitive = "I";
        else if (componentType.equals("long"))
            primitive = "J";
        else if (componentType.equals("short"))
            primitive = "S";
        else if (componentType.equals("void"))
            primitive = "V";
        
        
        if (primitive == null) {
            buffer.append('L');
            
            buffer.append(componentType.replace('.', '/'));
            
            buffer.append(';');
        }
        else
            buffer.append(primitive);
        
        return buffer.toString();
    }
    
    public static String toMethodSignature(Method method) {
        return toMethodSignature(method, false);
    }
    
    public static String toMethodSignature(Method method, boolean withClass) {
        return withClass ?
            ProjectData.getClassName(method.classId) + "." +
                toMethodSignature(ProjectData.getMethodName(method.memberId), ProjectData.getMethodDesc(method.descId)) :
            toMethodSignature(ProjectData.getMethodName(method.memberId), ProjectData.getMethodDesc(method.descId));
    }
    
    public static String toMethodSignature(String methodName, String desc) {
        return toMethodSignature(methodName, desc, false);
    }
    
    public static String toMethodSignature(String className, String methodName, String desc, boolean includeReturnType) {
        return toMethodSignature(className, methodName, desc, includeReturnType, false, false);
    }
    
    public static String toMethodSignature(String className, String methodName, String desc, boolean includeReturnType, boolean returnTypeAtEnd, boolean escapeHtml) {
        StringBuilder b = new StringBuilder();
        
        Type methodType = Type.getMethodType(desc);
        Type returnType = methodType.getReturnType();
        
        if (includeReturnType && !returnTypeAtEnd) {
            b.append(toString(returnType));
            b.append(" ");
        }
        
        if (className != null) {
            b.append(' ');
            b.append(className);
            b.append('.');
        }
        
        b.append(escapeHtml ? Util.escapeHtml(methodName) : methodName);
        b.append('(');
        b.append(toTypeString(methodType.getArgumentTypes()));
        b.append(')');
        
        if (includeReturnType && returnTypeAtEnd) {
            b.append(" : ");
            b.append(toString(returnType));
        }
        
        return b.toString();
        
    }
    
	public static String toMethodSignature(String methodName, String desc, boolean includeReturnType) {
	    return toMethodSignature(null, methodName, desc, includeReturnType);
	}
	
    public static String toMethodSignatureWithParameterNames(Method method, int boldIndex) {
        return toMethodSignatureWithParameterNames
            (ProjectData.getClassName(method.classId), 
             ProjectData.getMethodName(method.memberId), 
             ProjectData.getMethodDesc(method.descId), 
             ProjectData.getParameterNames(method), boldIndex);
    }
	
	public static String toMethodSignatureWithParameterNames(String className, String methodName, String desc, String parameterNames[], int boldIndex) {
        StringBuilder b = new StringBuilder();
        
        Type methodType = Type.getMethodType(desc);
        
        b.append(className);
        b.append('.');
        b.append(methodName);
        b.append('(');
        b.append(toTypeString(methodType.getArgumentTypes(), parameterNames, boldIndex));
        b.append(')');
        
        return b.toString();
	}
	
   public static String toParameterTypesString(String desc) {
       Type type = Type.getType(desc);
       return toTypeString(type.getArgumentTypes());
   }
   
   public static String toReturnTypeString(String desc) {
       Type type = Type.getType(desc);
       return toString(type.getReturnType());
   }
	
	public static String toTypeString(Type argTypes[]) {
	    StringBuilder buffer = new StringBuilder();
	    
	    for (int i=0; i<argTypes.length; i++) {
	        buffer.append(toString(argTypes[i]));
	        if (i<argTypes.length-1)
	            buffer.append(',');
	    }
	    
	    return buffer.toString();
	}
	
    public static String toTypeString(Type argTypes[], String parameterNames[], int boldIndex) {
        StringBuilder buffer = new StringBuilder();
        
        int offset = parameterNames == null ? 0 : argTypes.length - parameterNames.length;
        
        for (int i=0 + offset; i<argTypes.length-offset; i++) {
            if (boldIndex == i)
                buffer.append("<b>");
            try {
                buffer.append(toString(argTypes[i]));
                buffer.append(" ");

                buffer.append(parameterNames == null || parameterNames[i-offset] == null ? "???" : parameterNames[i-offset]);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            if (boldIndex == i-offset)
                buffer.append("</b>");
            if (i<argTypes.length-1)
                buffer.append(", ");
        }
        
        return buffer.toString();
    }
	
	public static String toTypeString(String desc) {
	    Type type = Type.getType(desc);
	    return toString(type);
	}
	
	public static String toString(Type type) {
	    StringBuilder buffer = new StringBuilder();
	    if (type.getSort() == Type.ARRAY) {
	        buffer.append(type.getElementType().getClassName());
	        int size = type.getDimensions();
	        for (int i=0; i<size; i++) {
	            buffer.append("[]");
	        }
	    }
	    else
	        buffer.append(type.getClassName());
	    return buffer.toString();
	}
	
	public static String toInternalParams(String types[]) {
	    StringBuilder buffer = new StringBuilder();
	    
	    for (int i=0; i<types.length; i++)
	        buffer.append(toInternalType(types[i]));
	    
	    return buffer.toString();
	}
	
	
    public static String toAnnotationString(AnnotationNode node) {
        AsmUtil.AnnotationOutputVisitor mav = new AsmUtil.AnnotationOutputVisitor(node.desc);
        node.accept(mav);
        mav.setAddHtmlBreakingSpaces(false);
        mav.setBoldAnnotations(false);
        return mav.toString();
    }
    
    public static class AnnotationOutputVisitor extends AnnotationNode {

        boolean boldAnnotations = false;
        boolean addHtmlBreakingSpaces = false;
        
        public AnnotationOutputVisitor(String desc) {
            super(Opcodes.ASM7, desc);
        }
        
        public void setBoldAnnotations(boolean boldAnnotations) {
            this.boldAnnotations = boldAnnotations;
        }
        
        public void setAddHtmlBreakingSpaces(boolean addHtmlBreakingSpaces) {
            this.addHtmlBreakingSpaces = addHtmlBreakingSpaces;
        }
        
        public String toString() {
            StringBuilder buffer = new StringBuilder();
            
            buffer.append('@');
            if (boldAnnotations)
                buffer.append("<b>");
            buffer.append(AsmUtil.toTypeString(desc));
            if (boldAnnotations)
                buffer.append("</b>");
            
            StringBuilder listBuffer = new StringBuilder();
            printAnnotationList(listBuffer, values);

            if (listBuffer.length() > 0) {
                buffer.append("(");
                buffer.append(listBuffer);
                buffer.append(")");
            }
            
            return buffer.toString();
        }
        
        void printAnnotationList(StringBuilder buffer, List<Object> list) {
            if (list != null) {

                Iterator<Object> it = list.listIterator();
                Object name;
                int size = list.size();
                while (it.hasNext()) {
                    name = it.next();

                    if (size > 2) {
                        buffer.append(name);
                        if (addHtmlBreakingSpaces)
                            buffer.append(" = ");
                        else
                            buffer.append('=');
                    }
                    
                    printValue(buffer, it.next());
                    
                    if (it.hasNext())
                        buffer.append(", ");
                }
            }
        }
        
        void printValueList(StringBuilder buffer, List<Object> list) {
            Iterator<Object> it = list.listIterator();
            Object value;
            while (it.hasNext()) {
                value = it.next();
                printValue(buffer, value);
                if (it.hasNext())
                    buffer.append(", ");
            }
        }
        
        void printValue(StringBuilder buffer, Object value) {
            if (value instanceof String)
                buffer.append("\"" + value + "\"");
            else if (value instanceof Type)
                buffer.append(AsmUtil.toString( (Type) value));
            else if (value instanceof AnnotationNode) {
                AnnotationNode node = (AnnotationNode) value;
                AnnotationOutputVisitor mav = new AnnotationOutputVisitor(node.desc);
                node.accept(mav);
                buffer.append(mav);
            }
            else if (value instanceof String[]) {
                String array[] = (String[]) value;
                buffer.append(AsmUtil.toTypeString( array[0]));
                buffer.append('.');
                buffer.append(array[1]);
            }
            else if (value instanceof List) {
                buffer.append('{');
                printValueList(buffer, (List<Object>) value);
                buffer.append('}');
            }
            else 
                buffer.append(value);
        }
    }
    
    public static AnnotationNode[] getInnerNodes(AnnotationNode node) {
        List<AnnotationNode> list = new ArrayList<>();
        getInnerNodes(node, list);
        return list.toArray(new AnnotationNode[list.size()]);
    }
    
    private static void getInnerNodes(AnnotationNode node, List<AnnotationNode> list) {
        if (node.values != null) {
            Iterator it = node.values.listIterator();
            Object o;
            AnnotationNode innerNode;
            while (it.hasNext()) {
                o = it.next();
                if (o instanceof AnnotationNode) {
                    innerNode = (AnnotationNode) o;
                    list.add(innerNode);
                    getInnerNodes( innerNode, list );
                }
            }
        }
    }
}
