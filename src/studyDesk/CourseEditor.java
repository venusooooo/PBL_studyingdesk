package studyDesk;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * 课程编辑弹窗的统一实现。
 * 「课程与课时」页和「课表」页都要新建课程，抽到这里，保证两处字段、校验、配色完全一致。
 */
final class CourseEditor {

    /** 课程主题色，与课表格子、标签颜色共用同一套。 */
    static final String[] PALETTE = {"#2F6FED", "#10B981", "#F59E0B", "#8B5CF6",
            "#EC4899", "#06B6D4", "#EF4444", "#84CC16"};

    private static final String[] COLOR_NAMES = {"蓝色", "绿色", "橙色", "紫色",
            "粉色", "青色", "红色", "青柠"};

    private CourseEditor() {
    }

    /**
     * 打开课程弹窗。course 为 null 表示新建。
     *
     * @return 保存成功返回课程对象；用户取消返回 null
     */
    static Course open(Component parent, Course course) {
        Frame owner = ownerOf(parent);
        boolean creating = course == null;
        String preName = creating ? "" : course.name;
        String preTeacher = creating ? "" : course.teacher;
        String preNote = creating ? "" : course.note;
        int preColor = paletteIndex(course, creating);

        while (true) {
            FormDialog dialog = new FormDialog(owner, creating ? "新建课程" : "编辑课程", "保存");
            JTextField name = dialog.addText("课程名", preName);
            JTextField teacher = dialog.addText("任课教师", preTeacher);
            JTextArea note = dialog.addArea("备注", preNote, 3);
            JComboBox<String> color = dialog.addCombo("主题色", COLOR_NAMES, preColor);

            if (!dialog.open(parent)) {
                return null;
            }

            String trimmed = name.getText().trim();
            if (trimmed.isEmpty()) {
                // 不静默丢弃用户输入：提示后带着已填内容重新打开。
                FormDialog.toast(parent, "课程名不能为空");
                preName = "";
                preTeacher = teacher.getText().trim();
                preNote = note.getText();
                preColor = clamp(color.getSelectedIndex());
                continue;
            }

            String hex = PALETTE[clamp(color.getSelectedIndex())];
            if (creating) {
                Course created = new Course(trimmed, teacher.getText().trim(), hex);
                created.note = note.getText().trim();
                Store.get().addCourse(created);
                return created;
            }
            course.name = trimmed;
            course.teacher = teacher.getText().trim();
            course.color = hex;
            course.note = note.getText().trim();
            Store.get().changed();
            return course;
        }
    }

    private static int paletteIndex(Course course, boolean creating) {
        if (creating || course == null) {
            return clamp(Store.get().courses().size());
        }
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i].equalsIgnoreCase(course.color)) {
                return i;
            }
        }
        return 0;
    }

    private static int clamp(int index) {
        return Math.max(0, Math.min(PALETTE.length - 1, index));
    }

    private static Frame ownerOf(Component parent) {
        if (parent instanceof Frame) {
            return (Frame) parent;
        }
        Window w = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        return w instanceof Frame ? (Frame) w : null;
    }
}
