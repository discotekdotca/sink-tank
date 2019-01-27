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

public class ParameterNamesMethodNode extends DefaultMutableTreeNode {

    static String[] toParameterNames(Integer ids[]) {
        String names[] = new String[ids.length];
        for (int i=0; i<names.length; i++)
            names[i] = ProjectData.getParameterName(ids[i]);
        return names;
    }
    
    public ParameterNamesMethodNode(Method method, int boldIndex) {
        this(method, ProjectData.getParameterNames(method.classId, method.memberId, method.descId), boldIndex);
    }
    
    public ParameterNamesMethodNode(Method method, Integer parameterNames[], int boldIndex) {
        this(method, toParameterNames(parameterNames), boldIndex);
    }
    
    public ParameterNamesMethodNode(Method method, String parameterNames[], int boldIndex) {
        String className = ProjectData.getClassName(method.classId);
        String methodName = ProjectData.getMethodName(method.memberId);
        String descName = ProjectData.getMethodDesc(method.descId);
        setUserObject("<html>" + AsmUtil.toMethodSignatureWithParameterNames(className, methodName, descName, parameterNames, boldIndex) + "</html>");
    }
}
