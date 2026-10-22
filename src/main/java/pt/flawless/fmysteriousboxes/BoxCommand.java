package pt.flawless.fmysteriousboxes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pt.flawless.fapi.items.ItemsApi;

/** /caixa: só staff (permissão no plugin.yml). Subcomando dar (alias give, fora do help e do tab). */
public class BoxCommand implements CommandExecutor, TabCompleter {

    private static final String USE_GIVE = "/caixa dar <jogador> <caixa> [qtd]";

    private final BoxRegistry registry;
    private final ItemsApi items;

    public BoxCommand(BoxRegistry registry, ItemsApi items) {
        this.registry = registry;
        this.items = items;
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
                sender.sendMessage("§e/caixa help §7- Mostra esta lista");
            }
            case "dar", "give" -> give(sender, args);
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
            if (qty < 1) {
                sender.sendMessage("§cQuantidade inválida: tem de ser um número inteiro maior que 0.");
                return;
            }
        }
        ItemStack stack = items.create(box.itemId(), qty).orElse(null);
        if (stack == null) {
            sender.sendMessage("§cO item da caixa '" + box.id() + "' não existe no fCustomItems.");
            return;
        }
        // addItem devolve o que não coube; larga-se aos pés do jogador para não perder caixas
        target.getInventory().addItem(stack).values()
                .forEach(rest -> target.getWorld().dropItemNaturally(target.getLocation(), rest));
        sender.sendMessage("§aDeste " + qty + "x " + box.title() + "§a a " + target.getName() + ".");
        if (sender != target) {
            target.sendMessage("§aRecebeste " + qty + "x " + box.title() + "§a.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            out.addAll(List.of("dar", "help"));
        } else if (args.length == 2 && isGive(args[0])) {
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        } else if (args.length == 3 && isGive(args[0])) {
            registry.all().forEach(b -> out.add(b.id()));
        }
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }

    private static boolean isGive(String s) {
        return s.equalsIgnoreCase("dar") || s.equalsIgnoreCase("give");
    }
}
