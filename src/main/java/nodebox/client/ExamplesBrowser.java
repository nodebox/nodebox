package nodebox.client;

import nodebox.node.NodeLibrary;
import nodebox.ui.Platform;
import nodebox.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static nodebox.ui.SwingUtils.drawShadowText;

public class ExamplesBrowser extends JFrame {

    private static final Image DEFAULT_EXAMPLE_IMAGE;
    private static final File examplesDir;
    private static final Pattern NUMBERS_PREFIX_PATTERN = Pattern.compile("^[0-9]+\\s");

    static {
        final File localDir = new File("examples");
        if (localDir.isDirectory()) {
            examplesDir = localDir;
        } else {
            examplesDir = nodebox.util.FileUtils.getApplicationFile("examples");
        }
        try {
            DEFAULT_EXAMPLE_IMAGE = ImageIO.read(ExamplesBrowser.class.getResourceAsStream("/default-example.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final JPanel categoriesPanel;
    private final JPanel subCategoriesPanel;
    private final JPanel examplesPanel;

    public ExamplesBrowser() {
        super("Examples");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);


        categoriesPanel = new CategoriesPanel();
        categoriesPanel.setBackground(Color.WHITE);

        subCategoriesPanel = new SubCategoriesPanel();

        examplesPanel = new JPanel(new ExampleLayout(10, 10));
        examplesPanel.setBackground(new Color(196, 196, 196));
        JScrollPane examplesScroll = new JScrollPane(examplesPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        examplesScroll.setBorder(null);
        examplesScroll.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateExamplesPanelSize();
            }
        });

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(categoriesPanel, BorderLayout.NORTH);
        mainPanel.add(subCategoriesPanel, BorderLayout.WEST);
        mainPanel.add(examplesScroll, BorderLayout.CENTER);

        setContentPane(mainPanel);
        if (Platform.onMac()) {
            setJMenuBar(new NodeBoxMenuBar());
        }

        mainPanel.getActionMap().put("Reload", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                reload();
            }
        });

        mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Platform.getKeyStroke(KeyEvent.VK_R), "Reload");

        reload();
    }

    /**
     * Refresh the examples browser by loading everything from disk.
     */
    private void reload() {
        final List<Category> categories = parseCategories(examplesDir);

        categoriesPanel.removeAll();
        for (final Category category : categories) {
            final CategoryButton b = new CategoryButton(category);
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    for (Component c : categoriesPanel.getComponents()) {
                        CategoryButton b = (CategoryButton) c;
                        b.setSelected(false);
                    }
                    b.setSelected(true);
                    selectCategory(category);
                }
            });
            categoriesPanel.add(b);
        }
        categoriesPanel.validate();
        categoriesPanel.repaint();
        ((CategoryButton) categoriesPanel.getComponent(0)).setSelected(true);
        selectCategory(categories.get(0));
    }


    private void selectCategory(Category category) {
        subCategoriesPanel.removeAll();
        final List<SubCategory> subCategories = category.subCategories;
        for (final SubCategory subCategory : subCategories) {
            final SubCategoryButton b = new SubCategoryButton(subCategory);
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    for (Component c : subCategoriesPanel.getComponents()) {
                        SubCategoryButton b = (SubCategoryButton) c;
                        b.setSelected(false);
                    }
                    b.setSelected(true);
                    selectSubCategory(subCategory);
                }
            });
            subCategoriesPanel.add(b);
        }
        subCategoriesPanel.validate();
        subCategoriesPanel.repaint();
        ((SubCategoryButton) subCategoriesPanel.getComponent(0)).setSelected(true);
        selectSubCategory(subCategories.get(0));
    }

    private void selectSubCategory(SubCategory subCategory) {
        List<Example> examples = subCategory.examples;
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
        updateExamplesPanelSize();
        examplesPanel.validate();
        examplesPanel.repaint();
    }

    private void updateExamplesPanelSize() {
        Component scrollPane = examplesPanel.getParent();
        int scrollPaneWidth = scrollPane.getWidth();
        ExampleLayout exampleLayout = (ExampleLayout) examplesPanel.getLayout();
        int contentsHeight = exampleLayout.calculateHeight(examplesPanel, scrollPaneWidth);
        examplesPanel.setSize(scrollPaneWidth, contentsHeight);
    }

    private void openExample(Example example) {
        Application.getInstance().openExample(example.file);
    }

    public static List<Category> parseCategories(File parentDirectory) {
        File[] directories = parentDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && !file.isHidden();
            }
        });

        ArrayList<Category> categories = new ArrayList<Category>();
        for (File d : directories) {
            String name = fileToTitle(d);
            List<SubCategory> subCategories = parseSubCategories(d);
            categories.add(new Category(name, d, subCategories));
        }
        return categories;
    }

    private static List<SubCategory> parseSubCategories(File directory) {
        File[] directories = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && !file.isHidden();
            }
        });

        ArrayList<SubCategory> subCategories = new ArrayList<SubCategory>();
        for (File d : directories) {
            String name = fileToTitle(d);
            List<Example> examples = parseExamples(d);
            subCategories.add(new SubCategory(name, d, examples));
        }
        return subCategories;
    }

    public static List<Example> parseExamples(File directory) {
        File[] directories = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File projectDirectory) {
                return projectDirectory.isDirectory() && nodeBoxFileForDirectory(projectDirectory).exists();
            }
        });

        ArrayList<Example> examples = new ArrayList<Example>();
        for (File projectDirectory : directories) {
            File nodeBoxFile = nodeBoxFileForDirectory(projectDirectory);
            Map<String, String> propertyMap = NodeLibrary.parseHeader(nodeBoxFile);
            examples.add(Example.fromNodeLibrary(nodeBoxFile, propertyMap));
        }
        return examples;
    }

    public static String fileToTitle(File file) {
        String baseName = FileUtils.getBaseName(file.getName());
        return NUMBERS_PREFIX_PATTERN.matcher(baseName).replaceFirst("");
    }

    public static File nodeBoxFileForDirectory(File projectDirectory) {
        return new File(projectDirectory, projectDirectory.getName() + ".ndbx");
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

    public static final class Category {
        public final String name;
        public final File directory;
        public final List<SubCategory> subCategories;

        public Category(String name, File directory, List<SubCategory> subCategories) {
            this.name = name;
            this.directory = directory;
            this.subCategories = subCategories;
        }
    }

    public static final class SubCategory {
        public final String name;
        public final File directory;
        public final List<Example> examples;

        public SubCategory(String name, File directory, List<Example> examples) {
            this.name = name;
            this.directory = directory;
            this.examples = examples;
        }
    }

    public static class Example {

        public final File file;
        public final String title;
        public final String description;
        public final Image thumbnail;

        public static Example fromNodeLibrary(File nodeBoxFile, Map<String, String> propertyMap) {
            String title = getProperty(propertyMap, "title", fileToTitle(nodeBoxFile));
            String description = getProperty(propertyMap, "description", "");
            Image thumbnail = thumbnailForLibraryFile(nodeBoxFile);
            return new Example(nodeBoxFile, title, description, thumbnail);
        }

        public Example(File file, String title, String description, Image thumbnail) {
            this.file = file;
            this.title = title;
            this.description = description;
            this.thumbnail = thumbnail;
        }
    }

    private static class CategoriesPanel extends JPanel {
        private CategoriesPanel() {
            super(new FlowLayout(FlowLayout.LEADING, 0, 0));
            setSize(300, 32);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(new Color(210, 210, 210));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(225, 225, 225));
            g.drawLine(0, 0, getWidth(), 0);
            g.setColor(new Color(136, 136, 136));
            g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
        }
    }

    private static void drawVLine(Graphics g, int x, int y, int height) {
        g.drawLine(x, y, x, y + height);
    }

    private static void drawHLine(Graphics g, int x, int y, int width) {
        g.drawLine(x, y, x + width, y);
    }

    private static class SubCategoriesPanel extends JPanel {
        private SubCategoriesPanel() {
            super();
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setSize(300, 32);
            setMinimumSize(new Dimension(150, 32));
            setMaximumSize(new Dimension(150, 1000));
            setPreferredSize(new Dimension(150, 500));
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(new Color(153, 153, 153));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(146, 146, 146));
            drawVLine(g, getWidth() - 3, 0, getHeight());
            g.setColor(new Color(133, 133, 133));
            drawVLine(g, getWidth() - 2, 0, getHeight());
            g.setColor(new Color(112, 112, 112));
            drawVLine(g, getWidth() - 1, 0, getHeight());
        }
    }

    private static class CategoryButton extends JToggleButton {

        private CategoryButton(Category category) {
            super(category.name);
            forceSize(this, 120, 32);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {

            int hMargin = 4;
            int vMargin = 3;
            Rectangle r = new Rectangle(0, 0, getWidth() - 1, getHeight() - 1);
            r.grow(-hMargin, -vMargin);

            if (isSelected()) {
                g.setColor(new Color(198, 198, 198));
                g.fillRect(r.x, r.y, r.width, r.height);
                g.setColor(new Color(166, 166, 166));
                g.drawRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2);
                g.setColor(new Color(119, 119, 119));
                g.drawLine(r.x, r.y, r.x + r.width, r.y);
                g.drawLine(r.x, r.y, r.x, r.y + r.height);
                g.setColor(new Color(237, 237, 237));
                g.drawLine(r.x, r.y + r.height, r.x + r.width, r.y + r.height);
                g.drawLine(r.x + r.width, r.y, r.x + r.width, r.y + r.height);
            } else {
                g.setColor(new Color(179, 179, 179));
                g.drawLine(0, 2, 0, getHeight() - 4);
                g.setColor(new Color(237, 237, 237));
                g.drawLine(1, 2, 1, getHeight() - 4);
            }

            g.setFont(Theme.SMALL_BOLD_FONT);
            g.setFont(Theme.SMALL_BOLD_FONT);
            if (isSelected()) {
                g.setColor(Theme.TEXT_NORMAL_COLOR);
            } else {
                g.setColor(Theme.TEXT_HEADER_COLOR);
                //g.setColor(new Color(160, 160, 160));
            }
            drawShadowText((Graphics2D) g, getText(), 10, 18);

            //g2.setColor(Color.GREEN);
            //g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }

    }

    private class SubCategoryButton extends JToggleButton {
        public SubCategoryButton(SubCategory subCategory) {
            super(subCategory.name);
            forceSize(this, 150, 32);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (isSelected()) {
                g.setColor(new Color(196, 196, 196));
                g.fillRect(0, 0, getWidth(), getHeight());
            } else if (isLastButton()) {
                g.setColor(new Color(255, 255, 255, 50));
                drawHLine(g, 0, 0, getWidth() - 2);
                g.setColor(new Color(0, 0, 0, 50));
                drawHLine(g, 0, getHeight() - 2, getWidth() - 2);
                g.setColor(new Color(255, 255, 255, 50));
                drawHLine(g, 0, getHeight() - 1, getWidth() - 2);
            } else {
                g.setColor(new Color(255, 255, 255, 50));
                drawHLine(g, 0, 0, getWidth() - 1);
                g.setColor(new Color(0, 0, 0, 50));
                drawHLine(g, 0, getHeight() - 1, getWidth() - 1);
            }

            g.setFont(Theme.SMALL_BOLD_FONT);
            g.setColor(Theme.TEXT_NORMAL_COLOR);
            if (isSelected()) {
                drawShadowText((Graphics2D) g, getText(), 5, 20);
            } else {
                drawShadowText((Graphics2D) g, getText(), 5, 20, Theme.DEFAULT_SHADOW_COLOR, 1);

            }
        }

        private boolean isLastButton() {
            return getParent().getComponent(getParent().getComponentCount() - 1) == SubCategoryButton.this;
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
            getIcon().paintIcon(this, g, 0, 0);
            g.setColor(new Color(140, 140, 140));
            drawHLine(g, 0, 0, 149);
            drawVLine(g, 0, 0, 100);
            g.setColor(new Color(237, 237, 237));
            drawHLine(g, 0, 100, 149);
            drawVLine(g, 149, 0, 100);
            g.setColor(new Color(166, 166, 166, 100));
            g.drawRect(1, 1, 147, 98);

            g.setFont(Theme.SMALL_BOLD_FONT);
            g.setColor(Theme.TEXT_NORMAL_COLOR);
            drawShadowText((Graphics2D) g, getText(), 0, 113);
        }
    }

    private static class ExampleLayout implements LayoutManager {

        private int hGap, vGap;

        private ExampleLayout(int hGap, int vGap) {
            this.hGap = hGap;
            this.vGap = vGap;
        }

        @Override
        public void addLayoutComponent(String s, Component component) {

        }

        @Override
        public void removeLayoutComponent(Component component) {
        }

        @Override
        public Dimension preferredLayoutSize(Container container) {
            return container.getSize();
        }

        @Override
        public Dimension minimumLayoutSize(Container container) {
            return container.getSize();
        }

        /**
         * Given a fixed width, what should be the height of this container?
         *
         * @param containerWidth The width of the parent component.
         * @return The height in pixels.
         */
        public int calculateHeight(Container container, int containerWidth) {
            int y = vGap;
            int x = hGap;
            int maxHeightForRow = 0;
            for (Component c : container.getComponents()) {
                int width = c.getWidth();
                int height = c.getHeight();
                maxHeightForRow = Math.max(maxHeightForRow, height);
                if (x > containerWidth - width + hGap * 2) {
                    x = hGap;
                    y += maxHeightForRow + vGap;
                    maxHeightForRow = 0;
                }
                x += width + hGap;
            }
            return y + 125 + vGap;
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

    private static void forceSize(Component c, int width, int height) {
        Dimension d = new Dimension(width, height);
        c.setSize(d);
        c.setMinimumSize(d);
        c.setMaximumSize(d);
        c.setPreferredSize(d);
    }

    public static void main(String[] args) {
        ExamplesBrowser browser = new ExamplesBrowser();
        browser.setVisible(true);
        browser.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}
