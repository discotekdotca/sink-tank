package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.benf.cfr.reader.Main;

import ca.discotek.sinktank.AsmUtil;
import ca.discotek.sinktank.Method;
import ca.discotek.sinktank.ProjectData;
import ca.discotek.sinktank.swing.ButtonPanel;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class SourceViewer extends JPanel {

    JTabbedPane tabber;
    
    public SourceViewer() {
        buildGui();
    }
    
    void buildGui() {
        setLayout(new BorderLayout());
        tabber = new JTabbedPane();
        add(tabber, BorderLayout.CENTER);
        
        ButtonPanel panel = new ButtonPanel();
        JButton button = new JButton("Clear All");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tabber.removeAll();
            }
        });
        panel.addButton(button);
        add(panel, BorderLayout.SOUTH);
    }
    
    static final File TMPDIR = new File(System.getProperty("java.io.tmpdir"), "_decompiled_");
    
    Map<String, SearchView> classMap = new HashMap<String, SearchView>();
    
    public void addMethod(File classFile, Method method) throws IOException{
        String javaName = addClass(classFile);
        
        if (javaName != null) {
            SearchView searchView = (SearchView) classMap.get(javaName);
            if (searchView != null) {
                searchView.field.setText(' ' + ProjectData.getMethodName(method.memberId) + '(');
                searchView.find();
            }
        }
    }
    
    public String addClass(File classFile) throws IOException{
        Main.main(new String[] {classFile.getAbsolutePath(), "--outputdir", TMPDIR.getAbsolutePath(), "--silent", "true", "--clobber", "true", "--comments", "false"});

        FileInputStream fis2 = null;
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        String javaName;
        try {
            fis = new FileInputStream(classFile);

            javaName = AsmUtil.getClassName(fis) + ".java";
            
            Component component = classMap.get(javaName);
            if (component == null) {
                File file = new File(TMPDIR, javaName);
                file.deleteOnExit();
                
                
                fis2 = new FileInputStream(file);
                isr = new InputStreamReader(fis2);
                br = new BufferedReader(isr);
                String line;
                
                StringBuilder buffer = new StringBuilder();
                boolean inComment = false;
                while ( (line = br.readLine()) != null) {
                    if (line.trim().startsWith("/*"))
                        inComment = true;
                    else if (line.trim().startsWith("*/")) {
                        inComment = false;
                        continue;
                    }
                    
                    if (inComment)
                        continue;
                    else 
                        buffer.append(line + "\n");
                }
                
                JEditorPane editor = new JEditorPane();
                editor.setFont(new Font("Courier New", Font.PLAIN, 14));
                editor.setContentType("text/plain");
                editor.setText(buffer.toString());
                editor.setCaretPosition(0);
                editor.setEditable(false);
                
                SearchView searchView = new SearchView(editor);
                
                String tabText = classFile.getName();
                tabber.addTab(tabText, searchView);
                int index = tabber.getTabCount()-1;
                
                CloseableTab.CloseListener l = new CloseableTab.CloseListener() {
                    public void tabClosed(String id) {
                        classMap.remove(id);
                    }
                };
                
                tabber.setTabComponentAt(index, new CloseableTab(javaName, tabText, tabber, searchView, l));
                tabber.setSelectedIndex(index);
                
                classMap.put(javaName, searchView);

            }
            else
                tabber.setSelectedComponent(component);
            
        }
        finally {
            if (br != null) try { br.close(); } catch (Exception e) { e.printStackTrace(); }
            if (isr != null) try { isr.close(); } catch (Exception e) { e.printStackTrace(); }
            if (fis != null) try { fis.close(); } catch (Exception e) { e.printStackTrace(); }
            if (fis2 != null) try { fis2.close(); } catch (Exception e) { e.printStackTrace(); }
        }

        return javaName;
    }
    
    class SearchView extends JPanel {
        JEditorPane editor;
        JTextField field;
        
        SearchView(JEditorPane editor) {
            this.editor = editor;
            buildGui();
        }
        
        void buildGui() {
            setLayout(new BorderLayout());
            add(new JScrollPane(editor), BorderLayout.CENTER);
            add(buildSearchPanel(), BorderLayout.SOUTH);
        }
        
        JPanel buildSearchPanel() {
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(2,2,2,2);
            gbc.gridx = gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.weighty = 0;
            
            field = new JTextField(50);
            panel.add(field, gbc);

            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridx++;
            JButton button = new JButton("Find");
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    find();
                }
            });
            button.setFocusable(false);
            panel.add(button, gbc);
            
            KeyListener l = new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        e.consume();
                        field.requestFocus();
                        find();
                    }
                }
            };
            
            field.addKeyListener(l);
            editor.addKeyListener(l);

            InputMap map = editor.getInputMap();
            while (map != null) {
                map.remove(KeyStroke.getKeyStroke("ENTER"));
                map = map.getParent();
            }
                        
            return panel;
        }
        
        void find() {
            final String searchText = field.getText();
            if (searchText.length() > 0) {
                int position = editor.getCaretPosition();
                String text = editor.getText();
                int index = text.indexOf(searchText, position);
                if (index < 0 && position > 0)
                    index = text.indexOf(searchText);
                if (index > 0) {
                    final int finalIndex = index;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            editor.requestFocus();
                            editor.select(finalIndex, finalIndex + searchText.length());
                        }
                    });
                }
            }
        }
    }
    
}
