package wtf.corbin.bonemeallogger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoneMealLogger extends JavaPlugin implements Listener {

    private final Map<UUID, Integer> bonemealDropCounts = new HashMap<>();
    private final Map<UUID, Long> lastDropTime = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // checks if item is bonemeal or not
        if (event.getItemDrop().getItemStack().getType() == Material.BONE_MEAL) {
            int amount = event.getItemDrop().getItemStack().getAmount();
            long currentTime = System.currentTimeMillis();

            // checks if they have dropped it in the last 5 seconds (change to whatever u think is best)
            if (lastDropTime.containsKey(playerId) && currentTime - lastDropTime.get(playerId) <= 5000) {
                bonemealDropCounts.put(playerId, bonemealDropCounts.get(playerId) + amount);
            } else {
                bonemealDropCounts.put(playerId, amount);
            }

            lastDropTime.put(playerId, currentTime);

            // if they have over 1728 bonemeal then log it to the webhook
            if (bonemealDropCounts.get(playerId) >= 1728) {
                Location loc = player.getLocation();
                String message = player.getName() + " dropped " + bonemealDropCounts.get(playerId)
                        + " bonemeal in " + (currentTime - lastDropTime.get(playerId)) / 1000 + " seconds at "
                        + "X: " + loc.getBlockX() + " Y: " + loc.getBlockY() + " Z: " + loc.getBlockZ();
                sendToDiscord(message);

                // reset counter after they drop it
                bonemealDropCounts.remove(playerId);
                lastDropTime.remove(playerId);
            }
        }
    }

    private void sendToDiscord(String message) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://discord.com/api/webhooks/1288665249477689397/eHkxaDWrgI5dFUErGcLdAjcBd-8aBJWhR0vpAVkFFM_Fn6MjT_ubR7JlrgB-5j4TpEJF");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json; utf-8");
                    connection.setDoOutput(true);

                    JSONObject json = new JSONObject();
                    json.put("content", message);

                    OutputStream os = connection.getOutputStream();
                    byte[] input = json.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);

                    connection.getResponseCode();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(this);
    }
}
