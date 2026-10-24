package fr.maceandpearlcooldown;

import io.papermc.paper.event.player.PlayerLaunchProjectileEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * MaceAndPearlCooldown : cooldown de 5 secondes sur la masse (mace) et l'ender pearl.
 */
public final class MaceAndPearlCooldown extends JavaPlugin implements Listener {

    // ---------------------------------------------------------------
    //  CONFIGURATION
    // ---------------------------------------------------------------
    private static final long PEARL_COOLDOWN_MS = 5_000L;
    private static final long MACE_COOLDOWN_MS  = 5_000L;
    private static final String BYPASS_PERMISSION = "maceandpearlcooldown.bypass";
    private static final String PEARL_MESSAGE = "Perle en recharge : %.1fs";
    private static final String MACE_MESSAGE  = "Masse en recharge : %.1fs";

    // ---------------------------------------------------------------
    //  DONNÉES
    // ---------------------------------------------------------------
    private final Map<UUID, Long> lastPearl = new HashMap<>();
    private final Map<UUID, Long> lastMace  = new HashMap<>();

    // ---------------------------------------------------------------
    //  CYCLE DE VIE
    // ---------------------------------------------------------------
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("MaceAndPearlCooldown activé.");
    }

    @Override
    public void onDisable() {
        lastPearl.clear();
        lastMace.clear();
    }

    // ---------------------------------------------------------------
    //  ENDER PEARL
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPearlLaunch(PlayerLaunchProjectileEvent event) {
        if (!(event.getProjectile() instanceof EnderPearl)) return;

        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) return;

        if (isOnCooldown(lastPearl, player, PEARL_COOLDOWN_MS, PEARL_MESSAGE)) {
            event.setCancelled(true); // la perle n'est pas consommée
        }
    }

    // ---------------------------------------------------------------
    //  MASSE
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMaceHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.MACE) return;
        if (player.hasPermission(BYPASS_PERMISSION)) return;

        if (isOnCooldown(lastMace, player, MACE_COOLDOWN_MS, MACE_MESSAGE)) {
            event.setCancelled(true); // le coup est annulé
        }
    }

    // ---------------------------------------------------------------
    //  OUTILS
    // ---------------------------------------------------------------
    /** Retourne true si le joueur est en cooldown (et l'en informe), sinon démarre le cooldown. */
    private boolean isOnCooldown(Map<UUID, Long> map, Player player, long durationMs, String message) {
        long now = System.currentTimeMillis();
        long remaining = durationMs - (now - map.getOrDefault(player.getUniqueId(), 0L));

        if (remaining > 0) {
            player.sendActionBar(Component.text(
                    String.format(Locale.FRANCE, message, remaining / 1000.0), NamedTextColor.RED));
            return true;
        }
        map.put(player.getUniqueId(), now);
        return false;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastPearl.remove(id);
        lastMace.remove(id);
    }
}
