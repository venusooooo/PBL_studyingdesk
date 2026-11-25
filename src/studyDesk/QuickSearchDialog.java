package studyDesk;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Ctrl+K 快速搜索：课程、课时、任务、错题、笔记、计划一起搜。
 * 只负责"找到并跳转"，不改变各模块自己的筛选状态。
 */
final class QuickSearchDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /** 一条搜索结果。 */
    private static final class Hit {
        final String label;
        final String type;
        final String hint;
        final PageId page;

        Hit(String label, String type, String hint, PageId page) {
            this.label = label;
            this.type = type;
            this.hint = hint;
            this.page = page;
        }

        @Override
        public String toString() {
            return "【" + type + "】" + label + (hint.isEmpty() ? "" : "   " + hint);
        }
    }

    private final JTextField field = new JTextField();
    private final DefaultListModel<Hit> model = new DefaultListModel<>();
    private final JList<Hit> list = new JList<>(model);
    private final MainFrame frame;

    QuickSearchDialog(MainFrame frame) {
        super(frame, "快速搜索", true);
        this.frame = frame;
        setUndecorated(true);
        setLayout(new BorderLayout(0, 6));

        field.setFont(new Font("Microsoft YaHei", Font.PLAIN, 15));
        field.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        field.putClientProperty("JTextField.placeholderText", "搜索课程 / 任务 / 错题 / 笔记…");

        list.setFont(AppTheme.FONT_BODY);
        list.setFixedCellHeight(30);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER));
        scroll.setPreferredSize(new Dimension(520, 320));

        JLabel tip = new JLabel("  ↑↓ 选择 · Enter 打开 · Esc 关闭");
        tip.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        tip.setForeground(AppTheme.TEXT_SECONDARY);
        tip.setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 12));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(AppTheme.SURFACE);
        top.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1));
        top.add(field, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(tip, BorderLayout.SOUTH);
        getContentPane().setBackground(AppTheme.SURFACE);

        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    move(1);
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    move(-1);
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    openSelected();
                } else {
                    search(field.getText());
                }
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelected();
                }
            }
        });

        search("");
        pack();
        setLocationRelativeTo(frame);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            field.requestFocusInWindow();
        }
    }

    private void move(int delta) {
        if (model.isEmpty()) {
            return;
        }
        int index = Math.max(0, Math.min(model.size() - 1, list.getSelectedIndex() + delta));
        list.setSelectedIndex(index);
        list.ensureIndexIsVisible(index);
    }

    private void openSelected() {
        Hit hit = list.getSelectedValue();
        if (hit == null && !model.isEmpty()) {
            hit = model.get(0);
        }
        if (hit == null) {
            return;
        }
        dispose();
        frame.navigateTo(hit.page, new java.util.HashMap<>());
        frame.setStatus("已打开 " + hit.page.getLabel() + " · " + hit.label);
    }

    private void search(String keyword) {
        model.clear();
        String key = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Hit> hits = collect();
        for (Hit hit : hits) {
            if (key.isEmpty()
                    || hit.label.toLowerCase().contains(key)
                    || hit.hint.toLowerCase().contains(key)
                    || hit.type.toLowerCase().contains(key)) {
                model.addElement(hit);
                if (model.size() >= 40) {
                    break;
                }
            }
        }
        if (!model.isEmpty()) {
            list.setSelectedIndex(0);
        }
    }

    private List<Hit> collect() {
        List<Hit> hits = new ArrayList<>();
        Store store = Store.get();
        for (Course c : store.courses()) {
            hits.add(new Hit(c.name, "课程",
                    c.teacher == null || c.teacher.isBlank() ? "" : "· " + c.teacher, PageId.COURSES));
        }
        for (Lesson l : store.lessons()) {
            hits.add(new Hit(store.courseName(l.courseId), "课时",
                    "· " + Dates.weekdayLabel(l.weekday) + " 第 " + l.startSlot + "–" + l.endSlot
                            + " 节 · " + l.weeksLabel(), PageId.SCHEDULE));
        }
        for (Task t : store.tasks()) {
            hits.add(new Hit(t.title, "任务",
                    (t.done ? "· 已完成" : "· " + t.priorityLabel())
                            + (t.due == null || t.due.isBlank() ? "" : " · " + t.due), PageId.PLANS));
        }
        for (Exercise e : store.exercises()) {
            hits.add(new Hit(e.title, "错题",
                    "· " + store.courseName(e.courseId) + " · " + e.masteryLabel(), PageId.EXERCISES));
        }
        for (Note n : store.notes()) {
            hits.add(new Hit(n.title, "笔记",
                    n.tags == null || n.tags.isBlank() ? "" : "· " + n.tags, PageId.NOTES));
        }
        for (Plan p : store.plans()) {
            hits.add(new Hit(p.title, "计划",
                    "· 进度 " + store.planProgress(p) + "%", PageId.PLANS));
        }
        return hits;
    }
}
