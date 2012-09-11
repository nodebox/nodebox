package nodebox.ui.syntax;

import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.ViewFactory;

public class SyntaxEditorKit extends DefaultEditorKit {
    private SyntaxColoredViewFactory viewFactory;
    private TokenMarker tokenMarker;

    public SyntaxEditorKit() {
        tokenMarker = new PythonTokenMarker();
        viewFactory = new SyntaxColoredViewFactory();
    }

    public Document createDefaultDocument() {
        SyntaxDocument doc = new SyntaxDocument();
        doc.setTokenMarker(tokenMarker);
        return doc;
    }

    public ViewFactory getViewFactory() {
        return viewFactory;
    }
}
