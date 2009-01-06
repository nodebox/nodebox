package net.nodebox.client.syntax;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: Frederik
 * Date: 1-apr-2005
 * Time: 11:26:46
 * To change this template use File | Settings | File Templates.
 */
public class SyntaxColoredView extends PlainView implements Runnable {

    private Segment segment;
    private Token currentLineTokens;

    public SyntaxColoredView(Element element) {
        super(element);
        segment = new Segment();
    }

    public final void run() {
        getContainer().repaint();
    }

    protected void drawLine(int lineIndex, Graphics g, int x, int y) {
        Document document = getDocument();
        JTextComponent jtc;
        int posStart = (jtc = (JTextComponent) getContainer()).getSelectionStart();
        int posEnd = jtc.getSelectionEnd();
        Element element = getElement().getElement(lineIndex);
        int beginOfLine = element.getStartOffset();
        int endOfLine = element.getEndOffset();

        g.setColor(getForeground());
        try {
            document.getText(beginOfLine, endOfLine - (beginOfLine + 1), segment);
            TokenMarker tokenMarker = ((SyntaxDocument) getDocument()).getTokenMarker();
            SyntaxStyle[] styles = SyntaxUtilities.getDefaultSyntaxStyles();
            // SyntaxStyle[] styles = ((SyntaxDocument) getDocument()).getStyles();
            if (tokenMarker == null) {
                Utilities.drawTabbedText(segment, x, y, g, this, 0);
            } else {
                currentLineTokens = tokenMarker.markTokens(segment, lineIndex);
                SyntaxUtilities.paintSyntaxLine(segment, currentLineTokens, styles, this, g, x, y);
                if (tokenMarker.isNextLineRequested() && lineIndex < document.getDefaultRootElement().getElementCount() - 1) {
                    SwingUtilities.invokeLater(this);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Color getForeground() {
        return getContainer().getForeground();
    }
}
