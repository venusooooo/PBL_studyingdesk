package studyDesk;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * 页面基类，固定"页头 + 卡片网格内容区"结构。
 * 内容区交给 JScrollPane 独立滚动，页头与状态栏保持固定。
 */
public abstract class BasePage implements Page {
    private final JPanel root = new JPanel(new BorderLayout());
    private final JPanel cardGrid = new JPanel(new GridBagLayout());
    private final JLabel headerTitle = new JLabel();
    private final JLabel headerHint = new JLabel();
    private PageContext context;
    private boolean built;

    protected BasePage() {
        root.setBackground(AppTheme.BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppTheme.SURFACE);
        header.setBorder(BorderFactory.createEmptyBorder(
                AppTheme.PAGE_PADDING, AppTheme.PAGE_PADDING,
                AppTheme.GRID * 2, AppTheme.PAGE_PADDING));
        headerTitle.setFont(AppTheme.FONT_TITLE);
        headerTitle.setForeground(AppTheme.TEXT);
        headerHint.setFont(AppTheme.FONT_SMALL);
        headerHint.setForeground(AppTheme.TEXT_SECONDARY);
        header.add(headerTitle, BorderLayout.NORTH);
        header.add(headerHint, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.setBackground(AppTheme.BG);
        gridWrapper.setBorder(BorderFactory.createEmptyBorder(
                AppTheme.GRID * 2, AppTheme.PAGE_PADDING,
                AppTheme.PAGE_PADDING, AppTheme.PAGE_PADDING));
        cardGrid.setBackground(AppTheme.BG);
        gridWrapper.add(cardGrid, BorderLayout.NORTH);
        root.add(gridWrapper, BorderLayout.CENTER);
    }

    /** 由主窗口在创建页面时绑定，页面获得跳转能力。 */
    public final void bind(PageContext context) {
        this.context = context;
    }

    protected final PageContext context() {
        return context;
    }

    @Override
    public final JComponent getView() {
        return root;
    }

    @Override
    public final String getTitle() {
        return getId().getLabel();
    }

    protected final void setHeaderHint(String text) {
        headerHint.setText(text);
    }

    /** 按行列把卡片放入网格，间距使用全局 16px 规范。 */
    protected final void addCard(JComponent card, int row, int col) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = col;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(AppTheme.CARD_GAP / 2, AppTheme.CARD_GAP / 2,
                AppTheme.CARD_GAP / 2, AppTheme.CARD_GAP / 2);
        cardGrid.add(card, gbc);
    }

    /** 子类在此构建自己的卡片内容，只调用一次。 */
    protected abstract void buildContent();

    @Override
    public final void onShow(Map<String, Object> params) {
        if (!built) {
            buildContent();
            built = true;
        }
        headerTitle.setText(getTitle());
    }
}
