package studyDesk;

import java.util.LinkedHashMap;
import java.util.Map;

/** 一节课：属于某门课程，占课表上的一个连续时间块。 */
public final class Lesson {
    public long id;
    public long courseId;
    /** 1 = 周一 ... 7 = 周日。 */
    public int weekday = 1;
    /** 第几节开始 / 结束，与课表左侧节次行对应。 */
    public int startSlot = 1;
    public int endSlot = 1;
    public String room = "";
    /** 上课周次：ALL 每周、ODD 单周、EVEN 双周。 */
    public String weeks = "ALL";

    public Lesson() {
    }

    public Lesson(long courseId, int weekday, int startSlot, int endSlot, String room) {
        this.courseId = courseId;
        this.weekday = weekday;
        this.startSlot = startSlot;
        this.endSlot = endSlot;
        this.room = room;
    }

    /** 这门课在第 week 周（从 1 开始）是否要上。 */
    public boolean occursIn(int week) {
        if ("ODD".equals(weeks)) {
            return week % 2 == 1;
        }
        if ("EVEN".equals(weeks)) {
            return week % 2 == 0;
        }
        return true;
    }

    public String weeksLabel() {
        switch (weeks == null ? "ALL" : weeks) {
            case "ODD":
                return "单周";
            case "EVEN":
                return "双周";
            default:
                return "每周";
        }
    }

    /** 两个课时的周次是否有交集，用于冲突检测。 */
    public static boolean weeksOverlap(String a, String b) {
        String left = a == null ? "ALL" : a;
        String right = b == null ? "ALL" : b;
        return "ALL".equals(left) || "ALL".equals(right) || left.equals(right);
    }

    Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("courseId", courseId);
        m.put("weekday", weekday);
        m.put("startSlot", startSlot);
        m.put("endSlot", endSlot);
        m.put("room", room);
        m.put("weeks", weeks);
        return m;
    }

    static Lesson fromMap(Map<String, Object> m) {
        Lesson l = new Lesson();
        l.id = Json.lng(m, "id", 0);
        l.courseId = Json.lng(m, "courseId", 0);
        l.weekday = Json.integer(m, "weekday", 1);
        l.startSlot = Json.integer(m, "startSlot", 1);
        l.endSlot = Json.integer(m, "endSlot", l.startSlot);
        l.room = Json.str(m, "room", "");
        l.weeks = Json.str(m, "weeks", "ALL");
        return l;
    }
}
