package top.babyzombie.addons.config;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.gui.CloseEventListener;
import io.github.notenoughupdates.moulconfig.gui.GuiContext;
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.managed.ManagedConfig;
import io.github.notenoughupdates.moulconfig.observer.Property;
import io.github.notenoughupdates.moulconfig.managed.ManagedConfigBuilder;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import top.babyzombie.addons.BabyzombieAddonsClient;
import top.babyzombie.addons.config.gui.WideSliderEditor;
import top.babyzombie.addons.module.misc.CopyItemInfoKey;
import top.babyzombie.addons.module.misc.SecondPersonKey;
import top.babyzombie.addons.module.chat.popup.PopupEventsModule;
import top.babyzombie.addons.util.KeyBindingUtil;

import java.io.File;

/**
 * MoulConfig 4.7.2 支持的设置控件一览：
 * ┌──────────────────────────────────────┬──────────────────────────────────┐
 * │ 注解                                 │ 字段类型                          │
 * ├──────────────────────────────────────┼──────────────────────────────────┤
 * │ @ConfigEditorBoolean               │ boolean                          │
 * │ @ConfigEditorButton(buttonText="")  │ Runnable                         │
 * │ @ConfigEditorColour                │ String 或 ChromaColour            │
 * │ @ConfigEditorDraggableList         │ {@code List<T>} (T = enum 或 int) │
 * │ @ConfigEditorDropdown              │ enum 或 int                      │
 * │ @ConfigEditorInfoText(infoTitle="")│ 任意（忽略字段值，只读展示）         │
 * │ @ConfigEditorKeybind(defaultKey=)  │ int (Minecraft key code)          │
 * │ @ConfigEditorSlider(min,max,step)  │ int 或 float                     │
 * │ @ConfigEditorText(forbidden="§")   │ String                           │
 * ├──────────────────────────────────────┼──────────────────────────────────┤
 * │ @ConfigOption(name="key", desc="") │ 设置项名称 / 描述                  │
 * │ @SearchTag("keyword")              │ 搜索关键词                         │
 * │ @Category(name="key", desc="")     │ 顶级分类 (需配合 @Expose)          │
 * │ @Accordion                         │ 折叠组 (需嵌套 POJO 类)            │
 * ├──────────────────────────────────────┼──────────────────────────────────┤
 * │ @ConfigEditorAccordion (已弃用)     │ → 改用 @Accordion                 │
 * │ @ConfigAccordionId      (已弃用)     │ → 改用 @Accordion                 │
 * └──────────────────────────────────────┴──────────────────────────────────┘
 */
public final class ModConfigManager {

    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("babyzombieaddons").resolve("settings.json").toFile();

    private static final ManagedConfig<ModConfig> MANAGED_CONFIG;
    static {
        var builder = new ManagedConfigBuilder<ModConfig>(CONFIG_FILE, ModConfig.class);
        builder.jsonMapper(mapper -> {
            mapper.getGsonBuilder().setPrettyPrinting();
            return kotlin.Unit.INSTANCE;
        });
        // Replace default slider with our wider version (only affects our config)
        builder.<ConfigEditorSlider>customProcessor(ConfigEditorSlider.class,
                (option, ann) -> new WideSliderEditor(option, ann.minValue(), ann.maxValue(), ann.minStep()));
        MANAGED_CONFIG = new ManagedConfig<>(builder);
    }

    static {
        // Auto-save on game shutdown
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> save());
    }

    private static MoulConfigEditor<?> currentEditor;

    private ModConfigManager() {}

    public static void init() {
        MANAGED_CONFIG.reloadFromFile();
        save();
        // Reactively update editor wide mode when toggle changes
        Property<Boolean> wideProp = get().misc.wideMoulConfig;
        wideProp.addObserver((oldVal, newVal) -> {
            if (currentEditor != null) currentEditor.wide = newVal;
        });
    }

    public static ModConfig get() {
        return MANAGED_CONFIG.getInstance();
    }

    public static void save() {
        MANAGED_CONFIG.saveToFile();
    }

    /** 打开设置页面前：从 KeyMapping 读值 → 写入 config 字段，保证 UI 显示最新值 */
    static void syncConfigFromKeyMappings() {
        var cfg = get();
        cfg.general.cancelKeyRelease = KeyBindingUtil.keyCodeFrom(BabyzombieAddonsClient.cancelKeyBindingRelease);
        cfg.general.secondPerson = KeyBindingUtil.keyCodeFrom(SecondPersonKey.KEY);
        cfg.general.handRender.toggleHandRenderKey = KeyBindingUtil.keyCodeFrom(BabyzombieAddonsClient.toggleHandRenderKey);
        cfg.popup.popupYes = KeyBindingUtil.keyCodeFrom(PopupEventsModule.keyYes);
        cfg.popup.popupNo = KeyBindingUtil.keyCodeFrom(PopupEventsModule.keyNo);
        cfg.misc.copyItemInfo = KeyBindingUtil.keyCodeFrom(CopyItemInfoKey.KEY);
    }

    /** 关闭设置页面后：从 config 字段 → 写入 KeyMapping */
    static void syncConfigToKeyMappings() {
        var cfg = get();
        KeyBindingUtil.syncToKeyMapping(BabyzombieAddonsClient.cancelKeyBindingRelease, cfg.general.cancelKeyRelease);
        KeyBindingUtil.syncToKeyMapping(SecondPersonKey.KEY, cfg.general.secondPerson);
        KeyBindingUtil.syncToKeyMapping(BabyzombieAddonsClient.toggleHandRenderKey, cfg.general.handRender.toggleHandRenderKey);
        KeyBindingUtil.syncToKeyMapping(PopupEventsModule.keyYes, cfg.popup.popupYes);
        KeyBindingUtil.syncToKeyMapping(PopupEventsModule.keyNo, cfg.popup.popupNo);
        KeyBindingUtil.syncToKeyMapping(CopyItemInfoKey.KEY, cfg.misc.copyItemInfo);
    }

    public static Screen createGUI(@Nullable Screen parent) {
        return createGUI(parent, "");
    }

    public static Screen createGUI(@Nullable Screen parent, String search) {
        syncConfigFromKeyMappings();
        MANAGED_CONFIG.rebuildConfigProcessor();
        var editor = MANAGED_CONFIG.getEditor();
        currentEditor = editor;
        editor.wide = get().misc.wideMoulConfig.get();
        var screen = new MoulConfigScreen(editor, parent);
        Minecraft.getInstance().setScreen(screen);
        return screen;
    }

    private static final class MoulConfigScreen extends MoulConfigScreenComponent {
        private final @Nullable Screen parent;

        MoulConfigScreen(MoulConfigEditor<?> editor, @Nullable Screen parent) {
            super(Component.empty(), new GuiContext(new GuiElementComponent(editor)), null);
            this.parent = parent;
        }

        @Override
        public void onClose() {
            if (getGuiContext().onBeforeClose() == CloseEventListener.CloseAction.NO_OBJECTIONS_TO_CLOSE) {
                save();
                syncConfigToKeyMappings();
                Minecraft.getInstance().setScreen(parent);
            }
        }
    }
}
