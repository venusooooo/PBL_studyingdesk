package studyDesk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 错题 / 习题。
 * mastery 表示掌握度：0 未掌握、1 复习中、2 已掌握。
 * nextReview 是下次复习日期（艾宾浩斯间隔），由 Store.review() 推进。
 */
public final class Exercise {
    public long id;
    public long courseId;
    public String title = "";
    /** 题干。 */
    public String content = "";
    /** 正确答案 / 解析。 */
    public String answer = "";
    /** 错因或知识点，用于复盘时提醒自己。 */
    public String reason = "";
    public int mastery;
    public int reviewCount;
    public String nextReview = "";
    public String createdAt = "";
    public String lastReview = "";

    public Exercise() {
    }

    public Exercise(String title, long courseId) {
        this.title = title;
        this.courseId = courseId;
        this.createdAt = Dates.today();
        this.nextReview = Dates.today();
    }

    public boolean mastered() {
        return mastery >= 2;
    }

    public boolean dueToday() {
        if (mastered()) {
            return false;
        }
        if (nextReview == null || nextReview.isBlank()) {
            return true;
        }
        return Dates.daysFromToday(nextReview) <= 0;
    }

    public String masteryLabel() {
        switch (mastery) {
            case 0:
                return "未掌握";
            case 1:
                return "复习中";
            default:
                return "已掌握";
        }
    }

    Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("courseId", courseId);
        m.put("title", title);
        m.put("content", content);
        m.put("answer", answer);
        m.put("reason", reason);
        m.put("mastery", mastery);
        m.put("reviewCount", reviewCount);
        m.put("nextReview", nextReview);
        m.put("createdAt", createdAt);
        m.put("lastReview", lastReview);
        return m;
    }

    static Exercise fromMap(Map<String, Object> m) {
        Exercise e = new Exercise();
        e.id = Json.lng(m, "id", 0);
        e.courseId = Json.lng(m, "courseId", 0);
        e.title = Json.str(m, "title", "未命名习题");
        e.content = Json.str(m, "content", "");
        e.answer = Json.str(m, "answer", "");
        e.reason = Json.str(m, "reason", "");
        e.mastery = Json.integer(m, "mastery", 0);
        e.reviewCount = Json.integer(m, "reviewCount", 0);
        e.nextReview = Json.str(m, "nextReview", "");
        e.createdAt = Json.str(m, "createdAt", "");
        e.lastReview = Json.str(m, "lastReview", "");
        return e;
    }
}
