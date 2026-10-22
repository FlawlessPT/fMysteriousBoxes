package pt.flawless.fmysteriousboxes;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pt.flawless.fapi.enums.FInventorySize;
import pt.flawless.fapi.inventory.FInventory;
import pt.flawless.fapi.items.FItemStack;
import pt.flawless.fapi.items.ItemsApi;

/** Menu só de leitura com as caixas (até 7, no centro); botão direito abre o preview. */
public class ListMenu implements InventoryHolder {

    private static final int FIRST_SLOT = 10;
    private static final int MAX_BOXES = 7;

    private final Inventory inventory;
    private final Map<Integer, Box> boxes = new HashMap<>();
    private final int hidden;

    public ListMenu(BoxRegistry registry, ItemsApi items) {
        inventory = new FInventory(this, FInventorySize.SIZE_27, "§8Caixas").getInventory();
        ItemStack gray = new FItemStack(Material.STAINED_GLASS_PANE, 1, " ").setDurability((short) 7).createItem();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, gray);
        }
        int slot = FIRST_SLOT;
        for (Box box : registry.all()) {
            if (boxes.size() == MAX_BOXES) {
                break;
            }
            ItemStack item = items.create(box.itemId()).orElse(null);
            if (item == null) {
                continue;
            }
            inventory.setItem(slot, Box.addLore(item, "§7Clica com o botão direito para previsualizar"));
            boxes.put(slot++, box);
        }
        hidden = registry.all().size() - boxes.size();
    }

    /** Caixa mostrada neste slot, ou nulo. */
    public Box boxAt(int slot) {
        return boxes.get(slot);
    }

    /** Caixas que não couberam no menu. */
    public int hidden() {
        return hidden;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
