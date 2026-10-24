package pt.flawless.fmysteriousboxes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import pt.flawless.fapi.items.ItemsApi;
import pt.flawless.fmysteriousboxes.Box.Prize;

public class BoxListener implements Listener {

    private final Plugin plugin;
    private final BoxRegistry registry;
    private final ItemsApi items;
    private final Random random = new Random();
    private final Map<UUID, Roulette> active = new HashMap<>();

    public BoxListener(Plugin plugin, BoxRegistry registry, ItemsApi items) {
        this.plugin = plugin;
        this.registry = registry;
        this.items = items;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        // outro plugin (ex.: fAuth antes do login) negou o uso do item; isCancelled() não serve no 1.8 (RIGHT_CLICK_AIR vem cancelado)
        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }
        // placas (lojas do fShop) tratam o clique; a caixa na mão não pode abrir nem cancelar
        if (event.getClickedBlock() != null) {
            Material clicked = event.getClickedBlock().getType();
            if (org.bukkit.Tag.ALL_SIGNS.isTagged(clicked)) {
                return;
            }
        }
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        Optional<Box> found = items.idOf(hand).flatMap(registry::findByItem);
        if (found.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        if (active.containsKey(uuid)) {
            return;
        }
        Box box = found.get();
        if (box.prizes().isEmpty()) {
            player.sendMessage("§cEsta caixa ainda não tem prémios.");
            return;
        }
        List<Integer> intervals = registry.intervals();
        int n = intervals.size();
        ItemStack winner;
        String winnerId;
        List<ItemStack> strip;
        try {
            Prize won = box.pick(random);
            winnerId = won.label();
            winner = box.create(won, items);
            strip = buildStrip(box, winner, n, random);
        } catch (IllegalStateException e) {
            player.sendMessage("§cEsta caixa está indisponível de momento. Avisa um administrador.");
            plugin.getLogger().warning("caixa '" + box.id() + "': " + e.getMessage());
            return;
        }
        // só consome 1 depois de o prémio e a faixa estarem criados
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
            player.getInventory().setItemInMainHand(hand);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        Roulette roulette = new Roulette(plugin, player, box, winner, winnerId, strip, intervals, registry.finalPause(),
                () -> active.remove(uuid));
        active.put(uuid, roulette);
        roulette.start();
    }

    private List<ItemStack> buildStrip(Box box, ItemStack winner, int n, Random random) {
        List<ItemStack> strip = new ArrayList<>(n + 9);
        for (int i = 0; i < n + 9; i++) {
            strip.add(i == n + 4 ? winner : box.create(box.pick(random), items));
        }
        return strip;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof Roulette
                || event.getInventory().getHolder() instanceof InfoMenu
                || event.getInventory().getHolder() instanceof ListMenu) {
            event.setCancelled(true);
        }
        if (event.getInventory().getHolder() instanceof ListMenu menu
                && event.getClick() == ClickType.RIGHT
                && event.getRawSlot() < event.getInventory().getSize()
                && event.getWhoClicked() instanceof Player player) {
            Box box = menu.boxAt(event.getRawSlot());
            if (box != null) {
                // abrir noutro tick: não se troca de inventário a meio do evento de clique
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> player.openInventory(new InfoMenu(box, items).getInventory()));
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Roulette
                || event.getInventory().getHolder() instanceof InfoMenu
                || event.getInventory().getHolder() instanceof ListMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Roulette roulette) {
            roulette.abort();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Roulette roulette = active.get(event.getPlayer().getUniqueId());
        if (roulette != null) {
            roulette.abort();
        }
    }

    /** Entrega todos os prémios pendentes e fecha as roletas (onDisable). */
    public void abortAll() {
        for (Roulette roulette : new ArrayList<>(active.values())) {
            roulette.abort();
            new ArrayList<>(roulette.getInventory().getViewers()).forEach(v -> v.closeInventory());
        }
        active.clear();
    }
}
