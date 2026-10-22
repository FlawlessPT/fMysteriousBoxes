package pt.flawless.fmysteriousboxes;

import org.bukkit.plugin.java.JavaPlugin;
import pt.flawless.fapi.items.Items;
import pt.flawless.fapi.items.ItemsApi;
import pt.flawless.fapi.items.ItemsUnavailableException;

public class FMysteriousBoxes extends JavaPlugin {

    private BoxRegistry registry;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ItemsApi items;
        try {
            items = Items.get();
        } catch (ItemsUnavailableException e) {
            getLogger().severe("não foi possível iniciar as caixas: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        registry = new BoxRegistry(getConfig(), items, getLogger());
        getLogger().info(registry.all().size() + " caixa(s) carregada(s)");
    }

    public BoxRegistry registry() {
        return registry;
    }
}
