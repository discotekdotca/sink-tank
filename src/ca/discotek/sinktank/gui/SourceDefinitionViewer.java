package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import ca.discotek.sinktank.Method;
import ca.discotek.sinktank.TaintListener;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class SourceDefinitionViewer extends JPanel implements TaintListener {

    MethodViewer methodViewer;
    AnnotationViewer annotationViewer;
    
    public SourceDefinitionViewer() {
        buildGui();
    }

    void buildGui() {
        setLayout(new BorderLayout());

        JTabbedPane tabber = new JTabbedPane();
        add(tabber, BorderLayout.CENTER);
        
        methodViewer = new MethodViewer(MethodTreeType.MethodParameters);
        tabber.addTab("Methods", methodViewer);

        annotationViewer = new AnnotationViewer();
        tabber.addTab("Annotations", annotationViewer);
    }
    
    public Method[] getMethods() {
        return methodViewer.getMethods();
    }
    
    public Method[] getSelectedMethods() {
        return methodViewer.getSelectedMethods();
    }
    
    public String[] getMethodAnnotations() {
        return annotationViewer.getMethodAnnotations();
    }
    
    public String[] getSelectedMethodAnnotations() {
        return annotationViewer.getSelectedMethodAnnotations();
    }
    
    public String[] getParameterAnnotations() {
        return annotationViewer.getParameterAnnotations();
    }
    
    public String[] getSelectedParameterAnnotations() {
        return annotationViewer.getSelectedParameterAnnotations();
    }
    
    public void resetConfiguration() {
        methodViewer.resetConfiguration();
        annotationViewer.resetConfiguration();
    }
    
    public void toggleMethodsEnabled(Method methods[]) {
        for (int i=0; i<methods.length; i++)
            methodViewer.toggleMethodEnabled(methods[i]);
    }
    
    public void toggleMethodAnnotationsEnabled(String annotations[]) {
        for (int i=0; i<annotations.length; i++)
            annotationViewer.toggleMethodAnnotationEnabled(annotations[i]);
    }
    
    public void toggleParameterAnnotationsEnabled(String annotations[]) {
        for (int i=0; i<annotations.length; i++)
            annotationViewer.toggleParameterAnnotationEnabled(annotations[i]);
    }
    
    public void addMethodPopupMouseListener(MouseListener l) {
        methodViewer.addPopupMouseListener(l);
    }
    
    public void addMethodAnnotationPopupMouseListener(MouseListener l) {
        annotationViewer.addMethodAnnotationPopupMouseListener(l);
    }
    
    public void addParameterAnnotationPopupMouseListener(MouseListener l) {
        annotationViewer.addParameterAnnotationPopupMouseListener(l);
    }
    
    public void addSourceMethods(Method methods[]) {
        for (int i=0; i<methods.length; i++)
            addSource(methods[i]);
    }
    
    
    public void addSource(Method method) {
        methodViewer.addMethod(method);
    }
    
    public void removeSource(Method method) {
        methodViewer.removeMethod(method);
    }
    
    public void removeSources(Method methods[]) {
        for (int i=0; i<methods.length; i++)
            removeSource(methods[i]);
    }
    
    public void addSourceMethodAnnotations(String annotations[]) {
        for (int i=0; i<annotations.length; i++)
            addSourceMethodAnnotation(annotations[i]);
    }
    
    public void removeSourceMethodAnnotations(String annotations[]) {
        for (int i=0; i<annotations.length; i++)
            removeSourceMethodAnnotation(annotations[i]);
    }
    
    public void addSourceMethodAnnotation(String annotation) {
        annotationViewer.addMethodAnnotation(annotation);
    }
    
    public void removeSourceMethodAnnotation(String annotation) {
        annotationViewer.removeMethodAnnotation(annotation);
    }
    
    public void addSourceParameterAnnotations(String annotations[]) {
        for (int i=0; i<annotations.length; i++)
            addSourceParameterAnnotation(annotations[i]);
    }
    
    public void addSourceParameterAnnotation(String annotation) {
        annotationViewer.addParameterAnnotation(annotation);
    }
    
    public void removeSourceMethodParameterAnnotation(String annotation) {
        annotationViewer.removeMethodParameterAnnotation(annotation);
    }
    
    public void removeSourceMethodParameterAnnotations(String annotations[]) {
        for (int i=0; i<annotations.length; i++)
            removeSourceMethodParameterAnnotation(annotations[i]);
    }
    
    public void expandAll() {
        methodViewer.expandAll();
    }
    
    @Override
    public void definedSourceMethod(Method source) {
        addSource(source);
    }
    
    @Override
    public void definedSourceAnnotation(String source, boolean isMethod) {
        if (isMethod)
            annotationViewer.addMethodAnnotation(source);
        else
            annotationViewer.addParameterAnnotation(source);
    }

    public void definedSinkMethod(Method source) {}
    public void foundSource(List<Method> list) {}
    public void foundSink(List<Method> list) {}
    public void foundSourceSink(List<Method> sourceList, List<Method> sinkList) {}
    public void definedSinkAnnotation(String sink, boolean isMethod) {}
}
