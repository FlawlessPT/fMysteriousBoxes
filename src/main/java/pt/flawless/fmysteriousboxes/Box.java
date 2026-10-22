package pt.flawless.fmysteriousboxes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pt.flawless.fapi.items.ItemsApi;

/** Uma caixa configurada: o item que se segura e os prémios possíveis. */
public record Box(String id, String itemId, String title, List<Prize> prizes) {

    /**
     * Prémio de uma caixa: um item do fCustomItems ({@code itemId}) ou um item normal ({@code material} + {@code data}).
     * {@code amount} nulo = quantidade do próprio item (1 nos itens normais).
     */
    public record Prize(String itemId, Material material, short data, int weight, Integer amount) {

        /** Cria o item do prémio; vazio se o item do fCustomItems já não existir. */
        public Optional<ItemStack> create(ItemsApi items) {
            if (material != null) {
                return Optional.of(new ItemStack(material, amount != null ? amount : 1, data));
            }
            return amount != null ? items.create(itemId, amount) : items.create(itemId);
        }

        /** Nome para mensagens quando o item não tem nome próprio. */
        public String label() {
            if (material == null) {
                return itemId;
            }
            StringBuilder name = new StringBuilder();
            for (String word : material.name().toLowerCase(Locale.ROOT).split("_")) {
                name.append(name.length() > 0 ? " " : "").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
            return name.toString();
        }
    }

    /** Acrescenta uma linha (precedida de linha vazia) à lore do item. */
    public static ItemStack addLore(ItemStack item, String line) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        lore.add(line);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Sorteia um prémio por peso; a soma dos pesos tem de ser > 0 (garantido pelo {@link BoxRegistry}). */
    public Prize pick(Random random) {
        int total = prizes.stream().mapToInt(Prize::weight).sum();
        int roll = random.nextInt(total);
        for (Prize prize : prizes) {
            roll -= prize.weight();
            if (roll < 0) {
                return prize;
            }
        }
        return prizes.get(prizes.size() - 1);
    }

    /** Cria o item do prémio, com a quantidade do prémio se existir. */
    public ItemStack create(Prize prize, ItemsApi items) {
        return prize.create(items).orElseThrow(() -> new IllegalStateException("item inexistente: " + prize.label()));
    }
}
