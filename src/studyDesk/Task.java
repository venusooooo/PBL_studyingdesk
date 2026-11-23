package studyDesk;

import java.util.LinkedHashMap;
import java.util.Map;

/** 学习任务。计划（Plan）通过 planId 聚合任务，进度由任务的完成情况算出。 */
public final class Task {
    public long id;
    public String title = "";
    public long courseId;
    /** 截止日期，ISO 字符串，可为空。 */
    public String due = "";
    /** 0 低 / 1 中 / 2 高。 */
    public int priority = 1;
    public boolean done;
    public String doneAt = "";
    /** 预计耗时（分钟）。 */
    public int estimateMin;
    public long planId;
    public String createdAt = "";

    public Task() {
    }

    public Task(String title) {
        this.title = title;
        this.createdAt = Dates.today();
    }

    public boolean overdue() {
        return !done && Dates.isOverdue(due);
    }

    public String priorityLabel() {
        switch (priority) {
            case 2:
                return "高";
            case 0:
                return "低";
            default:
                return "中";
        }
    }

    Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("title", title);
        m.put("courseId", courseId);
        m.put("due", due);
        m.put("priority", priority);
        m.put("done", done);
        m.put("doneAt", doneAt);
        m.put("estimateMin", estimateMin);
        m.put("planId", planId);
        m.put("createdAt", createdAt);
        return m;
    }

    static Task fromMap(Map<String, Object> m) {
        Task t = new Task();
        t.id = Json.lng(m, "id", 0);
        t.title = Json.str(m, "title", "未命名任务");
        t.courseId = Json.lng(m, "courseId", 0);
        t.due = Json.str(m, "due", "");
        t.priority = Json.integer(m, "priority", 1);
        t.done = Json.bool(m, "done", false);
        t.doneAt = Json.str(m, "doneAt", "");
        t.estimateMin = Json.integer(m, "estimateMin", 0);
        t.planId = Json.lng(m, "planId", 0);
        t.createdAt = Json.str(m, "createdAt", "");
        return t;
    }
}
