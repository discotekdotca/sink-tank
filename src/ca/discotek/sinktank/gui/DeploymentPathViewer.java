package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import ca.discotek.sinktank.FileSelectionListener;
import ca.discotek.sinktank.Method;
import ca.discotek.sinktank.Util;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class DeploymentPathViewer extends JTree {

    List<DeploymentListener> deploymentListenerList = new ArrayList<DeploymentListener>();
    List<FileSelectionListener> fileSelectionListenerList = new ArrayList<FileSelectionListener>();

    DefaultMutableTreeNode rootNode;
    DefaultTreeModel model;
    JTree tree;
    
    JTextField pathField;
    JButton addButton;
    JButton removeButton;

    Map<File, FileNode> fileNodeMap = new HashMap<File, FileNode>();
    
    MainView mainView;
    
    public DeploymentPathViewer(MainView mainView) {
        this.mainView = mainView;
        buildGui();
    }
    
    void buildGui() {
        setLayout(new BorderLayout());

        rootNode = new DefaultMutableTreeNode("Deployment Units");
        model = new DefaultTreeModel(rootNode);
        tree = new JTree(model) {
            public void paint(Graphics g) {
                super.paint(g);
                
                if (rootNode.getChildCount() == 0) {
                    Dimension d = getSize();
                    
                    Color color = UIManager.getColor("Label.disabledForeground");
                    String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()); 
                    JLabel label = new JLabel("<html><center><font color=" + hex + ">Drag Ear, War, and Jarfiles here.</font></center></html>");
                    label.setSize(d.width / 2, d.height / 2);

                    BufferedImage image = new BufferedImage(d.width / 2, d.height / 2, BufferedImage.TYPE_INT_RGB);
                    Graphics imageGraphics = image.getGraphics();
                    imageGraphics.setColor(getBackground());
                    imageGraphics.fillRect(0, 0, d.width, d.height);
                    label.paint(imageGraphics);
                    
                    g.drawImage(image, d.width / 4, d.height / 4, this);
                }
            }
        };
        tree.setToolTipText("Drag Ear, War, and Jar files here.");
        tree.setShowsRootHandles(true);
        add(new JScrollPane(tree), BorderLayout.CENTER);
        
        add(buildEditorBar(), BorderLayout.SOUTH);
        
        
        tree.setDragEnabled(true);
        tree.setTransferHandler(new TreeTransferHandler());
    }
    
    public File[] getDeployments() {
        return fileNodeMap.keySet().toArray(new File[fileNodeMap.size()]);
    }
    
    class TreeTransferHandler extends TransferHandler {
        public boolean canImport(TransferHandler.TransferSupport info) {
            return true;
        }
        
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!info.isDrop()) {
                return false;
            }
            
            if (!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                JOptionPane.showMessageDialog(tree, "Only files can be drag and dropped here.");
                return false;
            }

            Transferable t = info.getTransferable();
            try {
                List<File> fileList = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                Iterator<File> it = fileList.listIterator();
                File file;
                while (it.hasNext()) {
                    file = it.next();
                    addDeployment(file);
                }
            } 
            catch (Exception e) { return false; }

            
            return true;
        }
    }
    
    JPanel buildEditorBar() {
        JPanel panel = new JPanel();
        
        panel.setLayout(new BorderLayout());
        
        panel.add(buildFieldEditor(), BorderLayout.NORTH);
        panel.add(buildButtonPanel(), BorderLayout.SOUTH);
        
        return panel;
    }
    
    JPanel buildFieldEditor() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        pathField = new JTextField();
        panel.add(pathField, gbc);
        
        gbc.gridx++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JButton button = new JButton("Browse...");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showAddDeploymentDialog(false);
            }
        });
        panel.add(button, gbc);
        
        return panel;
    }
    
    JPanel buildButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        panel.add(new JLabel(), gbc);
        
        gbc.gridx++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;

        addButton = new JButton("Add");
        addButton.setEnabled(false);
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String value = pathField.getText().trim();
                if (value.length() > 0) {
                    String paths[] = value.split(File.pathSeparator);
                    for (int i=0; i<paths.length; i++) {
                        addDeployment(new File(paths[i]));
                    }
                    pathField.setText("");
                }
            }
        });
        panel.add(addButton, gbc);
        
        removeButton = new JButton("Remove");
        removeButton.setEnabled(false);
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File files[] = getSelectedFiles();
                if (files.length > 0)
                    removeDeployments(files);
            }
        });
        gbc.gridx++;
        panel.add(removeButton, gbc);
        
        pathField.getDocument().addDocumentListener(new DocumentListener() {
            public void removeUpdate(DocumentEvent e) { updateButtons(); }
            public void insertUpdate(DocumentEvent e) { updateButtons(); }
            public void changedUpdate(DocumentEvent e) { updateButtons(); }
            
            void updateButtons() {
                addButton.setEnabled(pathField.getText().trim().length() > 0);
            }
        });
        
        tree.addTreeSelectionListener((new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                /*
                 * Disabling this - too much processing for value.
                 */
                if (true)
                    return;
                
                boolean rootSelected = false;
                TreePath paths[] = tree.getSelectionModel().getSelectionPaths();
                boolean removeEnabled = false;

                Set<File> fileSet = new LinkedHashSet<File>();
                if (paths != null) {
                    Object o;
                    for (int i=0; i<paths.length; i++) {
                        o = paths[i].getLastPathComponent();
                        if (o == null)
                            continue;
                        else {
                            if (o instanceof FileNode) {
                                fileSet.add( ((FileNode) o).file ); 
                                removeEnabled = true;
                                break;
                            }
                            else if (o instanceof NestedArchiveNode) {
                                DefaultMutableTreeNode cursor = (DefaultMutableTreeNode) o;
                                while (cursor != null && cursor.getParent() != null) {
                                    if (cursor instanceof FileNode) {
                                        fileSet.add( ((FileNode) cursor).file );
                                        break;
                                    }
                                    else
                                        cursor = (DefaultMutableTreeNode) cursor.getParent();
                                }
                            }
                            else if (o == rootNode)
                                rootSelected = true;
                        }
                    }
                }
                
                File files[] = fileSet.toArray(new File[fileSet.size()]);
                Iterator<FileSelectionListener> it = fileSelectionListenerList.listIterator();
                while (it.hasNext()) {
                    it.next().fileSelected(files, rootSelected);
                }

                removeButton.setEnabled(removeEnabled);
            }
        }));
        
        return panel;
    }
    
    public File[] getSelectedFiles() {
        TreePath paths[] = tree.getSelectionModel().getSelectionPaths();

        List<File> fileList = new ArrayList<>();
        if (paths != null) {
            Object o;
            for (int i=0; i<paths.length; i++) {
                o = paths[i].getLastPathComponent();
                if (o instanceof FileNode)
                    fileList.add( ((FileNode) o).file ); 
            }
        }
        
        return fileList.toArray(new File[fileList.size()]);
    }
    
    public void showAddDeploymentDialog(boolean addDirectly) {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = 
            new FileNameExtensionFilter("JEE Deployments Units", "jar", "war", "ear");
        chooser.setFileFilter(filter);
        chooser.setMultiSelectionEnabled(true);
        String value = pathField.getText();
        File file = new File(value);
        if (file.isFile()) {
            chooser.setCurrentDirectory(file.getParentFile());
            chooser.setSelectedFile(file);
        }
        else {
            File cursor = file;
            while (cursor != null) {
                if (cursor.isDirectory())
                    chooser.setCurrentDirectory(cursor);
                cursor = file.getParentFile();
            }
        }
        
        int result = chooser.showOpenDialog(DeploymentPathViewer.this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File files[] = chooser.getSelectedFiles();
            StringBuilder buffer = new StringBuilder();
            for (int i=0; i<files.length; i++) {
                if (addDirectly) {
                    addDeployment(files[i]);
                }
                else {
                    if (i>0)
                        buffer.append(File.pathSeparator);
                    buffer.append(files[i].getAbsolutePath());
                }
            }
            
            if (!addDirectly)
                pathField.setText(buffer.toString());
        }
    }
    
    Map<String, Set<Method>> pathMethodMap = new HashMap<String, Set<Method>>();
    
    public void addDeployment(final File file) {
        if (fileNodeMap.containsKey(file)) {
            JOptionPane.showMessageDialog(SwingUtilities.windowForComponent(this), "File " + file.getAbsolutePath() + " already exists. Discarding.", "Invalid Deployment Added", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        Thread thread = new Thread() {
            public void run() {
                
                String name = file.getName();
                if (file.isFile() && (name.endsWith(".ear") || name.endsWith(".war") || name.endsWith(".jar"))) {
                    try ( FileInputStream fis = new FileInputStream(file); ) {
                        FileNode node = new FileNode(file);
                        fileNodeMap.put(file, node);
                        TreeUtil.insertNode(node, rootNode, model);
                        tree.expandPath(new TreePath(rootNode.getPath()));
                        tree.expandPath(new TreePath(node.getPath()));
                        
                        processArchive(fis, file.getAbsolutePath(), node);
                        
                        Iterator<DeploymentListener> it = deploymentListenerList.listIterator();
                        while (it.hasNext()) 
                            it.next().deploymentAdded(file);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog
                            (DeploymentPathViewer.this, "Error occured parsing " + file.getAbsolutePath() + ". See stderr for stack trace.", "Parse Error", JOptionPane.ERROR_MESSAGE);
                    }
                    catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
                else if (file.isDirectory()) {
                    FileNode node = new FileNode(file);
                    fileNodeMap.put(file, node);
                    TreeUtil.insertNode(node, rootNode, model);
                    tree.expandPath(new TreePath(rootNode.getPath()));
                    tree.expandPath(new TreePath(node.getPath()));
                    
                    Iterator<DeploymentListener> it = deploymentListenerList.listIterator();
                    while (it.hasNext()) 
                        it.next().deploymentAdded(file);
                }
            }
        };
        thread.start();
    }
    
    public void removeDeployments(File files[]) {
        int result = JOptionPane.showConfirmDialog(SwingUtilities.windowForComponent(this), "Do you want to remove the selected files?", "Confirm Remove Files", JOptionPane.ERROR_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            for (int i=0; i<files.length; i++)
                removeDeployment(files[i]);
        }
    }
    
    public void removeDeployment(File file) {
        removeDeployment(file, true);
    }
    
    public void removeDeployment(File file, boolean showWarning) {
        FileNode node = fileNodeMap.remove(file);
        if (node == null) {
            if (showWarning)
                JOptionPane.showMessageDialog(this, "Could not find node for file " + file.getAbsolutePath(), "Invalid File For Removal", JOptionPane.ERROR_MESSAGE);
        }
        else {
            model.removeNodeFromParent(node);
            Iterator<DeploymentListener> it = deploymentListenerList.listIterator();
            while (it.hasNext()) 
                it.next().deploymentRemoved(file);
        }
    }

    ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
    void processArchive(InputStream is, final String path, AbstractNode parentNode) throws IOException {
        if (mainView.callableSet.size() == 0)
            mainView.showProgressDialog();
        final ZipInputStream zis = new ZipInputStream(is);
        ZipEntry entry;
        String name;
        
        Callable<Object> c;
        while ( (entry = zis.getNextEntry()) != null) {
            name = entry.getName();
            
            if (name.endsWith(".jar") || name.endsWith(".war")) {
                NestedArchiveNode node = new NestedArchiveNode(name);
                TreeUtil.insertNode(node, parentNode, model);
                tree.expandPath(new TreePath(rootNode.getPath()));
                tree.expandPath(new TreePath(parentNode.getPath()));
                tree.expandPath(new TreePath(node.getPath()));
                
//                processArchive(zis, path + "!/" + name, node);
                
                final String finalName = name;
                final NestedArchiveNode finalNode = node;
                final byte bytes[] = Util.read(zis);
                c = new Callable<Object>() {
                    public String call() throws Exception {
                        processArchive(new ByteArrayInputStream(bytes), path + "!/" + finalName, finalNode);
                        mainView.callableSet.remove(this);
                        return null;
                    }
                };
                mainView.callableSet.add(c);
                service.submit(c);
            }
        }
    }
    
    public void setSelectedFile(File file) {
        pathField.setText(file == null ? "" : file.getAbsolutePath());
    }
    
    public void addDeployentListener(DeploymentListener listener) {
        deploymentListenerList.add(listener);
    }
    
    public void addFileSelectionListener(FileSelectionListener listener) {
        fileSelectionListenerList.add(listener);
    }
    
    public void addPopupMouseListener(MouseListener l) {
        tree.addMouseListener(l);
    }
    
    abstract class AbstractNode extends DefaultMutableTreeNode {
        
        public final String name;
        
        public AbstractNode(String name) {
            super(name);
            this.name = name;
        }
    }
    
    class FileNode extends AbstractNode {

        public final File file;
        
        public FileNode(File file) {
            super(file.getAbsolutePath());
            this.file = file;
        }
    }
    
    class NestedArchiveNode extends AbstractNode {
        public NestedArchiveNode(String name) {
            super(name);
        }
    }
}
