package me.yellowstrawberry.rs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Random;

public final class RandomSpawn extends JavaPlugin implements Listener {

    HashMap<String, Location> spawnpoints = new HashMap<>();
    Connection conn;

    Settings settings = new Settings(Bukkit.getPluginsFolder());

    @Override
    public void onEnable() {
        try {
            if(settings.sqliteFile == null) {
                Class.forName("org.mariadb.jdbc.Driver");
                conn = DriverManager.getConnection(
                        "jdbc:mariadb://" + settings.sqlURL + "/" + settings.databaseName + "?useUnicode=true&passwordCharacterEncoding=utf-8", "root", "root");
                conn.prepareStatement("CREATE TABLE IF NOT EXISTS `spawnpoint` (\n" +
                        "\t`uuid` LONGTEXT NOT NULL COLLATE 'utf8mb4_general_ci',\n" +
                        "\t`x` INT(11) NOT NULL DEFAULT '0',\n" +
                        "\t`y` INT(11) NOT NULL DEFAULT '0',\n" +
                        "\t`z` INT(11) NOT NULL DEFAULT '0'\n" +
                        ")").execute();
            }else {
                Class.forName("org.sqlite.JDBC");
                conn = DriverManager.getConnection("jdbc:sqlite:"+settings.sqliteFile.getAbsolutePath());
            }
        } catch (Exception se) {
            se.printStackTrace();
        }
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if(!Bukkit.getOfflinePlayer(event.getPlayer().getUniqueId()).hasPlayedBefore()) {
            Location loc = getRandomCoordinates();
            addSpawnpoint(event.getPlayer().getUniqueId().toString(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            event.getPlayer().setBedSpawnLocation(loc);
            event.getPlayer().setCompassTarget(loc);
            event.getPlayer().teleport(loc);
        }
    }

    public Location getRandomCoordinates() {
        Random r = new Random();
        int x = settings.centerX+(r.nextInt(settings.maxX*2)-settings.maxX), z = settings.centerZ+(r.nextInt(settings.maxZ*2)-settings.maxZ);
        int y = Bukkit.getWorld("world").getHighestBlockYAt(x, z)+1;
        Location loc = new Location(Bukkit.getWorld("world"), x, y, z);
        Bukkit.getWorld("world").loadChunk(loc.getChunk());
        if(loc.getBlock().getType() == Material.WATER || loc.getBlock().getType() == Material.LAVA) loc = getRandomCoordinates();
        return loc;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if(event.getPlayer().getBedSpawnLocation() == null) {
            event.getPlayer().setBedSpawnLocation(getSpawnpoint(event.getPlayer().getUniqueId().toString()));
            event.setRespawnLocation(getSpawnpoint(event.getPlayer().getUniqueId().toString()));
        }
    }

    public void addSpawnpoint(String uuid, int x, int y, int z){
        try {
            spawnpoints.put(uuid, new Location(Bukkit.getWorld("world"), x, y+0.1, z));
            conn.prepareStatement("INSERT INTO `spawnpoint` (uuid, x, y, z) VALUES ('"+uuid+"', "+x+", "+y+", "+z+")").execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Location getSpawnpoint(String uuid) {
        if(spawnpoints.containsKey(uuid)) return spawnpoints.get(uuid);
        try{
            ResultSet set = conn.prepareStatement("SELECT * FROM `spawnpoint` WHERE uuid='"+uuid+"'").executeQuery();
            set.first();
            spawnpoints.put(uuid, new Location(Bukkit.getWorld("world"), set.getInt("x"), set.getInt("y")+0.1, set.getInt("z")));
            return spawnpoints.get(uuid);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }
}
