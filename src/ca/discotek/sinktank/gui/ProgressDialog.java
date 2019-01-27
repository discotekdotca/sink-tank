package ca.discotek.sinktank.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

/**
 * 
 * @author Rob Kenworthy
 * @author discotek.ca
 * 
 * @see <a href="https://discotek.ca/blog/2019/01/23/the-magic-behind-burp-and-zap-and-other-proxies">Detailed Proxy Java Code Explanation</a>
 *
 */

public class ProgressDialog extends JDialog implements JobListener {

    Map<Callable<?>, JProgressBar> jobBarMap = new HashMap<Callable<?>, JProgressBar>();
    
    Map<JProgressBar, Integer> barEndMap = new HashMap<JProgressBar, Integer>();
    Map<JProgressBar, String> barTextMap = new HashMap<JProgressBar, String>();
    JPanel panel;
    GridBagConstraints gbc;
    
    JFrame parent;
    
    public ProgressDialog(JFrame f) {
        super(f, "Progress", true);
        this.parent = f;
        buildGui();
    }
    
    void buildGui() {
        setLayout(new BorderLayout());
        panel = new JPanel();
        add(new JScrollPane(panel));
        
        panel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(1, 1, 1, 1);
        
        JButton button = new JButton("Exit JVM");
        add(button, BorderLayout.SOUTH);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        
        setSize(300, 200);
    }
    
    public void setListeningEnabled(boolean enabled) {
        if (enabled)
            ProgressSupport.getInstance().addListener(this);
        else
            ProgressSupport.getInstance().removeListener(this);
    }
    
    @Override
    public synchronized void start(Callable<?> job, int start, int end, String text) {
        JProgressBar bar;
        
        if (start < 0) {
            bar = new JProgressBar();
            bar.setIndeterminate(true);
        }
        else {
            bar = new JProgressBar(start, end);
            bar.setStringPainted(true);
        }
        
        updateText(bar, start, end, text);
        
        jobBarMap.put(job, bar);
        barEndMap.put(bar, end);
        barTextMap.put(bar, text);
        gbc.gridy++;
        panel.add(bar, gbc);
        invalidate();
        revalidate();
        repaint();
        
        if (!isVisible()) {
            Thread t = new Thread() {
                public void run() {
                    setLocationRelativeTo(parent);
                    setVisible(true);
                }
            };
            t.start();
        }
    }
    
    void updateText(JProgressBar bar, int index, int end, String text) {
        if (end < 0)
            bar.setString('[' + text + ']');
        else
            bar.setString(index + " / " + end + " [" + text + ']');
    }

    @Override
    public void update(Callable<?> job, int index) {
        JProgressBar bar = jobBarMap.get(job);
        bar.setValue(index);
        updateText(bar, index, barEndMap.get(bar), barTextMap.get(bar));
    }

    @Override
    public synchronized void finished(Callable<?> job) {
        JProgressBar bar = jobBarMap.remove(job);
        barEndMap.remove(bar);
        panel.remove(bar);
        invalidate();
        revalidate();
        repaint();

        
        if (jobBarMap.size() == 0) {
            gbc.gridy = 0;
            setVisible(false);
        }
    }
}
