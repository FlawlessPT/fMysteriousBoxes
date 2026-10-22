package pt.flawless.fmysteriousboxes;

import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import pt.flawless.fapi.enums.FInventorySize;
import pt.flawless.fapi.inventory.FInventory;
import pt.flawless.fapi.items.FItemStack;
import pt.flawless.fapi.sounds.FSound;

/** Roleta de abertura: anima a faixa e entrega o prémio exatamente uma vez (fim, fecho, saída ou desligar). */
public class Roulette implements InventoryHolder {

    private static final int FIRST_SLOT = 9;
    private static final int WINDOW = 9;

    private final Plugin plugin;
    private final Player player;
    private final ItemStack prize;
    private final String prizeId;
    private final List<ItemStack> strip;
    private final List<Integer> intervals;
    private final int finalPause;
    private final Runnable onDone;
    private final Inventory inventory;

    private BukkitTask task;
    private boolean done;

    public Roulette(Plugin plugin, Player player, Box box, ItemStack prize, String prizeId, List<ItemStack> strip,
                    List<Integer> intervals, int finalPause, Runnable onDone) {
        this.plugin = plugin;
        this.player = player;
        this.prize = prize;
        this.prizeId = prizeId;
        this.strip = strip;
        this.intervals = intervals;
        this.finalPause = finalPause;
        this.onDone = onDone;

        this.inventory = new FInventory(this, FInventorySize.SIZE_27,
                ChatColor.translateAlternateColorCodes('&', box.title())).getInventory();
        ItemStack gray = new FItemStack(Material.STAINED_GLASS_PANE, 1, " ").setDurability((short) 7).createItem();
        ItemStack marker = new FItemStack(Material.STAINED_GLASS_PANE, 1, " ").setDurability((short) 4).createItem();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, gray);
        }
        inventory.setItem(4, marker);
        inventory.setItem(22, marker);
        showWindow(0);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void showWindow(int offset) {
        for (int i = 0; i < WINDOW; i++) {
            inventory.setItem(FIRST_SLOT + i, strip.get(offset + i));
        }
    }

    public void start() {
        player.openInventory(inventory);
        schedule(0);
    }

    private void schedule(int step) {
        task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (done) {
                return;
            }
            showWindow(step + 1);
            FSound.sendSound(player, Sound.CLICK);
            if (step + 1 < intervals.size()) {
                schedule(step + 1);
            } else {
                FSound.success(player);
                task = Bukkit.getScheduler().runTaskLater(plugin, this::finish, finalPause);
            }
        }, intervals.get(step));
    }

    private void finish() {
        if (deliver()) {
            // já marcada como concluída: o InventoryCloseEvent seguinte chama abort() e não faz nada
            player.closeInventory();
        }
    }

    /** Entrega o prémio na hora, cancela a tarefa; não faz nada se já concluída. */
    public void abort() {
        deliver();
    }

    private boolean deliver() {
        if (done) {
            return false;
        }
        done = true;
        if (task != null) {
            task.cancel();
        }
        if (player.isDead()) {
            // o inventário de um jogador morto já foi limpo e não é copiado ao reaparecer
            player.getWorld().dropItemNaturally(player.getLocation(), prize.clone());
        } else {
            Map<Integer, ItemStack> left = player.getInventory().addItem(prize.clone());
            for (ItemStack rest : left.values()) {
                player.getWorld().dropItem(player.getLocation(), rest);
            }
        }
        player.sendMessage("§aGanhaste: §f" + prize.getAmount() + "x " + name(prize));
        onDone.run();
        return true;
    }

    /** Nome do item; sem nome próprio usa o id do fCustomItems em vez do material. */
    private String name(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return prizeId;
    }
}
