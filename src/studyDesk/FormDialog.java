package studyDesk;

import java.awt.*;
import javax.swing.*;

/**
 * 表单弹窗：按行堆积"标签 + 控件"，底部统一"取消 / 保存"。
 * 各个模块的新增、编辑弹窗都复用它，保证交互一致、代码不重复。
 */
public class FormDialog extends LightDialog {

    private static final long serialVersionUID = 1L;

    private final JPanel form = new JPanel(new GridBagLayout());
    private final JPanel body = new JPanel(new BorderLayout());
    private int row;
    private boolean confirmed;

    public FormDialog(Frame owner, String title, String okText) {
        super(owner, title, true);
        form.setOpaque(false);
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 4, 24));
        body.add(form, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        JButton cancel = Ui.ghost("取消");
        cancel.addActionListener(e -> dispose());
        JButton ok = Ui.primary(okText);
        ok.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.GRID, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(8, 24, 16, 24));
        bar.add(cancel);
        bar.add(ok);
        add(bar, BorderLayout.SOUTH);
    }

    public JTextField addText(String label, String value) {
        JTextField f = Ui.field(26);
        f.setText(value == null ? "" : value);
        addRow(label, f);
        return f;
    }

    public JTextArea addArea(String label, String value, int rows, boolean mono) {
        JTextArea area = new JTextArea(value == null ? "" : value, rows, 26);
        area.setFont(mono ? new Font("Consolas", Font.PLAIN, 13) : AppTheme.FONT_BODY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1));
        sp.setPreferredSize(new Dimension(320, rows * 22 + 12));
        addRow(label, sp);
        return area;
    }

    public JTextArea addArea(String label, String value, int rows) {
        return addArea(label, value, rows, false);
    }

    /** 课程下拉框：第一项是"未归类"，其余按课程顺序。 */
    public JComboBox<String> addCourseCombo(String label, long selectedCourseId) {
        java.util.List<Course> courses = new java.util.ArrayList<>(Store.get().courses());
        String[] items = new String[courses.size() + 1];
        items[0] = "未归类";
        int selected = 0;
        for (int i = 0; i < courses.size(); i++) {
            items[i + 1] = courses.get(i).name;
            if (courses.get(i).id == selectedCourseId) {
                selected = i + 1;
            }
        }
        JComboBox<String> box = Ui.combo(items, selected);
        addRow(label, box);
        return box;
    }

    /** 把下拉框选择换算回课程 id。 */
    public static long courseIdOf(JComboBox<String> box) {
        int idx = box.getSelectedIndex();
        if (idx <= 0) {
            return 0;
        }
        java.util.List<Course> courses = new java.util.ArrayList<>(Store.get().courses());
        if (idx - 1 < courses.size()) {
            return courses.get(idx - 1).id;
        }
        return 0;
    }

    public JComboBox<String> addCombo(String label, String[] items, int selected) {
        JComboBox<String> box = Ui.combo(items, selected);
        addRow(label, box);
        return box;
    }

    public JSpinner addSpinner(String label, int value, int min, int max) {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
        sp.setFont(AppTheme.FONT_BODY);
        sp.setPreferredSize(new Dimension(88, 26));
        JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        holder.setOpaque(false);
        holder.add(sp);
        addRow(label, holder);
        return sp;
    }

    public JCheckBox addCheck(String label, boolean selected) {
        JCheckBox box = new JCheckBox(label, selected);
        box.setOpaque(false);
        box.setFont(AppTheme.FONT_BODY);
        addRow("", box);
        return box;
    }

    public void addRow(String label, JComponent comp) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row++;
        c.insets = new Insets(0, 0, AppTheme.GRID * 1, AppTheme.GRID);
        c.anchor = GridBagConstraints.NORTHWEST;

        JLabel l = new JLabel(label);
        l.setFont(AppTheme.FONT_SMALL);
        l.setForeground(AppTheme.TEXT_SECONDARY);
        l.setPreferredSize(new Dimension(68, 26));
        c.gridx = 0;
        form.add(l, c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, AppTheme.GRID * 1, 0);
        form.add(comp, c);
    }

    /** 显示并返回是否点击了保存。 */
    public boolean open(Component parent) {
        pack();
        setMinimumSize(new Dimension(420, getHeight()));
        setLocationRelativeTo(parent);
        setVisible(true);
        return confirmed;
    }

    // ========================= 常用小工具 =========================

    /** 单选确认框，返回 true 表示用户点了"确定"。 */
    public static boolean confirm(Component parent, String title, String message) {
        Object[] options = {"确定", "取消"};
        int r = JOptionPane.showOptionDialog(parent, message, title,
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                options, options[1]);
        return r == 0;
    }

    public static void toast(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }
}
