package pt.flawless.fmysteriousboxes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        // outro plugin (ex.: fAuth antes do login) negou o uso do item; isCancelled() não serve no 1.8 (RIGHT_CLICK_AIR vem cancelado)
        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack hand = player.getItemInHand();
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
        List<Integer> intervals = registry.intervals();
        int n = intervals.size();
        ItemStack winner;
        List<ItemStack> strip;
        try {
            Prize won = box.pick(random);
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
            player.setItemInHand(hand);
        } else {
            player.setItemInHand(null);
        }
        Roulette roulette = new Roulette(plugin, player, box, winner, strip, intervals, registry.finalPause(),
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
        if (event.getInventory().getHolder() instanceof Roulette) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Roulette) {
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
