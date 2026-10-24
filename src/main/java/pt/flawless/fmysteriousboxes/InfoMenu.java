package pt.flawless.fmysteriousboxes;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pt.flawless.fapi.enums.FInventorySize;
import pt.flawless.fapi.inventory.FInventory;
import pt.flawless.fapi.items.FItemStack;
import pt.flawless.fapi.items.ItemsApi;
import pt.flawless.fmysteriousboxes.Box.Prize;

/** Menu só de leitura com os prémios de uma caixa: moldura de vidro cinzento, a caixa no meio da linha de cima. */
public class InfoMenu implements InventoryHolder {

    /** Slots interiores do baú de 54 (4 linhas de 7). */
    private static final int MAX_PRIZES = 28;

    private final Inventory inventory;
    private final int hidden;

    public InfoMenu(Box box, ItemsApi items) {
        inventory = new FInventory(this, FInventorySize.SIZE_54,
                ChatColor.translateAlternateColorCodes('&', box.title())).getInventory();
        ItemStack gray = new FItemStack(Material.GRAY_STAINED_GLASS_PANE, 1, " ").createItem();
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, gray);
            }
        }
        items.create(box.itemId()).ifPresent(item -> inventory.setItem(4, item));

        int total = box.prizes().stream().mapToInt(Prize::weight).sum();
        int slot = 10;
        int shown = 0;
        for (Prize prize : box.prizes()) {
            if (shown == MAX_PRIZES) {
                break;
            }
            ItemStack item = prize.create(items).orElse(null);
            if (item == null) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(String.format("§7Chance: §e%.2f%% §7(peso %d)", prize.weight() * 100.0 / total, prize.weight()));
            meta.setLore(lore);
            item.setItemMeta(meta);
            while (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
            }
            inventory.setItem(slot++, item);
            shown++;
        }
        hidden = box.prizes().size() - shown;
    }

    /** Prémios que não couberam no menu. */
    public int hidden() {
        return hidden;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
