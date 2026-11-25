package studyDesk;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.swing.*;

/**
 * 右侧 AI 助手。三个高频场景：把自然语言拆成任务、根据错题出同类练习、总结今天。
 * 网络请求全部放在 SwingWorker 里，界面不会卡住。
 */
final class AiPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String SYSTEM =
            "你是 StudyDesk 的学习助理，面向中国大学生。用简体中文回答，简洁、具体、不说空话。";

    private final PageContext context;
    private final JTextArea input = new JTextArea(3, 16);
    private final JTextArea output = new JTextArea(9, 16);
    private final JLabel status = new JLabel();
    private final JButton splitBtn = Ui.ghost("拆成任务");
    private final JButton drillBtn = Ui.ghost("出练习题");
    private final JButton summaryBtn = Ui.ghost("今日总结");

    private final AiConfig config = AiConfig.load();

    AiPanel(PageContext context) {
        this.context = context;
        setLayout(new BorderLayout(0, 8));
        setOpaque(false);

        add(header(), BorderLayout.NORTH);
        add(body(), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);
        refreshStatus();
    }

    private JComponent header() {
        JLabel title = Ui.label("AI 助手", AppTheme.FONT_H2, AppTheme.TEXT);
        JButton settings = Ui.link("设置");
        settings.setToolTipText("配置接口地址 / API Key / 模型");
        settings.addActionListener(e -> openSettings());
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(title, BorderLayout.WEST);
        row.add(settings, BorderLayout.EAST);
        return row;
    }

    private JComponent body() {
        input.setFont(AppTheme.FONT_BODY);
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane inputScroll = new JScrollPane(input);
        inputScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1));
        inputScroll.setPreferredSize(new Dimension(240, 72));

        JPanel actions = new JPanel(new GridLayout(1, 3, 6, 0));
        actions.setOpaque(false);
        splitBtn.addActionListener(e -> splitTasks());
        drillBtn.addActionListener(e -> makeDrill());
        summaryBtn.addActionListener(e -> summarize());
        actions.add(splitBtn);
        actions.add(drillBtn);
        actions.add(summaryBtn);

        output.setEditable(false);
        output.setFont(AppTheme.FONT_SMALL);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        output.setBackground(AppTheme.SOFT);
        output.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        output.setText("在下面描述你的学习计划，或者选中一条错题后点上面的按钮。\n\n"
                + "AI 写入的数据会直接进入任务 / 错题模块，仍然保存在本地。");
        JScrollPane outputScroll = new JScrollPane(output);
        outputScroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER, 1));

        JPanel box = new JPanel(new BorderLayout(0, 8));
        box.setOpaque(false);
        box.add(inputScroll, BorderLayout.NORTH);
        JPanel mid = new JPanel(new BorderLayout(0, 8));
        mid.setOpaque(false);
        mid.add(actions, BorderLayout.NORTH);
        mid.add(outputScroll, BorderLayout.CENTER);
        box.add(mid, BorderLayout.CENTER);
        return box;
    }

    private JComponent footer() {
        status.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        status.setForeground(AppTheme.TEXT_SECONDARY);
        return status;
    }

    private void refreshStatus() {
        status.setText(config.configured()
                ? "模型：" + config.model
                : "未配置 API Key，点右上角「设置」");
    }

    // ========================= 三个场景 =========================

    private void splitTasks() {
        String text = input.getText().trim();
        if (text.isEmpty()) {
            say("先在上面的输入框写一句计划，比如「这周把高数第三章复习完，周五前做完习题」。");
            return;
        }
        if (!ensureConfigured()) {
            return;
        }
        String prompt = "今天是 " + Dates.today() + "。把下面的学习计划拆成 1~5 个可执行任务。\n"
                + "只输出 JSON 数组，不要解释，不要 Markdown 代码块。格式：\n"
                + "[{\"title\":\"任务标题\",\"due\":\"2026-11-28\",\"priority\":2,"
                + "\"estimateMin\":45,\"course\":\"课程名\"}]\n"
                + "priority 取值 0 普通 / 1 重要 / 2 紧急；due 用 yyyy-MM-dd，没有明确日期就填空字符串；"
                + "course 从这些课程里选，选不到填空字符串：" + courseNames() + "\n\n计划：" + text;

        run(prompt, raw -> {
            List<Object> arr;
            try {
                arr = Json.list(Json.parse(extract(raw, '[', ']')));
            } catch (RuntimeException ex) {
                show(raw);
                return;
            }
            int added = 0;
            StringBuilder report = new StringBuilder();
            for (Object o : arr) {
                Map<String, Object> m = Json.map(o);
                String title = Json.str(m, "title", "").trim();
                if (title.isEmpty()) {
                    continue;
                }
                Task task = new Task(title);
                String due = Json.str(m, "due", "").trim();
                task.due = Dates.parse(due) == null ? "" : due;
                task.priority = Math.max(0, Math.min(2, Json.integer(m, "priority", 0)));
                task.estimateMin = Math.max(0, Json.integer(m, "estimateMin", 0));
                task.courseId = courseIdOf(Json.str(m, "course", ""));
                Store.get().addTask(task);
                report.append("· ").append(title);
                if (!task.due.isEmpty()) {
                    report.append("（").append(task.due).append("）");
                }
                report.append('\n');
                added++;
            }
            if (added == 0) {
                show(raw);
                return;
            }
            say("已加入 " + added + " 个任务：\n" + report);
            context.setStatus("AI 已添加 " + added + " 个任务");
            context.navigateTo(PageId.PLANS, new java.util.HashMap<>());
        });
    }

    private void makeDrill() {
        String text = input.getText().trim();
        if (text.isEmpty()) {
            say("先在输入框粘贴一道错题的题干，或者写下要练习的知识点。");
            return;
        }
        if (!ensureConfigured()) {
            return;
        }
        String prompt = "根据下面的错题或知识点，出 1 道同类型的新练习题，难度相当。\n"
                + "只输出 JSON 对象，不要解释，不要 Markdown 代码块。格式：\n"
                + "{\"title\":\"短标题\",\"content\":\"题干\",\"answer\":\"答案与解析\",\"reason\":\"易错点\"}\n\n"
                + "材料：" + text;

        run(prompt, raw -> {
            Map<String, Object> m;
            try {
                m = Json.map(Json.parse(extract(raw, '{', '}')));
            } catch (RuntimeException ex) {
                show(raw);
                return;
            }
            String title = Json.str(m, "title", "").trim();
            if (title.isEmpty()) {
                show(raw);
                return;
            }
            Exercise exercise = new Exercise(title, courseIdOf(""));
            exercise.content = Json.str(m, "content", "");
            exercise.answer = Json.str(m, "answer", "");
            exercise.reason = Json.str(m, "reason", "");
            Store.get().addExercise(exercise);
            say("已加入错题本：\n" + title + "\n\n" + exercise.content
                    + "\n\n答案：" + exercise.answer);
            context.setStatus("AI 已生成 1 道练习题");
        });
    }

    private void summarize() {
        if (!ensureConfigured()) {
            return;
        }
        String prompt = "下面是用户今天的学习数据：\n" + todayContext()
                + "\n请用 3 句话总结今天的学习情况，再给 1 条明天可以立刻执行的建议。";
        run(prompt, this::say);
    }

    // ========================= 调用与提示 =========================

    private void run(String prompt, java.util.function.Consumer<String> onDone) {
        setBusy(true, "正在请求模型，最多等 60 秒…");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new AiClient(config).chat(SYSTEM, prompt);
            }

            @Override
            protected void done() {
                setBusy(false, "");
                try {
                    onDone.accept(get());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    say("调用失败：" + cause.getMessage()
                            + "\n\n检查一下：接口地址是否以 /v1 结尾、Key 是否有效、模型名是否存在。");
                    context.setStatus("AI 调用失败");
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy, String text) {
        splitBtn.setEnabled(!busy);
        drillBtn.setEnabled(!busy);
        summaryBtn.setEnabled(!busy);
        if (busy) {
            status.setText(text);
        } else {
            refreshStatus();
        }
    }

    private boolean ensureConfigured() {
        if (config.configured()) {
            return true;
        }
        openSettings();
        return config.configured();
    }

    private void openSettings() {
        Window window = SwingUtilities.getWindowAncestor(this);
        Frame owner = window instanceof Frame ? (Frame) window : null;
        FormDialog dialog = new FormDialog(owner, "AI 接口设置", "保存");
        JTextField base = dialog.addText("接口地址", config.baseUrl);
        JTextField key = dialog.addText("API Key", config.apiKey);
        JTextField model = dialog.addText("模型名称", config.model);
        dialog.addRow("", Ui.wrap("Key 只保存在本机 " + AiConfig.file() + "，不会上传。"
                + "兼容 OpenAI 格式的接口都可以填。", 300, AppTheme.FONT_SMALL, AppTheme.TEXT_SECONDARY));
        if (!dialog.open(this)) {
            return;
        }
        config.baseUrl = base.getText().trim();
        config.apiKey = key.getText().trim();
        config.model = model.getText().trim();
        try {
            config.save();
            context.setStatus("AI 配置已保存");
        } catch (IOException e) {
            say("配置保存失败：" + e.getMessage());
        }
        refreshStatus();
    }

    private void say(String text) {
        output.setText(text);
        output.setCaretPosition(0);
    }

    private void show(String raw) {
        say(raw);
    }

    // ========================= 上下文拼装 =========================

    private String courseNames() {
        StringBuilder sb = new StringBuilder();
        for (Course c : Store.get().courses()) {
            if (sb.length() > 0) {
                sb.append("、");
            }
            sb.append(c.name);
        }
        return sb.length() == 0 ? "（暂无课程）" : sb.toString();
    }

    private long courseIdOf(String name) {
        if (name == null || name.isBlank()) {
            return 0;
        }
        for (Course c : Store.get().courses()) {
            if (c.name.equals(name.trim())) {
                return c.id;
            }
        }
        return 0;
    }

    private String todayContext() {
        Store store = Store.get();
        StringBuilder sb = new StringBuilder();
        sb.append("日期：").append(Dates.today()).append('\n');
        sb.append("今日课时：");
        List<Lesson> lessons = store.lessonsToday();
        if (lessons.isEmpty()) {
            sb.append("无");
        } else {
            for (Lesson l : lessons) {
                sb.append(store.courseName(l.courseId)).append("（第 ").append(l.startSlot)
                        .append('-').append(l.endSlot).append(" 节） ");
            }
        }
        sb.append("\n今日专注：").append(store.todayFocusMinutes()).append(" 分钟，完成 ")
                .append(store.pomodoroToday()).append(" 个番茄\n");

        sb.append("今日任务：\n");
        List<Task> tasks = store.todayTasks();
        if (tasks.isEmpty()) {
            sb.append("（无未完成任务）\n");
        } else {
            for (Task t : tasks) {
                sb.append("- ").append(t.title).append("（").append(t.priorityLabel());
                if (!t.due.isEmpty()) {
                    sb.append("，截止 ").append(t.due);
                }
                sb.append("）\n");
            }
        }
        sb.append("待复习错题：").append(store.dueExercises().size()).append(" 道\n");
        sb.append("未完成计划：");
        boolean first = true;
        for (Plan p : store.plans()) {
            if (!first) {
                sb.append("、");
            }
            sb.append(p.title).append("（进度 ").append(store.planProgress(p)).append("%）");
            first = false;
        }
        if (first) {
            sb.append("无");
        }
        return sb.toString();
    }

    /** 模型有时会包一层 ```json，这里截出最外层的 JSON 片段。 */
    private static String extract(String raw, char open, char close) {
        int start = raw.indexOf(open);
        int end = raw.lastIndexOf(close);
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }
}
