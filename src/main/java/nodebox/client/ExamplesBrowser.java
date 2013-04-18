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

        categoriesPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
        File[] categoryFolders = getExampleCategoryFolders();
        for (final File f : categoryFolders) {
            JButton b = new JButton(f.getName());
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    selectCategory(f);
                }
            });
            categoriesPanel.add(b);
        }

        examplesPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));


        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(categoriesPanel, BorderLayout.NORTH);
        mainPanel.add(examplesPanel, BorderLayout.CENTER);

        setContentPane(mainPanel);
        setJMenuBar(new NodeBoxMenuBar());
        selectCategory(categoryFolders[0]);
    }


    private void selectCategory(File categoryFolder) {
        List<Example> examples = getExamples(categoryFolder);
        examplesPanel.removeAll();
        for (final Example e : examples) {
            JButton b = new JButton(e.name, new ImageIcon(e.thumbnail));
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    openExample(e);
                }
            });
            examplesPanel.add(b);
        }
        examplesPanel.validate();
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
            Map<String,String> propertyMap = NodeLibrary.parseHeader(nodeBoxFile);
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

    private static String getProperty(Map<String,String> propertyMap, String key, String defaultValue) {
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

        public static Example fromNodeLibrary(File nodeBoxFile, Map<String,String> propertyMap) {
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

    public static void main(String[] args) {
        ExamplesBrowser browser = new ExamplesBrowser();
        browser.setVisible(true);
        browser.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}
