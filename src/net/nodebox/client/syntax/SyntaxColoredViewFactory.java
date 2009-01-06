package net.nodebox.client.syntax;

import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * Created by IntelliJ IDEA.
 * User: Frederik
 * Date: 1-apr-2005
 * Time: 11:25:54
 * To change this template use File | Settings | File Templates.
 */
public class SyntaxColoredViewFactory implements ViewFactory {
    public SyntaxColoredViewFactory() {
    }

    public View create(Element elem) {
        return new SyntaxColoredView(elem);
    }
}
