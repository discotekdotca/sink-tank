package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import ca.discotek.sinktank.AsmUtil;
import ca.discotek.sinktank.Method;
import ca.discotek.sinktank.ProjectData;
import ca.discotek.sinktank.TaintListener;
import ca.discotek.sinktank.swing.ButtonPanel;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class SinkDefinitionViewer extends JPanel implements TaintListener {

    MethodViewer methodViewer;
    AnnotationViewer annotationViewer;
    JFrame parent;
    public SinkDefinitionViewer(JFrame parent) {
        this.parent = parent;
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
        Method[] methods = methodViewer.getMethods();
        Method sinkMethods[] = new Method[methods.length];
        for (int i=0; i<methods.length; i++)
            sinkMethods[i] = (Method) methods[i];
        return sinkMethods;
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
    
    public void expandAll() {
        methodViewer.expandAll();
    }
    
    public void updateUserObject(Method method, MethodNode node) {
        Integer params[] = method.getVulnerableParameters();
        StringBuilder buffer = new StringBuilder();
        for (int i=0; i<params.length; i++) {
            if (i>0)
                buffer.append(", ");
            buffer.append(params[i]);
        }
        
        String text = node.getUserObject().toString() + " [" + buffer.toString() + "]";
        node.setUserObject(text);
    }
    
    public void resetConfiguration() {
        methodViewer.resetConfiguration();
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
    
    public void addSinkMethods(Method methods[]) {
        for (int i=0; i<methods.length; i++) {
            addSinkMethod(methods[i]);
        }
    }
    
    public void addSinkMethod(Method method) {
        MethodNode node = methodViewer.addMethod(method);
        updateUserObject(method, node);
    }
    
    public void removeSinkMethod(Method method) {
        methodViewer.removeMethod(method);
    }
    
    public void removeSinkMethods(Method methods[]) {
        for (int i=0; i<methods.length; i++)
            removeSinkMethod(methods[i]);
    }
    
    public void addSinkMethodAnnotations(String annotations[]) {
        for (int i=0; i<annotations.length; i++)
            addSinkMethodAnnotation(annotations[i]);
    }
    
    public void addSinkMethodAnnotation(String annotation) {
        annotationViewer.addMethodAnnotation(annotation);
    }
    
    public void removeSinkMethodAnnotations(String annotations[]) {
        for (int i=0; i<annotations.length; i++)
            removeSinkMethodAnnotation(annotations[i]);
    }
    
    public void removeSinkMethodAnnotation(String annotation) {
        annotationViewer.removeMethodAnnotation(annotation);
    }
    
    public void addSinkParameterAnnotations(String annotations[]) {
        for (int i=0; i<annotations.length; i++)
            addSinkParameterAnnotation(annotations[i]);
    }
    
    public void addSinkParameterAnnotation(String annotation) {
        annotationViewer.addParameterAnnotation(annotation);
    }
    
    public void removeSinkMethodParameterAnnotations(String annotations[]) {
        for (int i=0; i<annotations.length; i++)
            removeSinkMethodParameterAnnotation(annotations[i]);
    }
    
    public void removeSinkMethodParameterAnnotation(String annotation) {
        annotationViewer.removeMethodParameterAnnotation(annotation);
    }
    
    @Override
    public void definedSinkMethod(Method sink) {
        addSinkMethod(sink);
    }
    

    @Override
    public void definedSinkAnnotation(String sink, boolean isMethod) {
        if (isMethod)
            annotationViewer.addMethodAnnotation(sink);
        else
            annotationViewer.addParameterAnnotation(sink);
    }


    public void definedSourceAnnotation(String source, boolean isMethod) {}
    public void definedSourceMethod(Method source) {}
    public void foundSource(List<Method> list) {}
    public void foundSink(List<Method> list) {}
    public void foundSourceSink(List<Method> sourceList, List<Method> sinkList) {}
    
    class SelectParameterDialog extends JDialog {
        
        int selectedIndexes[] = new int[0];
        ca.discotek.rebundled.org.objectweb.asm.Type argTypes[];
        JCheckBox checkboxes[];
        boolean wasCanceled = true;
        
        public SelectParameterDialog(JFrame parent, Method method) {
            super(parent, "Select Sink Parameter(s)", true);
            String desc = ProjectData.getMethodDesc(method.descId);
            ca.discotek.rebundled.org.objectweb.asm.Type methodType = 
                ca.discotek.rebundled.org.objectweb.asm.Type.getMethodType(desc);
            argTypes = methodType.getArgumentTypes();
            buildGui(method);
        }
        
        void buildGui(Method method) {
            setLayout(new BorderLayout());
            JLabel label = new JLabel("<html>Select sink parameter(s) for <b>" + AsmUtil.toMethodSignatureWithParameterNames(method, -1) + "</b></html>");
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            add(label, BorderLayout.NORTH);
            
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2,2,2,2);
            gbc.gridx = gbc.gridy = 0;
            gbc.weightx = gbc.weighty = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            checkboxes = new JCheckBox[argTypes.length];
            String parameterNames[] = ProjectData.getParameterNames(method);
            gbc.weightx = gbc.weighty = 0;
            gbc.anchor = GridBagConstraints.CENTER;
            for (int i=0; i<argTypes.length; i++) {
                gbc.gridy++;
                checkboxes[i] = new JCheckBox(AsmUtil.toTypeString(argTypes[i].getDescriptor()) + (parameterNames == null ? "" : " " + parameterNames[i]));
                panel.add(checkboxes[i], gbc);
            }
            
            add(panel, BorderLayout.CENTER);
            
            ButtonPanel buttonPanel = new ButtonPanel();
            JButton button = new JButton("Okay");
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    wasCanceled = false;
                    setVisible(false);
                    dispose();
                }
            });
            buttonPanel.addButton(button);
            
            button = new JButton("Cancel");
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    wasCanceled = true;
                    setVisible(false);
                    dispose();
                }
            });
            buttonPanel.addButton(button);
            
            add(buttonPanel, BorderLayout.SOUTH);
        }
        
        public boolean wasCanceled() {
            return wasCanceled;
        }
        
        public Integer[] getParameters() {
            List<Integer> list = new ArrayList<Integer>();
            
            for (int i=0; i<checkboxes.length; i++) {
                if (checkboxes[i].isSelected())
                    list.add(i);
            }
            
            return list.toArray(new Integer[list.size()]);
        }
    }
}
