package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import ca.discotek.sinktank.Configuration;
import ca.discotek.sinktank.DescriptorParser;
import ca.discotek.sinktank.FileSelectionListener;
import ca.discotek.sinktank.Method;
import ca.discotek.sinktank.MethodAnnotationDescriptor;
import ca.discotek.sinktank.NodeManager;
import ca.discotek.sinktank.ParameterAnnotationDescriptor;
import ca.discotek.sinktank.PathAnalyzer2;
import ca.discotek.sinktank.PathUtil;
import ca.discotek.sinktank.ResultsConsolidator;
import ca.discotek.sinktank.UserConfiguration;
import ca.discotek.sinktank.Util;
import ca.discotek.sinktank.dijkstra.Edge;
import ca.discotek.sinktank.jsp.JspJarBuilder;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class MainView extends JPanel implements DeploymentListener, FileSelectionListener {
    
    File configFile = null;

    JFrame parent;
    
    DeploymentPathViewer pathViewer;
    MethodViewer methodViewer;
    EdgeViewer outgoingEdgeViewer;
    EdgeViewer incomingEdgeViewer;
    AnnotationViewer annotationViewer;
    SourceDefinitionViewer sourceDefinitionViewer;
    SinkDefinitionViewer sinkDefinitionViewer;
    
    Map<File, Edge[]> deploymentEdgeMap = new HashMap<File, Edge[]>();
    Set<Edge> allEdgeSet = new HashSet<Edge>();
    
    Map<File, MethodAnnotationDescriptor[]> methodAnnotationDescriptorMap = new HashMap<File, MethodAnnotationDescriptor[]>();
    Map<File, ParameterAnnotationDescriptor[]> parameterAnnotationDescriptorMap = new HashMap<File, ParameterAnnotationDescriptor[]>();
    
    Map<File, Set<Method>> definedMethodMap = new HashMap<File, Set<Method>>();
    Map<File, Set<Method>> allMethodMap = new HashMap<File, Set<Method>>();
    
    JTabbedPane tabber;
    
    JSplitPane splitter;
    JSplitPane sourceSinkSplitter;
    JSplitPane definitionSplitter;
    
    final Configuration builtInConfiguration = new Configuration();

    UserConfiguration userConfiguration = new UserConfiguration();
    
    final Set<Callable<Object>> callableSet = new HashSet<>();
    
    public MainView(JFrame f) {
        this.parent = f;
        
        buildGui();
        

        DescriptorParser parser = new DescriptorParser();
        parser.addListener(sourceDefinitionViewer);
        parser.addListener(sinkDefinitionViewer);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            parser.parseMethodSources(cl.getResourceAsStream("method-sources.xml"));
            parser.parseAnnotations(cl.getResourceAsStream("method-annotation-sources.xml"), true, true);
            parser.parseAnnotations(cl.getResourceAsStream("parameter-annotation-sources.xml"), true, false);
            sourceDefinitionViewer.expandAll();
            
            parser.parseMethodSinks(cl.getResourceAsStream("method-sinks.xml"));
            parser.parseAnnotations(cl.getResourceAsStream("method-annotation-sinks.xml"), false, true);
            parser.parseAnnotations(cl.getResourceAsStream("parameter-annotation-sinks.xml"), false, false);
            sinkDefinitionViewer.expandAll();
            
            builtInConfiguration.setSourceMethods(sourceDefinitionViewer.getMethods());
            builtInConfiguration.setSourceMethodAnnotations(sourceDefinitionViewer.getMethodAnnotations());
            builtInConfiguration.setSourceParameterAnnotations(sourceDefinitionViewer.getParameterAnnotations());
            
            builtInConfiguration.setSinkMethods(sinkDefinitionViewer.getMethods());
            builtInConfiguration.setSinkMethodAnnotations(sinkDefinitionViewer.getMethodAnnotations());
            builtInConfiguration.setSinkParameterAnnotations(sinkDefinitionViewer.getParameterAnnotations());
        } 
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Couldn't parse predefined sinks and/or sources. See stderr for stack trace.", "Source/Sink Parsing Error", JOptionPane.ERROR_MESSAGE);
        }
        
        resetDividerLocations();
    }
    
    void buildGui() {
        setLayout(new BorderLayout());
   
        pathViewer = new DeploymentPathViewer(this);
        pathViewer.addDeployentListener(this);
        pathViewer.addFileSelectionListener(this);
        pathViewer.addPopupMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showDeploymentViewerPopupMenu(e);
                }
            }
        });
        
        tabber = new JTabbedPane();
        methodViewer = new MethodViewer(MethodTreeType.MethodParameters);
        methodViewer.addPopupMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMethodViewerPopupMenu(e);
                }
            }
        });
        tabber.addTab("All Methods", methodViewer);
        
        outgoingEdgeViewer = new EdgeViewer(MethodTreeType.MethodParameters, true);
        tabber.addTab("Outgoing Methods", outgoingEdgeViewer);
        outgoingEdgeViewer.addPopupMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showOutgoingEdgeViewerPopupMenu(e);
                }
            }
        });
        
        incomingEdgeViewer = new EdgeViewer(MethodTreeType.MethodParameters, false);
        tabber.addTab("Incoming Methods", incomingEdgeViewer);
        incomingEdgeViewer.addPopupMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showIncomingEdgeViewerPopupMenu(e);
                }
            }
        });
        
        annotationViewer = new AnnotationViewer();
        tabber.addTab("Annotations", annotationViewer);
        annotationViewer.addMethodAnnotationPopupMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMethodAnnotationViewerPopupMenu(e);
                }
            }
        });
        
        annotationViewer.addParameterAnnotationPopupMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showParameterAnnotationViewerPopupMenu(e);
                }
            }
        });
        
        splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JPanel(), new JPanel());
        splitter.setResizeWeight(.2);
        
        sourceDefinitionViewer = new SourceDefinitionViewer();
        sourceDefinitionViewer.setBorder(new TitledBorder("Source Defintions"));
        sourceDefinitionViewer.addMethodPopupMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showSourceDefinitionMethodPopupMenu(e);
                }
            }            
        });
        
        sourceDefinitionViewer.addMethodAnnotationPopupMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showSourceDefinitionMethodAnnotationPopupMenu(e);
                }
            }            
        });
        
        sourceDefinitionViewer.addParameterAnnotationPopupMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showSourceDefinitionParameterAnnotationPopupMenu(e);
                }
            }            
        });
        
        sinkDefinitionViewer = new SinkDefinitionViewer(parent);
        sinkDefinitionViewer.setBorder(new TitledBorder("Sink Defintions"));
        sinkDefinitionViewer.addMethodPopupMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showSinkDefinitionMethodPopupMenu(e);
                }
            }
        });
        
        sinkDefinitionViewer.addMethodAnnotationPopupMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showSinkDefinitionMethodAnnotationPopupMenu(e);
                }
            }
        });
        
        sinkDefinitionViewer.addParameterAnnotationPopupMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showSinkDefinitionParameterAnnotationPopupMenu(e);
                }
            }
        });
        
        sourceSinkSplitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JPanel(), new JPanel());
        sourceSinkSplitter.setResizeWeight(.5);
        
        definitionSplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitter, new JPanel());
        definitionSplitter.setResizeWeight(.80);
        add(definitionSplitter, BorderLayout.CENTER);
        
        
        /*
         * The JSplitPanes respect the preferred sizes of their components over the resize weight property.
         * This causes the source/sink viewers to be wider than I want them to be. To get around this,
         * lay everything out using plain JPanels. Once it is laid out, get the sizes and then set the scrollpane
         * preferred sizes, and set the real components into the the split panes.
         */
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                splitter.setLeftComponent(pathViewer);
                splitter.setRightComponent(tabber);
                
                sourceDefinitionViewer.setPreferredSize(sourceDefinitionViewer.getSize());
                sinkDefinitionViewer.setPreferredSize(sinkDefinitionViewer.getSize());

                sourceSinkSplitter.setTopComponent(sourceDefinitionViewer);
                sourceSinkSplitter.setBottomComponent(sinkDefinitionViewer);
                
                
                definitionSplitter.setLeftComponent(splitter);
                definitionSplitter.setRightComponent(sourceSinkSplitter);
                
             
                resetDividerLocations();
            }
        });
        
        buildMenuBar();
        
        ProgressDialog dialog = new ProgressDialog(parent);
        dialog.setListeningEnabled(true);
    }
    
    void buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        parent.setJMenuBar(bar);
        
        JMenu menu = new JMenu("File");
        bar.add(menu);
        
        JMenuItem item = new JMenuItem("Load Configuration...");
        menu.add(item);
        
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try { 
                    load();
                } 
                catch (IOException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(MainView.this, "An error occured loading configuration. See stderr for details.", "Load Configuration Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        item = new JMenuItem("Save Configuration");
        menu.add(item);
        
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try { save(); } 
                catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(MainView.this, "An error occured saving configuration. See stderr for details.", "Save Configuration Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        item = new JMenuItem("Save Configuration As...");
        menu.add(item);
        
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try { saveAs(); } 
                catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(MainView.this, "An error occured saving configuration. See stderr for details.", "Save Configuration Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        menu.add(new JSeparator());
        
        item = new JMenuItem("Analyze");
        menu.add(item);
        
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                
                boolean canceled = maybeShowMethodWarnings();
                if (canceled)
                    return;
                
                Thread thread = new Thread() {
                    public void run() {
                        showProgressDialog();
                        
                        compileJsps();
                        
                        Configuration configuration = new Configuration();
                        configuration.setSourceMethods(sourceDefinitionViewer.getMethods());
                        configuration.setSourceMethodAnnotations(sourceDefinitionViewer.getMethodAnnotations());
                        configuration.setSourceParameterAnnotations(sourceDefinitionViewer.getParameterAnnotations());
                        
                        configuration.setSinkMethods(sinkDefinitionViewer.getMethods());
                        configuration.setSinkMethodAnnotations(sinkDefinitionViewer.getMethodAnnotations());
                        configuration.setSinkParameterAnnotations(sinkDefinitionViewer.getParameterAnnotations());
                        
                        final PathAnalyzer2 pa = new PathAnalyzer2(configuration);
                        
                        File files[] = pathViewer.getDeployments();
                        for (int i=0; i<files.length; i++) {
                            try { pa.addApplication(files[i]); } 
                            catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                        
                        try {
                            pa.processClasspath(true);


                            NameSpaceSelectorDialog d = new NameSpaceSelectorDialog(parent, NodeManager.getNodes());
                            d.setSize(500, 500);
                            d.setLocationRelativeTo(parent);
                            d.setVisible(true);
                            
                            boolean canceled = d.getCanceled();
                            if (canceled)
                                return;

                            Map<Character, Set<String>> namespaceMap = d.getNamespaces();
                            
                            ResultsConsolidator consolidator = new ResultsConsolidator();
                            pa.addListener(consolidator);
                            
                            hideProgressDialog();
                            
                            pa.findPaths(namespaceMap);
                            
                            ResultsViewer resultsViewer = 
                                new ResultsViewer(pa.getProcessedUrls(), pa.getSourceOriginMap(), pa.getSinkOriginMap(), 
                                        pa.getSourceMethodAnnotationMap(), pa.getSinkMethodAnnotationMap(), pa.getSourceMethodParameterAnnotationMap(), pa.getSinkMethodParameterAnnotationMap());

                            resultsViewer.setResults(consolidator);
                            ResultsViewer.show(resultsViewer, parent);
                            
                        } 
                        catch (Exception e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                        finally {
                            hideProgressDialog();
                        }

                    }

                };
                thread.start();
            }
        });

        menu.add(new JSeparator());
        
        item = new JMenuItem("Exit");
        menu.add(item);
        
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }

    boolean maybeShowMethodWarnings() {
        
        
        Set<Method> definedMethodSet = new HashSet<Method>();
        Iterator<Set<Method>> it = definedMethodMap.values().iterator();
        while (it.hasNext()) {
            definedMethodSet.addAll(it.next());
        }
        
        
        Method sourceMethods[] = sourceDefinitionViewer.getMethods();
        Set<Method> sourceMethodSet = new HashSet<Method>(Arrays.asList(sourceMethods));
        for (int i=0; i<sourceMethods.length; i++) {
            if (definedMethodSet.contains(sourceMethods[i]))
                sourceMethodSet.remove(sourceMethods[i]);
        }
            
        Method sinkMethods[] = sinkDefinitionViewer.getMethods();
        Set<Method> sinkMethodSet = new HashSet<Method>(Arrays.asList(sinkMethods));
        for (int i=0; i<sinkMethods.length; i++) {
            if (definedMethodSet.contains(sinkMethods[i]))
                sinkMethodSet.remove(sinkMethods[i]);
        }
        
        if (sourceMethodSet.size() > 0 || sinkMethodSet.size() > 0) {
            UnvalidatedSourceSinksDialog dialog = new UnvalidatedSourceSinksDialog(parent, sourceMethodSet, sinkMethodSet);
            dialog.setSize(800, 600);
            dialog.setLocationRelativeTo(MainView.this);
            dialog.setVisible(true);
            
            if (dialog.getCanceled())
                return true;
        }

        
        Set<Method> allMethodSet = new HashSet<Method>();
        it = allMethodMap.values().iterator();
        while (it.hasNext()) {
            allMethodSet.addAll(it.next());
        }
        
        allMethodSet.removeAll(definedMethodSet);
        if (allMethodSet.size() > 0) {
            ReferencedMethodWarningDialog dialog = new ReferencedMethodWarningDialog(parent, allMethodSet);
            dialog.setSize(800, 600);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            boolean canceled = dialog.getCanceled();
            return canceled;
        }
        else
            return false;
    }
    
    IndeterminateProgressDialog progressDialog = null;
    
    void showProgressDialog() {
        JFrame f = (JFrame) SwingUtilities.windowForComponent(this);

        if (progressDialog == null)
            progressDialog = new IndeterminateProgressDialog(f);
        
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(f);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressDialog.setVisible(true);
                progressDialog.start();
            }
        });

    }
    
    void hideProgressDialog() {
        progressDialog.setVisible(false);
    }
    
    
    Set<File> jspFileSet = new HashSet<File>();
    
    void compileJsps() {
        // 1. Go through each archive, including jars. For any archive including jsps, generate a classpath
        // 2. classpath consists of the jar including the jsps, the jars and classes within the enclosing archives and all the top level jars
        // 3. generate jsp classes
        // 4. jar jsps
        // 5. add to deployment path viewer

        
        JspJarBuilder builder = new JspJarBuilder();
        
        try {
            Iterator<File> it = jspFileSet.iterator();
            while (it.hasNext())
                pathViewer.removeDeployment(it.next(), false);
            
            jspFileSet.clear();

            File files[] = pathViewer.getDeployments();
            builder.buildJspJars(files);
            File jspJars[] = builder.getJspJars();
            for (int i=0; i<jspJars.length; i++) {
                pathViewer.addDeployment(jspJars[i]);
                jspFileSet.add(jspJars[i]);
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    void load() throws IOException {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (configFile != null) {
            chooser.setCurrentDirectory(configFile.getParentFile());
            chooser.setSelectedFile(configFile);
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            userConfiguration.reset();
            userConfiguration.parse(file);

            sourceDefinitionViewer.addSourceMethods(userConfiguration.getAddedSourceMethods());
            sinkDefinitionViewer.addSinkMethods(userConfiguration.getAddedSinkMethods());

            sourceDefinitionViewer.addSourceMethodAnnotations(userConfiguration.getAddedSourceMethodAnnotations());
            sourceDefinitionViewer.addSourceParameterAnnotations(userConfiguration.getAddedSourceMethodParameterAnnotations());
            sinkDefinitionViewer.addSinkMethodAnnotations(userConfiguration.getAddedSinkMethodAnnotations());
            sinkDefinitionViewer.addSinkParameterAnnotations(userConfiguration.getAddedSinkMethodParameterAnnotations());

            sourceDefinitionViewer.toggleMethodsEnabled(userConfiguration.getDisabledSourceMethods());
            sinkDefinitionViewer.toggleMethodsEnabled(userConfiguration.getDisabledSinkMethods());
            
            sourceDefinitionViewer.toggleMethodAnnotationsEnabled(userConfiguration.getDisabledSourceMethodAnnotations());
            sourceDefinitionViewer.toggleParameterAnnotationsEnabled(userConfiguration.getDisabledSourceMethodParameterAnnotations());
            sinkDefinitionViewer.toggleMethodAnnotationsEnabled(userConfiguration.getDisabledSinkMethodAnnotations());
            sinkDefinitionViewer.toggleParameterAnnotationsEnabled(userConfiguration.getDisabledSinkMethodParameterAnnotations());
            
            repaint();
        }
    }
    
    void save() throws FileNotFoundException {
        if (configFile == null)
            saveAs();
        else
            saveAs(configFile);
    }
    
    void saveAs() throws FileNotFoundException {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (configFile != null) {
            chooser.setCurrentDirectory(configFile.getParentFile());
            chooser.setSelectedFile(configFile);
        }
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            saveAs(file);
        }
    }
    
    void saveAs(File file) throws FileNotFoundException {
        configFile = file;
        
        userConfiguration.save(file);
    }
    
    void showDeploymentViewerPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem item = new JMenuItem("Add Deployment...");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pathViewer.showAddDeploymentDialog(true);
            }
        });
        
        item = new JMenuItem("Remove Deployment(s)...");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File files[] = pathViewer.getSelectedFiles();
                if (files.length > 0)
                    pathViewer.removeDeployments(files);
            }
        });
        File files[] = pathViewer.getSelectedFiles();
        item.setEnabled(files.length > 0);
        
        menu.show(pathViewer, e.getX(), e.getY());
    }
    
    void showMethodViewerPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        final Method methods[] = methodViewer.getSelectedMethods();

        JMenuItem item = new JMenuItem("Add Source Method(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to add the selected methods to the Source method definitions?", "Confirm Add Source Methods", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    sourceDefinitionViewer.addSourceMethods(methods);
                    for (Method m : methods)
                        userConfiguration.addSourceMethod(m);
                }
            }
        });
        item.setEnabled(methods.length > 0);
        
        
        item = new JMenuItem("Add Sink Method(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to add the selected methods to the Sink method definitions?", "Confirm Add Sink Methods", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    if (!SinkParameterSelectorDialog.setSinkParameterIndex(parent, methods)) {
                        sinkDefinitionViewer.addSinkMethods(methods);
                        for (Method m : methods)
                            userConfiguration.addSinkMethod(m);
                    }
                }
            }
        });
        item.setEnabled(methods.length > 0);
        
        menu.show( (JTree) e.getSource(), e.getX(), e.getY());
    }
    
    void showOutgoingEdgeViewerPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        final Method methods[] = outgoingEdgeViewer.getSelectedMethods();

        JMenuItem item = new JMenuItem("Add Source Method(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to add the selected methods to the Source method definitions?", "Confirm Add Source Methods", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    sourceDefinitionViewer.addSourceMethods(methods);
                }
            }
        });
        item.setEnabled(methods.length > 0);
        
        item = new JMenuItem("Add Sink Method(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to add the selected methods to the Sink method definitions?", "Confirm Add Sink Methods", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    if (!SinkParameterSelectorDialog.setSinkParameterIndex(parent, methods)) {
                        sinkDefinitionViewer.addSinkMethods(methods);
                        for (Method m : methods)
                            userConfiguration.addSinkMethod(m);
                    }
                }
            }
        });
        item.setEnabled(methods.length > 0);
        
        menu.show( (JTree) e.getSource(), e.getX(), e.getY());
    }
    
    void showIncomingEdgeViewerPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        final Method methods[] = incomingEdgeViewer.getSelectedMethods();

        JMenuItem item = new JMenuItem("Add Source Method(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to add the selected methods to the Source method definitions?", "Confirm Add Source Methods", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    sourceDefinitionViewer.addSourceMethods(methods);
                    for (Method m : methods)
                        userConfiguration.addSourceMethod(m);
                }
            }
        });
        item.setEnabled(methods.length > 0);
        
        item = new JMenuItem("Add Sink Method(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to add the selected methods to the Sink method definitions?", "Confirm Add Sink Methods", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    if (!SinkParameterSelectorDialog.setSinkParameterIndex(parent, methods)) {
                        sinkDefinitionViewer.addSinkMethods(methods);
                        for (Method m : methods)
                            userConfiguration.addSinkMethod(m);
                    }
                }
            }
        });
        item.setEnabled(methods.length > 0);
        
        menu.show( (JTree) e.getSource(), e.getX(), e.getY());
    }
    
    void showMethodAnnotationViewerPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        final String annotations[] = annotationViewer.getSelectedMethodAnnotations();

        JMenuItem item = new JMenuItem("Add Source Method Annotation(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = 
                      JOptionPane.showConfirmDialog(MainView.this, "Do you want to add the selected method annotation(s) to the Source method annotations definitions?", "Confirm Add Source Method Annotations", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    sourceDefinitionViewer.addSourceMethodAnnotations(annotations);
                    for (String a : annotations)
                        userConfiguration.addSourceMethodAnnotation(a);
                }
            }
        });
        item.setEnabled(annotations.length > 0);
        
        item = new JMenuItem("Add Sink Method Annotation(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to add the selected annotation(s) to the Sink method annotations definitions?", "Confirm Add Sink Methods", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    sinkDefinitionViewer.addSinkMethodAnnotations(annotations);
                    for (String a : annotations)
                        userConfiguration.addSinkMethodAnnotation(a);
                }
            }
        });
        item.setEnabled(annotations.length > 0);
        
        menu.show( (JTree) e.getSource(), e.getX(), e.getY());
    }
    
    void showParameterAnnotationViewerPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        final String annotations[] = annotationViewer.getSelectedParameterAnnotations();

        JMenuItem item = new JMenuItem("Add Source Parameter Annotation(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to add the selected methods to the Source method parameter annotation definitions?", "Confirm Add Source Method Parameter Annotation", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    sourceDefinitionViewer.addSourceParameterAnnotations(annotations);
                    for (String a : annotations)
                        userConfiguration.addSourceMethodParameterAnnotation(a);
                }
            }
        });
        item.setEnabled(annotations.length > 0);
        
        item = new JMenuItem("Add Sink Parameter Annotation(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to add the selected annotation to the Sink method parameter annotation definitions?", "Confirm Add Sink Method Parameter Annotation", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    sinkDefinitionViewer.addSinkParameterAnnotations(annotations);
                    for (String a : annotations)
                        userConfiguration.addSinkMethodParameterAnnotation(a);
                }
            }
        });
        item.setEnabled(annotations.length > 0);
        
        menu.show( (JTree) e.getSource(), e.getX(), e.getY());
    }
    
    void showSourceDefinitionMethodPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        final Method methods[] = sourceDefinitionViewer.getSelectedMethods();

        JMenuItem item = new JMenuItem("Toggle Enabled/Disabled Source Method(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to toggle enabled/disabled the selected methods?", "Confirm Toggle Enable/Disable Source Methods", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    sourceDefinitionViewer.toggleMethodsEnabled(methods);
                    for (Method m : methods)
                        userConfiguration.toggleDisabledSourceMethod(m);
                }
            }
        });
        item.setEnabled(methods.length > 0);
        
        item = new JMenuItem("Remove Source Method(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Set<Method> set = new HashSet<Method>(Arrays.asList(builtInConfiguration.getSourceMethods()));
                List<Method> list = new ArrayList<Method>();
                for (Method m : methods)
                    if (set.contains(m))
                        list.add(m);
                
                if (list.size() > 0) {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append("<html>");
                    buffer.append("The following built-in method(s) cannot be removed. Built-in methods can only be disabled.");
                    buffer.append("<ul>");
                    for (Method m : list)
                        buffer.append(Util.escapeHtml(m.toString()));
                    buffer.append("</ul>");
                    buffer.append("</html>");
                    JOptionPane.showMessageDialog(MainView.this, buffer.toString(), "Remove Error", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to remove the selected methods?", "Confirm Remove Source Methods", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        sourceDefinitionViewer.removeSources(methods);
                        for (Method m : methods)
                            userConfiguration.removeSourceMethod(m);
                    }
                }
            }
        });
        item.setEnabled(methods.length > 0);

        
        menu.show( (JTree) e.getSource(), e.getX(), e.getY());
    }
    
    void showSourceDefinitionMethodAnnotationPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        final String annotations[] = sourceDefinitionViewer.getSelectedMethodAnnotations();

        JMenuItem item = new JMenuItem("Toggle Enabled/Disabled Source Method Annotation(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = 
                        JOptionPane.showConfirmDialog(MainView.this, "Do you want to toggle enabled/disabled the selected method annotation(s)?", "Confirm Toggle Enable/Disable Source Method Annotations", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    sourceDefinitionViewer.toggleMethodAnnotationsEnabled(annotations);
                    for (String a : annotations)
                        userConfiguration.toggleDisabledSourceMethodAnnotation(a);
                }
            }
        });
        item.setEnabled(annotations.length > 0);
        
        item = new JMenuItem("Remove Source Method Annotation(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Set<String> set = new HashSet<String>(Arrays.asList(builtInConfiguration.getSourceMethodAnnotations()));
                List<String> list = new ArrayList<String>();
                for (String a : annotations)
                    if (set.contains(a))
                        list.add(a);
                
                if (list.size() > 0) {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append("<html>");
                    buffer.append("The following built-in method annotation(s) cannot be removed. Built-in annotations can only be disabled.");
                    buffer.append("<ul>");
                    for (String a : list)
                        buffer.append(Util.escapeHtml(a));
                    buffer.append("</ul>");
                    buffer.append("</html>");
                    JOptionPane.showMessageDialog(MainView.this, buffer.toString(), "Remove Error", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    int result = 
                            JOptionPane.showConfirmDialog(MainView.this, "Do you want to remove the selected method annotation(s)?", "Confirm Remove Source Method Annotations", JOptionPane.YES_NO_OPTION);
                      if (result == JOptionPane.OK_OPTION) {
                          sourceDefinitionViewer.removeSourceMethodAnnotations(annotations);
                          for (String a : annotations)
                              userConfiguration.removeSourceMethodAnnotation(a);
                      }
                }
            }
        });
        item.setEnabled(annotations.length > 0);
        
        menu.show( (JTree) e.getSource(), e.getX(), e.getY());
    }
    
    void showSourceDefinitionParameterAnnotationPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        final String annotations[] = sourceDefinitionViewer.getSelectedParameterAnnotations();

        JMenuItem item = new JMenuItem("Toggle Enabled/Disabled Source Parameter Annotation(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to toggle enabled/disabled the selected parameter annotation(s)?", "Confirm Toggle Enable/Disable Source Parameter Annotations", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    sourceDefinitionViewer.toggleParameterAnnotationsEnabled(annotations);
                    for (String a : annotations)
                        userConfiguration.toggleDisabledSourceMethodParameterAnnotation(a);
                }
            }
        });
        item.setEnabled(annotations.length > 0);
        
        item = new JMenuItem("Remove Source Parameter Annotation(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                
                Set<String> set = new HashSet<String>(Arrays.asList(builtInConfiguration.getSourceParameterAnnotations()));
                List<String> list = new ArrayList<String>();
                for (String a : annotations)
                    if (set.contains(a))
                        list.add(a);
                
                if (list.size() > 0) {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append("<html>");
                    buffer.append("The following built-in method parameter annotation(s) cannot be removed. Built-in annotations can only be disabled.");
                    buffer.append("<ul>");
                    for (String a : list)
                        buffer.append(Util.escapeHtml(a));
                    buffer.append("</ul>");
                    buffer.append("</html>");
                    JOptionPane.showMessageDialog(MainView.this, buffer.toString(), "Remove Error", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to remove the selected parameter annotation(s)?", "Confirm Remove Source Parameter Annotations", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        sourceDefinitionViewer.removeSourceMethodAnnotations(annotations);
                        for (String a : annotations)
                            userConfiguration.removeSourceMethodParameterAnnotation(a);
                    }
                }
            }
        });
        item.setEnabled(annotations.length > 0);
        
        
        menu.show( (JTree) e.getSource(), e.getX(), e.getY());
    }
    

    void showSinkDefinitionMethodPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        final Method methods[] = sinkDefinitionViewer.getSelectedMethods();

        JMenuItem item = new JMenuItem("Toggle Enabled/Disabled Sink Method(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to toggle enabled/disabled the selected methods?", "Confirm Toggle Enable/Disable Sink Methods", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    sinkDefinitionViewer.toggleMethodsEnabled(methods);
                    for (Method m : methods)
                        userConfiguration.toggleDisabledSinkMethod(m);
                }
            }
        });
        item.setEnabled(methods.length > 0);
        
        item = new JMenuItem("Remove Sink Method(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Set<Method> set = new HashSet<Method>(Arrays.asList(builtInConfiguration.getSourceMethods()));
                List<Method> list = new ArrayList<Method>();
                for (Method m : methods)
                    if (set.contains(m))
                        list.add(m);
                
                if (list.size() > 0) {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append("<html>");
                    buffer.append("The following built-in method(s) cannot be removed. Built-in methods can only be disabled.");
                    buffer.append("<ul>");
                    for (Method m : list)
                        buffer.append(Util.escapeHtml(m.toString()));
                    buffer.append("</ul>");
                    buffer.append("</html>");
                    JOptionPane.showMessageDialog(MainView.this, buffer.toString(), "Remove Error", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to remove the selected methods?", "Confirm Remove Sink Methods", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        sinkDefinitionViewer.removeSinkMethods(methods);
                        for (Method m : methods)
                            userConfiguration.removeSinkMethod(m);
                    }
                }
            }
        });
        item.setEnabled(methods.length > 0);

        
        menu.show( (JTree) e.getSource(), e.getX(), e.getY());
    }
    
    void showSinkDefinitionMethodAnnotationPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        final String annotations[] = sinkDefinitionViewer.getSelectedMethodAnnotations();

        JMenuItem item = new JMenuItem("Toggle Enabled/Disabled Sink Method Annotation(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = 
                      JOptionPane.showConfirmDialog(MainView.this, "Do you want to toggle enabled/disabled the selected method annotation(s)?", "Confirm Toggle Enable/Disable Sink Method Annotations", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    sinkDefinitionViewer.toggleMethodAnnotationsEnabled(annotations);
                    for (String a : annotations)
                        userConfiguration.toggleDisabledSinkMethodAnnotation(a);
                }
            }
        });
        item.setEnabled(annotations.length > 0);
        
        item = new JMenuItem("Remove Sink Method Annotation(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Set<String> set = new HashSet<String>(Arrays.asList(builtInConfiguration.getSourceParameterAnnotations()));
                List<String> list = new ArrayList<String>();
                for (String a : annotations)
                    if (set.contains(a))
                        list.add(a);
                
                if (list.size() > 0) {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append("<html>");
                    buffer.append("The following built-in method annotation(s) cannot be removed. Built-in annotations can only be disabled.");
                    buffer.append("<ul>");
                    for (String a : list)
                        buffer.append(Util.escapeHtml(a));
                    buffer.append("</ul>");
                    buffer.append("</html>");
                    JOptionPane.showMessageDialog(MainView.this, buffer.toString(), "Remove Error", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    int result = 
                            JOptionPane.showConfirmDialog(MainView.this, "Do you want to remove the selected method annotation(s)?", "Confirm Remove Sink Method Annotations", JOptionPane.YES_NO_OPTION);
                      if (result == JOptionPane.OK_OPTION) {
                          sinkDefinitionViewer.removeSinkMethodAnnotations(annotations);
                          for (String a : annotations)
                              userConfiguration.removeSinkMethodAnnotation(a);
                      }
                }
            }
        });
        item.setEnabled(annotations.length > 0);
        
        menu.show( (JTree) e.getSource(), e.getX(), e.getY());
    }
    
    void showSinkDefinitionParameterAnnotationPopupMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        final String annotations[] = sinkDefinitionViewer.getSelectedParameterAnnotations();

        JMenuItem item = new JMenuItem("Toggle Enabled/Disabled Source Parameter Annotation(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to toggle enabled/disabled the selected parameter annotation(s)?", "Confirm Toggle Enable/Disable Sink Parameter Annotations", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    sinkDefinitionViewer.toggleParameterAnnotationsEnabled(annotations);
                    for (String a : annotations)
                        userConfiguration.toggleDisabledSinkMethodParameterAnnotation(a);
                }
            }
        });
        item.setEnabled(annotations.length > 0);
        
        item = new JMenuItem("Remove Source Parameter Annotation(s)");
        menu.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Set<String> set = new HashSet<String>(Arrays.asList(builtInConfiguration.getSourceParameterAnnotations()));
                List<String> list = new ArrayList<String>();
                for (String a : annotations)
                    if (set.contains(a))
                        list.add(a);
                
                if (list.size() > 0) {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append("<html>");
                    buffer.append("The following built-in method parameter annotation(s) cannot be removed. Built-in annotations can only be disabled.");
                    buffer.append("<ul>");
                    for (String a : list)
                        buffer.append(Util.escapeHtml(a));
                    buffer.append("</ul>");
                    buffer.append("</html>");
                    JOptionPane.showMessageDialog(MainView.this, buffer.toString(), "Remove Error", JOptionPane.ERROR_MESSAGE);
                }
                else {
                    int result = JOptionPane.showConfirmDialog(MainView.this, "Do you want to remove the selected parameter annotation(s)?", "Confirm Remove Sink Parameter Annotations", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.OK_OPTION) {
                        sinkDefinitionViewer.removeSinkMethodParameterAnnotations(annotations);
                        for (String a : annotations)
                            userConfiguration.removeSinkMethodParameterAnnotation(a);
                    }
                }
            }
        });
        item.setEnabled(annotations.length > 0);
        
        menu.show( (JTree) e.getSource(), e.getX(), e.getY());
    }
    
    
    void resetDividerLocations() {
        splitter.setDividerLocation(.2);
        sourceSinkSplitter.setDividerLocation(.5);
        definitionSplitter.setDividerLocation(.80);
        
    }
    
//    public JProgressBar getProgressBar() {
//        return bar;
//    }
    
    Map<File, Set<Edge>> fileEdgeMap = new HashMap<File, Set<Edge>>();
    Map<File, Set<Method>> fileMethodMap = new HashMap<File, Set<Method>>();
    
    @Override
    public void deploymentAdded(final File file) {

        if (fileMethodMap.containsKey(file)) {
            JOptionPane.showMessageDialog(this, "File " + file.getAbsolutePath() + " already exists. Discarding.", "Invalid Deployment Added", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Thread thread = new Thread() {
            public void run() {
                try {
                    
                    showProgressDialog();

                    
//                    bar.setIndeterminate(true);
                    final PathUtil util = new PathUtil(file);
                    util.process();
                    
                    allMethodMap.put(file, util.getAllMethods());
                    definedMethodMap.put(file, util.getDefinedMethods());
                    
                    final Edge edges[] = util.getEdges();
                    Set<Edge> edgeSet = new LinkedHashSet<Edge>();
                    edgeSet.addAll(Arrays.asList(edges));
                    fileEdgeMap.put(file, edgeSet);
                    final Set<Method> methodSet = new LinkedHashSet<Method>();
                    fileMethodMap.put(file,  methodSet);
                    
                    for (int i=0; i<edges.length; i++) {
                        methodSet.add(edges[i].source);
                        methodSet.add(edges[i].destination);
                    }
                    
                    ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                    Callable<Object> c = new Callable<Object>() {
                        public Object call() throws Exception {
                            methodViewer.addMethods(methodSet.toArray(new Method[methodSet.size()]));
                            return null;
                        }
                    };
                    callableSet.add(c);
                    
                    c = new Callable<Object>() {
                        public Object call() throws Exception {
                            outgoingEdgeViewer.addEdges(edges);
                            return null;
                        }
                    };
                    c = new Callable<Object>() {
                        public Object call() throws Exception {
                            incomingEdgeViewer.addEdges(edges);
                            return null;
                        }
                    };
                    callableSet.add(c);
                    
                    c = new Callable<Object>() {
                        public Object call() throws Exception {
                            MethodAnnotationDescriptor methodDescriptors[] = util.getMethodAnnotations();
                            annotationViewer.addMethodAnnotations(methodDescriptors);
                            methodAnnotationDescriptorMap.put(file, methodDescriptors);
                                
                            return null;
                        }
                    };
                    callableSet.add(c);
                    
                    c = new Callable<Object>() {
                        public Object call() throws Exception {
                            ParameterAnnotationDescriptor parameterDescriptors[] = util.getParameterAnnotations();
                            annotationViewer.addParameterAnnotations(parameterDescriptors);
                            parameterAnnotationDescriptorMap.put(file, parameterDescriptors);
                            return null;
                        }
                    };
                    callableSet.add(c);
//                    service.submit(c);
                    
                    
                    try { 
                        service.invokeAll(callableSet);
                        service.shutdown();
                        service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS); 
                    } 
                    catch (InterruptedException e) {
                        // should never happen.
                        throw new RuntimeException("Bug.", e);
                    }
                    finally {
                        hideProgressDialog();
                    }
                } 
                catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(MainView.this, "An error occured while processing " + file.getAbsolutePath() + ". See stderr for details.", "Processing Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        thread.start();
    }

    @Override
    public void deploymentRemoved(File file) {
        allMethodMap.remove(file);
        definedMethodMap.remove(file);
        Set<Method> methodSet = fileMethodMap.remove(file);
        Iterator<Set<Method>> fileMethodSetIt = fileMethodMap.values().iterator();
        Set<Method> mappedMethodSet;
        Set<Method> stillReferencedMethodSet = new HashSet<Method>();
        Set<Method> intersectionMethodSet = new HashSet<Method>();
        while (fileMethodSetIt.hasNext()) {
            mappedMethodSet = fileMethodSetIt.next();
            intersectionMethodSet.clear();
            intersectionMethodSet.addAll(mappedMethodSet);
            intersectionMethodSet.retainAll(methodSet);
            stillReferencedMethodSet.addAll(intersectionMethodSet);
        }
        
        methodSet.removeAll(stillReferencedMethodSet);
        Iterator<Method> methodIt = methodSet.iterator();
        while (methodIt.hasNext()) {
            methodViewer.removeMethod(methodIt.next());
        }

        Set<Edge> edgeSet = fileEdgeMap.remove(file);
        Iterator<Set<Edge>> fileEdgeSetIt = fileEdgeMap.values().iterator();
        Set<Edge> mappedEdgeSet;
        Set<Edge> stillReferencedEdgeSet = new HashSet<Edge>();
        Set<Edge> intersectionEdgeSet = new HashSet<Edge>();
        while (fileEdgeSetIt.hasNext()) {
            mappedEdgeSet = fileEdgeSetIt.next();
            intersectionEdgeSet.clear();
            intersectionEdgeSet.addAll(mappedEdgeSet);
            intersectionEdgeSet.retainAll(edgeSet);
            stillReferencedEdgeSet.addAll(intersectionEdgeSet);
        }
        
        edgeSet.removeAll(stillReferencedEdgeSet);
        Iterator<Edge> edgeIt = edgeSet.iterator();
        Edge edge;
        while (edgeIt.hasNext()) {
            edge = edgeIt.next();
            outgoingEdgeViewer.removeEdge(edge);
            incomingEdgeViewer.removeEdge(edge);
        }
        
        
        Set<MethodAnnotationDescriptor> stillReferencedMethodAnnotationSet = new HashSet<MethodAnnotationDescriptor>();
        Set<MethodAnnotationDescriptor> intersectionMethodAnnotationSet = new HashSet<MethodAnnotationDescriptor>();
        Set<MethodAnnotationDescriptor> methodDescriptorSet = new LinkedHashSet<MethodAnnotationDescriptor>(Arrays.asList(methodAnnotationDescriptorMap.remove(file)));
        Set<MethodAnnotationDescriptor> mappedMethodAnnotationSet;
        Iterator<MethodAnnotationDescriptor[]> methodAnnotationIt = methodAnnotationDescriptorMap.values().iterator();
        while (methodAnnotationIt.hasNext()) {
            mappedMethodAnnotationSet = new LinkedHashSet<MethodAnnotationDescriptor>(Arrays.asList(methodAnnotationIt.next()));
            intersectionMethodAnnotationSet.clear();
            intersectionMethodAnnotationSet.addAll(mappedMethodAnnotationSet);
            intersectionMethodAnnotationSet.retainAll(methodDescriptorSet);
            stillReferencedMethodAnnotationSet.addAll(intersectionMethodAnnotationSet);
        }
        
        methodDescriptorSet.removeAll(stillReferencedMethodAnnotationSet);
        Iterator<MethodAnnotationDescriptor> madIt = methodDescriptorSet.iterator();
        while (madIt.hasNext()) {
            annotationViewer.removeMethodAnnotation(madIt.next());
        }
        
        Set<ParameterAnnotationDescriptor> stillReferencedParameterAnnotationSet = new HashSet<ParameterAnnotationDescriptor>();
        Set<ParameterAnnotationDescriptor> intersectionParameterAnnotationSet = new HashSet<ParameterAnnotationDescriptor>();
        Set<ParameterAnnotationDescriptor> parameterDescriptorSet = new LinkedHashSet<ParameterAnnotationDescriptor>(Arrays.asList(parameterAnnotationDescriptorMap.remove(file)));
        Set<ParameterAnnotationDescriptor> mappedParameterAnnotationSet;
        Iterator<ParameterAnnotationDescriptor[]> parameterAnnotationIt = parameterAnnotationDescriptorMap.values().iterator();
        while (methodAnnotationIt.hasNext()) {
            mappedParameterAnnotationSet = new LinkedHashSet<ParameterAnnotationDescriptor>(Arrays.asList(parameterAnnotationIt.next()));
            intersectionParameterAnnotationSet.clear();
            intersectionParameterAnnotationSet.addAll(mappedParameterAnnotationSet);
            intersectionParameterAnnotationSet.retainAll(parameterDescriptorSet);
            stillReferencedParameterAnnotationSet.addAll(intersectionParameterAnnotationSet);
        }
        
        parameterDescriptorSet.removeAll(stillReferencedParameterAnnotationSet);
        Iterator<ParameterAnnotationDescriptor> padIt = parameterDescriptorSet.iterator();
        while (padIt.hasNext()) {
            annotationViewer.removeParameterAnnotation(padIt.next());
        }
    }
    
    @Override
    public void fileSelected(File files[], boolean rootSelected) {
        methodViewer.reset();
        outgoingEdgeViewer.reset();
        incomingEdgeViewer.reset();
        annotationViewer.reset();

        Set<Method> methodSet = new LinkedHashSet<Method>();
        Set<Edge> edgeSet = new LinkedHashSet<Edge>();
        Set<MethodAnnotationDescriptor> methodAnnotationSet = new LinkedHashSet<MethodAnnotationDescriptor>();
        Set<ParameterAnnotationDescriptor> parameterAnnotationSet = new LinkedHashSet<ParameterAnnotationDescriptor>();
        
        if (rootSelected) {
            Collection<Set<Method>> methodCollection = fileMethodMap.values();
            Iterator<Set<Method>> methodIt = methodCollection.iterator();
            while (methodIt.hasNext())
                methodSet.addAll(methodIt.next());

            Collection<Set<Edge>> edgeCollection = fileEdgeMap.values();
            Iterator<Set<Edge>> edgeIt = edgeCollection.iterator();
            while (edgeIt.hasNext())
                edgeSet.addAll(edgeIt.next());
            
            Iterator<MethodAnnotationDescriptor[]> madIt = methodAnnotationDescriptorMap.values().iterator();
            while (madIt.hasNext()) {
                methodAnnotationSet.addAll(Arrays.asList(madIt.next()));
            }
            
            Iterator<ParameterAnnotationDescriptor[]> padIt = parameterAnnotationDescriptorMap.values().iterator();
            while (padIt.hasNext()) {
                parameterAnnotationSet.addAll(Arrays.asList(padIt.next()));
            }
        }
        else {
            for (int i=0; i<files.length; i++) {
                methodSet.addAll(fileMethodMap.get(files[i]));
                edgeSet.addAll(fileEdgeMap.get(files[i]));
                
                methodAnnotationSet.addAll(Arrays.asList(methodAnnotationDescriptorMap.get(files[i])));
                parameterAnnotationSet.addAll(Arrays.asList(parameterAnnotationDescriptorMap.get(files[i])));
            }
        }
        
        methodViewer.addMethods(methodSet.toArray(new Method[methodSet.size()]));
        outgoingEdgeViewer.addEdges(edgeSet.toArray(new Edge[edgeSet.size()]));
        incomingEdgeViewer.addEdges(edgeSet.toArray(new Edge[edgeSet.size()]));
        annotationViewer.addMethodAnnotations(methodAnnotationSet.toArray(new MethodAnnotationDescriptor[methodAnnotationSet.size()]));
        annotationViewer.addParameterAnnotations(parameterAnnotationSet.toArray(new ParameterAnnotationDescriptor[parameterAnnotationSet.size()]));
        
        pathViewer.setSelectedFile(rootSelected || files.length == 0 ? null : files[files.length-1]);
    }

    
    static void placeFrame(JFrame f) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        int width = (int) (screenSize.width * .85);
        int height = (int) (screenSize.height * .85);
        
        f.setSize(width, height);
        f.setLocation((screenSize.width - width) / 2, 0);
    }
    
    
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        
        final JFrame f = new JFrame("Sink Tank");
        f.setIconImages(SinkTankIconUtil.DEEPDIVE_ICON_LIST);
        f.add(new MainView(f));
        placeFrame(f);
        
        f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                int response = JOptionPane.showConfirmDialog(f, "Do you really want to exit?", "Confirm Exit", JOptionPane.OK_CANCEL_OPTION);
                if (response == JOptionPane.OK_OPTION)
                    System.exit(0);
            }
        });
        
        f.setVisible(true);
    }
}
