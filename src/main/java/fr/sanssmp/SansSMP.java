package fr.sanssmp;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;

public class SansSMP extends JavaPlugin implements Listener {

    private final String PREFIX = ChatColor.AQUA + "" + ChatColor.BOLD + "[Sans SMP] " + ChatColor.RESET;

    private NamespacedKey swordKey;
    private NamespacedKey ritualKey;
    private Enchantment enchDensity;
    private Enchantment enchBreach;

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Set<Location> protectedBlocks = new HashSet<>();

    // Etat du rituel
    private boolean ritualActive = false;
    private Item ritualItem;
    private BossBar ritualBar;
    private BukkitTask ritualParticleTask;
    private final List<BukkitTask> ritualTasks = new ArrayList<>();
    private double ritualAngle = 0;

    @Override
    public void onEnable() {
        swordKey = new NamespacedKey(this, "sans_sword");
        ritualKey = new NamespacedKey(this, "ritual_item");
        enchDensity = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("density"));
        enchBreach = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("breach"));
        getServer().getPluginManager().registerEvents(this, this);

        // Empeche de crafter la masse (seule la masse du serveur est obtenable)
        try { Bukkit.removeRecipe(NamespacedKey.minecraft("mace")); } catch (Throwable ignored) {}

        // Tab pour les joueurs deja connectes
        for (Player p : Bukkit.getOnlinePlayers()) setTab(p);

        // Verifie en continu que les masses ne depassent pas Densite 2 / Breach 2
        Bukkit.getScheduler().runTaskTimer(this, this::capAllMaces, 40L, 40L);

        getLogger().info("Sans SMP active !");
    }

    @Override
    public void onDisable() {
        if (ritualActive) endRitual(false);
    }

    // ========================================================================
    //  TAB LIST
    // ========================================================================

    private void setTab(Player p) {
        TextColor violet = TextColor.color(0xB14CFF);
        Component header = Component.text("")
                .append(Component.newline())
                .append(Component.text("\u2726  SANS SMP  \u2726", violet, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Le serveur des epees legendaires", NamedTextColor.GRAY))
                .append(Component.newline());
        Component footer = Component.text("")
                .append(Component.newline())
                .append(Component.text("discord.gg/ymAUVG5Uq", NamedTextColor.LIGHT_PURPLE))
                .append(Component.newline());
        p.sendPlayerListHeaderAndFooter(header, footer);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        setTab(p);
        if (ritualActive && ritualBar != null) ritualBar.addPlayer(p);
    }

    // ========================================================================
    //  COMMANDES
    // ========================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("epee")) {
            if (args.length < 1) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Usage : /epee <dash|foudre|glace|vampire|vide|explosive|ocean|catch|masse> [joueur]");
                return true;
            }
            SwordType type = SwordType.fromId(args[0]);
            if (type == null) { sender.sendMessage(PREFIX + ChatColor.RED + "Objet inconnu : " + args[0]); return true; }
            Player target;
            if (args.length >= 2) {
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(PREFIX + ChatColor.RED + "Joueur introuvable."); return true; }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else { sender.sendMessage(PREFIX + ChatColor.RED + "Precise un joueur."); return true; }
            target.getInventory().addItem(type.createItem(swordKey));
            target.sendMessage(PREFIX + ChatColor.GREEN + "Tu as recu : " + type.getDisplayName());
            return true;
        }

        if (command.getName().equalsIgnoreCase("rituel")) {
            if (args.length >= 1 && args[0].equalsIgnoreCase("stop")) {
                if (!ritualActive) { sender.sendMessage(PREFIX + ChatColor.RED + "Aucun rituel en cours."); return true; }
                endRitual(true);
                return true;
            }
            if (!(sender instanceof Player player)) { sender.sendMessage(PREFIX + ChatColor.RED + "Commande en jeu uniquement."); return true; }
            if (ritualActive) { sender.sendMessage(PREFIX + ChatColor.RED + "Un rituel est deja en cours ! (/rituel stop pour annuler)"); return true; }
            SwordType type;
            if (args.length >= 1) {
                type = SwordType.fromId(args[0]);
                if (type == null) { sender.sendMessage(PREFIX + ChatColor.RED + "Objet inconnu : " + args[0]); return true; }
            } else {
                SwordType[] all = SwordType.values();
                type = all[new Random().nextInt(all.length)];
            }
            startRitual(type, player.getLocation());
            return true;
        }
        return false;
    }

    // ========================================================================
    //  RITUEL
    // ========================================================================

    private void startRitual(SwordType type, Location center) {
        World world = center.getWorld();
        if (world == null) return;
        ritualActive = true;

        Location loc = center.clone().add(0, 3, 0);
        ItemStack stack = type.createItem(swordKey);
        Item item = world.dropItem(loc, stack);
        item.setGravity(false);
        item.setVelocity(new Vector(0, 0, 0));
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setInvulnerable(true);
        item.setGlowing(true);
        item.setCustomNameVisible(true);
        item.setCustomName(type.getDisplayName());
        item.setCanMobPickup(false);
        item.setWillAge(false);
        item.getPersistentDataContainer().set(ritualKey, PersistentDataType.BYTE, (byte) 1);
        ritualItem = item;

        // Coordonnees affichees en haut de l'ecran de tous les joueurs
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        ritualBar = Bukkit.createBossBar(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "RITUEL "
                + ChatColor.WHITE + type.getDisplayName() + ChatColor.GRAY + "  |  "
                + ChatColor.AQUA + "X: " + x + "  Y: " + y + "  Z: " + z, BarColor.PURPLE, BarStyle.SOLID);
        for (Player p : Bukkit.getOnlinePlayers()) ritualBar.addPlayer(p);

        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
        Bukkit.broadcastMessage(PREFIX + ChatColor.LIGHT_PURPLE + "Un RITUEL a commence ! "
                + type.getDisplayName() + ChatColor.LIGHT_PURPLE + " flotte en X:" + x + " Y:" + y + " Z:" + z
                + ". Elle tombe dans 2 minutes !");

        // Particules + objets qui tournent autour pour le style
        ritualAngle = 0;
        ritualParticleTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (ritualItem == null || !ritualItem.isValid()) return;
            Location l = ritualItem.getLocation();
            ritualAngle += 0.20;
            double r = 1.1;
            for (int i = 0; i < 3; i++) {
                double a = ritualAngle + (i * (2 * Math.PI / 3));
                double ox = Math.cos(a) * r;
                double oz = Math.sin(a) * r;
                double oy = Math.sin(ritualAngle * 2 + i) * 0.4;
                Location pt = l.clone().add(ox, oy, oz);
                world.spawnParticle(Particle.END_ROD, pt, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.WITCH, pt, 1, 0, 0, 0, 0);
            }
            world.spawnParticle(Particle.ENCHANT, l.clone().add(0, 0.4, 0), 8, 0.5, 0.5, 0.5, 0.6);
            world.spawnParticle(Particle.PORTAL, l, 6, 0.3, 0.3, 0.3, 0.2);
        }, 0L, 2L);

        ritualTasks.add(Bukkit.getScheduler().runTaskLater(this, () ->
                Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW + "L'objet tombe dans 60 secondes !"), 60 * 20L));
        ritualTasks.add(Bukkit.getScheduler().runTaskLater(this, () ->
                Bukkit.broadcastMessage(PREFIX + ChatColor.GOLD + "L'objet tombe dans 30 secondes !"), 90 * 20L));
        ritualTasks.add(Bukkit.getScheduler().runTaskLater(this, () ->
                Bukkit.broadcastMessage(PREFIX + ChatColor.RED + "L'objet tombe dans 10 secondes !"), 110 * 20L));

        ritualTasks.add(Bukkit.getScheduler().runTaskLater(this, () -> {
            if (ritualItem == null || !ritualItem.isValid()) { endRitual(false); return; }
            ritualItem.setGravity(true);
            ritualItem.setInvulnerable(false);
            ritualItem.setPickupDelay(0);
            world.strikeLightningEffect(ritualItem.getLocation());
            world.playSound(ritualItem.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1.2f);
            if (ritualParticleTask != null) ritualParticleTask.cancel();
            if (ritualBar != null) ritualBar.setTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "L'OBJET EST TOMBE ! Premier arrive, premier servi !");
            Bukkit.broadcastMessage(PREFIX + ChatColor.GREEN + "L'objet est TOMBE ! " + ChatColor.BOLD + "Premier arrive, premier servi !");
        }, 120 * 20L));
    }

    private void endRitual(boolean announce) {
        ritualActive = false;
        for (BukkitTask t : ritualTasks) { if (t != null) t.cancel(); }
        ritualTasks.clear();
        if (ritualParticleTask != null) { ritualParticleTask.cancel(); ritualParticleTask = null; }
        if (ritualBar != null) { ritualBar.removeAll(); ritualBar = null; }
        if (ritualItem != null && ritualItem.isValid()) ritualItem.remove();
        ritualItem = null;
        if (announce) Bukkit.broadcastMessage(PREFIX + ChatColor.RED + "Le rituel a ete annule.");
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        Item item = event.getItem();
        if (!item.getPersistentDataContainer().has(ritualKey, PersistentDataType.BYTE)) return;
        if (!(event.getEntity() instanceof Player player)) { event.setCancelled(true); return; }
        SwordType type = SwordType.fromItem(item.getItemStack(), swordKey);
        String name = (type != null) ? type.getDisplayName() : (ChatColor.AQUA + "l'objet du rituel");
        Bukkit.broadcastMessage(PREFIX + ChatColor.GOLD + player.getName() + ChatColor.YELLOW
                + " a remporte le rituel et obtenu " + name + ChatColor.YELLOW + " !");
        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        if (ritualParticleTask != null) { ritualParticleTask.cancel(); ritualParticleTask = null; }
        if (ritualBar != null) { ritualBar.removeAll(); ritualBar = null; }
        for (BukkitTask t : ritualTasks) { if (t != null) t.cancel(); }
        ritualTasks.clear();
        ritualItem = null;
        ritualActive = false;
    }

    // ========================================================================
    //  CAPACITES (clic droit)
    // ========================================================================

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        SwordType type = SwordType.fromItem(inHand, swordKey);
        if (type == null || type == SwordType.VAMPIRE) return;
        if (isOnCooldown(player, type)) return;
        event.setCancelled(true);
        switch (type) {
            case DASH -> abilityDash(player);
            case FOUDRE -> abilityLightning(player);
            case GLACE -> abilityIce(player);
            case VIDE -> abilityVoid(player);
            case EXPLOSIVE -> abilityExplosive(player);
            case OCEAN -> abilityOcean(player);
            case CATCH -> abilityCatch(player);
            case MASSE -> abilityStun(player);
            default -> { }
        }
    }

    private void abilityDash(Player player) {
        Vector dir = player.getLocation().getDirection().normalize().multiply(2.4);
        dir.setY(Math.max(dir.getY(), 0.35) + 0.25);
        player.setVelocity(dir);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.6f);
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            int t = 0;
            @Override public void run() {
                if (t++ > 10 || !player.isOnline()) return;
                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 0.3, 0), 4, 0.2, 0.1, 0.2, 0.01);
            }
        }, 0L, 1L);
    }

    private void abilityLightning(Player player) {
        Block target = player.getTargetBlockExact(30);
        Location loc = (target != null) ? target.getLocation().add(0.5, 1, 0.5)
                : player.getEyeLocation().add(player.getLocation().getDirection().multiply(15));
        World world = player.getWorld();
        world.strikeLightning(loc);
        for (Entity e : world.getNearbyEntities(loc, 4, 4, 4))
            if (e instanceof LivingEntity le && !e.equals(player)) le.damage(8.0, player);
    }

    private void abilityIce(Player player) {
        World world = player.getWorld();
        RayTraceResult res = world.rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(),
                25, 1.0, e -> e != player && e instanceof LivingEntity);
        if (res != null && res.getHitEntity() instanceof LivingEntity le) {
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 4));
            le.setFreezeTicks(160);
            le.getWorld().spawnParticle(Particle.SNOWFLAKE, le.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.05);
            le.getWorld().playSound(le.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.4f);
        } else player.sendMessage(PREFIX + ChatColor.GRAY + "Aucune cible a geler.");
    }

    private void abilityVoid(Player player) {
        Block target = player.getTargetBlockExact(40);
        if (target == null) { player.sendMessage(PREFIX + ChatColor.GRAY + "Aucun bloc en vue."); return; }
        Location from = player.getLocation();
        Location dest = target.getLocation().add(0.5, 1, 0.5);
        dest.setYaw(from.getYaw()); dest.setPitch(from.getPitch());
        from.getWorld().spawnParticle(Particle.PORTAL, from.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.6);
        player.teleport(dest);
        dest.getWorld().spawnParticle(Particle.PORTAL, dest.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.6);
        player.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }

    private void abilityExplosive(Player player) {
        Block target = player.getTargetBlockExact(40);
        Location loc = (target != null) ? target.getLocation().add(0.5, 1, 0.5)
                : player.getEyeLocation().add(player.getLocation().getDirection().multiply(12));
        player.getWorld().createExplosion(loc, 3.0f, false, false, player);
    }

    // Ocean Sword : bulle d'eau de 4 blocs pendant 8s
    private void abilityOcean(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_DOLPHIN_SPLASH, 1f, 1f);
        player.sendMessage(PREFIX + ChatColor.AQUA + "Bulle d'ocean activee !");
        final World world = player.getWorld();
        final double radius = 4.0;
        BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks >= 160 || !player.isOnline()) { holder[0].cancel(); return; }
                ticks += 4;
                Location c = player.getLocation().add(0, 1, 0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 60, 0));
                // bulle visuelle
                for (int i = 0; i < 30; i++) {
                    double theta = Math.random() * Math.PI * 2;
                    double phi = Math.acos(2 * Math.random() - 1);
                    double rx = radius * Math.sin(phi) * Math.cos(theta);
                    double ry = radius * Math.cos(phi);
                    double rz = radius * Math.sin(phi) * Math.sin(theta);
                    world.spawnParticle(Particle.BUBBLE_COLUMN_UP, c.clone().add(rx, ry, rz), 1, 0, 0, 0, 0);
                }
                world.spawnParticle(Particle.SPLASH, c, 10, radius / 2, radius / 2, radius / 2, 0.1);
                // intrus
                for (Entity e : world.getNearbyEntities(c, radius, radius, radius)) {
                    if (e instanceof Player other && !other.equals(player)) {
                        other.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 0));
                        other.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0));
                    }
                }
            }
        }, 0L, 4L);
    }

    // Catch Axe : prison d'obsidienne 10x10 pendant 20s
    private void abilityCatch(Player player) {
        World world = player.getWorld();
        RayTraceResult res = world.rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(),
                30, 1.0, e -> e != player && e instanceof Player);
        if (res == null || !(res.getHitEntity() instanceof Player target)) {
            player.sendMessage(PREFIX + ChatColor.GRAY + "Aucun joueur vise.");
            return;
        }
        Location center = target.getLocation();
        // teleporte l'utilisateur dans la salle
        player.teleport(center.clone().add(2, 0, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 2));
        buildPrison(center);
        player.getWorld().playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 0.6f);
        Bukkit.broadcastMessage(PREFIX + ChatColor.DARK_PURPLE + player.getName() + " a emprisonne "
                + target.getName() + " dans une salle d'obsidienne (20s) !");
    }

    private void buildPrison(Location center) {
        World w = center.getWorld();
        if (w == null) return;
        int bx = center.getBlockX(), by = center.getBlockY(), bz = center.getBlockZ();
        final Map<Block, BlockData> saved = new HashMap<>();
        final List<Location> placed = new ArrayList<>();
        int half = 5;
        for (int dx = -half; dx <= half; dx++)
            for (int dy = -1; dy <= 9; dy++)
                for (int dz = -half; dz <= half; dz++) {
                    boolean shell = dx == -half || dx == half || dz == -half || dz == half || dy == -1 || dy == 9;
                    if (!shell) continue;
                    Block b = w.getBlockAt(bx + dx, by + dy, bz + dz);
                    saved.put(b, b.getBlockData());
                    b.setType(Material.OBSIDIAN);
                    Location loc = b.getLocation();
                    protectedBlocks.add(loc);
                    placed.add(loc);
                }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Map.Entry<Block, BlockData> e : saved.entrySet()) e.getKey().setBlockData(e.getValue());
            placed.forEach(protectedBlocks::remove);
        }, 20 * 20L);
    }

    // Masse : etourdissement
    private void abilityStun(Player player) {
        World world = player.getWorld();
        Location c = player.getLocation();
        boolean hit = false;
        for (Entity e : world.getNearbyEntities(c, 6, 4, 6)) {
            if (e instanceof Player other && !other.equals(player)) {
                other.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 6));
                other.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 128));
                other.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 3));
                other.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                other.getWorld().spawnParticle(Particle.CRIT, other.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.2);
                other.sendMessage(PREFIX + ChatColor.RED + "Tu es etourdi !");
                hit = true;
            }
        }
        world.playSound(c, Sound.BLOCK_ANVIL_LAND, 1f, 0.8f);
        if (!hit) player.sendMessage(PREFIX + ChatColor.GRAY + "Aucun joueur a portee.");
    }

    // Vampire : passif
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (SwordType.fromItem(player.getInventory().getItemInMainHand(), swordKey) != SwordType.VAMPIRE) return;
        double heal = event.getFinalDamage() * 0.35;
        player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 3, 0.3, 0.3, 0.3, 0);
    }

    // ========================================================================
    //  MASSE : non-craftable + enchantements limites
    // ========================================================================

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getRecipe() != null && event.getRecipe().getResult().getType() == Material.MACE)
            event.setCancelled(true);
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack r = event.getResult();
        if (r == null || r.getType() != Material.MACE) return;
        boolean changed = false;
        if (enchDensity != null && r.getEnchantmentLevel(enchDensity) > 2) { r.addUnsafeEnchantment(enchDensity, 2); changed = true; }
        if (enchBreach != null && r.getEnchantmentLevel(enchBreach) > 2) { r.addUnsafeEnchantment(enchBreach, 2); changed = true; }
        if (changed) event.setResult(r);
    }

    private void capAllMaces() {
        for (Player p : Bukkit.getOnlinePlayers())
            for (ItemStack it : p.getInventory().getContents()) {
                if (it == null || it.getType() != Material.MACE) continue;
                if (enchDensity != null && it.getEnchantmentLevel(enchDensity) > 2) it.addUnsafeEnchantment(enchDensity, 2);
                if (enchBreach != null && it.getEnchantmentLevel(enchBreach) > 2) it.addUnsafeEnchantment(enchBreach, 2);
            }
    }

    // ========================================================================
    //  BLOCS PROTEGES (prison)
    // ========================================================================

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (protectedBlocks.contains(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> protectedBlocks.contains(b.getLocation()));
    }

    // ========================================================================
    //  COOLDOWNS
    // ========================================================================

    private boolean isOnCooldown(Player player, SwordType type) {
        if (type.getCooldown() <= 0) return false;
        long now = System.currentTimeMillis();
        Map<String, Long> map = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        long readyAt = map.getOrDefault(type.getId(), 0L) + type.getCooldown() * 1000L;
        if (now < readyAt) {
            double left = (readyAt - now) / 1000.0;
            player.sendActionBar(Component.text(String.format("Recharge : %.1fs", left), NamedTextColor.RED));
            return true;
        }
        map.put(type.getId(), now);
        return false;
    }
}
