package nodebox.client.editor;

import nodebox.client.CodeArea;
import nodebox.client.PaneView;
import nodebox.client.Theme;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.util.ArrayList;

public class SimpleEditor extends JPanel implements PaneView, DocumentListener {

    private ArrayList<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
    private CodeArea codeArea;
    private boolean changed = false;

    public SimpleEditor() {
        setLayout(new BorderLayout());
        codeArea = new CodeArea();
        codeArea.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        codeArea.getDocument().addDocumentListener(this);
        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setBorder(null);
        add(codeScroll, BorderLayout.CENTER);
    }

    public UndoManager getUndoManager() {
        return codeArea.getUndoManager();
    }

    public void setUndoManager(UndoManager undoManager) {
        codeArea.setUndoManager(undoManager);
    }

    public String getSource() {
        return codeArea.getText();
    }

    public void setSource(String source) {
        codeArea.setText(source);
        codeArea.setCaretPosition(0);
        changed = false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        codeArea.setEnabled(enabled);
        if (enabled) {
            codeArea.setBackground(Color.white);
        } else {
            codeArea.setBackground(Theme.EDITOR_DISABLED_BACKGROUND_COLOR);
        }
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
        if (changed) {
            fireDocumentChanged();
        }
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

    public void addCaretListener(CaretListener l) {
        codeArea.addCaretListener(l);
    }

    public void removeCaretListener(CaretListener l) {
        codeArea.removeCaretListener(l);
    }

    public void addChangeListener(ChangeListener l) {
        changeListeners.add(l);
    }

    public void removeChangeListener(ChangeListener l) {
        changeListeners.remove(l);
    }

    public void fireDocumentChanged() {
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener l : changeListeners) {
            l.stateChanged(e);
        }
    }
}
