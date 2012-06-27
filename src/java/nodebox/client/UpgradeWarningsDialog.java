package nodebox.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nodebox.node.UpgradeResult;
import nodebox.ui.Theme;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class UpgradeWarningsDialog extends JDialog {

    public UpgradeWarningsDialog(UpgradeResult upgradeResult) {
        UpgradeResult upgradeResult1 = upgradeResult;
        List<Map<String, String>> warnings = new ArrayList<Map<String, String>>();
        for (String warning : upgradeResult.getWarnings()) {
            warnings.add(ImmutableMap.of("Description", warning));
        }

        JTable table = new JTable();
        table.setModel(new ListOfMapsModel(warnings));
        JScrollPane tableScroll = new JScrollPane(table);

        tableScroll.setBorder(new Theme.TopBottomBorder(Theme.BORDER_COLOR, Theme.BORDER_COLOR));

        JLabel warningLabel = new JLabel("Some warnings occurred while upgrading your document:");
        warningLabel.setFont(Theme.SMALL_BOLD_FONT);
        JPanel warningPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        warningPanel.add(warningLabel);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dispose();
            }
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttonPanel.add(closeButton);

        JPanel dialogPanel = new JPanel(new BorderLayout());
        dialogPanel.add(warningPanel, BorderLayout.NORTH);
        dialogPanel.add(tableScroll, BorderLayout.CENTER);
        dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(dialogPanel);
        setSize(450, 280);
    }

    private class ListOfMapsModel extends AbstractTableModel {

        private List<Map<String, String>> data;
        private List<String> keys = ImmutableList.of();


        private ListOfMapsModel(List<Map<String, String>> data) {
            setData(data);
        }

        public void setData(List<Map<String, String>> data) {
            checkNotNull(data);
            this.data = data;
            if (data.isEmpty()) {
                keys = ImmutableList.of();
            } else {
                // The ordering of the key is random.
                keys = ImmutableList.copyOf(data.get(0).keySet());
            }
            fireTableDataChanged();
        }

        public int getRowCount() {
            return this.data.size();
        }

        public int getColumnCount() {
            return keys.size();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return keys.get(columnIndex);
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            checkArgument(rowIndex < data.size(), "The row index %s is larger than the number of values.", rowIndex);
            checkArgument(columnIndex < keys.size() + 1, "The column index %s is larger than the number of columns.", columnIndex);
            Map<String, String> row = data.get(rowIndex);
            String key = keys.get(columnIndex);
            return row.get(key);
        }
    }

}
