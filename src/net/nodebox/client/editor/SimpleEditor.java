package net.nodebox.client.editor;

import net.nodebox.client.CodeArea;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class SimpleEditor extends JPanel implements DocumentListener {

    private CodeArea codeArea;
    private boolean changed = false;

    public SimpleEditor() {
        setLayout(new BorderLayout());
        codeArea = new CodeArea();
        codeArea.getDocument().addDocumentListener(this);
        JScrollPane codeScroll = new JScrollPane(codeArea);
        add(codeScroll, BorderLayout.CENTER);
    }

    public String getSource() {
        return codeArea.getText();
    }

    public void setSource(String source) {
        codeArea.setText(source);
        changed = false;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
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
