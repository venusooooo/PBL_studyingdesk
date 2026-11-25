package studyDesk;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * 学习统计：专注热力图、每周任务完成率、错题掌握度分布。
 * 三张图全部用 Graphics2D 自绘，不引任何图表库。
 */
public final class StatsPage extends BasePage {

    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("M/d");

    @Override
    public PageId getId() {
        return PageId.STATS;
    }

    @Override
    protected void buildContent() {
        Store store = Store.get();
        setHeaderHint("近 12 周专注热力图、每周任务完成率、错题掌握度分布 —— 数据都来自你在各模块的记录");

        addCard(summary(store), 0, 0, 2);
        addCard(Ui.card("专注热力图 · 近 12 周", legend(), new HeatmapPanel(store)), 1, 0, 2);
        addCard(Ui.card("每周任务完成率", null, new WeeklyBars(store.tasks())), 2, 0);
        addCard(Ui.card("错题掌握度分布", null, new MasteryBars(store.exercises())), 2, 1);
        addCard(Ui.card("数据洞察", null, insights(store)), 3, 0, 2);
    }

    // ========================= 概览与洞察 =========================

    private JComponent summary(Store store) {
        int total = 0;
        for (FocusRecord f : store.focusRecords()) {
            total += f.minutes;
        }
        int last7 = 0;
        LocalDate from = LocalDate.now().minusDays(6);
        for (FocusRecord f : store.focusRecords()) {
            LocalDate d = Dates.parse(f.date);
            if (d != null && !d.isBefore(from)) {
                last7 += f.minutes;
            }
        }
        int doneTasks = 0;
        for (Task t : store.tasks()) {
            if (t.done) {
                doneTasks++;
            }
        }
        int mastered = 0;
        for (Exercise e : store.exercises()) {
            if (e.mastered()) {
                mastered++;
            }
        }
        int rate = store.exercises().isEmpty() ? 0 : mastered * 100 / store.exercises().size();

        JPanel row = new JPanel(new GridLayout(1, 4, AppTheme.CARD_GAP, 0));
        row.setOpaque(false);
        row.add(Ui.stat(hours(total), "累计专注", AppTheme.PRIMARY));
        row.add(Ui.stat(last7 + "′", "近 7 天专注", AppTheme.WARN));
        row.add(Ui.stat(String.valueOf(doneTasks), "已完成任务", AppTheme.SUCCESS));
        row.add(Ui.stat(rate + "%", "错题掌握率", new Color(0x8B, 0x5C, 0xF6)));
        return Ui.card(null, null, row);
    }

    private String hours(int minutes) {
        if (minutes < 60) {
            return minutes + "′";
        }
        return String.format("%.1f h", minutes / 60.0);
    }

    private JComponent insights(Store store) {
        JPanel box = Ui.vbox(6);
        for (String line : insightLines(store)) {
            JLabel label = Ui.wrap("· " + line, 900, AppTheme.FONT_BODY, AppTheme.TEXT);
            box.add(label);
        }
        if (box.getComponentCount() == 0) {
            box.add(Ui.muted("记录几天数据后，这里会给出一些趋势判断。"));
        }
        return box;
    }

    private List<String> insightLines(Store store) {
        List<String> lines = new ArrayList<>();
        int total = 0;
        for (FocusRecord f : store.focusRecords()) {
            total += f.minutes;
        }
        if (total > 0) {
            int activeDays = 0;
            for (int i = 0; i < 90; i++) {
                if (store.focusMinutes(Dates.iso(LocalDate.now().minusDays(i))) > 0) {
                    activeDays++;
                }
            }
            lines.add("近 90 天有 " + activeDays + " 天产生过专注记录，累计 " + hours(total) + "。");
        }

        int streak = 0;
        for (int i = 0; i < 365; i++) {
            if (store.focusMinutes(Dates.iso(LocalDate.now().minusDays(i))) > 0) {
                streak++;
            } else if (i > 0) {
                break;
            }
        }
        if (streak > 0) {
            lines.add("已连续 " + streak + " 天保持专注，别断在今天。");
        }

        int weekDone = 0;
        int weekDue = 0;
        for (Task t : store.tasks()) {
            LocalDate due = Dates.parse(t.due);
            if (due != null && Dates.isThisWeek(t.due)) {
                weekDue++;
                if (t.done) {
                    weekDone++;
                }
            }
        }
        if (weekDue > 0) {
            lines.add("本周有截止日的任务 " + weekDue + " 个，已完成 " + weekDone + " 个（"
                    + (weekDone * 100 / weekDue) + "%）。");
        }

        long weakestCourse = -1;
        int weakestCount = 0;
        for (Course c : store.courses()) {
            int count = 0;
            for (Exercise e : store.exercises()) {
                if (e.courseId == c.id && !e.mastered()) {
                    count++;
                }
            }
            if (count > weakestCount) {
                weakestCount = count;
                weakestCourse = c.id;
            }
        }
        if (weakestCount > 0) {
            lines.add("「" + store.courseName(weakestCourse) + "」还有 " + weakestCount
                    + " 道错题没掌握，建议优先安排复习。");
        }
        return lines;
    }

    // ========================= 图例 =========================

    private JComponent legend() {
        JPanel box = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        box.setOpaque(false);
        box.add(Ui.muted("少"));
        int[] reds = {0xEF, 0xC7, 0x9C, 0x6C, 0x2F};
        int[] greens = {0xF2, 0xD9, 0xBC, 0x99, 0x6F};
        int[] blues = {0xF7, 0xFB, 0xF7, 0xF2, 0xED};
        for (int i = 0; i < reds.length; i++) {
            JLabel cell = new JLabel();
            cell.setOpaque(true);
            cell.setBackground(new Color(reds[i], greens[i], blues[i]));
            cell.setPreferredSize(new Dimension(11, 11));
            cell.setBorder(BorderFactory.createLineBorder(new Color(0xE1, 0xE5, 0xEC), 1));
            box.add(cell);
        }
        box.add(Ui.muted("多"));
        return box;
    }

    // ========================= 热力图 =========================

    private static final class HeatmapPanel extends JPanel {

        private static final long serialVersionUID = 1L;
        private static final int WEEKS = 12;
        private static final int GAP = 4;
        private static final int LABEL_W = 34;

        private final int[][] minutes = new int[WEEKS][7];
        private final LocalDate[] weekStart = new LocalDate[WEEKS];
        private final LocalDate today = LocalDate.now();

        HeatmapPanel(Store store) {
            setOpaque(false);
            setPreferredSize(new Dimension(560, 268));
            for (int w = 0; w < WEEKS; w++) {
                weekStart[w] = Dates.weekStart().minusWeeks(WEEKS - 1 - w);
                for (int d = 0; d < 7; d++) {
                    LocalDate day = weekStart[w].plusDays(d);
                    if (!day.isAfter(today)) {
                        minutes[w][d] = store.focusMinutes(Dates.iso(day));
                    }
                }
            }
            setToolTipText("");
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        /** 格子边长随宽度自适应，窗口越宽格子越大，最多 26px。 */
        private int cell() {
            int avail = getWidth() - LABEL_W - 24;
            return Math.max(10, Math.min(26, (avail - (WEEKS - 1) * GAP) / WEEKS));
        }

        private int gridWidth() {
            return WEEKS * cell() + (WEEKS - 1) * GAP;
        }

        /** 热力图 + 右侧小结作为一组居中，避免超宽卡片里内容全挤在左边。 */
        private int originX() {
            int group = LABEL_W + gridWidth() + 48 + 220;
            int left = Math.max(0, (getWidth() - group) / 2);
            return left + LABEL_W;
        }

        private int originY() {
            return 30;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));

            int cell = cell();
            int step = cell + GAP;
            int x0 = originX();
            int y0 = originY();

            String[] rowLabels = {"一", "二", "三", "四", "五", "六", "日"};
            g2.setColor(AppTheme.TEXT_SECONDARY);
            for (int d = 0; d < 7; d++) {
                int y = y0 + d * step + cell / 2 + 4;
                g2.drawString(rowLabels[d], x0 - 16, y);
            }

            int total = 0;
            int activeDays = 0;
            int best = 0;
            LocalDate bestDay = null;
            for (int w = 0; w < WEEKS; w++) {
                LocalDate monday = weekStart[w];
                if (monday.getDayOfMonth() <= 7) {
                    g2.setColor(AppTheme.TEXT_SECONDARY);
                    g2.drawString(monday.getMonthValue() + " 月", x0 + w * step, y0 - 10);
                }
                for (int d = 0; d < 7; d++) {
                    LocalDate day = monday.plusDays(d);
                    int x = x0 + w * step;
                    int y = y0 + d * step;
                    if (day.isAfter(today)) {
                        continue;
                    }
                    int m = minutes[w][d];
                    if (!day.isBefore(weekStart[0])) {
                        total += m;
                        if (m > 0) {
                            activeDays++;
                        }
                        if (m > best) {
                            best = m;
                            bestDay = day;
                        }
                    }
                    g2.setColor(cellColor(m));
                    g2.fillRoundRect(x, y, cell, cell, 5, 5);
                }
            }

            g2.setColor(AppTheme.TEXT_SECONDARY);
            g2.drawString("每格 = 一天，颜色越深专注越久", x0, y0 + 7 * step + 12);

            // 右侧留白较多时放一块小结，避免整张卡片显得空。
            int textX = x0 + gridWidth() + 48;
            if (textX + 160 < getWidth()) {
                g2.setColor(AppTheme.TEXT_SECONDARY);
                g2.drawString("近 12 周", textX, y0 + 4);
                g2.setColor(AppTheme.TEXT);
                g2.setFont(new Font("Microsoft YaHei", Font.BOLD, 28));
                g2.drawString(hoursText(total), textX, y0 + 40);
                g2.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
                g2.setColor(AppTheme.TEXT_SECONDARY);
                g2.drawString("共 " + activeDays + " 天有专注记录", textX, y0 + 66);
                g2.drawString(bestDay == null ? "还没有专注记录"
                                : "最多的一天：" + bestDay.getMonthValue() + " 月 "
                                        + bestDay.getDayOfMonth() + " 日 · " + best + " 分钟",
                        textX, y0 + 88);
            }
            g2.dispose();
        }

        private String hoursText(int minutes) {
            if (minutes < 60) {
                return minutes + " 分钟";
            }
            return String.format("%.1f 小时", minutes / 60.0);
        }

        private Color cellColor(int m) {
            if (m <= 0) {
                return new Color(0xEF, 0xF2, 0xF7);
            }
            if (m < 30) {
                return new Color(0xC7, 0xD9, 0xFB);
            }
            if (m < 60) {
                return new Color(0x9C, 0xBC, 0xF7);
            }
            if (m < 120) {
                return new Color(0x6C, 0x99, 0xF2);
            }
            return AppTheme.PRIMARY;
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            int cell = cell();
            int step = cell + GAP;
            int x0 = originX();
            int y0 = originY();
            int w = (e.getX() - x0) / step;
            int d = (e.getY() - y0) / step;
            if (w < 0 || w >= WEEKS || d < 0 || d >= 7) {
                return null;
            }
            if (e.getX() - (x0 + w * step) > cell || e.getY() - (y0 + d * step) > cell) {
                return null;
            }
            LocalDate day = weekStart[w].plusDays(d);
            if (day.isAfter(today)) {
                return null;
            }
            return day.getMonthValue() + " 月 " + day.getDayOfMonth() + " 日 · "
                    + Dates.weekdayLabel(Dates.weekdayOf(day)) + " · "
                    + minutes[w][d] + " 分钟";
        }
    }

    // ========================= 每周任务完成率 =========================

    private static final class WeeklyBars extends JPanel {

        private static final long serialVersionUID = 1L;
        private static final int WEEKS = 8;

        private final String[] labels = new String[WEEKS];
        private final int[] totals = new int[WEEKS];
        private final int[] dones = new int[WEEKS];

        WeeklyBars(List<Task> tasks) {
            setOpaque(false);
            setPreferredSize(new Dimension(520, 190));
            LocalDate thisMonday = Dates.weekStart();
            for (int i = 0; i < WEEKS; i++) {
                LocalDate monday = thisMonday.minusWeeks(WEEKS - 1 - i);
                LocalDate sunday = monday.plusDays(6);
                labels[i] = monday.format(MONTH_DAY);
                for (Task t : tasks) {
                    LocalDate due = Dates.parse(t.due);
                    if (due == null || due.isBefore(monday) || due.isAfter(sunday)) {
                        continue;
                    }
                    totals[i]++;
                    if (t.done) {
                        dones[i]++;
                    }
                }
            }
            setToolTipText("");
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));

            int left = 34;
            int right = 12;
            int top = 18;
            int bottom = 30;
            int chartW = Math.max(60, getWidth() - left - right);
            int chartH = Math.max(40, getHeight() - top - bottom);
            int slot = chartW / WEEKS;
            int barW = Math.min(34, slot - 10);

            g2.setColor(new Color(0xE5, 0xE7, 0xEB));
            g2.drawLine(left, top + chartH, left + chartW, top + chartH);
            g2.drawString("100%", 6, top + 8);
            g2.drawString("0", 6, top + chartH);

            for (int i = 0; i < WEEKS; i++) {
                int x = left + i * slot + (slot - barW) / 2;
                int pct = totals[i] == 0 ? 0 : dones[i] * 100 / totals[i];
                int h = totals[i] == 0 ? 0 : Math.max(3, chartH * pct / 100);
                int y = top + chartH - h;
                g2.setColor(totals[i] == 0 ? new Color(0xEF, 0xF2, 0xF7) : AppTheme.SUCCESS);
                g2.fillRoundRect(x, totals[i] == 0 ? top + chartH - 3 : y, barW, h, 6, 6);

                g2.setColor(AppTheme.TEXT_SECONDARY);
                String value = totals[i] == 0 ? "—" : pct + "%";
                int vw = g2.getFontMetrics().stringWidth(value);
                g2.drawString(value, x + (barW - vw) / 2, (totals[i] == 0 ? top + chartH - 8 : y - 4));
                int lw = g2.getFontMetrics().stringWidth(labels[i]);
                g2.drawString(labels[i], x + (barW - lw) / 2, top + chartH + 16);
            }
            g2.dispose();
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            int left = 34;
            int chartW = Math.max(60, getWidth() - left - 12);
            int slot = chartW / WEEKS;
            int i = (e.getX() - left) / Math.max(1, slot);
            if (i < 0 || i >= WEEKS) {
                return null;
            }
            if (totals[i] == 0) {
                return labels[i] + " 开始这一周 · 没有带截止日的任务";
            }
            return labels[i] + " 开始这一周 · 完成 " + dones[i] + "/" + totals[i] + " 个任务";
        }
    }

    // ========================= 错题掌握度分布 =========================

    private static final class MasteryBars extends JPanel {

        private static final long serialVersionUID = 1L;

        private final int[] counts = new int[3];
        private final String[] labels = {"未掌握", "复习中", "已掌握"};
        private final Color[] colors = {AppTheme.DANGER, AppTheme.WARN, AppTheme.SUCCESS};

        MasteryBars(List<Exercise> exercises) {
            setOpaque(false);
            setPreferredSize(new Dimension(480, 190));
            for (Exercise e : exercises) {
                counts[Math.max(0, Math.min(2, e.mastery))]++;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

            int total = counts[0] + counts[1] + counts[2];
            int left = 64;
            int right = 46;
            int barH = 18;
            int gap = 34;
            int top = 26;
            int maxW = Math.max(60, getWidth() - left - right);

            for (int i = 0; i < 3; i++) {
                int y = top + i * gap;
                g2.setColor(AppTheme.TEXT_SECONDARY);
                g2.drawString(labels[i], 8, y + barH - 4);

                g2.setColor(new Color(0xF1, 0xF4, 0xF9));
                g2.fillRoundRect(left, y, maxW, barH, 8, 8);

                int w = total == 0 ? 0 : maxW * counts[i] / total;
                if (w > 0) {
                    g2.setColor(colors[i]);
                    g2.fillRoundRect(left, y, Math.max(8, w), barH, 8, 8);
                }

                g2.setColor(AppTheme.TEXT);
                String text = counts[i] + (total == 0 ? "" : "（" + counts[i] * 100 / total + "%）");
                g2.drawString(text, left + maxW + 6, y + barH - 4);
            }

            g2.setColor(AppTheme.TEXT_SECONDARY);
            g2.drawString("共 " + total + " 道错题", 8, top + 3 * gap + 12);
            g2.dispose();
        }
    }
}
