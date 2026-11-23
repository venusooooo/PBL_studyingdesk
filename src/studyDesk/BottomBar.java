package studyDesk;

import java.awt.*;
import javax.swing.*;

/** 底部状态栏：本地运行模式、数据保存状态、AI 服务状态占位。 */
public final class BottomBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel status = new JLabel("本地运行模式");
    private final JLabel save = new JLabel("数据保存：未变更");
    private final JLabel ollama = new JLabel("AI 服务：未启用");

    public BottomBar() {
        setLayout(new BorderLayout(AppTheme.GRID * 2, 0));
        setBackground(AppTheme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(
                AppTheme.GRID / 2, AppTheme.PAGE_PADDING,
                AppTheme.GRID / 2, AppTheme.PAGE_PADDING));

        Font font = AppTheme.FONT_SMALL;
        Color secondary = AppTheme.TEXT_SECONDARY;
        status.setFont(font);
        status.setForeground(secondary);
        save.setFont(font);
        save.setForeground(secondary);
        ollama.setFont(font);
        ollama.setForeground(secondary);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.GRID * 2, 0));
        left.setOpaque(false);
        left.add(status);
        left.add(save);
        add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(ollama);
        add(right, BorderLayout.EAST);
    }

    public void setStatus(String text) {
        status.setText("本地运行模式 · " + text);
    }

    public void setSaveState(String text) {
        save.setText("数据保存：" + text);
    }
}
