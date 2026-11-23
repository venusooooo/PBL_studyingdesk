package studyDesk;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** 学习笔记：按课程归类、关键词搜索、置顶、增删改。 */
public final class NotesPage extends BasePage {

    private String keyword = "";
    private long courseFilter = -1;
    private JPanel listPanel;

    @Override
    public PageId getId() {
        return PageId.NOTES;
    }

    @Override
    protected void buildContent() {
        Store store = Store.get();
        setHeaderHint("课堂速记、踩坑记录、代码片段都放这里；支持按课程筛选与关键词搜索。");

        JTextField search = Ui.search(14, "搜索标题 / 内容 / 标签");
        search.setText(keyword);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                keyword = search.getText().trim();
                renderList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                keyword = search.getText().trim();
                renderList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                keyword = search.getText().trim();
                renderList();
            }
        });

        List<Course> courses = new ArrayList<>(store.courses());
        String[] items = new String[courses.size() + 1];
        items[0] = "全部课程";
        int idx = 0;
        for (int i = 0; i < courses.size(); i++) {
            items[i + 1] = courses.get(i).name;
            if (courses.get(i).id == courseFilter) {
                idx = i + 1;
            }
        }
        JComboBox<String> courseBox = Ui.combo(items, idx);
        courseBox.addActionListener(e -> {
            int sel = courseBox.getSelectedIndex();
            courseFilter = sel <= 0 ? -1 : courses.get(sel - 1).id;
            renderList();
        });

        JButton add = Ui.primary("+ 写笔记");
        add.addActionListener(e -> openEditor(null));

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.GRID, 0));
        tools.setOpaque(false);
        tools.add(search);
        tools.add(courseBox);
        tools.add(add);

        JPanel toolbar = Ui.card(null, null, null);
        toolbar.add(tools, BorderLayout.CENTER);
        addCard(toolbar, 0, 0);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        addCard(Ui.card("全部笔记", null, listPanel), 1, 0);

        renderList();
    }

    private void renderList() {
        if (listPanel == null) {
            return;
        }
        listPanel.removeAll();
        List<Note> list = filtered();
        if (list.isEmpty()) {
            listPanel.add(Ui.empty("没有笔记。点右上角「写笔记」记录第一条。"));
        } else {
            boolean first = true;
            for (Note n : list) {
                if (!first) {
                    listPanel.add(Ui.divider());
                }
                listPanel.add(row(n));
                first = false;
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private List<Note> filtered() {
        List<Note> out = new ArrayList<>();
        for (Note n : Store.get().notes()) {
            if (courseFilter > 0 && n.courseId != courseFilter) {
                continue;
            }
            if (!keyword.isEmpty()) {
                String hay = (n.title + " " + n.content + " " + n.tags).toLowerCase();
                if (!hay.contains(keyword.toLowerCase())) {
                    continue;
                }
            }
            out.add(n);
        }
        out.sort((a, b) -> {
            if (a.pinned != b.pinned) {
                return a.pinned ? -1 : 1;
            }
            return b.updatedAt.compareTo(a.updatedAt);
        });
        return out;
    }

    private JComponent row(Note n) {
        JPanel row = Ui.row();

        JPanel left = new JPanel(new BorderLayout(0, 3));
        left.setOpaque(false);

        JPanel line1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        line1.setOpaque(false);
        if (n.pinned) {
            line1.add(Ui.chip("置顶", AppTheme.WARN));
        }
        line1.add(Ui.chip(Store.get().courseName(n.courseId), Ui.courseColor(n.courseId)));
        JButton title = Ui.link(n.title);
        title.setFont(AppTheme.FONT_BODY);
        title.setForeground(AppTheme.TEXT);
        title.addActionListener(e -> openEditor(n));
        line1.add(title);

        JLabel preview = Ui.label(n.preview(46), AppTheme.FONT_SMALL, AppTheme.TEXT_SECONDARY);
        preview.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));

        JPanel line2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        line2.setOpaque(false);
        if (n.tags != null && !n.tags.isBlank()) {
            for (String tag : n.tags.split("[,，\\s]+")) {
                if (!tag.isBlank()) {
                    line2.add(Ui.chip("#" + tag, AppTheme.TEXT_SECONDARY));
                }
            }
        }
        line2.add(Ui.label("更新于 " + Dates.friendly(n.updatedAt), AppTheme.FONT_SMALL,
                new Color(0x9A, 0xA1, 0xAE)));

        left.add(line1, BorderLayout.NORTH);
        left.add(preview, BorderLayout.CENTER);
        left.add(line2, BorderLayout.SOUTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        actions.setOpaque(false);
        JButton pin = Ui.link(n.pinned ? "取消置顶" : "置顶");
        pin.addActionListener(e -> {
            n.pinned = !n.pinned;
            Store.get().changed();
            renderList();
        });
        JButton edit = Ui.link("编辑");
        edit.addActionListener(e -> openEditor(n));
        JButton del = Ui.link("删除");
        del.setForeground(AppTheme.DANGER);
        del.addActionListener(e -> {
            if (FormDialog.confirm(getView(), "删除笔记", "确定删除《" + n.title + "》吗？")) {
                Store.get().removeNote(n);
                renderList();
                context().setStatus("已删除笔记");
            }
        });
        actions.add(pin);
        actions.add(edit);
        actions.add(del);

        row.add(left, BorderLayout.CENTER);
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private void openEditor(Note note) {
        boolean creating = note == null;
        FormDialog dialog = new FormDialog(owner(), creating ? "写笔记" : "编辑笔记", "保存");
        JTextField title = dialog.addText("标题", creating ? "" : note.title);
        JComboBox<String> course = dialog.addCourseCombo("所属课程", creating ? 0 : note.courseId);
        JTextField tags = dialog.addText("标签", creating ? "" : note.tags);
        JTextArea content = dialog.addArea("内容", creating ? "" : note.content, 10);

        if (!dialog.open(getView())) {
            return;
        }
        String t = title.getText().trim();
        if (t.isEmpty()) {
            FormDialog.toast(getView(), "标题不能为空");
            return;
        }
        Store store = Store.get();
        if (creating) {
            Note n = new Note(t, content.getText(), FormDialog.courseIdOf(course));
            n.tags = tags.getText().trim().replace("，", ",");
            store.addNote(n);
            context().setStatus("已保存笔记：" + t);
        } else {
            note.title = t;
            note.content = content.getText();
            note.courseId = FormDialog.courseIdOf(course);
            note.tags = tags.getText().trim().replace("，", ",");
            note.updatedAt = Dates.today();
            store.changed();
            context().setStatus("已更新笔记：" + t);
        }
        refresh();
    }
}
