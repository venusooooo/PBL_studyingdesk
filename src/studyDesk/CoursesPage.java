package studyDesk;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;

/** 课程与课时：课程的增删改，以及每门课的课时 / 错题 / 任务概况。 */
public final class CoursesPage extends BasePage {

    @Override
    public PageId getId() {
        return PageId.COURSES;
    }

    @Override
    protected void onNewAction() {
        openEditor(null);
    }

    @Override
    protected void buildContent() {
        Store store = Store.get();
        setHeaderHint("课程是课表、错题、笔记、任务的归类依据，删除课程会一并清理它的课时。");

        JButton add = Ui.primary("+ 新建课程");
        add.addActionListener(e -> openEditor(null));
        JPanel toolbar = Ui.card("我的课程", Ui.hbox(AppTheme.GRID, add), null);
        addCard(toolbar, 0, 0);

        List<Course> courses = store.courses();
        if (courses.isEmpty()) {
            JPanel emptyCard = Ui.card(null, null, Ui.empty("还没有课程，先新建一门课，再去课表里排课。"));
            addCard(emptyCard, 1, 0);
            return;
        }

        JPanel grid = new JPanel(new GridLayout(0, 2, AppTheme.CARD_GAP, AppTheme.CARD_GAP));
        grid.setOpaque(false);
        for (Course c : courses) {
            grid.add(Ui.width(courseCard(c), 360));
        }
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(grid, BorderLayout.NORTH);
        addCard(wrapper, 1, 0);
    }

    private JComponent courseCard(Course c) {
        Store store = Store.get();
        Color accent = Ui.color(c.color);

        JPanel card = new JPanel(new BorderLayout(0, AppTheme.GRID));
        card.setBackground(AppTheme.SURFACE);
        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                        BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        card.setBorder(border);

        int lessonCount = 0;
        for (Lesson l : store.lessons()) {
            if (l.courseId == c.id) {
                lessonCount++;
            }
        }
        int exerciseCount = 0;
        int dueCount = 0;
        for (Exercise e : store.exercises()) {
            if (e.courseId == c.id) {
                exerciseCount++;
                if (e.dueToday()) {
                    dueCount++;
                }
            }
        }
        int openTasks = 0;
        for (Task t : store.tasks()) {
            if (t.courseId == c.id && !t.done) {
                openTasks++;
            }
        }
        int noteCount = 0;
        for (Note n : store.notes()) {
            if (n.courseId == c.id) {
                noteCount++;
            }
        }

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel name = Ui.label(c.name, AppTheme.FONT_H2, AppTheme.TEXT);
        JLabel teacher = Ui.muted(c.teacher == null || c.teacher.isBlank() ? "未填写教师" : c.teacher);
        head.add(name, BorderLayout.NORTH);
        head.add(teacher, BorderLayout.SOUTH);

        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        meta.setOpaque(false);
        meta.add(Ui.label("课时 " + lessonCount + " · 错题 " + exerciseCount
                + " · 待办 " + openTasks + " · 笔记 " + noteCount,
                AppTheme.FONT_SMALL, AppTheme.TEXT_SECONDARY));
        if (dueCount > 0) {
            meta.add(Ui.chip("待复习 " + dueCount, AppTheme.DANGER));
        }

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        String note = c.note == null || c.note.isBlank() ? "暂无备注" : c.note;
        bottom.add(Ui.wrap(note, 300), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.setOpaque(false);
        JButton edit = Ui.link("编辑");
        edit.addActionListener(e -> openEditor(c));
        final int lessonTotal = lessonCount;
        JButton del = Ui.link("删除");
        del.setForeground(AppTheme.DANGER);
        del.addActionListener(e -> {
            if (FormDialog.confirm(getView(), "删除课程",
                    "删除《" + c.name + "》会同时删除它的 " + lessonTotal + " 个课时，\n"
                            + "相关错题 / 笔记 / 任务会变成「未归类」。确定删除吗？")) {
                store.removeCourse(c);
                context().setStatus("已删除课程：" + c.name);
                refresh();
                Ui.undoToast(getView(), "已删除课程《" + c.name + "》", () -> {
                    Store.get().undoDelete();
                    refresh();
                    context().setStatus("已撤销删除");
                });
            }
        });
        actions.add(edit);
        actions.add(del);
        bottom.add(actions, BorderLayout.EAST);

        card.add(head, BorderLayout.NORTH);
        card.add(meta, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private void openEditor(Course course) {
        boolean creating = course == null;
        Course saved = CourseEditor.open(getView(), course);
        if (saved == null) {
            return;
        }
        context().setStatus((creating ? "已新建课程：" : "已更新课程：") + saved.name);
        refresh();
    }
}
