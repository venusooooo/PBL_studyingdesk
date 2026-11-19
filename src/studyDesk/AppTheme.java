package studyDesk;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * 全局样式规范：8px 栅格、16px 卡片间距、24px 页面内边距。
 * 所有页面统一使用这里的常量，便于后续做主题切换（功能 9）。
 */
public final class AppTheme {
    public static final int GRID = 8;
    public static final int CARD_GAP = 16;
    public static final int PAGE_PADDING = 24;

    public static final Color BG = new Color(0xF5, 0xF7, 0xFA);
    public static final Color SURFACE = Color.WHITE;
    public static final Color PRIMARY = new Color(0x2F, 0x6F, 0xED);
    public static final Color TEXT = new Color(0x1F, 0x29, 0x37);
    public static final Color TEXT_SECONDARY = new Color(0x6B, 0x72, 0x80);
    public static final Color BORDER = new Color(0xE5, 0xE7, 0xEB);
    public static final Color NAV_ACTIVE = new Color(0xE8, 0xF0, 0xFE);

    public static final Font FONT_TITLE = new Font("Microsoft YaHei", Font.BOLD, 20);
    public static final Font FONT_BODY = new Font("Microsoft YaHei", Font.PLAIN, 14);
    public static final Font FONT_SMALL = new Font("Microsoft YaHei", Font.PLAIN, 12);

    private AppTheme() {
    }

    /** 统一卡片边框：1px 描边 + 内边距。 */
    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(GRID * 2, GRID * 2, GRID * 2, GRID * 2));
    }

    /** 创建一张标准卡片，后续业务页面可直接复用。 */
    public static JPanel createCard(String title, String description) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(SURFACE);
        card.setBorder(cardBorder());

        JLabel t = new JLabel(title);
        t.setFont(FONT_TITLE);
        t.setForeground(TEXT);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel d = new JLabel(description);
        d.setFont(FONT_SMALL);
        d.setForeground(TEXT_SECONDARY);
        d.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(t);
        card.add(Box.createVerticalStrut(GRID));
        card.add(d);
        return card;
    }
}
