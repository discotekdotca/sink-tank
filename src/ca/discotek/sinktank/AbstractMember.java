package ca.discotek.sinktank;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public abstract class AbstractMember {

    public final int classId;
    public final int memberId;
    public final int descId;
    
    public final int hashCode;
    
    boolean isApp = false;
    
    public AbstractMember(int classId, int memberId, int descId) {
        this.classId = classId;
        this.memberId = memberId;
        this.descId = descId;
        
        this.hashCode = classId + memberId + descId;
    }
    
    public void setApp(boolean isApp) {
        this.isApp = isApp;
    }
    
    public boolean isApp() {
        return isApp;
    }
    
    public int hashCode() {
        return hashCode;
    }
    
    public boolean equals(Object o) {
        if (o instanceof AbstractMember) {
            AbstractMember f = (AbstractMember) o;
            boolean result = hashCode == f.hashCode;

            return result;

        }
        return false;
    }
}
