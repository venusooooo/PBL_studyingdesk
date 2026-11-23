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

    public Lesson() {
    }

    public Lesson(long courseId, int weekday, int startSlot, int endSlot, String room) {
        this.courseId = courseId;
        this.weekday = weekday;
        this.startSlot = startSlot;
        this.endSlot = endSlot;
        this.room = room;
    }

    Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("courseId", courseId);
        m.put("weekday", weekday);
        m.put("startSlot", startSlot);
        m.put("endSlot", endSlot);
        m.put("room", room);
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
        return l;
    }
}
