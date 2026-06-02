package fr.sanssmp;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tous les objets custom du Sans SMP (epees, hache, masse).
 * id, materiau, nom colore, capacite, cooldown (secondes), description.
 */
public enum SwordType {

    DASH("dash", Material.DIAMOND_SWORD,
            ChatColor.AQUA + "" + ChatColor.BOLD + "Dash Sword",
            ChatColor.AQUA + "\u26A1 Dash fulgurant", 20,
            Arrays.asList(ChatColor.GRAY + "Propulse-toi en avant en un eclair.",
                    ChatColor.DARK_GRAY + "Clic droit")),

    FOUDRE("foudre", Material.DIAMOND_SWORD,
            ChatColor.YELLOW + "" + ChatColor.BOLD + "Lame de Foudre",
            ChatColor.YELLOW + "\uD83C\uDF29 Frappe celeste", 20,
            Arrays.asList(ChatColor.GRAY + "Invoque un eclair + degats de zone.",
                    ChatColor.DARK_GRAY + "Clic droit")),

    GLACE("glace", Material.DIAMOND_SWORD,
            ChatColor.AQUA + "" + ChatColor.BOLD + "Lame de Glace",
            ChatColor.AQUA + "\u2744 Gel instantane", 20,
            Arrays.asList(ChatColor.GRAY + "Fige et ralentit la cible visee.",
                    ChatColor.DARK_GRAY + "Clic droit")),

    VAMPIRE("vampire", Material.DIAMOND_SWORD,
            ChatColor.DARK_RED + "" + ChatColor.BOLD + "Lame Vampirique",
            ChatColor.RED + "\uD83E\uDE78 Vol de vie", 0,
            Arrays.asList(ChatColor.GRAY + "Chaque coup te rend de la vie.",
                    ChatColor.DARK_GRAY + "Passif")),

    VIDE("vide", Material.DIAMOND_SWORD,
            ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Lame du Vide",
            ChatColor.LIGHT_PURPLE + "\uD83C\uDF00 Teleportation", 20,
            Arrays.asList(ChatColor.GRAY + "Teleporte-toi au bloc vise.",
                    ChatColor.DARK_GRAY + "Clic droit")),

    EXPLOSIVE("explosive", Material.DIAMOND_SWORD,
            ChatColor.GOLD + "" + ChatColor.BOLD + "Lame Explosive",
            ChatColor.GOLD + "\uD83D\uDCA5 Onde de choc", 20,
            Arrays.asList(ChatColor.GRAY + "Explosion qui repousse (sans casser les blocs).",
                    ChatColor.DARK_GRAY + "Clic droit")),

    OCEAN("ocean", Material.DIAMOND_SWORD,
            ChatColor.BLUE + "" + ChatColor.BOLD + "Ocean Sword",
            ChatColor.AQUA + "\uD83C\uDF0A Bulle d'ocean", 60,
            Arrays.asList(ChatColor.GRAY + "Cree une bulle d'eau de 4 blocs (8s).",
                    ChatColor.GRAY + "Tu gagnes Regeneration II.",
                    ChatColor.GRAY + "Les intrus recoivent Poison + Cecite.",
                    ChatColor.DARK_GRAY + "Clic droit")),

    CATCH("catch", Material.DIAMOND_AXE,
            ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Catch Axe",
            ChatColor.DARK_PURPLE + "\u26D3 Prison d'obsidienne", 90,
            Arrays.asList(ChatColor.GRAY + "Emprisonne ta cible et toi dans une",
                    ChatColor.GRAY + "salle d'obsidienne 10x10 (20s).",
                    ChatColor.GRAY + "Tu gagnes Vitesse III.",
                    ChatColor.DARK_GRAY + "Clic droit sur un joueur")),

    MASSE("masse", Material.MACE,
            ChatColor.RED + "" + ChatColor.BOLD + "Masse du Sans SMP",
            ChatColor.RED + "\uD83D\uDCA2 Etourdissement", 30,
            Arrays.asList(ChatColor.GRAY + "La seule masse du serveur.",
                    ChatColor.GRAY + "Clic droit : etourdit les joueurs proches.",
                    ChatColor.DARK_GRAY + "Densite & Breach limites a 2"));

    private final String id;
    private final Material material;
    private final String displayName;
    private final String ability;
    private final int cooldown;
    private final List<String> loreLines;

    SwordType(String id, Material material, String displayName, String ability,
              int cooldown, List<String> loreLines) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.ability = ability;
        this.cooldown = cooldown;
        this.loreLines = loreLines;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getCooldown() { return cooldown; }

    public ItemStack createItem(NamespacedKey key) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            List<String> lore = new ArrayList<>();
            lore.add(ability);
            lore.add("");
            lore.addAll(loreLines);
            lore.add("");
            lore.add(ChatColor.DARK_AQUA + "" + ChatColor.ITALIC + "Objet legendaire du Sans SMP");
            meta.setLore(lore);
            meta.setEnchantmentGlintOverride(true);
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            if (this == MASSE) {
                Enchantment density = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft("density"));
                Enchantment breach = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft("breach"));
                if (density != null) meta.addEnchant(density, 2, true);
                if (breach != null) meta.addEnchant(breach, 2, true);
            }
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static SwordType fromItem(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String id = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return fromId(id);
    }

    public static SwordType fromId(String id) {
        if (id == null) return null;
        for (SwordType t : values()) if (t.id.equalsIgnoreCase(id)) return t;
        return null;
    }
}
