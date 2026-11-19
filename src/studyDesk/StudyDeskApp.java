package studyDesk;

import javax.swing.*;

/** 程序入口。 */
public final class StudyDeskApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // 保持默认 L&F 即可，不影响功能
            }
            new MainFrame().setVisible(true);
        });
    }
}
