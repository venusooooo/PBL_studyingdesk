package studyDesk;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/** 课表管理：周一到周日 × 10 节课的周视图，点击格子即可添加 / 编辑课时。 */
public final class SchedulePage extends BasePage {

    private static final int SLOTS = 10;
    private static final String[] SLOT_TIME = {
            "08:00", "08:55", "10:00", "10:55", "14:00",
            "14:55", "16:00", "16:55", "19:00", "19:55"};

    /** 从「+ 新建课程」直接排课时，记住刚建好的课程，弹窗里预选它。 */
    private long pendingCourseId;

    @Override
    public PageId getId() {
        return PageId.SCHEDULE;
    }

    @Override
    protected void onNewAction() {
        openEditor(null, Dates.weekdayOf(java.time.LocalDate.now()), 1);
    }

    @Override
    protected void buildContent() {
        Store store = Store.get();
        List<Lesson> all = store.lessons();
        int week = store.currentWeek();
        List<Lesson> visible = new ArrayList<>();
        for (Lesson lesson : all) {
            if (lesson.occursIn(week)) {
                visible.add(lesson);
            }
        }
        setHeaderHint(store.courses().isEmpty()
                ? "还没有课程。先点「+ 新建课程」建一门课，再点表格里的空格子排课。"
                : "共 " + all.size() + " 个课时 · 第 " + week + " 周显示 " + visible.size()
                        + " 个 · 点空格子添加，点已排课时编辑");

        JButton add = Ui.primary("+ 添加课时");
        add.addActionListener(e -> openEditor(null, Dates.weekdayOf(java.time.LocalDate.now()), 1));
        JButton newCourse = Ui.ghost("+ 新建课程");
        newCourse.setToolTipText("不离开课表，直接建一门课");
        newCourse.addActionListener(e -> createCourse());
        JButton export = Ui.ghost("导出 .ics");
        export.setToolTipText("导出成日历文件，可导入手机日历");
        export.addActionListener(e -> exportIcs());
        JPanel toolbar = Ui.card("周课表", Ui.hbox(AppTheme.GRID, export, newCourse, add), null);
        addCard(toolbar, 0, 0);

        TimetableGrid grid = new TimetableGrid();

        // ---------- 表头 ----------
        addGridCell(grid, new JLabel(), 0, 0, 1, 1);
        for (int w = 1; w <= 7; w++) {
            boolean isToday = w == Dates.weekdayOf(java.time.LocalDate.now());
            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(false);
            head.setPreferredSize(new Dimension(92, 26));
            JLabel l = new JLabel(Dates.weekdayLabel(w), SwingConstants.CENTER);
            l.setFont(AppTheme.FONT_SMALL);
            l.setForeground(isToday ? AppTheme.PRIMARY : AppTheme.TEXT_SECONDARY);
            l.setText(isToday ? Dates.weekdayLabel(w) + "（今天）" : Dates.weekdayLabel(w));
            head.add(l, BorderLayout.CENTER);
            addGridCell(grid, head, w, 0, 1, 1);
        }

        // ---------- 节次列 ----------
        for (int s = 1; s <= SLOTS; s++) {
            JPanel slot = new JPanel();
            slot.setLayout(new BoxLayout(slot, BoxLayout.Y_AXIS));
            slot.setOpaque(false);
            slot.setPreferredSize(new Dimension(60, 48));
            JLabel index = new JLabel("第 " + s + " 节");
            index.setFont(AppTheme.FONT_SMALL);
            index.setForeground(AppTheme.TEXT_SECONDARY);
            index.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel time = new JLabel(SLOT_TIME[s - 1]);
            time.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
            time.setForeground(new Color(0xA5, 0xAC, 0xB8));
            time.setAlignmentX(Component.CENTER_ALIGNMENT);
            slot.setBorder(BorderFactory.createEmptyBorder(6, 2, 0, 2));
            slot.add(index);
            slot.add(time);
            addGridCell(grid, slot, 0, s, 1, 1);
        }

        // ---------- 课时 ----------
        boolean[][] covered = new boolean[8][SLOTS + 1];
        for (int s = 1; s <= SLOTS; s++) {
            covered[0][s] = true;
        }
        for (Lesson lesson : visible) {
            int w = Math.max(1, Math.min(7, lesson.weekday));
            int start = Math.max(1, Math.min(SLOTS, lesson.startSlot));
            int end = Math.max(start, Math.min(SLOTS, lesson.endSlot));
            for (int s = start; s <= end; s++) {
                covered[w][s] = true;
            }
            grid.place(lessonCell(lesson), w, start, 1, end - start + 1);
        }

        // ---------- 空格子 ----------
        for (int w = 1; w <= 7; w++) {
            for (int s = 1; s <= SLOTS; s++) {
                if (!covered[w][s]) {
                    grid.place(emptyCell(w, s), w, s, 1, 1);
                }
            }
        }

        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.setOpaque(false);
        gridWrapper.add(grid, BorderLayout.CENTER);
        addCard(Ui.card(null, null, gridWrapper), 1, 0);
    }

    private void addGridCell(TimetableGrid grid, JComponent comp, int x, int y, int w, int h) {
        grid.place(comp, x, y, w, h);
    }

    /** 按像素宽度裁剪文本，超出部分用省略号，避免课表格子里的文字被硬截断。 */
    private static String fit(String text, Font font, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, true, true);
        int width = (int) Math.ceil(font.getStringBounds(text, frc).getWidth());
        if (width <= maxWidth) {
            return text;
        }
        String start = "…";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String next = sb.toString() + text.charAt(i);
            if ((int) Math.ceil(font.getStringBounds(next + start, frc).getWidth()) > maxWidth) {
                break;
            }
            sb.append(text.charAt(i));
        }
        return sb + start;
    }

    /**
     * 课表网格：8 列（第 0 列是节次）等宽、表头 + 10 个节次等高的手工布局。
     * 用固定规则算坐标，避免 GridBagLayout 在空间不足时把七列压成不一样的宽度。
     */
    private static final class TimetableGrid extends JPanel {

        private static final long serialVersionUID = 1L;
        private static final int SLOT_W = 66;
        private static final int HEAD_H = 28;
        private static final int CELL_H = 38;
        private static final int GAP = 2;
        private static final int MIN_CELL_W = 56;
        private static final int MIN_CELL_H = 30;

        private final List<JComponent> comps = new ArrayList<>();
        private final List<int[]> spec = new ArrayList<>();

        TimetableGrid() {
            setLayout(null);
            setOpaque(false);
        }

        void place(JComponent comp, int col, int row, int spanCols, int spanRows) {
            comps.add(comp);
            spec.add(new int[]{col, row, Math.max(1, spanCols), Math.max(1, spanRows)});
            add(comp);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(SLOT_W + 7 * 92 + 8 * GAP, HEAD_H + SLOTS * CELL_H + 12 * GAP);
        }

        @Override
        public void doLayout() {
            int cellW = Math.max(MIN_CELL_W, (getWidth() - SLOT_W - 8 * GAP) / 7);
            int cellH = Math.max(MIN_CELL_H, (getHeight() - HEAD_H - 12 * GAP) / SLOTS);
            for (int i = 0; i < comps.size(); i++) {
                int[] s = spec.get(i);
                int x = s[0] == 0 ? 0 : SLOT_W + (s[0] - 1) * (cellW + GAP) + GAP;
                int y = s[1] == 0 ? 0 : HEAD_H + (s[1] - 1) * (cellH + GAP) + GAP;
                int w = s[0] == 0 ? SLOT_W : s[2] * cellW + (s[2] - 1) * GAP;
                int h = s[1] == 0 ? HEAD_H : s[3] * cellH + (s[3] - 1) * GAP;
                comps.get(i).setBounds(x, y, w, h);
            }
        }
    }

    private JComponent emptyCell(int weekday, int slot) {
        JButton cell = new JButton("");
        cell.setBackground(AppTheme.SURFACE);
        cell.setOpaque(true);
        cell.setContentAreaFilled(true);
        cell.setFocusPainted(false);
        cell.setPreferredSize(new Dimension(92, 48));
        cell.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1));
        cell.setToolTipText("点击添加：" + Dates.weekdayLabel(weekday) + " 第 " + slot + " 节");
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cell.addActionListener(e -> openEditor(null, weekday, slot));
        cell.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cell.setBackground(new Color(0xF7, 0xF9, 0xFD));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cell.setBackground(AppTheme.SURFACE);
            }
        });
        return cell;
    }

    private JComponent lessonCell(Lesson lesson) {
        Color accent = Ui.courseColor(lesson.courseId);
        String name = Store.get().courseName(lesson.courseId);
        String room = lesson.room == null || lesson.room.isBlank() ? "" : lesson.room;
        Font nameFont = new Font("Microsoft YaHei", Font.BOLD, 11);
        Font subFont = new Font("Microsoft YaHei", Font.PLAIN, 10);
        String html = "<html><div style='text-align:center;width:74px'>"
                + "<b>" + Ui.escape(fit(name, nameFont, 72)) + "</b>"
                + (room.isEmpty() ? "" : "<br><span style='font-size:9px'>"
                        + Ui.escape(fit(room, subFont, 74)) + "</span>")
                + "</div></html>";

        JButton cell = new JButton(html);
        cell.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        cell.setForeground(AppTheme.TEXT);
        cell.setBackground(Ui.tint(accent, 0.86f));
        cell.setOpaque(true);
        cell.setContentAreaFilled(true);
        cell.setFocusPainted(false);
        cell.setPreferredSize(new Dimension(92, 48));
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Ui.tint(accent, 0.55f), 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cell.setToolTipText(name + " · 第 " + lesson.startSlot + "–" + lesson.endSlot + " 节 · "
                + lesson.weeksLabel());
        cell.addActionListener(e -> openEditor(lesson, lesson.weekday, lesson.startSlot));

        JPopupMenu menu = new JPopupMenu();
        JMenuItem edit = new JMenuItem("编辑课时");
        edit.addActionListener(e -> openEditor(lesson, lesson.weekday, lesson.startSlot));
        JMenuItem del = new JMenuItem("删除课时");
        del.addActionListener(e -> removeLesson(lesson));
        menu.add(edit);
        menu.add(del);
        cell.setComponentPopupMenu(menu);
        return cell;
    }

    /** 在课表页直接新建课程，建完刷新课表；如果用户接着要排课，再点格子即可。 */
    private void createCourse() {
        Course created = CourseEditor.open(getView(), null);
        if (created == null) {
            return;
        }
        context().setStatus("已新建课程：" + created.name + "，点表格里的空格子即可排课");
        refresh();
        if (FormDialog.confirm(getView(), "课程已创建",
                "《" + created.name + "》已添加。\n现在为它安排一节课吗？")) {
            openEditorFor(created, Dates.weekdayOf(java.time.LocalDate.now()), 1);
        }
    }

    /** 指定课程的排课入口：弹窗里课程下拉框默认停在这门课上。 */
    private void openEditorFor(Course course, int weekday, int startSlot) {
        pendingCourseId = course.id;
        try {
            openEditor(null, weekday, startSlot);
        } finally {
            pendingCourseId = 0;
        }
    }

    private void removeLesson(Lesson lesson) {
        if (FormDialog.confirm(getView(), "删除课时",
                "删除 " + Dates.weekdayLabel(lesson.weekday) + " 第 " + lesson.startSlot + " 节的《"
                        + Store.get().courseName(lesson.courseId) + "》？")) {
            Store.get().removeLesson(lesson);
            context().setStatus("已删除课时");
            Ui.undoToast(getView(), "已删除课时", Store.get()::undoDelete);
            refresh();
        }
    }

    /** 导出成 .ics，导入手机日历后按周次重复提醒。 */
    private void exportIcs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("导出课表到日历");
        chooser.setSelectedFile(new java.io.File("StudyDesk-课表.ics"));
        if (chooser.showSaveDialog(getView()) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        java.io.File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase().endsWith(".ics")) {
            target = new java.io.File(target.getParentFile(), target.getName() + ".ics");
        }
        try {
            int count = IcsExporter.export(target.toPath());
            context().setStatus("已导出 " + count + " 个课时到 " + target.getName());
            FormDialog.toast(getView(), "已导出 " + count + " 个课时：\n" + target.getAbsolutePath()
                    + "\n\n双击该文件即可导入手机或电脑日历。");
        } catch (Exception ex) {
            FormDialog.toast(getView(), "导出失败：" + ex.getMessage());
        }
    }

    private void openEditor(Lesson lesson, int weekday, int startSlot) {
        Store store = Store.get();
        if (store.courses().isEmpty()) {
            // 没有课程时不再把用户支到别的页面：就地建一门，建完继续排课。
            Course created = CourseEditor.open(getView(), null);
            if (created == null) {
                return;
            }
        }
        boolean creating = lesson == null;
        FormDialog dialog = new FormDialog(owner(), creating ? "添加课时" : "编辑课时", "保存");

        List<Course> courses = new ArrayList<>(store.courses());
        String[] courseItems = new String[courses.size()];
        int courseIdx = 0;
        for (int i = 0; i < courses.size(); i++) {
            courseItems[i] = courses.get(i).name;
            if (!creating && courses.get(i).id == lesson.courseId) {
                courseIdx = i;
            } else if (creating && pendingCourseId != 0 && courses.get(i).id == pendingCourseId) {
                courseIdx = i;
            }
        }
        JComboBox<String> course = Ui.combo(courseItems, courseIdx);
        JButton addCourse = Ui.link("+ 新建课程");
        addCourse.setToolTipText("列表里没有？直接建一门，建完自动选中");
        JPanel courseRow = new JPanel(new BorderLayout(AppTheme.GRID, 0));
        courseRow.setOpaque(false);
        courseRow.add(course, BorderLayout.CENTER);
        courseRow.add(addCourse, BorderLayout.EAST);
        dialog.addRow("课程", courseRow);
        addCourse.addActionListener(e -> {
            Course made = CourseEditor.open(course, null);
            if (made != null) {
                courses.add(made);
                course.addItem(made.name);
                course.setSelectedItem(made.name);
            }
        });

        String[] weekdayItems = new String[7];
        for (int i = 0; i < 7; i++) {
            weekdayItems[i] = Dates.weekdayLabel(i + 1);
        }
        int wd = creating ? weekday : lesson.weekday;
        JComboBox<String> weekdayBox = dialog.addCombo("星期", weekdayItems, Math.max(0, wd - 1));
        int weekIdx = 0;
        if (!creating) {
            weekIdx = "ODD".equals(lesson.weeks) ? 1 : ("EVEN".equals(lesson.weeks) ? 2 : 0);
        }
        JComboBox<String> weeksBox = dialog.addCombo("上课周次",
                new String[]{"每周", "单周", "双周"}, weekIdx);

        JSpinner start = dialog.addSpinner("开始节次", creating ? startSlot : lesson.startSlot, 1, SLOTS);
        JSpinner end = dialog.addSpinner("结束节次", creating ? startSlot : lesson.endSlot, 1, SLOTS);
        JTextField room = dialog.addText("上课地点", creating ? "" : lesson.room);

        if (!dialog.open(getView())) {
            return;
        }
        int weekdayValue = weekdayBox.getSelectedIndex() + 1;
        int startValue = (Integer) start.getValue();
        int endValue = (Integer) end.getValue();
        String weeksValue = new String[]{"ALL", "ODD", "EVEN"}[
                Math.max(0, Math.min(2, weeksBox.getSelectedIndex()))];
        if (endValue < startValue) {
            FormDialog.toast(getView(), "结束节次不能早于开始节次");
            return;
        }
        for (Lesson other : store.lessons()) {
            if (!creating && other.id == lesson.id) {
                continue;
            }
            if (other.weekday == weekdayValue && Lesson.weeksOverlap(weeksValue, other.weeks)
                    && startValue <= other.endSlot && endValue >= other.startSlot) {
                FormDialog.toast(getView(), "时间冲突：该时段已有《" + store.courseName(other.courseId)
                        + "》（" + other.weeksLabel() + "）");
                return;
            }
        }

        long courseId = courses.get(Math.max(0, course.getSelectedIndex())).id;
        if (creating) {
            Lesson l = new Lesson(courseId, weekdayValue, startValue, endValue, room.getText().trim());
            l.weeks = weeksValue;
            store.addLesson(l);
            context().setStatus("已添加课时：" + store.courseName(courseId));
        } else {
            lesson.courseId = courseId;
            lesson.weekday = weekdayValue;
            lesson.startSlot = startValue;
            lesson.endSlot = endValue;
            lesson.room = room.getText().trim();
            lesson.weeks = weeksValue;
            store.changed();
            context().setStatus("已更新课时");
        }
        refresh();
    }
}
