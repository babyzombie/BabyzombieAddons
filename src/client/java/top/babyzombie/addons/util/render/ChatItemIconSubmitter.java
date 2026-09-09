package top.babyzombie.addons.util.render;

/**
 * 聊天物品图标 blit 提交入口（供 GuiRenderStateMixin 在打包字形进聊天节点时调用，
 * 避免跨 mixin 直接引用导致 Sponge "Mixin class cannot be referenced directly"）。
 * 由 {@code GuiRendererMixin} 实现。
 */
public interface ChatItemIconSubmitter {

    /** 在聊天文本所在节点烘烤并提交该图标的 blit。 */
    void submitChatIconBlit(ChatItemIconRenderable icon);
}