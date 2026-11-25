package studyDesk;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局数据仓库（单例）。
 * 负责：内存中的实体集合、CRUD、JSON 持久化、变更通知。
 * 数据文件默认放在 用户目录/.studydesk/data.json，纯本地，不联网。
 */
public final class Store {

    /** 艾宾浩斯复习间隔：第 1/2/3/4/5/6 次复习分别间隔多少天。 */
    private static final int[] REVIEW_INTERVALS = {1, 2, 4, 7, 15, 30};

    private static Store instance;

    private final Path dir;
    private final Path file;
    /** 上一个版本的副本，主文件损坏时用于恢复。 */
    private final Path backupFile;
    private final List<Runnable> listeners = new ArrayList<>();

    private long seq = 1;

    private final List<Course> courses = new ArrayList<>();
    private final List<Lesson> lessons = new ArrayList<>();
    private final List<Exercise> exercises = new ArrayList<>();
    private final List<Note> notes = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();
    private final List<Plan> plans = new ArrayList<>();
    private final List<FocusRecord> focusRecords = new ArrayList<>();

    /** 学期第一周的周一，用来算"第 N 周"；为空时按第 1 周显示。 */
    private String termStart = "";
    private String saveState = "未变更";

    /** 删除前的整库快照，供 5 秒内撤销；只保留最近一次删除。 */
    private String undoSnapshot;
    private String undoLabel = "";

    private Store() {
        dir = resolveDataDir();
        file = dir.resolve("data.json");
        backupFile = dir.resolve("data.bak.json");
        load();
    }

    /**
     * 数据目录优先级：
     * 1. 启动参数 {@code -Dstudydesk.home=<dir>}，用于便携版或切换多套数据；
     * 2. 程序所在目录（或上一级）存在 data 文件夹时进入便携模式；
     * 3. 默认 用户目录/.studydesk。
     */
    private static Path resolveDataDir() {
        String custom = System.getProperty("studydesk.home", "").trim();
        if (!custom.isEmpty()) {
            try {
                return Paths.get(custom).toAbsolutePath().normalize();
            } catch (InvalidPathException e) {
                System.err.println("studydesk.home 不是合法路径，改用默认目录：" + custom);
            }
        }
        Path portable = portableDataDir();
        if (portable != null) {
            return portable;
        }
        return Paths.get(System.getProperty("user.home", ".")).resolve(".studydesk");
    }

    /**
     * 便携模式探测：jar / class 所在目录及其上一级里若有 data 文件夹，就用它。
     * jpackage 的 app-image 布局是 exe 在根、jar 在 app/ 下，所以要看上一级。
     */
    private static Path portableDataDir() {
        try {
            URL location = Store.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return null;
            }
            Path base = Paths.get(location.toURI());
            if (!Files.isDirectory(base)) {
                base = base.getParent();
            }
            if (base == null) {
                return null;
            }
            Path parent = base.getParent();
            Path[] candidates = {base.resolve("data"), parent == null ? null : parent.resolve("data")};
            for (Path candidate : candidates) {
                if (candidate != null && Files.isDirectory(candidate)) {
                    return candidate.toAbsolutePath().normalize();
                }
            }
        } catch (Exception ignored) {
            // 取不到程序位置就退回默认目录，不影响启动
        }
        return null;
    }

    public static synchronized Store get() {
        if (instance == null) {
            instance = new Store();
        }
        return instance;
    }

    // ========================= 持久化 =========================

    public Path dataFile() {
        return file;
    }

    public Path dataDir() {
        return dir;
    }

    /** 上一版数据的备份文件，主文件损坏时会被自动用来恢复。 */
    public Path backupFile() {
        return backupFile;
    }

    public String saveState() {
        return saveState;
    }

    public void addListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    private void fireChanged() {
        for (Runnable r : new ArrayList<>(listeners)) {
            r.run();
        }
    }

    /** 数据变更后调用：落盘 + 通知界面刷新。 */
    public void changed() {
        save();
        fireChanged();
    }

    /** 把内存中的全部数据整理成待序列化的 Map，save 与撤销快照共用。 */
    private Map<String, Object> toRoot() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", 2);
        root.put("seq", seq);
        root.put("termStart", termStart);
        root.put("courses", mapList(courses, Course::toMap));
        root.put("lessons", mapList(lessons, Lesson::toMap));
        root.put("exercises", mapList(exercises, Exercise::toMap));
        root.put("notes", mapList(notes, Note::toMap));
        root.put("tasks", mapList(tasks, Task::toMap));
        root.put("plans", mapList(plans, Plan::toMap));
        root.put("focus", mapList(focusRecords, FocusRecord::toMap));
        return root;
    }

    /** 只落盘不刷新（例如修改纯展示用字段）。 */
    public void save() {
        byte[] bytes = Json.write(toRoot()).getBytes(StandardCharsets.UTF_8);
        Path tmp = dir.resolve("data.json.tmp");
        try {
            Files.createDirectories(dir);
            // 先写临时文件并强制刷盘，再原子替换：写到一半崩溃也不会破坏 data.json。
            try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                channel.write(ByteBuffer.wrap(bytes));
                channel.force(true);
            }
            keepPreviousVersion();
            moveAtomic(tmp, file);
            saveState = "已保存 " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (IOException e) {
            saveState = "保存失败：" + e.getMessage();
            System.err.println("数据保存失败：" + e);
        }
    }

    /** 把当前 data.json 留一份成 data.bak.json，备份失败不阻断本次保存。 */
    private void keepPreviousVersion() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            Files.copy(file, backupFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("备份上一版数据失败（不影响保存）：" + e);
        }
    }

    private static void moveAtomic(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // 个别网络盘 / 老文件系统不支持原子移动，退化为普通替换
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void load() {
        if (!Files.exists(file)) {
            seed();
            save();
            return;
        }
        if (!readFrom(file)) {
            recover();
            return;
        }
        snapshotDaily();
        if (courses.isEmpty() && lessons.isEmpty() && tasks.isEmpty()) {
            seed();
            save();
        }
    }

    /**
     * 每天首次启动留一份只读快照到 backups/ 下，保留最近 7 份。
     * 和 data.bak.json 不同，它防的是"改错了想找回上周的数据"。
     */
    private void snapshotDaily() {
        try {
            if (!Files.exists(file)) {
                return;
            }
            Path backupDir = dir.resolve("backups");
            Files.createDirectories(backupDir);
            Path today = backupDir.resolve("data-" + Dates.today() + ".json");
            if (Files.exists(today)) {
                return;
            }
            Files.copy(file, today, StandardCopyOption.REPLACE_EXISTING);
            List<Path> all = new ArrayList<>();
            try (java.util.stream.Stream<Path> stream = Files.list(backupDir)) {
                stream.filter(p -> p.getFileName().toString().startsWith("data-")
                        && p.getFileName().toString().endsWith(".json"))
                        .forEach(all::add);
            }
            all.sort((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));
            while (all.size() > 7) {
                Files.deleteIfExists(all.remove(0));
            }
        } catch (IOException e) {
            System.err.println("生成每日快照失败（不影响使用）：" + e);
        }
    }

    /**
     * 主文件读不出来时：先把它改名留档，再尝试用 data.bak.json 恢复，
     * 备份也不可用才重建示例数据。任何时候都不静默覆盖用户原始文件。
     */
    private void recover() {
        System.err.println("主数据文件读取失败，尝试用备份恢复");
        try {
            Files.move(file, dir.resolve("data.broken.bak"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("保留损坏文件失败：" + e);
        }
        clearMemory();
        if (Files.exists(backupFile) && readFrom(backupFile)) {
            save();
            saveState = "已从备份恢复（损坏文件另存为 data.broken.bak）";
            return;
        }
        clearMemory();
        seed();
        save();
        saveState = "数据损坏且无可用备份，已重建示例数据";
    }

    /** 解析指定文件并填充内存，成功返回 true；失败时内存可能被部分填充，由调用方 clearMemory()。 */
    private boolean readFrom(Path source) {
        try {
            return parseText(Files.readString(source, StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("读取 " + source.getFileName() + " 失败：" + e);
            return false;
        }
    }

    /** 解析 JSON 文本并填充内存；撤销删除也复用这条路径。 */
    private boolean parseText(String text) {
        try {
            Map<String, Object> root = Json.map(Json.parse(text));
            seq = Json.lng(root, "seq", 1);
            termStart = Json.str(root, "termStart", "");

            for (Object o : Json.list(root.get("courses"))) {
                courses.add(Course.fromMap(Json.map(o)));
            }
            for (Object o : Json.list(root.get("lessons"))) {
                lessons.add(Lesson.fromMap(Json.map(o)));
            }
            for (Object o : Json.list(root.get("exercises"))) {
                exercises.add(Exercise.fromMap(Json.map(o)));
            }
            for (Object o : Json.list(root.get("notes"))) {
                notes.add(Note.fromMap(Json.map(o)));
            }
            for (Object o : Json.list(root.get("tasks"))) {
                tasks.add(Task.fromMap(Json.map(o)));
            }
            for (Object o : Json.list(root.get("plans"))) {
                plans.add(Plan.fromMap(Json.map(o)));
            }
            for (Object o : Json.list(root.get("focus"))) {
                focusRecords.add(FocusRecord.fromMap(Json.map(o)));
            }
            saveState = "已加载本地数据";
            return true;
        } catch (Exception e) {
            System.err.println("数据解析失败：" + e);
            return false;
        }
    }

    private static <T> List<Object> mapList(List<T> src, java.util.function.Function<T, Map<String, Object>> f) {
        List<Object> out = new ArrayList<>(src.size());
        for (T t : src) {
            out.add(f.apply(t));
        }
        return out;
    }

    // ========================= ID =========================

    private long newId() {
        return ++seq;
    }

    // ========================= 课程 =========================

    public List<Course> courses() {
        return Collections.unmodifiableList(courses);
    }

    public Course course(long id) {
        for (Course c : courses) {
            if (c.id == id) {
                return c;
            }
        }
        return null;
    }

    public String courseName(long id) {
        Course c = course(id);
        return c == null ? "未归类" : c.name;
    }

    public Course addCourse(Course c) {
        c.id = newId();
        courses.add(c);
        changed();
        return c;
    }

    public void removeCourse(Course c) {
        rememberUndo("课程《" + c.name + "》");
        courses.remove(c);
        lessons.removeIf(l -> l.courseId == c.id);
        exercises.forEach(e -> {
            if (e.courseId == c.id) {
                e.courseId = 0;
            }
        });
        notes.forEach(n -> {
            if (n.courseId == c.id) {
                n.courseId = 0;
            }
        });
        tasks.forEach(t -> {
            if (t.courseId == c.id) {
                t.courseId = 0;
            }
        });
        changed();
    }

    // ========================= 课表 =========================

    public List<Lesson> lessons() {
        return Collections.unmodifiableList(lessons);
    }

    public List<Lesson> lessonsOn(int weekday) {
        List<Lesson> out = new ArrayList<>();
        for (Lesson l : lessons) {
            if (l.weekday == weekday) {
                out.add(l);
            }
        }
        out.sort((a, b) -> Integer.compare(a.startSlot, b.startSlot));
        return out;
    }

    public List<Lesson> lessonsToday() {
        return lessonsOn(Dates.weekdayOf(LocalDate.now()));
    }

    public Lesson addLesson(Lesson l) {
        l.id = newId();
        lessons.add(l);
        changed();
        return l;
    }

    public void removeLesson(Lesson l) {
        rememberUndo("课时");
        lessons.remove(l);
        changed();
    }

    // ========================= 错题 =========================

    public List<Exercise> exercises() {
        return Collections.unmodifiableList(exercises);
    }

    public Exercise addExercise(Exercise e) {
        e.id = newId();
        if (e.createdAt == null || e.createdAt.isBlank()) {
            e.createdAt = Dates.today();
        }
        if (e.nextReview == null || e.nextReview.isBlank()) {
            e.nextReview = Dates.today();
        }
        exercises.add(e);
        changed();
        return e;
    }

    public void removeExercise(Exercise e) {
        rememberUndo("错题《" + e.title + "》");
        exercises.remove(e);
        changed();
    }

    /**
     * 记录一次复习。
     * remembered=true 按艾宾浩斯间隔推后下次复习日期；
     * false 则退回未掌握、明天再来，并把进度退一格。
     * <p>
     * 注意：答错时不能让 reviewCount 继续增长，否则"错两次再答对"会直接
     * 取到 7 天的间隔（reviewCount 越大间隔越长），刚学会的题会被推得太远。
     */
    public void review(Exercise e, boolean remembered) {
        e.lastReview = Dates.today();
        if (remembered) {
            e.reviewCount++;
            int idx = Math.min(e.reviewCount, REVIEW_INTERVALS.length) - 1;
            e.nextReview = LocalDate.now().plusDays(REVIEW_INTERVALS[idx]).toString();
            e.mastery = e.reviewCount >= 3 ? 2 : 1;
        } else {
            e.lapses++;
            e.reviewCount = Math.max(0, e.reviewCount - 1);
            e.mastery = 0;
            e.nextReview = LocalDate.now().plusDays(1).toString();
        }
        changed();
    }

    public List<Exercise> dueExercises() {
        List<Exercise> out = new ArrayList<>();
        for (Exercise e : exercises) {
            if (e.dueToday()) {
                out.add(e);
            }
        }
        return out;
    }

    // ========================= 笔记 =========================

    public List<Note> notes() {
        return Collections.unmodifiableList(notes);
    }

    public Note addNote(Note n) {
        n.id = newId();
        notes.add(n);
        changed();
        return n;
    }

    public void removeNote(Note n) {
        rememberUndo("笔记《" + n.title + "》");
        notes.remove(n);
        changed();
    }

    // ========================= 任务 =========================

    public List<Task> tasks() {
        return Collections.unmodifiableList(tasks);
    }

    public Task addTask(Task t) {
        t.id = newId();
        if (t.createdAt == null || t.createdAt.isBlank()) {
            t.createdAt = Dates.today();
        }
        tasks.add(t);
        changed();
        return t;
    }

    public void removeTask(Task t) {
        rememberUndo("任务《" + t.title + "》");
        tasks.remove(t);
        changed();
    }

    public void toggleTask(Task t) {
        t.done = !t.done;
        t.doneAt = t.done ? Dates.today() : "";
        changed();
    }

    /** 今日待办：截止今天（含逾期）且未完成，或者没有截止日期但今天创建的。 */
    public List<Task> todayTasks() {
        List<Task> out = new ArrayList<>();
        for (Task t : tasks) {
            if (t.done) {
                continue;
            }
            if (t.due == null || t.due.isBlank()) {
                if (Dates.isToday(t.createdAt)) {
                    out.add(t);
                }
            } else if (Dates.daysFromToday(t.due) <= 0) {
                out.add(t);
            }
        }
        out.sort((a, b) -> Integer.compare(b.priority, a.priority));
        return out;
    }

    public List<Task> openTasks() {
        List<Task> out = new ArrayList<>();
        for (Task t : tasks) {
            if (!t.done) {
                out.add(t);
            }
        }
        return out;
    }

    public List<Task> tasksOfPlan(long planId) {
        List<Task> out = new ArrayList<>();
        for (Task t : tasks) {
            if (t.planId == planId) {
                out.add(t);
            }
        }
        return out;
    }

    // ========================= 计划 =========================

    public List<Plan> plans() {
        return Collections.unmodifiableList(plans);
    }

    public Plan addPlan(Plan p) {
        p.id = newId();
        plans.add(p);
        changed();
        return p;
    }

    public void removePlan(Plan p) {
        rememberUndo("计划《" + p.title + "》");
        plans.remove(p);
        tasks.forEach(t -> {
            if (t.planId == p.id) {
                t.planId = 0;
            }
        });
        changed();
    }

    /** 计划进度 0~100。 */
    public int planProgress(Plan p) {
        List<Task> list = tasksOfPlan(p.id);
        if (list.isEmpty()) {
            return 0;
        }
        int done = 0;
        for (Task t : list) {
            if (t.done) {
                done++;
            }
        }
        return done * 100 / list.size();
    }

    // ========================= 专注记录 =========================

    public void addFocus(int minutes, String label) {
        focusRecords.add(new FocusRecord(Dates.today(), minutes, label));
        changed();
    }

    /** 番茄钟完成一个 25 分钟专注时调用。 */
    public void addPomodoro(int minutes, String label) {
        FocusRecord f = new FocusRecord(Dates.today(), minutes, label);
        f.pomodoro = true;
        focusRecords.add(f);
        changed();
    }

    public int pomodoroToday() {
        int count = 0;
        for (FocusRecord f : focusRecords) {
            if (f.pomodoro && Dates.isToday(f.date)) {
                count++;
            }
        }
        return count;
    }

    public List<FocusRecord> focusRecords() {
        return Collections.unmodifiableList(focusRecords);
    }

    public int focusMinutes(String isoDate) {
        int sum = 0;
        for (FocusRecord f : focusRecords) {
            if (f.date.equals(isoDate)) {
                sum += f.minutes;
            }
        }
        return sum;
    }

    public int todayFocusMinutes() {
        return focusMinutes(Dates.today());
    }

    public int weekFocusMinutes() {
        int sum = 0;
        LocalDate start = Dates.weekStart();
        for (FocusRecord f : focusRecords) {
            LocalDate d = Dates.parse(f.date);
            if (d != null && !d.isBefore(start) && !d.isAfter(start.plusDays(6))) {
                sum += f.minutes;
            }
        }
        return sum;
    }

    // ========================= 学期周次 =========================

    public String termStart() {
        return termStart;
    }

    public void setTermStart(String iso) {
        termStart = iso == null ? "" : iso;
        changed();
    }

    public int currentWeek() {
        LocalDate start = Dates.parse(termStart);
        if (start == null) {
            return 1;
        }
        long days = ChronoUnit.DAYS.between(start, LocalDate.now());
        if (days < 0) {
            return 1;
        }
        return (int) (days / 7) + 1;
    }

    // ========================= 删除撤销 =========================

    /**
     * 每次删除前调用：把整库序列化一份留在内存里。
     * 数据量是"一个人的学习记录"（百 KB 以内），比给每个实体写反向操作简单且不会漏掉级联。
     */
    private void rememberUndo(String label) {
        undoSnapshot = Json.write(toRoot());
        undoLabel = label;
    }

    public boolean canUndoDelete() {
        return undoSnapshot != null;
    }

    public String undoDeleteLabel() {
        return undoLabel;
    }

    /** 恢复删除前的数据；只保留最近一次删除，重复调用无效。 */
    public void undoDelete() {
        String text = undoSnapshot;
        if (text == null) {
            return;
        }
        undoSnapshot = null;
        clearMemory();
        if (!parseText(text)) {
            saveState = "撤销失败：快照无法解析";
            fireChanged();
            return;
        }
        save();
        fireChanged();
    }

    // ========================= 维护操作 =========================

    private void clearMemory() {
        courses.clear();
        lessons.clear();
        exercises.clear();
        notes.clear();
        tasks.clear();
        plans.clear();
        focusRecords.clear();
        seq = 1;
        // 清空/重置后不能再撤销回旧数据
        undoSnapshot = null;
    }

    public void clearAll() {
        clearMemory();
        changed();
    }

    public void restoreSamples() {
        clearMemory();
        seed();
        changed();
    }

    /** 把数据导出成三份 CSV 到指定目录，返回实际写入的目录。 */
    public Path exportCsv(Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        Files.writeString(targetDir.resolve("tasks.csv"), csvTasks(), StandardCharsets.UTF_8);
        Files.writeString(targetDir.resolve("exercises.csv"), csvExercises(), StandardCharsets.UTF_8);
        Files.writeString(targetDir.resolve("notes.csv"), csvNotes(), StandardCharsets.UTF_8);
        return targetDir;
    }

    private String csvTasks() {
        StringBuilder sb = new StringBuilder("id,标题,课程,截止日期,优先级,已完成,预计分钟\n");
        for (Task t : tasks) {
            sb.append(t.id).append(',')
                    .append(csv(t.title)).append(',')
                    .append(csv(courseName(t.courseId))).append(',')
                    .append(t.due).append(',')
                    .append(t.priorityLabel()).append(',')
                    .append(t.done ? "是" : "否").append(',')
                    .append(t.estimateMin).append('\n');
        }
        return sb.toString();
    }

    private String csvExercises() {
        StringBuilder sb = new StringBuilder("id,题目,课程,掌握度,复习次数,下次复习\n");
        for (Exercise e : exercises) {
            sb.append(e.id).append(',')
                    .append(csv(e.title)).append(',')
                    .append(csv(courseName(e.courseId))).append(',')
                    .append(e.masteryLabel()).append(',')
                    .append(e.reviewCount).append(',')
                    .append(e.nextReview).append('\n');
        }
        return sb.toString();
    }

    private String csvNotes() {
        StringBuilder sb = new StringBuilder("id,标题,课程,标签,置顶,更新时间\n");
        for (Note n : notes) {
            sb.append(n.id).append(',')
                    .append(csv(n.title)).append(',')
                    .append(csv(courseName(n.courseId))).append(',')
                    .append(csv(n.tags)).append(',')
                    .append(n.pinned ? "是" : "否").append(',')
                    .append(n.updatedAt).append('\n');
        }
        return sb.toString();
    }

    private static String csv(String raw) {
        String s = raw == null ? "" : raw;
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // ========================= 首次运行示例数据 =========================

    private void seed() {
        String today = Dates.today();

        Course java = new Course("Java 程序设计", "王老师", "#2F6FED");
        Course math = new Course("高等数学", "李老师", "#10B981");
        Course english = new Course("大学英语", "Emma", "#F59E0B");
        courses.add(java);
        courses.add(math);
        courses.add(english);
        java.id = newId();
        math.id = newId();
        english.id = newId();

        lessons.add(new Lesson(java.id, 1, 1, 2, "实验楼 A305"));
        lessons.add(new Lesson(math.id, 1, 3, 4, "教三 201"));
        lessons.add(new Lesson(english.id, 2, 1, 2, "外语楼 108"));
        lessons.add(new Lesson(java.id, 3, 3, 4, "实验楼 A305"));
        lessons.add(new Lesson(math.id, 4, 1, 2, "教三 201"));
        lessons.add(new Lesson(english.id, 5, 3, 4, "外语楼 108"));
        for (Lesson l : lessons) {
            l.id = newId();
        }

        Plan pbl = new Plan("StudyDesk 项目 v1", today, Dates.iso(LocalDate.now().plusDays(20)));
        pbl.description = "把界面框架补成能真正记录学习数据的桌面应用";
        pbl.id = newId();
        plans.add(pbl);
        Plan final_ = new Plan("期末复习（高数）", today, Dates.iso(LocalDate.now().plusDays(35)));
        final_.description = "按章节刷题，错题全部进入复习队列";
        final_.id = newId();
        plans.add(final_);

        Task t1 = new Task("实现课表周视图与课时编辑");
        t1.courseId = java.id;
        t1.planId = pbl.id;
        t1.priority = 2;
        t1.due = today;
        t1.estimateMin = 90;

        Task t2 = new Task("高数 3.2 节习题 10 道");
        t2.courseId = math.id;
        t2.planId = final_.id;
        t2.priority = 1;
        t2.due = today;
        t2.estimateMin = 60;

        Task t3 = new Task("背 50 个四六级单词");
        t3.courseId = english.id;
        t3.priority = 0;
        t3.due = Dates.iso(LocalDate.now().plusDays(1));

        Task t4 = new Task("整理本周错题并标注错因");
        t4.courseId = java.id;
        t4.priority = 2;
        t4.due = Dates.iso(LocalDate.now().minusDays(1));

        Task t5 = new Task("写项目 README 的架构说明");
        t5.courseId = java.id;
        t5.planId = pbl.id;
        t5.priority = 1;
        t5.due = Dates.iso(LocalDate.now().plusDays(3));
        t5.done = true;
        t5.doneAt = today;

        tasks.add(t1);
        tasks.add(t2);
        tasks.add(t3);
        tasks.add(t4);
        tasks.add(t5);
        for (Task t : tasks) {
            t.id = newId();
            if (t.createdAt == null || t.createdAt.isBlank()) {
                t.createdAt = today;
            }
        }

        Exercise e1 = new Exercise("String 与 StringBuilder 的区别", java.id);
        e1.content = "为什么循环里拼接字符串不推荐直接用 String？";
        e1.answer = "String 不可变，每次 + 都会新建对象；StringBuilder 在原对象上追加，"
                + "循环场景下应使用 StringBuilder 避免大量临时对象。";
        e1.reason = "把 String 当成可变对象，忽略了常量池与对象创建开销";
        e1.mastery = 1;
        e1.reviewCount = 1;
        e1.nextReview = today;

        Exercise e2 = new Exercise("求极限 lim(x→0) sin3x / x", math.id);
        e2.content = "用等价无穷小求极限。";
        e2.answer = "sin3x ~ 3x，所以原式 = 3。";
        e2.reason = "等价无穷小替换时漏乘系数 3";
        e2.lapses = 1;
        e2.mastery = 0;
        e2.nextReview = today;

        Exercise e3 = new Exercise("虚拟语气中的倒装结构", english.id);
        e3.content = "Were I you, I would take the offer. 属于哪种虚拟语气？";
        e3.answer = "与现在事实相反的虚拟条件句，省略 if 后 were 提前形成倒装。";
        e3.reason = "省略 if 后忘记把 were / had / should 提前";
        e3.mastery = 2;
        e3.reviewCount = 4;
        e3.lastReview = Dates.iso(LocalDate.now().minusDays(2));
        e3.nextReview = Dates.iso(LocalDate.now().plusDays(7));

        exercises.add(e1);
        exercises.add(e2);
        exercises.add(e3);
        for (Exercise e : exercises) {
            e.id = newId();
            e.createdAt = today;
        }

        Note n1 = new Note("Java 集合框架速记", "List 有序可重复：ArrayList 查快改慢，LinkedList 改快查慢。\n"
                + "Set 不可重复：HashSet 无序，TreeSet 按自然顺序。\n"
                + "Map 键值对：HashMap 允许一个 null 键，TreeMap 按键排序。", java.id);
        n1.tags = "集合,面试";
        n1.pinned = true;

        Note n2 = new Note("课表布局的坐标映射", "课表用 8 列 × 10 行的网格：第 0 列是节次，1~7 列是周一到周日。\n"
                + "Lesson 存 weekday 与 startSlot/endSlot，渲染时直接按坐标落到对应格子，"
                + "跨节的课用 GridBagLayout 的 gridheight 占多行。", java.id);
        n2.tags = "Swing,布局";

        Note n3 = new Note("高数第三章易错点", "1. 等价无穷小只能在乘除中替换，加减要慎用。\n"
                + "2. 洛必达前先确认是 0/0 或 ∞/∞。\n3. 变限积分求导记得乘以上限的导数。", math.id);
        n3.tags = "高数,极限";

        notes.add(n1);
        notes.add(n2);
        notes.add(n3);
        for (Note n : notes) {
            n.id = newId();
            n.createdAt = today;
            n.updatedAt = today;
        }

        focusRecords.add(new FocusRecord(Dates.iso(LocalDate.now().minusDays(1)), 50, "错题复盘"));
        focusRecords.add(new FocusRecord(today, 25, "课堂笔记整理"));

        termStart = Dates.iso(Dates.weekStart().minusWeeks(2));
    }
}
