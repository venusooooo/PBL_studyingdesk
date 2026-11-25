package studyDesk;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.*;

/**
 * 页面基类，固定"页头 + 卡片网格内容区"结构。
 * 内容区交给 JScrollPane 独立滚动，页头与状态栏保持固定。
 */
public abstract class BasePage implements Page {
    private final JPanel root = new JPanel(new BorderLayout());
    private final JPanel cardGrid = new JPanel();
    private final JLabel headerTitle = new JLabel();
    private final JLabel headerHint = new JLabel();
    /** 待排版的卡片，按行号归组；buildContent 结束后统一装进 cardGrid。 */
    private final Map<Integer, List<CardSlot>> pending = new TreeMap<>();
    private PageContext context;
    private boolean built;

    /** 一张卡片在网格里的位置。 */
    private static final class CardSlot {
        final JComponent card;
        final int col;
        final int span;

        CardSlot(JComponent card, int col, int span) {
            this.card = card;
            this.col = col;
            this.span = span;
        }
    }

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
        // 行与行纵向堆叠、各自保持自然高度：字段多的页面靠滚动查看，而不是被压扁。
        cardGrid.setLayout(new BoxLayout(cardGrid, BoxLayout.Y_AXIS));
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

    /** 弹窗需要一个 owner 窗口，这里统一取主窗口。 */
    protected final Frame owner() {
        Window w = SwingUtilities.getWindowAncestor(getView());
        return w instanceof Frame ? (Frame) w : null;
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
        addCard(card, row, col, 1);
    }

    /** 指定跨列数，用于需要横跨整行（如统计条）的卡片。 */
    protected final void addCard(JComponent card, int row, int col, int colSpan) {
        pending.computeIfAbsent(row, key -> new ArrayList<>())
                .add(new CardSlot(card, Math.max(0, col), Math.max(1, colSpan)));
    }

    /**
     * 把收集到的卡片按行装进内容区。
     * 每行用 GridLayout 平分宽度、行高由内容决定。这里刻意不用 GridBagLayout：
     * 它在空间不足时会把卡片压成几像素高（图表和课表都被压没过）。
     */
    private void materialize() {
        cardGrid.removeAll();
        for (List<CardSlot> rowCards : pending.values()) {
            rowCards.sort(Comparator.comparingInt(slot -> slot.col));
            JPanel row = new JPanel(new GridLayout(1, rowCards.size(), AppTheme.CARD_GAP, 0));
            row.setOpaque(false);
            for (CardSlot slot : rowCards) {
                row.add(slot.card);
            }
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            // 行高只由内容决定：既不被拉高，也不允许 BoxLayout 在空间不足时压扁行。
            Dimension rowHeight = new Dimension(0, row.getPreferredSize().height);
            row.setMinimumSize(rowHeight);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowHeight.height));
            if (cardGrid.getComponentCount() > 0) {
                cardGrid.add(Box.createVerticalStrut(AppTheme.CARD_GAP));
            }
            cardGrid.add(row);
        }
        pending.clear();
    }

    /** 子类在此构建自己的卡片内容。会被反复调用，因此只创建组件、不写一次性状态。 */
    protected abstract void buildContent();

    /** 子类可覆写，用于处理带参跳转（例如从今日面板跳到错题页并带上筛选条件）。 */
    protected void onShowPage(Map<String, Object> params) {
    }

    /** 子类覆写后，在主窗口按 N 即可直接打开该页的"新建"弹窗。 */
    protected void onNewAction() {
    }

    @Override
    public final void onNew() {
        onNewAction();
    }

    @Override
    public final void onShow(Map<String, Object> params) {
        if (!built) {
            buildContent();
            materialize();
            built = true;
        } else {
            // 数据可能在别的页面被改过，每次进入都重画一遍，保证看到的是最新数据。
            rebuild();
        }
        headerTitle.setText(getTitle());
        onShowPage(params == null ? new HashMap<>() : params);
    }

    /** 数据变化后由页面自己调用，重画卡片区。 */
    protected final void refresh() {
        if (built) {
            rebuild();
        }
    }

    private void rebuild() {
        pending.clear();
        cardGrid.removeAll();
        buildContent();
        materialize();
        cardGrid.revalidate();
        cardGrid.repaint();
        root.revalidate();
        root.repaint();
    }
}
