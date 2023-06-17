package net.onebeastchris.geyserperserverpacks;

import net.onebeastchris.geyserperserverpacks.common.Configurate;
import net.onebeastchris.geyserperserverpacks.common.utils.FloodgateJavaPlayerChecker;
import net.onebeastchris.geyserperserverpacks.common.utils.GeyserJavaPlayerChecker;
import net.onebeastchris.geyserperserverpacks.common.utils.JavaPlayerChecker;
import net.onebeastchris.geyserperserverpacks.common.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class GeyserPerServerPacksSpigot extends JavaPlugin implements Listener {
    private JavaPlayerChecker playerChecker;
    public Configurate config;

    @Override
    public void onEnable() {
        config = Configurate.create(this.getDataFolder().toPath());
        boolean hasFloodgate = Bukkit.getPluginManager().getPlugin("floodgate") != null;
        boolean hasGeyser = Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null;

        if (!hasFloodgate && !hasGeyser) {
            getLogger().warning("There is no Geyser or Floodgate plugin detected! Disabling...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (hasFloodgate) {
            this.playerChecker = new FloodgateJavaPlayerChecker();
        } else {
            this.playerChecker = new GeyserJavaPlayerChecker();
        }

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPermission(Permission.bypassPermission)) {
            return;
        }

        boolean isBedrockPlayer = this.playerChecker.isBedrockPlayer(event.getPlayer().getUniqueId());

        if (!isBedrockPlayer) {
           //event.getPlayer().kickPlayer(ChatColor.translateAlternateColorCodes('&',config.getBlockJavaMessage()));
        }
    }
}
