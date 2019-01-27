package ca.discotek.sinktank.gui;

import javax.swing.tree.DefaultMutableTreeNode;

import ca.discotek.sinktank.AsmUtil;
import ca.discotek.sinktank.Method;
import ca.discotek.sinktank.ProjectData;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class MethodNode extends DefaultMutableTreeNode {

    public final Method method;
    
    MethodTreeType type;
    
    public MethodNode(Method method) {
        this(method, MethodTreeType.MethodParameters);
    }
    
    public MethodNode(Method method, MethodTreeType type) {
        if (type == MethodTreeType.Fqn) {
            String className = ProjectData.getClassName(method.classId);
            setUserObject(className + '.' + AsmUtil.toMethodSignature(method, false));
        }
        else if (type == MethodTreeType.ClassMethodParameters) {
            String className = ProjectData.getClassName(method.classId);
            int index = className.lastIndexOf('.');
            className = index < 0 ? className : className.substring(index+1, className.length());
            setUserObject(className + '.' + AsmUtil.toMethodSignature(method, false));
        }
        else if (type == MethodTreeType.MethodParameters)
            setUserObject(AsmUtil.toMethodSignature(method, false));
        else if (type == MethodTreeType.Method)
            setUserObject(ProjectData.getMethodName(method.memberId));
        else 
            throw new RuntimeException("Unsupported tree type: " + type);
        
        this.method = method;
    }
}
