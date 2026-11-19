package studyDesk;

/** 一级导航对应的主页面标识。 */
public enum PageId {
    TODAY("今日面板"),
    SCHEDULE("课表管理"),
    COURSES("课程与课时"),
    EXERCISES("习题与错题"),
    NOTES("学习笔记"),
    PLANS("学习计划与任务"),
    SETTINGS("系统设置");

    private final String label;

    PageId(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
