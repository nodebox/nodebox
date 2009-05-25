package net.nodebox.client;

import net.nodebox.client.editor.SimpleEditor;
import net.nodebox.node.Node;
import net.nodebox.node.NodeCode;
import net.nodebox.node.Parameter;
import net.nodebox.node.PythonCode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class EditorPane extends Pane {

    private PaneHeader paneHeader;
    private NetworkAddressBar networkAddressBar;
    private SimpleEditor editor;
    //private CodeArea codeArea;
    private Node node;

    public EditorPane(NodeBoxDocument document) {
        this();
        setDocument(document);
    }

    public EditorPane() {
        setLayout(new BorderLayout(0, 0));
        paneHeader = new PaneHeader(this);
        networkAddressBar = new NetworkAddressBar(this);
        paneHeader.add(networkAddressBar);
        JButton reloadButton = new JButton(new ReloadAction());
        paneHeader.add(reloadButton);
        reloadButton.setBorderPainted(false);
        reloadButton.setText("");
        editor = new SimpleEditor();
        CodeArea.defaultInputMap.put(PlatformUtils.getKeyStroke(KeyEvent.VK_R), new ReloadAction());
        add(paneHeader, BorderLayout.NORTH);
        add(editor, BorderLayout.CENTER);
    }

    @Override
    public void setDocument(NodeBoxDocument document) {
        super.setDocument(document);
        if (document == null) return;
        setNode(document.getActiveNode());
    }

    public Pane clone() {
        return new EditorPane(getDocument());
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        if (this.node == node) return;
        this.node = node;
        networkAddressBar.setNode(node);
        if (node == null) return;

        Parameter pCode = node.getParameter("_code");
        if (pCode == null) {
            editor.setSource("# This node has no source code.");
        } else {
            String code = pCode.asCode().getSource();
            editor.setSource(code);
        }
    }

    @Override
    public void focusedNodeChanged(Node activeNode) {
        setNode(activeNode);
    }

    private class ReloadAction extends AbstractAction {
        private ReloadAction() {
            super("Reload");
            ImageIcon icon = new ImageIcon("res/reload-16.png", "Reload");
            putValue(LARGE_ICON_KEY, icon);
        }

        public void actionPerformed(ActionEvent e) {
            Parameter pCode = node.getParameter("_code");
            if (pCode == null) return;
            NodeCode code = new PythonCode(editor.getSource());
            pCode.set(code);
        }
    }
}
