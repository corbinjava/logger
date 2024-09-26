package wtf.corbin.bonemeallogger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
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

    private final Map<UUID, Integer> bonemealRemovedCounts = new HashMap<>();
    private final Map<UUID, Long> lastRemovalTime = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // checks if theres a player
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            UUID playerId = player.getUniqueId();

            // checks if the inveotry type is crafting 
            if (event.getView().getTopInventory().getType() == InventoryType.CRAFTING) {

                // get the item thats being clicked a
                ItemStack currentItem = event.getCurrentItem();

                // make sure that item is bonem,eal
                if (currentItem != null && currentItem.getType() == Material.BONE_MEAL) {
                    int amount = currentItem.getAmount();
                    long currentTime = System.currentTimeMillis();

                    // check if the player has removed the threshold in <= 5 seconds
                    if (lastRemovalTime.containsKey(playerId) && currentTime - lastRemovalTime.get(playerId) <= 5000) {
                        bonemealRemovedCounts.put(playerId, bonemealRemovedCounts.get(playerId) + amount);
                    } else {
                        bonemealRemovedCounts.put(playerId, amount);
                    }

                    lastRemovalTime.put(playerId, currentTime);

                    // if the threshold is met in less than 5 seconds, this 
                    if (bonemealRemovedCounts.get(playerId) >= 1728) {
                        Location loc = player.getLocation();
                        String message = player.getName() + " flagged at " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
                        sendToDiscord(message);

                        // resets when logged
                        bonemealRemovedCounts.remove(playerId);
                        lastRemovalTime.remove(playerId);
                    }
                }
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
