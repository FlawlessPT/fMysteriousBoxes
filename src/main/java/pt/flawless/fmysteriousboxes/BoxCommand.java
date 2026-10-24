package pt.flawless.fmysteriousboxes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pt.flawless.fapi.items.ItemsApi;

/** /caixa: só staff (permissão no plugin.yml). Subcomando dar (alias give, fora do help e do tab). */
public class BoxCommand implements CommandExecutor, TabCompleter {

    private static final String USE_GIVE = "/caixa dar <jogador> <caixa> [qtd]";

    private static final String USE_CREATE = "/caixa criar <caixa>";
    private static final String USE_INFO = "/caixa info <caixa>";
    private static final String USE_LIST = "/caixa list";

    private final Plugin plugin;
    private final BoxRegistry registry;
    private final ItemsApi items;

    public BoxCommand(Plugin plugin, BoxRegistry registry, ItemsApi items) {
        this.plugin = plugin;
        this.registry = registry;
        this.items = items;
    }

    private void create(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSó um jogador pode usar este comando.");
            return;
        }
        if (args.length != 2) {
            sender.sendMessage("§cUso: " + USE_CREATE);
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_-]+")) {
            sender.sendMessage("§cId inválido: usa só letras, números, '_' e '-'.");
            return;
        }
        if (registry.find(id).isPresent()) {
            sender.sendMessage("§cJá existe uma caixa '" + id + "'.");
            return;
        }
        String itemId = items.idOf(player.getInventory().getItemInMainHand()).orElse(null);
        if (itemId == null) {
            sender.sendMessage("§cSegura na mão um item do fCustomItems (cria-o antes com /customitem criar).");
            return;
        }
        Box used = registry.findByItem(itemId).orElse(null);
        if (used != null) {
            sender.sendMessage("§cEsse item já é a caixa '" + used.id() + "'.");
            return;
        }
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("caixas." + id + ".item", itemId);
        cfg.set("caixas." + id + ".premios", new ArrayList<>());
        plugin.saveConfig();
        registry.add(new Box(id, itemId, BoxRegistry.defaultTitle(items, itemId, id), List.of()));
        sender.sendMessage("§aCaixa '" + id + "' criada com o item '" + itemId
                + "'. Define os prémios em plugins/fMysteriousBoxes/config.yml e faz /reload.");
    }

    private void list(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSó um jogador pode usar este comando.");
            return;
        }
        if (args.length != 1) {
            sender.sendMessage("§cUso: " + USE_LIST);
            return;
        }
        ListMenu menu = new ListMenu(registry, items);
        player.openInventory(menu.getInventory());
        if (menu.hidden() > 0) {
            sender.sendMessage("§e" + menu.hidden() + " caixa(s) não cabem no menu.");
        }
    }

    private void info(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSó um jogador pode usar este comando.");
            return;
        }
        if (args.length != 2) {
            sender.sendMessage("§cUso: " + USE_INFO);
            return;
        }
        Box box = registry.find(args[1]).orElse(null);
        if (box == null) {
            sender.sendMessage("§cA caixa '" + args[1] + "' não existe.");
            return;
        }
        InfoMenu menu = new InfoMenu(box, items);
        player.openInventory(menu.getInventory());
        if (menu.hidden() > 0) {
            sender.sendMessage("§e" + menu.hidden() + " prémio(s) não cabem no menu.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUso: /caixa help");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> {
                sender.sendMessage("§e" + USE_GIVE + " §7- Dá caixas a um jogador");
                sender.sendMessage("§e" + USE_CREATE + " §7- Cria uma caixa com o item da mão");
                sender.sendMessage("§e" + USE_INFO + " §7- Mostra os prémios de uma caixa");
                sender.sendMessage("§e" + USE_LIST + " §7- Mostra todas as caixas");
                sender.sendMessage("§e/caixa help §7- Mostra esta lista");
            }
            case "dar", "give" -> give(sender, args);
            case "criar", "create" -> create(sender, args);
            case "info" -> info(sender, args);
            case "list", "lista" -> list(sender, args);
            default -> sender.sendMessage("§cSubcomando desconhecido. Usa /caixa help para ver os comandos disponíveis.");
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 3 || args.length > 4) {
            sender.sendMessage("§cUso: " + USE_GIVE);
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cJogador '" + args[1] + "' não está online.");
            return;
        }
        Box box = registry.find(args[2]).orElse(null);
        if (box == null) {
            sender.sendMessage("§cA caixa '" + args[2] + "' não existe.");
            return;
        }
        int qty = 1;
        if (args.length == 4) {
            try {
                qty = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                qty = 0;
            }
            if (qty < 1 || qty > 2304) {
                sender.sendMessage("§cQuantidade inválida: tem de ser um número inteiro entre 1 e 2304.");
                return;
            }
        }
        ItemStack stack = items.create(box.itemId(), qty).orElse(null);
        if (stack == null) {
            sender.sendMessage("§cO item da caixa '" + box.id() + "' não existe no fCustomItems.");
            return;
        }
        Box.addLore(stack, "§7Clica com o botão direito para abrir");
        // addItem devolve o que não coube; larga-se aos pés do jogador para não perder caixas
        target.getInventory().addItem(stack).values()
                .forEach(rest -> target.getWorld().dropItemNaturally(target.getLocation(), rest));
        String title = ChatColor.translateAlternateColorCodes('&', box.title());
        sender.sendMessage("§aDeste " + qty + "x " + title + "§a a " + target.getName() + ".");
        if (sender != target) {
            target.sendMessage("§aRecebeste " + qty + "x " + title + "§a.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            out.addAll(List.of("dar", "criar", "info", "list", "help"));
        } else if (args.length == 2 && isGive(args[0])) {
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        } else if (args.length == 3 && isGive(args[0])) {
            registry.all().forEach(b -> out.add(b.id()));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            registry.all().forEach(b -> out.add(b.id()));
        }
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }

    private static boolean isGive(String s) {
        return s.equalsIgnoreCase("dar") || s.equalsIgnoreCase("give");
    }
}
