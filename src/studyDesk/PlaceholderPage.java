package studyDesk;

/** 后续功能的页面占位：框架阶段保证导航可达，业务逻辑留给对应功能迭代。 */
public final class PlaceholderPage extends BasePage {
    private final PageId id;

    public PlaceholderPage(PageId id) {
        this.id = id;
    }

    @Override
    public PageId getId() {
        return id;
    }

    @Override
    protected void buildContent() {
        setHeaderHint("该页面由界面框架预留，具体功能将在后续迭代中实现。");
        addCard(AppTheme.createCard(id.getLabel(), "功能待实现 · StudyDesk 框架占位"), 0, 0);
    }
}
