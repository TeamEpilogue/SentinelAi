package net.shieldbreak.sentinelai;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class Main extends JavaPlugin {

    public static Main instance;
    public static String serverBaseUrl = "https://gpu.wireway.ch/sentinelapi/";

    public static String prefix = "§x§1§b§d§9§6§e§lSentinel §r§8| §r";

    public static String prefixDebugger = "§x§1§b§d§9§6§e§lSentinel §x§f§d§2§1§7§7[DEBUG] §r§8| §r";

    private static List<Player> inDebug = new ArrayList<>();
    private static List<Player> playerAdminNotifications = new ArrayList<>();
    
    public static void addPlayerToDebug(Player player) {
        if (!inDebug.contains(player)) {
            inDebug.add(player);
        }
    }
    
    public static void removePlayerFromDebug(Player player) {
        inDebug.remove(player);
    }
    
    public static boolean isPlayerInDebug(Player player) {
        return inDebug.contains(player);
    }

    public static List<Player> getDebugPlayers() {
        return new ArrayList<>(inDebug);
    }

    public static void addPlayerToAdminNotifications(Player player) {
        if (!playerAdminNotifications.contains(player)) {
            playerAdminNotifications.add(player);
        }
    }
    
    public static void removePlayerFromAdminNotifications(Player player) {
        playerAdminNotifications.remove(player);
    }
    
    public static boolean isPlayerInAdminNotifications(Player player) {
        return playerAdminNotifications.contains(player);
    }

    public static List<Player> getAdminNotificationPlayers() {
        return new ArrayList<>(playerAdminNotifications);
    }






    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        getCommand("sentinel").setExecutor(new Commands());

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new Listener(),instance);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    public static Main getInstance() {
        return instance;
    }

    public static String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public static String getPrefix() {
        return prefix;
    }

    public static String getPrefixDebugger() {
        return prefixDebugger;
    }
}
