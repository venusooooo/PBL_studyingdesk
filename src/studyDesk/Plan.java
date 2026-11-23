package studyDesk;

import java.util.LinkedHashMap;
import java.util.Map;

/** 学习计划（阶段目标）。进度不落库，按关联任务的完成比例实时计算。 */
public final class Plan {
    public long id;
    public String title = "";
    public String description = "";
    public String startDate = "";
    public String endDate = "";

    public Plan() {
    }

    public Plan(String title, String startDate, String endDate) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("title", title);
        m.put("description", description);
        m.put("startDate", startDate);
        m.put("endDate", endDate);
        return m;
    }

    static Plan fromMap(Map<String, Object> m) {
        Plan p = new Plan();
        p.id = Json.lng(m, "id", 0);
        p.title = Json.str(m, "title", "未命名计划");
        p.description = Json.str(m, "description", "");
        p.startDate = Json.str(m, "startDate", "");
        p.endDate = Json.str(m, "endDate", "");
        return p;
    }
}
