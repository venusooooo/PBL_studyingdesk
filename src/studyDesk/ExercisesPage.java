package studyDesk;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** 习题与错题：录入、筛选、按艾宾浩斯间隔复习。 */
public final class ExercisesPage extends BasePage {

    private String keyword = "";
    private long courseFilter = -1;
    private int statusFilter = 0;
    private JPanel listPanel;

    @Override
    public PageId getId() {
        return PageId.EXERCISES;
    }

    @Override
    protected void onShowPage(java.util.Map<String, Object> params) {
        if (Boolean.TRUE.equals(params.get("dueOnly")) && statusFilter != 4) {
            statusFilter = 4;
            // 进入时已经按旧筛选画过一次，这里重画一遍让筛选框与列表同步
            refresh();
        }
    }

    @Override
    protected void onNewAction() {
        openEditor(null);
    }

    @Override
    protected void buildContent() {
        Store store = Store.get();
        setHeaderHint("答错的题会进入复习队列，按 1 / 2 / 4 / 7 / 15 天的间隔自动安排下次复习。");

        // ---------- 工具栏 ----------
        JTextField search = Ui.search(14, "搜索题目 / 错因");
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
        String[] courseItems = new String[courses.size() + 1];
        courseItems[0] = "全部课程";
        int courseIdx = 0;
        for (int i = 0; i < courses.size(); i++) {
            courseItems[i + 1] = courses.get(i).name;
            if (courses.get(i).id == courseFilter) {
                courseIdx = i + 1;
            }
        }
        JComboBox<String> courseBox = Ui.combo(courseItems, courseIdx);
        courseBox.addActionListener(e -> {
            int idx = courseBox.getSelectedIndex();
            courseFilter = idx <= 0 ? -1 : courses.get(idx - 1).id;
            renderList();
        });

        String[] statusItems = {"全部状态", "未掌握", "复习中", "已掌握", "今日待复习"};
        JComboBox<String> statusBox = Ui.combo(statusItems, statusFilter);
        statusBox.addActionListener(e -> {
            statusFilter = statusBox.getSelectedIndex();
            renderList();
        });

        JButton add = Ui.primary("+ 录入错题");
        add.addActionListener(e -> openEditor(null));

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.GRID, 0));
        tools.setOpaque(false);
        tools.add(search);
        tools.add(courseBox);
        tools.add(statusBox);
        tools.add(add);

        List<Exercise> due = store.dueExercises();
        JLabel dueInfo = Ui.label(due.isEmpty() ? "今天没有待复习的题" : "今天有 " + due.size() + " 道题待复习",
                AppTheme.FONT_SMALL, due.isEmpty() ? AppTheme.SUCCESS : AppTheme.DANGER);
        JButton startReview = Ui.ghost("开始复习");
        startReview.setEnabled(!due.isEmpty());
        startReview.addActionListener(e -> startReviewFlow());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.GRID, 0));
        right.setOpaque(false);
        right.add(dueInfo);
        right.add(startReview);

        JPanel toolbar = Ui.card(null, null, null);
        JPanel toolbarInner = new JPanel(new BorderLayout());
        toolbarInner.setOpaque(false);
        toolbarInner.add(tools, BorderLayout.WEST);
        toolbarInner.add(right, BorderLayout.EAST);
        toolbar.add(toolbarInner, BorderLayout.CENTER);
        addCard(toolbar, 0, 0);

        // ---------- 列表 ----------
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        JPanel listCard = Ui.card("错题本", null, listPanel);
        addCard(listCard, 1, 0);

        renderList();
    }

    private void renderList() {
        if (listPanel == null) {
            return;
        }
        listPanel.removeAll();
        List<Exercise> list = filtered();

        if (list.isEmpty()) {
            listPanel.add(Ui.empty("没有符合条件的题目。换个筛选条件，或者录入一道新错题。"));
        } else {
            boolean first = true;
            for (Exercise e : list) {
                if (!first) {
                    listPanel.add(Ui.divider());
                }
                listPanel.add(row(e));
                first = false;
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private List<Exercise> filtered() {
        Store store = Store.get();
        List<Exercise> out = new ArrayList<>();
        for (Exercise e : store.exercises()) {
            if (courseFilter > 0 && e.courseId != courseFilter) {
                continue;
            }
            if (statusFilter == 4 && !e.dueToday()) {
                continue;
            }
            if (statusFilter > 0 && statusFilter < 4) {
                int want = statusFilter - 1;
                if (e.mastery != want) {
                    continue;
                }
            }
            if (!keyword.isEmpty()) {
                String hay = (e.title + " " + e.content + " " + e.reason).toLowerCase();
                if (!hay.contains(keyword.toLowerCase())) {
                    continue;
                }
            }
            out.add(e);
        }
        out.sort((a, b) -> {
            if (a.dueToday() != b.dueToday()) {
                return a.dueToday() ? -1 : 1;
            }
            if (a.mastery != b.mastery) {
                return Integer.compare(a.mastery, b.mastery);
            }
            return Long.compare(b.id, a.id);
        });
        return out;
    }

    private JComponent row(Exercise e) {
        JPanel row = Ui.row();

        JPanel left = new JPanel(new BorderLayout(0, 4));
        left.setOpaque(false);

        JPanel line1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        line1.setOpaque(false);
        Color masteryColor = e.mastery == 0 ? AppTheme.DANGER
                : (e.mastery == 1 ? AppTheme.WARN : AppTheme.SUCCESS);
        line1.add(Ui.chip(e.masteryLabel(), masteryColor));
        line1.add(Ui.chip(Store.get().courseName(e.courseId), Ui.courseColor(e.courseId)));
        if (e.dueToday()) {
            line1.add(Ui.chip("今日待复习", AppTheme.PRIMARY));
        }
        JLabel title = Ui.label(e.title, AppTheme.FONT_BODY, AppTheme.TEXT);
        line1.add(title);

        JPanel line2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        line2.setOpaque(false);
        String reason = e.reason == null || e.reason.isBlank() ? "未记录错因" : "错因：" + e.reason;
        line2.add(Ui.label(reason, AppTheme.FONT_SMALL, AppTheme.TEXT_SECONDARY));
        String meta = "复习 " + e.reviewCount + " 次";
        if (e.lapses > 0) {
            meta += " · 遗忘 " + e.lapses + " 次";
        }
        meta += " · 下次 " + Dates.friendly(e.nextReview);
        line2.add(Ui.label(meta, AppTheme.FONT_SMALL, new Color(0x9A, 0xA1, 0xAE)));

        left.add(line1, BorderLayout.NORTH);
        left.add(line2, BorderLayout.SOUTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        actions.setOpaque(false);
        JButton review = Ui.link("复习");
        review.addActionListener(e2 -> openReview(e));
        JButton edit = Ui.link("编辑");
        edit.addActionListener(e2 -> openEditor(e));
        JButton del = Ui.link("删除");
        del.setForeground(AppTheme.DANGER);
        del.addActionListener(e2 -> {
            Store.get().removeExercise(e);
            renderList();
            context().setStatus("已删除错题：" + e.title);
            Ui.undoToast(getView(), "已删除《" + e.title + "》", () -> {
                Store.get().undoDelete();
                renderList();
                context().setStatus("已撤销删除");
            });
        });
        actions.add(review);
        actions.add(edit);
        actions.add(del);

        row.add(left, BorderLayout.CENTER);
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    /** 连续复习：从今日待复习的题目里一道一道过。 */
    private void startReviewFlow() {
        List<Exercise> queue = Store.get().dueExercises();
        if (queue.isEmpty()) {
            FormDialog.toast(getView(), "今天没有待复习的题目");
            return;
        }
        openReview(queue.get(0));
    }

    private void openReview(Exercise e) {
        ReviewDialog dialog = new ReviewDialog(owner(), e);
        dialog.setVisible(true);
        renderList();
    }

    private void openEditor(Exercise exercise) {
        boolean creating = exercise == null;
        FormDialog dialog = new FormDialog(owner(), creating ? "录入错题" : "编辑错题", "保存");
        JTextField title = dialog.addText("题目", creating ? "" : exercise.title);
        JComboBox<String> course = dialog.addCourseCombo("所属课程", creating ? 0 : exercise.courseId);
        JTextArea content = dialog.addArea("题干", creating ? "" : exercise.content, 4);
        JTextArea answer = dialog.addArea("答案/解析", creating ? "" : exercise.answer, 4);
        JTextArea reason = dialog.addArea("错因", creating ? "" : exercise.reason, 3);
        String[] masteryItems = {"未掌握", "复习中", "已掌握"};
        JComboBox<String> mastery = dialog.addCombo("掌握度", masteryItems, creating ? 0 : exercise.mastery);

        if (!dialog.open(getView())) {
            return;
        }
        if (title.getText().trim().isEmpty()) {
            FormDialog.toast(getView(), "题目不能为空");
            return;
        }
        Store store = Store.get();
        if (creating) {
            Exercise e = new Exercise(title.getText().trim(), FormDialog.courseIdOf(course));
            e.content = content.getText().trim();
            e.answer = answer.getText().trim();
            e.reason = reason.getText().trim();
            e.mastery = mastery.getSelectedIndex();
            store.addExercise(e);
            context().setStatus("已录入错题：" + e.title);
        } else {
            exercise.title = title.getText().trim();
            exercise.courseId = FormDialog.courseIdOf(course);
            exercise.content = content.getText().trim();
            exercise.answer = answer.getText().trim();
            exercise.reason = reason.getText().trim();
            exercise.mastery = mastery.getSelectedIndex();
            store.changed();
            context().setStatus("已更新错题");
        }
        refresh();
    }

    /** 单题复习弹窗：先看题、再展开答案、最后判断记住没。 */
    private final class ReviewDialog extends LightDialog {

        private static final long serialVersionUID = 1L;

        private final Exercise exercise;
        private final JTextArea answerArea = new JTextArea();
        private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.GRID, 0));

        ReviewDialog(Frame owner, Exercise e) {
            super(owner, "错题复习", true);
            this.exercise = e;
            setLayout(new BorderLayout());

            JPanel head = new JPanel(new BorderLayout(0, 6));
            head.setOpaque(false);
            head.setBorder(BorderFactory.createEmptyBorder(20, 24, 0, 24));
            JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            chips.setOpaque(false);
            chips.add(Ui.chip(Store.get().courseName(e.courseId), Ui.courseColor(e.courseId)));
            chips.add(Ui.chip("第 " + (e.reviewCount + 1) + " 次复习", AppTheme.PRIMARY));
            chips.add(Ui.chip(e.masteryLabel(), e.mastery == 0 ? AppTheme.DANGER : AppTheme.WARN));
            head.add(chips, BorderLayout.NORTH);

            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setOpaque(false);
            body.setBorder(BorderFactory.createEmptyBorder(12, 24, 16, 24));

            JLabel title = Ui.label(e.title, AppTheme.FONT_H2, AppTheme.TEXT);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(title);
            body.add(Box.createVerticalStrut(8));

            if (e.content != null && !e.content.isBlank()) {
                JLabel q = Ui.wrap(e.content, 420, AppTheme.FONT_BODY, AppTheme.TEXT);
                q.setAlignmentX(Component.LEFT_ALIGNMENT);
                body.add(q);
                body.add(Box.createVerticalStrut(12));
            }

            answerArea.setEditable(false);
            answerArea.setLineWrap(true);
            answerArea.setWrapStyleWord(true);
            answerArea.setFont(AppTheme.FONT_BODY);
            answerArea.setBackground(AppTheme.SOFT);
            answerArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            answerArea.setText("答案：" + (e.answer == null || e.answer.isBlank() ? "（还没写解析）" : e.answer)
                    + "\n\n错因：" + (e.reason == null || e.reason.isBlank() ? "（未记录）" : e.reason));
            answerArea.setVisible(false);
            answerArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
            answerArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(answerArea);

            JPanel revealRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            revealRow.setOpaque(false);
            JButton reveal = Ui.ghost("显示答案");
            revealRow.add(reveal);
            revealRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(revealRow);

            add(head, BorderLayout.NORTH);
            add(body, BorderLayout.CENTER);

            JButton forgot = Ui.danger("没记住，明天再来");
            JButton remembered = Ui.primary("记住了");
            JButton close = Ui.ghost("关闭");
            actions.setOpaque(false);
            actions.setBorder(BorderFactory.createEmptyBorder(8, 24, 16, 24));
            actions.add(close);
            actions.add(forgot);
            actions.add(remembered);
            add(actions, BorderLayout.SOUTH);

            reveal.addActionListener(ev -> {
                answerArea.setVisible(true);
                revealRow.setVisible(false);
                pack();
                setLocationRelativeTo(getOwner());
            });
            close.addActionListener(ev -> dispose());
            forgot.addActionListener(ev -> {
                Store.get().review(e, false);
                context().setStatus("已加入明日复习：" + e.title);
                dispose();
            });
            remembered.addActionListener(ev -> {
                Store.get().review(e, true);
                context().setStatus("已记录复习：" + e.title + " · 下次 " + Dates.friendly(e.nextReview));
                dispose();
            });

            setPreferredSize(null);
            pack();
            setLocationRelativeTo(owner);
            setMinimumSize(new Dimension(460, getHeight()));
        }
    }
}
