package nodebox.client.port;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import nodebox.client.NodeBoxDocument;
import nodebox.node.Port;
import nodebox.ui.Theme;

import org.apache.commons.lang.StringUtils;

import com.jidesoft.popup.JidePopup;

public class TextControl extends AbstractPortControl implements ActionListener {

	private JTextField textField;
	private JButton externalWindowButton;
	private String portValue;

	public TextControl(String nodePath, Port port) {
		super(nodePath, port);
		setLayout(new BorderLayout(0, 0));
		textField = new JTextField();
		textField.putClientProperty("JComponent.sizeVariant", "small");
		textField.setFont(Theme.SMALL_BOLD_FONT);
		textField.addActionListener(this);
		textField.setEditable(false);
		externalWindowButton = new JButton("...");
		externalWindowButton.putClientProperty("JComponent.sizeVariant",
				"small");
		externalWindowButton
				.putClientProperty("JButton.buttonType", "gradient");
		externalWindowButton.setFont(Theme.SMALL_BOLD_FONT);
		externalWindowButton.addActionListener(this);
		add(textField, BorderLayout.CENTER);
		add(externalWindowButton, BorderLayout.EAST);
		setValueForControl(port.getValue());
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		textField.setEnabled(enabled);
		externalWindowButton.setEnabled(enabled);
	}

	@Override
	public void setValueForControl(Object v) {
		portValue = v.toString();
		textField.setText(StringUtils.abbreviate(v.toString(), 30));
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == textField) {
			// setPortValue(textField.getText());
		} else if (e.getSource() == externalWindowButton) {
			NodeBoxDocument doc = NodeBoxDocument.getCurrentDocument();
			if (doc == null)
				throw new RuntimeException("No current active document.");

			JidePopup popup = new JidePopup();
			popup.getContentPane().setLayout(new BorderLayout());
			final JTextArea text = new JTextArea(10, 40);
			text.setEditable(true);
			text.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent arg0) {
					portValue = text.getText();
					setPortValue(portValue);
				}
			});
			text.setText(portValue);
			popup.getContentPane().add(new JScrollPane(text),
					BorderLayout.CENTER);
			popup.setDefaultFocusComponent(text);
			popup.showPopup(this);
		}
	}

}
