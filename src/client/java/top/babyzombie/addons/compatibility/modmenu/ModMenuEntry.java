package top.babyzombie.addons.compatibility.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import top.babyzombie.addons.config.ModConfigManager;

/**
 * ModMenu 集成。
 * 更新检查交给 ModMenu 内置的 Modrinth 检查（按 mod id 查项目 slug），不再提供自定义 checker。
 */
public class ModMenuEntry implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModConfigManager::createGUI;
    }
}
