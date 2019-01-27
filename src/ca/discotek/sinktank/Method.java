package ca.discotek.sinktank;

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

public class Method extends AbstractMember implements Comparable<Method> {
    
    List<String> sourceExplanationList = new ArrayList<>();
    List<String> sinkExplanationList = new ArrayList<>();
    
    List<Integer> vulnerableParameterIndexList = new ArrayList<Integer>();
    
    public Method(String className, String methodName, String desc) {
        this(ProjectData.addClassName(className), ProjectData.addMethodName(methodName), ProjectData.addMethodDesc(desc));
    }
    
    private Method(int classId, int methodId, int descId) {
        super(classId, methodId, descId);
    }
    
    public void addSourceExplanation(String explanation) {
        sourceExplanationList.add(explanation);
    }
    
    public String[] getSourceExplanations() {
        return sourceExplanationList.toArray(new String[sourceExplanationList.size()]);
    }
    
    public void addSinkExplanation(String explanation) {
        sinkExplanationList.add(explanation);
    }
    
    public String[] getSinkExplanations() {
        return sinkExplanationList.toArray(new String[sourceExplanationList.size()]);
    }
    
    public void clearExplanations() {
        sourceExplanationList.clear();
        sinkExplanationList.clear();
    }
    
    public void addVulnerableParameterIndex(int index) {
        vulnerableParameterIndexList.add(index);
    }
    
    public boolean isParameterIndexVulnerable(int index) {
        return vulnerableParameterIndexList.contains(index);
    }
    
    public Integer[] getVulnerableParameters() {
        return vulnerableParameterIndexList.toArray(new Integer[vulnerableParameterIndexList.size()]);
    }
    
    // equals and hashCode are implmented in superclass!
    
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(ProjectData.getClassName(classId));
        buffer.append('.');
        buffer.append(AsmUtil.toMethodSignature(ProjectData.getMethodName(memberId), ProjectData.getMethodDesc(descId)));
        
        return buffer.toString();
    }

    @Override
    public int compareTo(Method o) {
        return 0;
    }
    
    
}
