package nodebox.client;

import nodebox.node.NodeLibrary;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExamplesBrowser extends JFrame {

    private static final Image DEFAULT_EXAMPLE_IMAGE;
    private static final File examplesFolder = new File("examples");
    private static final Font EXAMPLE_TITLE_FONT = new Font(Font.DIALOG, Font.BOLD, 12);
    private static final Color EXAMPLE_TITLE_COLOR = new Color(60, 60, 200);

    static {
        try {
            DEFAULT_EXAMPLE_IMAGE = ImageIO.read(ExamplesBrowser.class.getResourceAsStream("/default-example.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final JPanel categoriesPanel;
    private final JPanel examplesPanel;

    public ExamplesBrowser() {
        super("Examples");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);

        examplesPanel = new JPanel(new ExampleLayout(10, 10));
        examplesPanel.setBackground(Color.WHITE);

        categoriesPanel = new CategoriesPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        categoriesPanel.setBackground(Color.WHITE);
        File[] categoryFolders = getExampleCategoryFolders();
        final List<CategoryButton> categoryButtons = new ArrayList<CategoryButton>();
        for (final File f : categoryFolders) {
            final CategoryButton b = new CategoryButton(f.getName().substring(3));
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    for (CategoryButton b : categoryButtons) {
                        b.setActive(false);
                    }
                    b.setActive(true);
                    selectCategory(f);
                }
            });
            categoryButtons.add(b);
            categoriesPanel.add(b);
        }

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(categoriesPanel, BorderLayout.NORTH);
        mainPanel.add(examplesPanel, BorderLayout.CENTER);

        setContentPane(mainPanel);
        setJMenuBar(new NodeBoxMenuBar());
        categoryButtons.get(0).setActive(true);
        selectCategory(categoryFolders[0]);
    }


    private void selectCategory(File categoryFolder) {
        List<Example> examples = getExamples(categoryFolder);
        examplesPanel.removeAll();
        for (final Example e : examples) {
            ExampleButton b = new ExampleButton(e.title, new ImageIcon(e.thumbnail));
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    openExample(e);
                }
            });
            examplesPanel.add(b);
        }
        examplesPanel.validate();
        examplesPanel.repaint();
    }

    private void openExample(Example example) {
        Application.getInstance().openDocument(example.file);
    }

    public static File[] getExampleCategoryFolders() {
        return examplesFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && !file.isHidden();
            }
        });
    }

    public static File[] getExampleFiles(File directory) {
        return directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File projectDirectory) {
                if (!projectDirectory.isDirectory()) return false;
                return nodeBoxFileForDirectory(projectDirectory).exists();
            }
        });
    }

    public static File nodeBoxFileForDirectory(File projectDirectory) {
        return new File(projectDirectory, projectDirectory.getName() + ".ndbx");
    }

    public static List<Example> getExamples(File directory) {
        ArrayList<Example> examples = new ArrayList<Example>();
        File[] projectDirectories = getExampleFiles(directory);
        for (File projectDirectory : projectDirectories) {
            File nodeBoxFile = nodeBoxFileForDirectory(projectDirectory);
            Map<String, String> propertyMap = NodeLibrary.parseHeader(nodeBoxFile);
            examples.add(Example.fromNodeLibrary(nodeBoxFile, propertyMap));
        }
        return examples;
    }

    public static Image thumbnailForLibraryFile(File nodeBoxFile) {
        if (nodeBoxFile == null) return DEFAULT_EXAMPLE_IMAGE;
        File projectDirectory = nodeBoxFile.getParentFile();
        String baseName = FileUtils.getBaseName(nodeBoxFile.getName());
        File imageFile = new File(projectDirectory, baseName + ".png");
        if (imageFile.exists()) {
            try {
                return ImageIO.read(imageFile);
            } catch (IOException e) {
                return DEFAULT_EXAMPLE_IMAGE;
            }
        } else {
            return DEFAULT_EXAMPLE_IMAGE;
        }
    }

    private static String getProperty(Map<String, String> propertyMap, String key, String defaultValue) {
        if (propertyMap.containsKey(key)) {
            return propertyMap.get(key);
        } else {
            return defaultValue;
        }
    }

    public static class Example {

        public final File file;
        public final String name;
        public final String title;
        public final String description;
        public final String category;
        public final String subCategory;
        public final Image thumbnail;

        public static Example fromNodeLibrary(File nodeBoxFile, Map<String, String> propertyMap) {
            String name = FileUtils.getBaseName(nodeBoxFile.getName());
            String title = getProperty(propertyMap, "title", name);
            String description = getProperty(propertyMap, "description", "");
            String category = getProperty(propertyMap, "category", "Uncategorized");
            String subCategory = getProperty(propertyMap, "subCategory", "Uncategorized");
            Image thumbnail = thumbnailForLibraryFile(nodeBoxFile);
            return new Example(nodeBoxFile, name, title, description, category, subCategory, thumbnail);
        }

        public Example(File file, String name, String title, String description, String category, String subCategory, Image thumbnail) {
            this.file = file;
            this.name = name;
            this.title = title;
            this.description = description;
            this.category = category;
            this.subCategory = subCategory;
            this.thumbnail = thumbnail;
        }
    }

    private static class CategoriesPanel extends JPanel {
        private CategoriesPanel(LayoutManager layoutManager) {
            super(layoutManager);
            setSize(300, 32);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(new Color(240, 240, 240));
            g.fillRect(0, 0, getWidth(), getHeight() - 1);
            g.setColor(new Color(200, 200, 200));
            g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
        }
    }

    private static class CategoryButton extends JButton {

        private boolean active;

        private CategoryButton(String title) {
            super(title);
            setSize(150, 32);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            active = false;
        }

        private boolean isActive() {
            return active;
        }

        private void setActive(boolean active) {
            this.active = active;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            if (active) {
                g2.setColor(new Color(2, 164, 228));
                g2.fillRect(0, 0, getWidth(), getHeight());
            } else {
                g2.setColor(new Color(255, 255, 255));
                g2.drawLine(0, 0, 0, getHeight()-2);
                g2.setColor(new Color(210, 210, 210));
                g2.drawLine(getWidth()-1, 0, getWidth()-1, getHeight()-2);
            }

            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
            if (active) {
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(new Color(160, 160, 160));
            }
            g2.drawString(getText(), 10, 20);

            //g2.setColor(Color.GREEN);
            //g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }

    }

    private static class ExampleButton extends JButton {
        private ExampleButton(String title, Icon icon) {
            super(title, icon);
            setSize(150, 125);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            getIcon().paintIcon(this, g, 0, 0);
            g2.setFont(EXAMPLE_TITLE_FONT);
            g2.setColor(EXAMPLE_TITLE_COLOR);
            g2.drawString(getText(), 1, 117);
        }
    }

    private static class ExampleLayout implements LayoutManager {

        private ArrayList<Component> components = new ArrayList<Component>();

        private int hGap, vGap;

        private ExampleLayout(int hGap, int vGap) {
            this.hGap = hGap;
            this.vGap = vGap;
        }

        @Override
        public void addLayoutComponent(String s, Component component) {
            components.add(component);

        }

        @Override
        public void removeLayoutComponent(Component component) {
            components.remove(component);
        }

        @Override
        public Dimension preferredLayoutSize(Container container) {
            return container.getSize();
        }

        @Override
        public Dimension minimumLayoutSize(Container container) {
            return container.getSize();
        }

        @Override
        public void layoutContainer(Container container) {
            int y = vGap;
            int x = hGap;
            int maxHeightForRow = 0;
            for (Component c : container.getComponents()) {
                int width = c.getWidth();
                int height = c.getHeight();
                maxHeightForRow = Math.max(maxHeightForRow, height);
                if (x > container.getWidth() - width) {
                    x = hGap;
                    y += maxHeightForRow + vGap;
                    maxHeightForRow = 0;
                }
                c.setBounds(x, y, width, height);
                x += width + hGap;
            }
        }
    }


    public static void main(String[] args) {
        ExamplesBrowser browser = new ExamplesBrowser();
        browser.setVisible(true);
        browser.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}
