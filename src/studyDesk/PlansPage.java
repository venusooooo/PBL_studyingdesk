package studyDesk;

import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/** 学习计划与任务：阶段目标（计划）+ 可勾选的待办清单。 */
public final class PlansPage extends BasePage {

    private int taskFilter = 0;
    private JPanel taskPanel;

    @Override
    public PageId getId() {
        return PageId.PLANS;
    }

    @Override
    protected void buildContent() {
        Store store = Store.get();
        setHeaderHint("计划是阶段目标，任务是可勾选的小步骤；计划进度 = 已完成任务 / 关联任务。");

        // ---------- 计划区 ----------
        JButton addPlan = Ui.primary("+ 新建计划");
        addPlan.addActionListener(e -> openPlanEditor(null));
        JPanel planHead = Ui.card("学习计划", Ui.hbox(AppTheme.GRID, addPlan), null);
        addCard(planHead, 0, 0);

        List<Plan> plans = store.plans();
        if (plans.isEmpty()) {
            addCard(Ui.card(null, null, Ui.empty("还没有学习计划。建一个阶段目标，再把任务挂到它下面。")), 1, 0);
        } else {
            JPanel grid = new JPanel(new GridLayout(0, 2, AppTheme.CARD_GAP, AppTheme.CARD_GAP));
            grid.setOpaque(false);
            for (Plan p : plans) {
                grid.add(Ui.width(planCard(p), 360));
            }
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(false);
            wrapper.add(grid, BorderLayout.NORTH);
            addCard(wrapper, 1, 0);
        }

        // ---------- 任务区 ----------
        String[] tabs = {"今天", "本周", "全部未完成", "已完成"};
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        tabBar.setOpaque(false);
        for (int i = 0; i < tabs.length; i++) {
            int index = i;
            JButton b = new JButton(tabs[i]);
            b.setFont(AppTheme.FONT_SMALL);
            b.setFocusPainted(false);
            b.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            boolean active = taskFilter == i;
            b.setOpaque(true);
            b.setBorderPainted(active);
            b.setContentAreaFilled(true);
            b.setBackground(active ? AppTheme.NAV_ACTIVE : AppTheme.SURFACE);
            b.setForeground(active ? AppTheme.PRIMARY : AppTheme.TEXT_SECONDARY);
            b.setBorder(active
                    ? BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.PRIMARY, 1),
                    BorderFactory.createEmptyBorder(4, 11, 4, 11))
                    : BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                    BorderFactory.createEmptyBorder(4, 11, 4, 11)));
            b.addActionListener(e -> {
                taskFilter = index;
                refresh();
            });
            tabBar.add(b);
        }

        JButton addTask = Ui.primary("+ 新建任务");
        addTask.addActionListener(e -> newTask(null));

        JPanel headRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.GRID, 0));
        headRight.setOpaque(false);
        headRight.add(tabBar);
        headRight.add(addTask);

        taskPanel = new JPanel();
        taskPanel.setLayout(new BoxLayout(taskPanel, BoxLayout.Y_AXIS));
        taskPanel.setOpaque(false);

        JPanel taskCard = Ui.card("任务清单", headRight, taskPanel);
        addCard(taskCard, 2, 0);

        renderTasks();
    }

    private JComponent planCard(Plan p) {
        Store store = Store.get();
        int progress = store.planProgress(p);
        List<Task> planTasks = store.tasksOfPlan(p.id);
        int done = 0;
        for (Task t : planTasks) {
            if (t.done) {
                done++;
            }
        }

        JPanel card = Ui.card();
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JLabel title = Ui.label(p.title, AppTheme.FONT_H2, AppTheme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(title);
        inner.add(Box.createVerticalStrut(4));

        JLabel range = Ui.muted(Dates.shortDate(p.startDate) + " → " + Dates.shortDate(p.endDate)
                + " · " + done + "/" + planTasks.size() + " 个任务完成");
        range.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(range);
        inner.add(Box.createVerticalStrut(10));

        JPanel barRow = new JPanel(new BorderLayout(AppTheme.GRID, 0));
        barRow.setOpaque(false);
        JProgressBar bar = Ui.progress(progress, progress >= 100 ? AppTheme.SUCCESS : AppTheme.PRIMARY);
        barRow.add(bar, BorderLayout.CENTER);
        JLabel pct = Ui.label(progress + "%", AppTheme.FONT_SMALL, AppTheme.TEXT_SECONDARY);
        barRow.add(pct, BorderLayout.EAST);
        barRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        barRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        inner.add(barRow);

        if (p.description != null && !p.description.isBlank()) {
            inner.add(Box.createVerticalStrut(8));
            JLabel desc = Ui.wrap(p.description, 300);
            desc.setAlignmentX(Component.LEFT_ALIGNMENT);
            inner.add(desc);
        }

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        actions.setOpaque(false);
        JButton addTask = Ui.link("+ 添加任务");
        addTask.addActionListener(e -> newTask(p));
        JButton edit = Ui.link("编辑");
        edit.addActionListener(e -> openPlanEditor(p));
        JButton del = Ui.link("删除");
        del.setForeground(AppTheme.DANGER);
        del.addActionListener(e -> {
            if (FormDialog.confirm(getView(), "删除计划",
                    "删除《" + p.title + "》后，它的 " + planTasks.size() + " 个任务会变成独立任务，不会被删除。")) {
                store.removePlan(p);
                refresh();
            }
        });
        actions.add(addTask);
        actions.add(edit);
        actions.add(del);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(Box.createVerticalStrut(8));
        inner.add(actions);

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private void renderTasks() {
        if (taskPanel == null) {
            return;
        }
        taskPanel.removeAll();
        List<Task> list = filteredTasks();
        if (list.isEmpty()) {
            taskPanel.add(Ui.empty(taskFilter == 3 ? "还没有完成的任务。" : "这个范围里没有待办，先歇会儿。"));
        } else {
            boolean first = true;
            for (Task t : list) {
                if (!first) {
                    taskPanel.add(Ui.divider());
                }
                taskPanel.add(taskRow(t));
                first = false;
            }
        }
        taskPanel.revalidate();
        taskPanel.repaint();
    }

    private List<Task> filteredTasks() {
        Store store = Store.get();
        List<Task> out = new ArrayList<>();
        for (Task t : store.tasks()) {
            switch (taskFilter) {
                case 0:
                    if (t.done || !(Dates.isToday(t.due) || Dates.isOverdue(t.due) || t.due.isBlank())) {
                        continue;
                    }
                    break;
                case 1:
                    if (t.done || !(Dates.isThisWeek(t.due) || Dates.isOverdue(t.due) || t.due.isBlank())) {
                        continue;
                    }
                    break;
                case 2:
                    if (t.done) {
                        continue;
                    }
                    break;
                default:
                    if (!t.done) {
                        continue;
                    }
            }
            out.add(t);
        }
        out.sort((a, b) -> {
            if (a.done != b.done) {
                return a.done ? 1 : -1;
            }
            int byPriority = Integer.compare(b.priority, a.priority);
            if (byPriority != 0) {
                return byPriority;
            }
            return a.due.compareTo(b.due);
        });
        return out;
    }

    private JComponent taskRow(Task t) {
        JPanel row = Ui.row();

        JCheckBox done = new JCheckBox();
        done.setOpaque(false);
        done.setSelected(t.done);
        done.addActionListener(e -> {
            Store.get().toggleTask(t);
            refresh();
        });

        JPanel main = new JPanel(new BorderLayout(0, 2));
        main.setOpaque(false);

        JPanel line1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        line1.setOpaque(false);
        Color priorityColor = t.priority == 2 ? AppTheme.DANGER
                : (t.priority == 1 ? AppTheme.WARN : AppTheme.TEXT_SECONDARY);
        line1.add(Ui.chip(t.priorityLabel() + "优先", priorityColor));
        JLabel title = Ui.label(t.title, AppTheme.FONT_BODY, t.done ? AppTheme.TEXT_SECONDARY : AppTheme.TEXT);
        if (t.done) {
            title.setText("<html><strike>" + Ui.escape(t.title) + "</strike></html>");
        }
        line1.add(title);

        JPanel line2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        line2.setOpaque(false);
        if (t.courseId > 0) {
            line2.add(Ui.label(Store.get().courseName(t.courseId), AppTheme.FONT_SMALL,
                    Ui.courseColor(t.courseId)));
        }
        if (t.planId > 0) {
            for (Plan p : Store.get().plans()) {
                if (p.id == t.planId) {
                    line2.add(Ui.label("计划：" + p.title, AppTheme.FONT_SMALL, AppTheme.TEXT_SECONDARY));
                    break;
                }
            }
        }
        if (t.due != null && !t.due.isBlank()) {
            String text = "截止 " + Dates.friendly(t.due);
            Color color = t.overdue() ? AppTheme.DANGER : AppTheme.TEXT_SECONDARY;
            line2.add(Ui.label(text, AppTheme.FONT_SMALL, color));
        }
        if (t.estimateMin > 0) {
            line2.add(Ui.label(t.estimateMin + " 分钟", AppTheme.FONT_SMALL, new Color(0x9A, 0xA1, 0xAE)));
        }

        main.add(line1, BorderLayout.NORTH);
        main.add(line2, BorderLayout.SOUTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        actions.setOpaque(false);
        JButton edit = Ui.link("编辑");
        edit.addActionListener(e -> editTask(t));
        JButton del = Ui.link("删除");
        del.setForeground(AppTheme.DANGER);
        del.addActionListener(e -> {
            if (FormDialog.confirm(getView(), "删除任务", "确定删除任务「" + t.title + "」吗？")) {
                Store.get().removeTask(t);
                refresh();
            }
        });
        actions.add(edit);
        actions.add(del);

        row.add(done, BorderLayout.WEST);
        row.add(main, BorderLayout.CENTER);
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    // ========================= 编辑弹窗 =========================

    private void openPlanEditor(Plan plan) {
        boolean creating = plan == null;
        FormDialog dialog = new FormDialog(owner(), creating ? "新建计划" : "编辑计划", "保存");
        JTextField title = dialog.addText("计划名", creating ? "" : plan.title);
        JTextField start = dialog.addText("开始日期", creating ? Dates.today() : plan.startDate);
        JTextField end = dialog.addText("结束日期", creating
                ? Dates.iso(LocalDate.now().plusDays(14)) : plan.endDate);
        JTextArea desc = dialog.addArea("目标说明", creating ? "" : plan.description, 4);

        if (!dialog.open(getView())) {
            return;
        }
        String t = title.getText().trim();
        if (t.isEmpty()) {
            FormDialog.toast(getView(), "计划名不能为空");
            return;
        }
        if (Dates.parse(start.getText()) == null || Dates.parse(end.getText()) == null) {
            FormDialog.toast(getView(), "日期格式应为 2026-11-25");
            return;
        }
        Store store = Store.get();
        if (creating) {
            Plan p = new Plan(t, start.getText().trim(), end.getText().trim());
            p.description = desc.getText().trim();
            store.addPlan(p);
            context().setStatus("已新建计划：" + t);
        } else {
            plan.title = t;
            plan.startDate = start.getText().trim();
            plan.endDate = end.getText().trim();
            plan.description = desc.getText().trim();
            store.changed();
            context().setStatus("已更新计划：" + t);
        }
        refresh();
    }

    private void newTask(Plan presetPlan) {
        openTaskEditor(null, presetPlan);
    }

    private void editTask(Task task) {
        openTaskEditor(task, null);
    }

    private void openTaskEditor(Task task, Plan presetPlan) {
        boolean creating = task == null;
        FormDialog dialog = new FormDialog(owner(), creating ? "新建任务" : "编辑任务", "保存");
        JTextField title = dialog.addText("任务", creating ? "" : task.title);
        JComboBox<String> course = dialog.addCourseCombo("课程", creating ? 0 : task.courseId);

        List<Plan> plans = new ArrayList<>(Store.get().plans());
        String[] planItems = new String[plans.size() + 1];
        planItems[0] = "不关联计划";
        int planIdx = 0;
        for (int i = 0; i < plans.size(); i++) {
            planItems[i + 1] = plans.get(i).title;
            if (!creating && plans.get(i).id == task.planId) {
                planIdx = i + 1;
            }
            if (creating && presetPlan != null && plans.get(i).id == presetPlan.id) {
                planIdx = i + 1;
            }
        }
        JComboBox<String> planBox = dialog.addCombo("所属计划", planItems, planIdx);

        JTextField due = dialog.addText("截止日期", creating ? Dates.today() : task.due);
        String[] priorityItems = {"低", "中", "高"};
        JComboBox<String> priority = dialog.addCombo("优先级",
                priorityItems, creating ? 1 : task.priority);
        String[] estimateItems = {"不设置", "15 分钟", "25 分钟", "45 分钟", "60 分钟", "90 分钟", "120 分钟"};
        int[] estimateValues = {0, 15, 25, 45, 60, 90, 120};
        int estIdx = 0;
        if (!creating) {
            for (int i = 0; i < estimateValues.length; i++) {
                if (estimateValues[i] == task.estimateMin) {
                    estIdx = i;
                }
            }
        }
        JComboBox<String> estimate = dialog.addCombo("预计耗时", estimateItems, estIdx);

        if (!dialog.open(getView())) {
            return;
        }
        String t = title.getText().trim();
        if (t.isEmpty()) {
            FormDialog.toast(getView(), "任务内容不能为空");
            return;
        }
        String dueText = due.getText().trim();
        if (!dueText.isEmpty() && Dates.parse(dueText) == null) {
            FormDialog.toast(getView(), "日期格式应为 2026-11-25，或留空表示不设截止日期");
            return;
        }
        long planId = planBox.getSelectedIndex() <= 0 ? 0 : plans.get(planBox.getSelectedIndex() - 1).id;
        Store store = Store.get();
        if (creating) {
            Task nt = new Task(t);
            nt.courseId = FormDialog.courseIdOf(course);
            nt.planId = planId;
            nt.due = dueText;
            nt.priority = priority.getSelectedIndex();
            nt.estimateMin = estimateValues[Math.max(0, estimate.getSelectedIndex())];
            store.addTask(nt);
            context().setStatus("已新建任务：" + t);
        } else {
            task.title = t;
            task.courseId = FormDialog.courseIdOf(course);
            task.planId = planId;
            task.due = dueText;
            task.priority = priority.getSelectedIndex();
            task.estimateMin = estimateValues[Math.max(0, estimate.getSelectedIndex())];
            store.changed();
            context().setStatus("已更新任务：" + t);
        }
        refresh();
    }
}
