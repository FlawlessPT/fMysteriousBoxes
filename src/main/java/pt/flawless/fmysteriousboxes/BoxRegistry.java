package pt.flawless.fmysteriousboxes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
                Object materialName = p.get("material");
                Object data = p.get("data");
                Object weight = p.get("peso");
                Object amount = p.get("quantidade");
                Object label = item != null ? item : materialName;
                Material material = null;
                if (item != null && materialName != null) {
                    log.warning("caixa '" + key + "': prémio '" + item + "' ignorado (usa item ou material, não os dois)");
                    continue;
                }
                if (materialName != null) {
                    material = Material.matchMaterial(materialName.toString());
                }
                if (item == null && materialName == null) {
                    log.warning("caixa '" + key + "': prémio ignorado (falta item ou material)");
                } else if (item != null && !items.exists(item.toString())) {
                    log.warning("caixa '" + key + "': prémio '" + item + "' ignorado (item inexistente)");
                } else if (materialName != null && (material == null || material == Material.AIR)) {
                    log.warning("caixa '" + key + "': prémio '" + materialName + "' ignorado (material inexistente)");
                } else if (data != null && (item != null || !(data instanceof Integer d && d >= 0 && d <= Short.MAX_VALUE))) {
                    log.warning("caixa '" + key + "': prémio '" + label + "' ignorado (data inválida ou usada com item)");
                } else if (!(weight instanceof Integer w) || w < 1) {
                    log.warning("caixa '" + key + "': prémio '" + label + "' ignorado (peso inválido)");
                } else if (amount != null && !(amount instanceof Integer a && a >= 1)) {
                    log.warning("caixa '" + key + "': prémio '" + label + "' ignorado (quantidade inválida)");
                } else {
                    prizes.add(new Prize(item != null ? item.toString() : null, material,
                            data != null ? ((Integer) data).shortValue() : 0, w, (Integer) amount));
                }
            }
            if (prizes.isEmpty()) {
                log.warning("caixa '" + key + "' sem prémios válidos: não abre até ter conteúdo");
            }
            if (boxes.containsKey(id)) {
                log.warning("caixa '" + key + "' ignorada: id repetido");
                continue;
            }
            boxes.put(id, new Box(id, itemId, s.getString("titulo", defaultTitle(items, itemId, id)), List.copyOf(prizes)));
        }
    }

    /** Título por omissão: o nome do custom item; sem nome, o id da caixa. */
    static String defaultTitle(ItemsApi items, String itemId, String boxId) {
        return items.create(itemId).map(ItemStack::getItemMeta)
                .filter(ItemMeta::hasDisplayName).map(ItemMeta::getDisplayName).orElse(boxId);
    }

    public Optional<Box> find(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(boxes.get(id.toLowerCase(Locale.ROOT)));
    }

    public void add(Box box) {
        boxes.put(box.id(), box);
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
