package nodebox.ui.syntax;

import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

public class SyntaxColoredViewFactory implements ViewFactory {
    public SyntaxColoredViewFactory() {
    }

    public View create(Element elem) {
        return new SyntaxColoredView(elem);
    }
}
