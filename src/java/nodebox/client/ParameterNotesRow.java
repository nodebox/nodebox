package nodebox.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class ParameterNotesRow extends JComponent {

    private JLabel notesLabel;

    private static final int TOP_PADDING = 2;
    private static final int BOTTOM_PADDING = 2;

    private static Image notesBackgroundImage;

    static {
        try {
            notesBackgroundImage = ImageIO.read(new File("res/notes-background.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ParameterNotesRow(String notes) {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        notesLabel = new JLabel(notes);
        notesLabel.setFont(Theme.SMALL_FONT);
        notesLabel.setForeground(Theme.TEXT_NORMAL_COLOR);
        notesLabel.setBorder(BorderFactory.createEmptyBorder(TOP_PADDING, 0, BOTTOM_PADDING, 0));

        add(Box.createHorizontalStrut(ParameterView.LABEL_WIDTH + 10));
        add(this.notesLabel);
        add(Box.createHorizontalGlue());

        setBorder(Theme.PARAMETER_NOTES_BORDER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(notesBackgroundImage, ParameterView.LABEL_WIDTH, 0, getWidth(), 20, null);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 21);
    }
}
