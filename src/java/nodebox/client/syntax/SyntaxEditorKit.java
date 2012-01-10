package nodebox.client.syntax;

import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.ViewFactory;

/**
 * Created by IntelliJ IDEA.
 * User: Frederik
 * Date: 1-apr-2005
 * Time: 11:25:14
 * To change this template use File | Settings | File Templates.
 */
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
