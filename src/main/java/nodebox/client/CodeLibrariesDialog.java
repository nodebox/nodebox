package nodebox.client;

import com.google.common.collect.ImmutableMap;
import nodebox.function.CoreFunctions;
import nodebox.function.Function;
import nodebox.function.FunctionLibrary;
import nodebox.function.FunctionRepository;
import nodebox.ui.ActionHeader;
import nodebox.ui.InsetLabel;
import nodebox.ui.MessageBar;
import nodebox.ui.Theme;
import nodebox.util.FileUtils;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class CodeLibrariesDialog extends JDialog {

    private static final ImageIcon clojureIcon = new ImageIcon(CodeLibrariesDialog.class.getResource("/functions-clojure.png"));
    private static final ImageIcon pythonIcon = new ImageIcon(CodeLibrariesDialog.class.getResource("/functions-python.png"));
    private static final ImageIcon minusIcon = new ImageIcon(CodeLibrariesDialog.class.getResource("/action-minus.png"));
    private static final ImageIcon plusIcon = new ImageIcon(CodeLibrariesDialog.class.getResource("/action-plus-arrow.png"));
    private static Map<String, ImageIcon> ICON_LANGUAGE_MAP = ImmutableMap.of(
            "python", pythonIcon,
            "clojure", clojureIcon);

    private class FunctionLibraryListModel implements ListModel<FunctionLibrary> {
        private java.util.List<FunctionLibrary> functionLibraries;

        private FunctionLibraryListModel() {
            updateFunctionLibraries();
        }

        public void updateFunctionLibraries() {
            functionLibraries = new ArrayList<>();
            functionLibraries.addAll(functionRepository.getLibraries());
            functionLibraries.remove(CoreFunctions.LIBRARY);
        }

        public int getSize() {
            return functionLibraries.size();
        }

        public FunctionLibrary getElementAt(int index) {
            return functionLibraries.get(index);
        }

        public void addListDataListener(ListDataListener l) {
        }

        public void removeListDataListener(ListDataListener l) {
        }
    }

    private class FunctionLibraryRenderer extends JLabel implements ListCellRenderer<FunctionLibrary> {

        private FunctionLibraryRenderer() {
            setEnabled(true);
            setBorder(BorderFactory.createEmptyBorder(8, 10, 5, 10));
            setFont(Theme.MESSAGE_FONT);
            setForeground(Theme.TEXT_NORMAL_COLOR);
            setOpaque(true);
            setPreferredSize(new Dimension(300, 40));
        }

        public Component getListCellRendererComponent(JList<? extends FunctionLibrary> list, FunctionLibrary library, int index, boolean isSelected, boolean cellHasFocus) {
            setText(library.getSimpleIdentifier());
            setIcon(ICON_LANGUAGE_MAP.get(library.getLanguage()));
            if (isSelected) {
                setBackground(Theme.NODE_SELECTION_ACTIVE_BACKGROUND_COLOR);
            } else {
                setBackground(Theme.NODE_SELECTION_BACKGROUND_COLOR);
            }
            return this;
        }
    }

    private final NodeBoxDocument document;
    private FunctionRepository functionRepository;
    private JList<FunctionLibrary> functionLibraryList;
    private FunctionLibraryListModel functionLibraryListModel;
    private boolean repositoryChanged = false;

    public CodeLibrariesDialog(NodeBoxDocument document, FunctionRepository functionRepository) {
        super(document, "Code Libraries", true);
        this.document = document;
        this.functionRepository = functionRepository;
        functionLibraryListModel = new FunctionLibraryListModel();

        JPanel panel = new JPanel(new BorderLayout());

        if (document.getDocumentFile() == null) {
            panel.add(new MessageBar("<html>&nbsp;&nbsp;&nbsp;<b>Please save your document first.</b></html>"), BorderLayout.NORTH);
        } else {
            ActionHeader actionHeader = new ActionHeader();
            actionHeader.setLayout(new BoxLayout(actionHeader, BoxLayout.LINE_AXIS));
            InsetLabel actionHeaderLabel = new InsetLabel("Code Libraries");
            actionHeader.add(Box.createHorizontalStrut(10));
            actionHeader.add(actionHeaderLabel);
            actionHeader.add(Box.createHorizontalGlue());
            JButton removeLibraryButton = new JButton(minusIcon);
            removeLibraryButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    removeSelectedLibrary();
                }
            });
            removeLibraryButton.setBorder(null);
            final LanguagePopupMenu languagePopup = new LanguagePopupMenu();
            final JButton plusLibraryButton = new JButton(plusIcon);
            plusLibraryButton.setBorder(null);
            plusLibraryButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    languagePopup.show(plusLibraryButton, -20, 21);
                }
            });
            actionHeader.addDivider();
            actionHeader.add(Box.createHorizontalStrut(10));
            actionHeader.add(removeLibraryButton);
            actionHeader.add(Box.createHorizontalStrut(20));
            actionHeader.add(plusLibraryButton);
            actionHeader.add(Box.createHorizontalStrut(10));
            panel.add(actionHeader, BorderLayout.NORTH);
        }


        functionLibraryList = new JList<>(functionLibraryListModel);
        JScrollPane libraryScroll = new JScrollPane(functionLibraryList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        libraryScroll.setBorder(null);
        functionLibraryList.setCellRenderer(new FunctionLibraryRenderer());
        panel.add(libraryScroll, BorderLayout.CENTER);


        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                CodeLibrariesDialog.this.setVisible(false);
            }
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 5));
        buttonPanel.add(closeButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(panel);
        setSize(300, 400);
        setMinimumSize(new Dimension(300, 200));
        setLocationRelativeTo(document);
    }

    private class LanguagePopupMenu extends JPopupMenu {
        private LanguagePopupMenu() {
            add(new AddPythonLibraryAction());
            add(new AddClojureLibraryAction());
        }
    }

    private class AddPythonLibraryAction extends AbstractAction {
        private AddPythonLibraryAction() {
            super("Python");
        }

        public void actionPerformed(ActionEvent actionEvent) {
            File chosenFile = chooseFileWithExtension("py", "Python file");
            if (chosenFile != null && chosenFile.getName().endsWith(".py")) {
                addLibrary("python", chosenFile);
            }
        }
    }

    private class AddClojureLibraryAction extends AbstractAction {
        private AddClojureLibraryAction() {
            super("Clojure");
        }

        public void actionPerformed(ActionEvent actionEvent) {
            File chosenFile = chooseFileWithExtension("clj", "Clojure file");
            if (chosenFile != null && chosenFile.getName().endsWith(".clj")) {
                addLibrary("clojure", chosenFile);
            }
        }

    }

    private void addLibrary(String prefix, File libraryFile) {
        String relativePath = FileUtils.getRelativePath(libraryFile, document.getDocumentFile().getParentFile());
        FunctionLibrary library = FunctionLibrary.load(document.getDocumentFile(), prefix + ":" + relativePath);
        functionRepository = functionRepository.withLibraryAdded(library);
        reloadListModel();
        repositoryChanged = true;

    }

    public FunctionRepository getFunctionRepository() {
        if (repositoryChanged)
            return functionRepository;
        return null;
    }

    private File chooseFileWithExtension(String extension, String extensionDescription) {
        return nodebox.util.FileUtils.showOpenDialog(NodeBoxDocument.getCurrentDocument(), NodeBoxDocument.lastFilePath, extension, extensionDescription);
    }

    private void removeSelectedLibrary() {
        int index = functionLibraryList.getSelectedIndex();
        if (index >= 0) {
            FunctionLibrary library = (FunctionLibrary) functionLibraryListModel.getElementAt(index);
            functionRepository = functionRepository.withLibraryRemoved(library);
            reloadListModel();
            repositoryChanged = true;
        }
    }

    private void reloadListModel() {
        functionLibraryListModel.updateFunctionLibraries();
        functionLibraryList.setModel(functionLibraryListModel);
        functionLibraryList.setSelectedIndex(0);
        functionLibraryList.ensureIndexIsVisible(0);
        functionLibraryList.setCellRenderer(new FunctionLibraryRenderer());
        repaint();
    }
}
