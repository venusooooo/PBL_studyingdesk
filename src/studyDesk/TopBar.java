package studyDesk;

import java.awt.*;
import javax.swing.*;

/** 顶部状态栏：页面标题、当前周次、AI 开关（本阶段为禁用占位）。 */
public final class TopBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel title = new JLabel();
    private final JLabel week = new JLabel("第 1 周");
    private final JToggleButton aiSwitch = new JToggleButton("AI 关闭");

    public TopBar() {
        setLayout(new BorderLayout(AppTheme.GRID, 0));
        setBackground(AppTheme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(
                AppTheme.GRID, AppTheme.PAGE_PADDING, AppTheme.GRID, AppTheme.PAGE_PADDING));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, AppTheme.GRID, 0));
        left.setOpaque(false);
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.TEXT);
        left.add(title);
        add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, AppTheme.GRID, 0));
        right.setOpaque(false);
        week.setFont(AppTheme.FONT_BODY);
        week.setForeground(AppTheme.TEXT_SECONDARY);
        aiSwitch.setFont(AppTheme.FONT_SMALL);
        aiSwitch.setEnabled(false);
        aiSwitch.setMargin(new Insets(3, 12, 3, 12));
        aiSwitch.setPreferredSize(new Dimension(78, 26));
        aiSwitch.setToolTipText("AI 功能不在本阶段范围内");
        right.add(week);
        right.add(aiSwitch);
        add(right, BorderLayout.EAST);
    }

    public void setPageTitle(String text) {
        title.setText(text);
    }

    public void setWeek(int value) {
        week.setText("第 " + value + " 周");
    }
}
