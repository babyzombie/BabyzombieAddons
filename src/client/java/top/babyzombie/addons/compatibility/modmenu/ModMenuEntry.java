package top.babyzombie.addons.compatibility.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.UpdateChecker;
import top.babyzombie.addons.compatibility.modmenu.updatechecker.GitHubUpdateChecker;
import top.babyzombie.addons.config.ModConfigManager;

public class ModMenuEntry implements ModMenuApi {
    private static final String MOD_ID = "babyzombieaddons";

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModConfigManager::createGUI;
    }

    @Override
    public UpdateChecker getUpdateChecker() {
        return new GitHubUpdateChecker(MOD_ID);
    }
}
