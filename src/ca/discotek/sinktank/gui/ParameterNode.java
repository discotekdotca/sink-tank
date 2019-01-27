package ca.discotek.sinktank.gui;

import javax.swing.tree.DefaultMutableTreeNode;

import ca.discotek.rebundled.org.objectweb.asm.Type;
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

public class ParameterNode extends DefaultMutableTreeNode {

    public final Method method;
    public final int parameterIndex;
    
    public ParameterNode(Method method, int parameterIndex) {
        Type methodType = Type.getMethodType(ProjectData.getMethodDesc(method.descId));
        Type argTypes[] = methodType.getArgumentTypes();
        setUserObject(AsmUtil.toTypeString(argTypes[parameterIndex].getDescriptor()));
        
        this.method = method;
        this.parameterIndex = parameterIndex;
    }
}
