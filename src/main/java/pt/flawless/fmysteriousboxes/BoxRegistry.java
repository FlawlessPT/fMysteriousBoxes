package pt.flawless.fmysteriousboxes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import pt.flawless.fapi.items.ItemsApi;
import pt.flawless.fmysteriousboxes.Box.Prize;

/** Lê e valida as caixas do config; entradas inválidas são avisadas e ignoradas. */
public class BoxRegistry {

    private static final List<Integer> DEFAULT_INTERVALS =
            List.of(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 6, 7, 8, 10);
    private static final int DEFAULT_FINAL_PAUSE = 30;

    private final Map<String, Box> boxes = new LinkedHashMap<>();
    private final List<Integer> intervals;
    private final int finalPause;

    public BoxRegistry(FileConfiguration cfg, ItemsApi items, Logger log) {
        List<Integer> read = cfg.getIntegerList("animacao.intervalos");
        if (read.isEmpty() || read.stream().anyMatch(i -> i < 1)) {
            log.warning("animacao.intervalos vazio ou com valores < 1; a usar o valor por omissão");
            read = DEFAULT_INTERVALS;
        }
        intervals = List.copyOf(read);
        finalPause = Math.max(0, cfg.getInt("animacao.pausa-final", DEFAULT_FINAL_PAUSE));

        ConfigurationSection section = cfg.getConfigurationSection("caixas");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) {
                log.warning("caixa '" + key + "' ignorada: formato inválido");
                continue;
            }
            String id = key.toLowerCase(Locale.ROOT);
            String itemId = s.getString("item");
            if (itemId == null || !items.exists(itemId)) {
                log.warning("caixa '" + key + "' ignorada: item '" + itemId + "' não existe no fCustomItems");
                continue;
            }
            List<Prize> prizes = new ArrayList<>();
            for (Map<?, ?> p : s.getMapList("premios")) {
                Object item = p.get("item");
                Object weight = p.get("peso");
                Object amount = p.get("quantidade");
                if (item == null || !items.exists(item.toString())) {
                    log.warning("caixa '" + key + "': prémio '" + item + "' ignorado (item inexistente)");
                } else if (!(weight instanceof Integer w) || w < 1) {
                    log.warning("caixa '" + key + "': prémio '" + item + "' ignorado (peso inválido)");
                } else if (amount != null && !(amount instanceof Integer a && a >= 1)) {
                    log.warning("caixa '" + key + "': prémio '" + item + "' ignorado (quantidade inválida)");
                } else {
                    prizes.add(new Prize(item.toString(), w, (Integer) amount));
                }
            }
            if (prizes.isEmpty()) {
                log.warning("caixa '" + key + "' ignorada: sem prémios válidos");
                continue;
            }
            if (boxes.containsKey(id)) {
                log.warning("caixa '" + key + "' ignorada: id repetido");
                continue;
            }
            boxes.put(id, new Box(id, itemId, s.getString("titulo", id), List.copyOf(prizes)));
        }
    }

    public Optional<Box> find(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(boxes.get(id.toLowerCase(Locale.ROOT)));
    }

    public Optional<Box> findByItem(String itemId) {
        return boxes.values().stream().filter(b -> b.itemId().equalsIgnoreCase(itemId)).findFirst();
    }

    public Collection<Box> all() {
        return boxes.values();
    }

    public List<Integer> intervals() {
        return intervals;
    }

    public int finalPause() {
        return finalPause;
    }
}
