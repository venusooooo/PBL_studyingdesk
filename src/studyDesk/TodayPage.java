package studyDesk;

import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.swing.*;

/** 今日面板：把课表、任务、错题、专注四件事汇总到一屏。 */
public final class TodayPage extends BasePage {

    private static final int POMODORO_SECONDS = 25 * 60;

    private final JLabel timerLabel = Ui.label("25:00", new Font("Consolas", Font.BOLD, 34), AppTheme.TEXT);
    private final JProgressBar timerBar = Ui.progress(100, AppTheme.PRIMARY);
    private final Timer timer;

    private int remaining = POMODORO_SECONDS;
    private boolean running;

    public TodayPage() {
        timer = new Timer(1000, e -> tick());
        timerBar.setPreferredSize(new Dimension(220, 6));
    }

    @Override
    public PageId getId() {
        return PageId.TODAY;
    }

    @Override
    protected void buildContent() {
        Store store = Store.get();
        LocalDate today = LocalDate.now();
        setHeaderHint(today.getMonthValue() + " 月 " + today.getDayOfMonth() + " 日 · "
                + Dates.weekdayLabel(Dates.weekdayOf(today)) + " · 第 " + store.currentWeek() + " 周");

        // ---------- 顶部统计 ----------
        List<Task> todayTasks = store.todayTasks();
        int doneToday = 0;
        for (Task t : store.tasks()) {
            if (t.done && Dates.isToday(t.doneAt)) {
                doneToday++;
            }
        }
        int totalToday = todayTasks.size() + doneToday;

        JPanel stats = new JPanel(new GridLayout(1, 4, AppTheme.CARD_GAP, 0));
        stats.setOpaque(false);
        stats.add(Ui.stat(String.valueOf(todayTasks.size()), "今日待办", AppTheme.PRIMARY));
        stats.add(Ui.stat(String.valueOf(store.dueExercises().size()), "错题待复习", AppTheme.DANGER));
        stats.add(Ui.stat(String.valueOf(store.lessonsToday().size()), "今日课时", AppTheme.SUCCESS));
        stats.add(Ui.stat(store.todayFocusMinutes() + "′", "今日专注", AppTheme.WARN));
        JPanel statCard = Ui.card(null, null, stats);
        addCard(statCard, 0, 0, 2);

        // ---------- 今日课程 + 今日任务 ----------
        addCard(courseCard(), 1, 0);
        addCard(taskCard(todayTasks, totalToday, doneToday), 1, 1);

        // ---------- 待复习 + 专注 ----------
        addCard(reviewCard(), 2, 0);
        addCard(focusCard(), 2, 1);
    }

    private JComponent courseCard() {
        List<Lesson> lessons = Store.get().lessonsToday();
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        if (lessons.isEmpty()) {
            body.add(Ui.empty("今天没有排课，适合集中推进项目或刷题。"));
        } else {
            for (Lesson l : lessons) {
                JPanel row = new JPanel(new BorderLayout(AppTheme.GRID, 0));
                row.setOpaque(false);
                row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

                Color accent = Ui.courseColor(l.courseId);
                JLabel time = Ui.label(lessonTime(l), AppTheme.FONT_SMALL, AppTheme.TEXT_SECONDARY);
                time.setPreferredSize(new Dimension(86, 22));

                JLabel dot = new JLabel("●");
                dot.setForeground(accent);
                dot.setFont(AppTheme.FONT_SMALL);
                JLabel name = Ui.label(Store.get().courseName(l.courseId), AppTheme.FONT_BODY, AppTheme.TEXT);
                String roomText = l.room == null || l.room.isBlank() ? "" : l.room;

                JPanel mid = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
                mid.setOpaque(false);
                mid.add(dot);
                mid.add(name);

                JLabel room = Ui.label(roomText, AppTheme.FONT_SMALL, new Color(0x9A, 0xA1, 0xAE));

                row.add(time, BorderLayout.WEST);
                row.add(mid, BorderLayout.CENTER);
                row.add(room, BorderLayout.EAST);
                body.add(row);
            }

            body.add(Box.createVerticalGlue());
            JLabel footer = Ui.label("今天共 " + lessons.size() + " 节课 · "
                    + clockOf(lessons.get(0)) + " 开始",
                    AppTheme.FONT_SMALL, new Color(0x9A, 0xA1, 0xAE));
            JPanel footBar = new JPanel(new BorderLayout());
            footBar.setOpaque(false);
            footBar.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
            footBar.add(footer, BorderLayout.WEST);
            footBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            body.add(footBar);
        }

        JButton open = Ui.link("查看课表 →");
        open.addActionListener(e -> context().navigateTo(PageId.SCHEDULE, Map.of()));
        return Ui.card("今日课程", Ui.hbox(AppTheme.GRID, open), body);
    }

    private String lessonTime(Lesson l) {
        return "第 " + l.startSlot + " 节 " + clockOf(l);
    }

    /** 第 n 节对应的钟点，例如第 1 节 → 08:00。 */
    private String clockOf(Lesson l) {
        String[] times = {"08:00", "08:55", "10:00", "10:55", "14:00",
                "14:55", "16:00", "16:55", "19:00", "19:55"};
        int s = Math.max(1, Math.min(times.length, l.startSlot));
        return times[s - 1];
    }

    private JComponent taskCard(List<Task> tasks, int total, int done) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        if (tasks.isEmpty()) {
            body.add(Ui.empty("今日待办清空了，可以安排一点复习或预习。"));
        } else {
            int shown = 0;
            for (Task t : tasks) {
                if (shown >= 5) {
                    break;
                }
                if (shown > 0) {
                    body.add(Ui.divider());
                }
                body.add(taskLine(t));
                shown++;
            }
            if (tasks.size() > 5) {
                body.add(Ui.muted("还有 " + (tasks.size() - 5) + " 条，去「学习计划与任务」查看"));
            }
        }

        JButton open = Ui.link("全部任务 →");
        open.addActionListener(e -> context().navigateTo(PageId.PLANS, Map.of()));
        String title = total == 0 ? "今日任务" : "今日任务 · " + done + "/" + total + " 完成";
        return Ui.card(title, Ui.hbox(AppTheme.GRID, open), body);
    }

    private JComponent taskLine(Task t) {
        JPanel row = new JPanel(new BorderLayout(AppTheme.GRID, 0));
        row.setOpaque(false);

        JCheckBox box = new JCheckBox();
        box.setOpaque(false);
        box.setSelected(t.done);
        box.addActionListener(e -> {
            Store.get().toggleTask(t);
            refresh();
        });

        JPanel mid = new JPanel(new BorderLayout(0, 2));
        mid.setOpaque(false);
        JLabel title = Ui.wrap(t.title, 190, AppTheme.FONT_BODY, AppTheme.TEXT);
        mid.add(title, BorderLayout.NORTH);
        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        meta.setOpaque(false);
        if (t.courseId > 0) {
            meta.add(Ui.label(Store.get().courseName(t.courseId), AppTheme.FONT_SMALL,
                    Ui.courseColor(t.courseId)));
        }
        if (t.overdue()) {
            meta.add(Ui.label("已逾期 · " + Dates.friendly(t.due), AppTheme.FONT_SMALL, AppTheme.DANGER));
        } else if (Dates.isToday(t.due)) {
            meta.add(Ui.label("今天截止", AppTheme.FONT_SMALL, AppTheme.WARN));
        }
        mid.add(meta, BorderLayout.SOUTH);

        JLabel priority = Ui.chip(t.priorityLabel(), t.priority == 2 ? AppTheme.DANGER
                : (t.priority == 1 ? AppTheme.WARN : AppTheme.TEXT_SECONDARY));

        row.add(box, BorderLayout.WEST);
        row.add(mid, BorderLayout.CENTER);
        row.add(Ui.alignRight(priority), BorderLayout.EAST);
        row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        return row;
    }

    private JComponent reviewCard() {
        List<Exercise> due = Store.get().dueExercises();
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        if (due.isEmpty()) {
            body.add(Ui.empty("今天没有到期的错题，保持住。"));
        } else {
            int shown = 0;
            for (Exercise e : due) {
                if (shown >= 3) {
                    break;
                }
                if (shown > 0) {
                    body.add(Ui.divider());
                }
                shown++;
                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(false);
                row.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
                row.add(Ui.label(e.title, AppTheme.FONT_BODY, AppTheme.TEXT), BorderLayout.WEST);
                row.add(Ui.alignRight(Ui.chip(Store.get().courseName(e.courseId),
                        Ui.courseColor(e.courseId))), BorderLayout.EAST);
                body.add(row);
            }
            if (due.size() > 3) {
                body.add(Ui.muted("…还有 " + (due.size() - 3) + " 道"));
            }
        }

        JButton go = Ui.primary("去复习");
        go.addActionListener(e -> context().navigateTo(PageId.EXERCISES, Map.of("dueOnly", true)));
        return Ui.card("错题待复习 · " + due.size() + " 道", Ui.hbox(AppTheme.GRID, go), body);
    }

    private JComponent focusCard() {
        updateTimerUi();

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timerLabel.setFont(new Font("Consolas", Font.BOLD, 34));
        timerBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(timerLabel);
        center.add(Box.createVerticalStrut(6));
        center.add(timerBar);

        JButton toggle = running ? Ui.ghost("暂停") : Ui.primary("开始专注");
        toggle.addActionListener(e -> {
            if (running) {
                pause();
            } else {
                start();
            }
            refresh();
        });
        JButton reset = Ui.link("重置");
        reset.addActionListener(e -> {
            resetTimer();
            refresh();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, AppTheme.GRID, 0));
        buttons.setOpaque(false);
        buttons.add(toggle);
        buttons.add(reset);

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(center, BorderLayout.CENTER);
        body.add(buttons, BorderLayout.SOUTH);

        JLabel info = Ui.muted("今日 " + Store.get().todayFocusMinutes() + " 分钟 · 本周 "
                + Store.get().weekFocusMinutes() + " 分钟");
        info.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);
        wrapper.add(body, BorderLayout.CENTER);
        wrapper.add(info, BorderLayout.SOUTH);

        return Ui.card("专注计时（25 分钟）", null, wrapper);
    }

    // ========================= 番茄钟 =========================

    private void start() {
        running = true;
        timer.start();
    }

    private void pause() {
        running = false;
        timer.stop();
    }

    private void resetTimer() {
        pause();
        remaining = POMODORO_SECONDS;
        updateTimerUi();
    }

    private void tick() {
        if (remaining > 0) {
            remaining--;
            updateTimerUi();
            return;
        }
        pause();
        Store.get().addFocus(25, "番茄钟");
        remaining = POMODORO_SECONDS;
        updateTimerUi();
        context().setStatus("完成一个 25 分钟专注，已记录到今日专注时长");
        refresh();
    }

    private void updateTimerUi() {
        int m = remaining / 60;
        int s = remaining % 60;
        timerLabel.setText(String.format("%02d:%02d", m, s));
        timerBar.setValue(100 - (remaining * 100 / POMODORO_SECONDS));
    }
}
