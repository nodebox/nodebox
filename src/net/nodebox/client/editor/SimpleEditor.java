package net.nodebox.client.editor;

import net.nodebox.client.CodeArea;
import net.nodebox.client.FileUtils;
import net.nodebox.client.PlatformUtils;
import net.nodebox.client.SaveDialog;
import net.nodebox.util.PythonUtils;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

public class SimpleEditor extends JPanel implements TreeSelectionListener {

    private static final Color inactiveColor = new Color(222, 222, 222);
    private static final Color activeColor = new Color(246, 246, 246);

    private File editorDirectory;
    private ArrayList<SimpleDocument> openDocuments = new ArrayList<SimpleDocument>();
    private JLabel placeHolder;
    private TreeModel fileModel;
    private TabBar tabBar;
    private JTree fileTree;

    public SimpleEditor() {
        setLayout(new BorderLayout());
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        PythonUtils.initializePython();
        FileCellRenderer fileCellRenderer = new FileCellRenderer();
        tabBar = new TabBar();
        placeHolder = new JLabel();
        placeHolder.setPreferredSize(new Dimension(500000, 5000000));
        placeHolder.setLayout(new BorderLayout());
        JPanel contentsPanel = new JPanel(new BorderLayout());
        contentsPanel.add(tabBar, BorderLayout.NORTH);
        contentsPanel.add(placeHolder, BorderLayout.CENTER);
        fileTree = new JTree();
        fileTree.getSelectionModel().addTreeSelectionListener(this);
        fileTree.setCellRenderer(fileCellRenderer);
        JScrollPane fileTreeScroll = new JScrollPane(fileTree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setEditorDirectory(editorDirectory);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fileTreeScroll, contentsPanel);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setDividerLocation(0.5);
        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        File editorDirectory = new File(PlatformUtils.getUserNodeTypeLibraryDirectory());
        JFrame frame = new JFrame("Simple Editor");
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        SimpleEditor e = new SimpleEditor();
        e.setEditorDirectory(editorDirectory);
        rootPanel.add(e);
        frame.setContentPane(rootPanel);
        frame.setLocationByPlatform(true);
        frame.setSize(1100, 800);
        frame.setVisible(true);
    }

    public File getEditorDirectory() {
        return editorDirectory;
    }

    public void setEditorDirectory(File editorDirectory) {
        if (this.editorDirectory != null && this.editorDirectory.equals(editorDirectory)) return;
        for (SimpleDocument doc : openDocuments) {
            closeDocument(doc);
        }
        if (editorDirectory == null) {
            this.editorDirectory = null;
            fileTree.setModel(null);
            return;
        }
        if (!editorDirectory.isDirectory())
            throw new RuntimeException("The given editor directory is not a directory.");
        this.editorDirectory = editorDirectory;
        fileModel = new FileModel(editorDirectory);
        fileTree.setModel(fileModel);

    }

    public void switchToDocument(SimpleDocument document) {
        for (SimpleDocument doc : openDocuments) {
            doc.getFileTab().setActive(false);
        }
        placeHolder.removeAll();
        if (document != null) {
            placeHolder.add(document.getCodeScroll(), BorderLayout.CENTER);
            document.getFileTab().setActive(true);
        }
        placeHolder.revalidate();
        placeHolder.repaint();
    }

    public SimpleDocument getDocument(File file) {
        for (SimpleDocument doc : openDocuments) {
            if (doc.getFile().equals(file))
                return doc;
        }
        return null;
    }

    public SimpleDocument getDocument(FileTab fileTab) {
        for (SimpleDocument doc : openDocuments) {
            if (doc.getFileTab() == fileTab)
                return doc;
        }
        return null;
    }

    public void newTabWithFile(File file) {
        if (file.isDirectory()) return;
        SimpleDocument doc = getDocument(file);
        if (doc == null) {
            String contents = FileUtils.readFile(file);
            CodeArea codeArea = new CodeArea();
            codeArea.setText(contents);
            FileTab fileTab = new FileTab(file);
            doc = new SimpleDocument(file, codeArea, fileTab);
            openDocuments.add(doc);
            tabBar.add(fileTab);
            tabBar.revalidate();
            tabBar.repaint();
        }
        switchToDocument(doc);
    }


    public void closeDocument(SimpleDocument document) {
        if (document.isChanged()) {
            SaveDialog sd = new SaveDialog();
            int retVal = sd.show((JFrame) SwingUtilities.getWindowAncestor(this));
            if (retVal == JOptionPane.YES_OPTION) {
                document.save();
                // Now fall through, which will close the document
            } else if (retVal == JOptionPane.NO_OPTION) {
                // Fall through, which will close the document
            } else if (retVal == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
        tabBar.remove(document.getFileTab());
        tabBar.revalidate();
        tabBar.repaint();
        int documentIndex = openDocuments.indexOf(document);
        openDocuments.remove(document);
        placeHolder.removeAll();
        SimpleDocument doc = null;
        if (openDocuments.size() == 0) {
            switchToDocument(doc);
        } else {
            switchToDocument(openDocuments.get(Math.max(0, documentIndex - 1)));
        }
    }

    public void closeTab(FileTab fileTab) {
        SimpleDocument doc = getDocument(fileTab);
        closeDocument(doc);
    }

    public void switchToTab(FileTab fileTab) {
        SimpleDocument doc = getDocument(fileTab);
        switchToDocument(doc);
    }

    public void valueChanged(TreeSelectionEvent e) {
        File f = (File) e.getPath().getLastPathComponent();
        if (f.isDirectory()) return;
        newTabWithFile(f);
    }

    public void saveAll() {
        for (SimpleDocument doc : openDocuments) {
            doc.save();
        }
    }


    public class TabBar extends JPanel {
        public TabBar() {
            setLayout(new FlowLayout(FlowLayout.LEADING));
            setPreferredSize(new Dimension(300, 26));
        }
    }

    public static class SimpleDocument implements DocumentListener {
        private File file;
        private JScrollPane codeScroll;
        private CodeArea codeArea;
        private FileTab fileTab;
        private boolean changed;

        public SimpleDocument(File file, CodeArea codeArea, FileTab fileTab) {
            this.file = file;
            this.codeArea = codeArea;
            codeArea.getDocument().addDocumentListener(this);
            this.codeScroll = new JScrollPane(codeArea);
            this.fileTab = fileTab;
        }

        public File getFile() {
            return file;
        }

        public CodeArea getCodeArea() {
            return codeArea;
        }

        public FileTab getFileTab() {
            return fileTab;
        }

        public JScrollPane getCodeScroll() {
            return codeScroll;
        }

        @Override
        public String toString() {
            return file.getName();
        }

        public boolean isChanged() {
            return changed;
        }

        public void setChanged(boolean changed) {
            this.changed = changed;
            fileTab.setChanged(true);
        }

        public void save() {
            FileUtils.writeFile(file, codeArea.getText());
            setChanged(false);
        }

        public void insertUpdate(DocumentEvent e) {
            setChanged(true);
        }

        public void removeUpdate(DocumentEvent e) {
            setChanged(true);
        }

        public void changedUpdate(DocumentEvent e) {
            setChanged(true);
        }
    }

    public class FileTab extends JComponent implements MouseListener {

        private File file;
        private boolean active;
        private boolean changed;

        private JLabel fileLabel;
        private CloseButton closeButton;

        public FileTab(File file) {
            setLayout(new FlowLayout(FlowLayout.LEADING));
            setBorder(BorderFactory.createLineBorder(Color.GRAY));
            this.file = file;
            fileLabel = new JLabel(file.getName());
            closeButton = new CloseButton();
            closeButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    closeTab(FileTab.this);
                }
            });
            add(closeButton);
            add(fileLabel);
            addMouseListener(this);
            setOpaque(true);
        }

        @Override
        public void paintComponent(Graphics g) {
            Rectangle r = g.getClipBounds();
            g.setColor(getBackground());
            g.fillRoundRect(r.x, r.y, r.width, r.height, 5, 5);
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
            Color bgColor = active ? activeColor : inactiveColor;
            setBackground(bgColor);
            repaint();
        }

        public boolean isChanged() {
            return changed;
        }

        public void setChanged(boolean changed) {
            this.changed = changed;
            String closeText = changed ? "o" : "x";
            closeButton.setText(closeText);
            closeButton.repaint();
        }

        public File getFile() {
            return file;
        }

        public void mouseClicked(MouseEvent e) {
            switchToTab(this);
        }

        public void mousePressed(MouseEvent e) {

        }

        public void mouseReleased(MouseEvent e) {

        }

        public void mouseEntered(MouseEvent e) {

        }

        public void mouseExited(MouseEvent e) {

        }
    }

    public class CloseButton extends JButton {
        public CloseButton() {
            super("x");
            setPreferredSize(new Dimension(12, 12));
            setBorder(BorderFactory.createEmptyBorder());
        }
    }


    public class FileModel implements TreeModel, FileFilter {

        private File rootDirectory;

        public FileModel(File rootDirectory) {
            this.rootDirectory = rootDirectory;
        }

        public Object getRoot() {
            return rootDirectory;
        }

        public boolean accept(File pathname) {
            if (pathname.getName().equals(".svn")) return false;
            if (pathname.getName().equals(".git")) return false;
            if (pathname.isDirectory()) return true;
            if (pathname.getName().endsWith(".py")) return true;
            if (pathname.getName().endsWith(".ntl")) return true;
            return false;
        }

        private File[] getChildFiles(Object parent) {
            assert (parent instanceof File);
            return ((File) parent).listFiles(this);
        }

        public Object getChild(Object parent, int index) {
            File[] files = getChildFiles(parent);
            if (files == null) return null;
            return files[index];
        }

        public int getChildCount(Object parent) {
            File[] files = getChildFiles(parent);
            if (files == null) return 0;
            return files.length;
        }

        public boolean isLeaf(Object node) {
            assert (node instanceof File);
            return !((File) node).isDirectory();
        }

        public void valueForPathChanged(TreePath path, Object newValue) {
        }

        public int getIndexOfChild(Object parent, Object child) {
            if (parent instanceof File && child instanceof File) {
                File[] files = getChildFiles(parent);
                if (files == null) return -1;
                for (int i = 0; i < files.length; i++) {
                    if (files[i].equals(child)) return i;
                }
            }
            return -1;
        }

        public void addTreeModelListener(TreeModelListener l) {
        }

        public void removeTreeModelListener(TreeModelListener l) {
        }
    }

    private class FileCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            //String stringValue = tree.convertValueToText(value, sel,
            //        expanded, leaf, row, hasFocus);

            this.hasFocus = hasFocus;
            String stringValue = ((File) value).getName();

            setText(stringValue);
            if (sel)
                setForeground(getTextSelectionColor());
            else
                setForeground(getTextNonSelectionColor());
            // There needs to be a way to specify disabled icons.
            if (!tree.isEnabled()) {
                setEnabled(false);
                if (leaf) {
                    setDisabledIcon(getLeafIcon());
                } else if (expanded) {
                    setDisabledIcon(getOpenIcon());
                } else {
                    setDisabledIcon(getClosedIcon());
                }
            } else {
                setEnabled(true);
                if (leaf) {
                    setIcon(getLeafIcon());
                } else if (expanded) {
                    setIcon(getOpenIcon());
                } else {
                    setIcon(getClosedIcon());
                }
            }
            setComponentOrientation(tree.getComponentOrientation());

            selected = sel;

            return this;
        }
    }
}
