package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import ca.discotek.sinktank.Method;
import ca.discotek.sinktank.MethodNodeSelectionListener;
import ca.discotek.sinktank.Path;
import ca.discotek.sinktank.ProjectData;
import ca.discotek.sinktank.ResourceHelper;
import ca.discotek.sinktank.ResultsConsolidator;
import ca.discotek.sinktank.TaintListener;
import ca.discotek.sinktank.TempFileUtil;
import ca.discotek.sinktank.io.PersistUtil;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class ResultsViewer extends JPanel implements TaintListener, MethodNodeSelectionListener {

    JTabbedPane tabber;
    
    SourceSinkView sourceSinkViewer;
    PathView sourcePathViewer;
    PathView sinkPathViewer;

    SourceViewer sourceViewer;

    ResourceHelper helper;
    
    final Map<Method, List<Method>> sourceOriginMap;
    final Map<Method, List<Method>> sinkOriginMap;
    
    ResultsConsolidator consolidator = null;
    
    final Map<Method, List<String>> sourceMethodAnnotationMap;
    final Map<Method, List<String>> sinkMethodAnnotationMap;
    final Map<Method, List<String>> sourceMethodParameterAnnotationMap;
    final Map<Method, List<String>> sinkMethodParameterAnnotationMap;
    
    public ResultsViewer
        (String urls[], Map<Method, List<Method>> sourceOriginMap, Map<Method, List<Method>> sinkOriginMap, 
         Map<Method, List<String>> sourceMethodAnnotationMap, Map<Method, List<String>> sinkMethodAnnotationMap,
         Map<Method, List<String>> sourceMethodParameterAnnotationMap, Map<Method, List<String>> sinkMethodParameterAnnotationMap) throws MalformedURLException {
        
        this.sourceOriginMap = sourceOriginMap;
        this.sinkOriginMap = sinkOriginMap;
        
        this.sourceMethodAnnotationMap = sourceMethodAnnotationMap;
        this.sinkMethodAnnotationMap = sinkMethodAnnotationMap;
        this.sourceMethodParameterAnnotationMap = sourceMethodParameterAnnotationMap;
        this.sinkMethodParameterAnnotationMap = sinkMethodParameterAnnotationMap;
        
        buildGui();
        
        helper = new ResourceHelper();
        for (int i=0; i<urls.length; i++)
            helper.addUrl(urls[i]);
    }
    
    void buildGui() {
        setLayout(new BorderLayout());
        tabber = new JTabbedPane();
        
        sourceSinkViewer = new SourceSinkView(sourceOriginMap, sinkOriginMap, sourceMethodAnnotationMap, sinkMethodAnnotationMap, sourceMethodParameterAnnotationMap, sinkMethodParameterAnnotationMap);
        tabber.addTab("Source/Sinks", sourceSinkViewer);
        
        sourcePathViewer = new PathView(sourceOriginMap, sourceMethodAnnotationMap, sourceMethodParameterAnnotationMap);
        tabber.addTab("All Source Paths", sourcePathViewer);
        
        sinkPathViewer = new PathView(sinkOriginMap, sinkMethodAnnotationMap, sinkMethodParameterAnnotationMap);
        tabber.addTab("All Sink Paths", sinkPathViewer);

        sourceViewer = new SourceViewer();
        JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabber, sourceViewer);
        splitter.setResizeWeight(0.6);
        
        add(splitter, BorderLayout.CENTER);

        sourceSinkViewer.setMethodNodeSelectionListener(this);
        sourcePathViewer.setClassNodeSelectionListener(this);
        sinkPathViewer.setClassNodeSelectionListener(this);
    }
    
    public void setResults(ResultsConsolidator consolidator) {
        this.consolidator = consolidator;
        setSourcePaths(consolidator.getSourcePaths(true));
        setSinkPaths(consolidator.getSinkPaths(true));
        setSourceSinkPaths(consolidator.getSourceSinkPaths(true, true));
    }
    
    public void setSourcePaths(Path paths[]) {
        for (Path path : paths)
            sourcePathViewer.addPath(path);
    }
    
    public void setSinkPaths(Path paths[]) {
        for (Path path : paths)
            sinkPathViewer.addPath(path);
    }
    
    public void setSourceSinkPaths(Path paths[][]) {
        for (int i=0; i<paths.length; i++)
            sourceSinkViewer.addSourceSink(paths[i][0], paths[i][1]);
    }

    public void definedSourceMethod(Method source) {}
    public void definedSinkMethod(Method source) {}
    public void definedSourceAnnotation(String source, boolean isMethod) {}
    public void definedSinkAnnotation(String sink, boolean isMethod) {}

    public void foundSource(List<Method> list) {
        sourcePathViewer.addPath(new Path(list.toArray(new Method[list.size()])));
    }

    public void foundSink(List<Method> list) {
        sinkPathViewer.addPath(new Path(list.toArray(new Method[list.size()])));
    }

    public void foundSourceSink(List<Method> sourceList, List<Method> sinkList) {
        sourceSinkViewer.addSourceSink(new Path(sourceList.toArray(new Method[sourceList.size()])), new Path(sinkList.toArray(new Method[sinkList.size()])));
    }

    public void methodSelected(Method method) {
        String className = ProjectData.getClassName(method.classId);
        
        String classloaderName = className.replace('.', '/') + ".class";
        InputStream is = helper.getResourceAsStream(classloaderName);
        
        if (is == null) {
            JOptionPane.showMessageDialog(this, "<html>Can't find <i>" + className + "</i> on classpath.</html>");
            return;
        }
        
        int index = className.lastIndexOf('.');
        if (index < 0)
            index = 0;
        else
            index++;
        
        String s = className.substring(index, className.length());
        
        try {
            File tmpFile = TempFileUtil.createTemporaryFile(is, s);
            sourceViewer.addMethod(tmpFile, method);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static void show(final ResultsViewer resultsViewer, JFrame parent) {
        JFrame f = new JFrame("Sink Tank Report");
        f.setIconImages(SinkTankIconUtil.DEEPDIVE_ICON_LIST);

        JMenuBar bar = new JMenuBar();
        f.setJMenuBar(bar);
        JMenu menu = new JMenu("File");
        bar.add(menu);
        JMenuItem item = new JMenuItem("Save As...");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = chooser.showSaveDialog(resultsViewer);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File directory = chooser.getSelectedFile();
                    PersistUtil util = new PersistUtil();
                    try {
                        util.saveToHtml(directory, resultsViewer.consolidator, "Source-&gt;Sink Analysis Results", resultsViewer.sourceOriginMap, resultsViewer.sinkOriginMap, resultsViewer.sourceMethodAnnotationMap, resultsViewer.sinkMethodAnnotationMap, resultsViewer.sourceMethodParameterAnnotationMap, resultsViewer.sinkMethodParameterAnnotationMap);
                    } 
                    catch (IOException e1) {
                        e1.printStackTrace();
                        JOptionPane.showMessageDialog(resultsViewer, "Could not save results. See stderr for stack trace.", "Save Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        f.add(resultsViewer);
        f.setSize(800, 600);
        f.setLocationRelativeTo(parent);
        f.setVisible(true);
        
    }
}
