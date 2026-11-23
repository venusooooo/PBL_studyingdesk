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

    @Override
    public PageId getId() {
        return PageId.SCHEDULE;
    }

    @Override
    protected void buildContent() {
        Store store = Store.get();
        List<Lesson> all = store.lessons();
        setHeaderHint("共 " + all.size() + " 个课时 · 第 " + store.currentWeek() + " 周 · 点空格子添加，点已排课时编辑");

        JButton add = Ui.primary("+ 添加课时");
        add.addActionListener(e -> openEditor(null, Dates.weekdayOf(java.time.LocalDate.now()), 1));
        JPanel toolbar = Ui.card("周课表", Ui.hbox(AppTheme.GRID, add), null);
        addCard(toolbar, 0, 0);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(AppTheme.SURFACE);
        grid.setPreferredSize(new Dimension(7 * 92 + 60, (SLOTS + 1) * 48));

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
        for (Lesson lesson : all) {
            int w = Math.max(1, Math.min(7, lesson.weekday));
            int start = Math.max(1, Math.min(SLOTS, lesson.startSlot));
            int end = Math.max(start, Math.min(SLOTS, lesson.endSlot));
            for (int s = start; s <= end; s++) {
                covered[w][s] = true;
            }
            grid.add(lessonCell(lesson), cellConstraints(w, start, 1, end - start + 1));
        }

        // ---------- 空格子 ----------
        for (int w = 1; w <= 7; w++) {
            for (int s = 1; s <= SLOTS; s++) {
                if (!covered[w][s]) {
                    grid.add(emptyCell(w, s), cellConstraints(w, s, 1, 1));
                }
            }
        }

        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.setOpaque(false);
        gridWrapper.add(grid, BorderLayout.CENTER);
        addCard(Ui.card(null, null, gridWrapper), 1, 0);
    }

    private void addGridCell(JPanel grid, JComponent comp, int x, int y, int w, int h) {
        grid.add(comp, cellConstraints(x, y, w, h));
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

    private GridBagConstraints cellConstraints(int x, int y, int w, int h) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = w;
        c.gridheight = h;
        c.weightx = x == 0 ? 0 : 1;
        c.weighty = y == 0 ? 0 : 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        return c;
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
        cell.setToolTipText(name + " · 第 " + lesson.startSlot + "–" + lesson.endSlot + " 节");
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

    private void removeLesson(Lesson lesson) {
        if (FormDialog.confirm(getView(), "删除课时",
                "删除 " + Dates.weekdayLabel(lesson.weekday) + " 第 " + lesson.startSlot + " 节的《"
                        + Store.get().courseName(lesson.courseId) + "》？")) {
            Store.get().removeLesson(lesson);
            context().setStatus("已删除课时");
            refresh();
        }
    }

    private void openEditor(Lesson lesson, int weekday, int startSlot) {
        Store store = Store.get();
        if (store.courses().isEmpty()) {
            FormDialog.toast(getView(), "请先在「课程与课时」里新建至少一门课程");
            return;
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
            }
        }
        JComboBox<String> course = Ui.combo(courseItems, courseIdx);
        dialog.addRow("课程", course);

        String[] weekdayItems = new String[7];
        for (int i = 0; i < 7; i++) {
            weekdayItems[i] = Dates.weekdayLabel(i + 1);
        }
        int wd = creating ? weekday : lesson.weekday;
        JComboBox<String> weekdayBox = dialog.addCombo("星期", weekdayItems, Math.max(0, wd - 1));

        JSpinner start = dialog.addSpinner("开始节次", creating ? startSlot : lesson.startSlot, 1, SLOTS);
        JSpinner end = dialog.addSpinner("结束节次", creating ? startSlot : lesson.endSlot, 1, SLOTS);
        JTextField room = dialog.addText("上课地点", creating ? "" : lesson.room);

        if (!dialog.open(getView())) {
            return;
        }
        int weekdayValue = weekdayBox.getSelectedIndex() + 1;
        int startValue = (Integer) start.getValue();
        int endValue = (Integer) end.getValue();
        if (endValue < startValue) {
            FormDialog.toast(getView(), "结束节次不能早于开始节次");
            return;
        }
        for (Lesson other : store.lessons()) {
            if (!creating && other.id == lesson.id) {
                continue;
            }
            if (other.weekday == weekdayValue
                    && startValue <= other.endSlot && endValue >= other.startSlot) {
                FormDialog.toast(getView(), "时间冲突：该时段已有《" + store.courseName(other.courseId) + "》");
                return;
            }
        }

        long courseId = courses.get(Math.max(0, course.getSelectedIndex())).id;
        if (creating) {
            Lesson l = new Lesson(courseId, weekdayValue, startValue, endValue, room.getText().trim());
            store.addLesson(l);
            context().setStatus("已添加课时：" + store.courseName(courseId));
        } else {
            lesson.courseId = courseId;
            lesson.weekday = weekdayValue;
            lesson.startSlot = startValue;
            lesson.endSlot = endValue;
            lesson.room = room.getText().trim();
            store.changed();
            context().setStatus("已更新课时");
        }
        refresh();
    }
}
