package pt.flawless.fmysteriousboxes;

import java.util.List;
import java.util.Random;
import org.bukkit.inventory.ItemStack;
import pt.flawless.fapi.items.ItemsApi;

/** Uma caixa configurada: o item que se segura e os prémios possíveis. */
public record Box(String id, String itemId, String title, List<Prize> prizes) {

    /** Prémio de uma caixa; {@code amount} nulo = quantidade do próprio item. */
    public record Prize(String itemId, int weight, Integer amount) {
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
        return (prize.amount() != null ? items.create(prize.itemId(), prize.amount()) : items.create(prize.itemId()))
                .orElseThrow(() -> new IllegalStateException("item inexistente: " + prize.itemId()));
    }
}
