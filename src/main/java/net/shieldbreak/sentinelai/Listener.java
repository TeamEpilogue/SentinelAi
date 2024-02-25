package net.shieldbreak.sentinelai;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Listener implements org.bukkit.event.Listener {

    private final Map<UUID, List<MovementData>> movementDataMap = new HashMap<>();


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Ensure player uuid is in the movementdatamap
        movementDataMap.putIfAbsent(playerId, new ArrayList<>());

        startRecording(player);
    }


    // recording logic

    private void startRecording(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<MovementData> movementDataList = movementDataMap.get(player.getUniqueId());

                if (movementDataList == null) {
                    // This should not happen, but to avoid NPE, return if movementDataList is null
                    return;
                }

                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (movementDataList.size() >= 400) {
                    stopRecording(player,movementDataList);
                    // Reset the movementDataList to start over
                    movementDataList.clear();
                    movementDataMap.putIfAbsent(player.getUniqueId(), new ArrayList<>());
                }

                if (player.isOnline()) {
                    recordPlayerData(player.getUniqueId(), player);
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private void stopRecording(Player player,List<MovementData> movementDataList) {

        if (player != null) {
            String json = generateJson(player.getName(),movementDataList);

            if (player.isOnline()) {
                sendJsonDataToServer(json,player);
            }

            // Remove player data
            movementDataMap.remove(player.getUniqueId());
        }
    }

    private void sendJsonDataToServer(String json, Player player) {
        try {
            URL url = new URL(Main.getServerBaseUrl() + "analyse/large");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = json.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String jsonResponse = readResponse(connection);

                Map<String, Double> extractedData = extractKeys(jsonResponse, "cheating", "legitimate", "baritone","baritone.mining","baritone.walking");

                String largestKey = findLargestKey(extractedData);
                for (Player debugPlayer : Main.getDebugPlayers()) {
                    debugPlayer.sendMessage(Main.getPrefixDebugger() + jsonResponse);
                }
                
                if (!"legitimate".equals(largestKey)) {
                    for (Player adminPlayer : Main.getAdminNotificationPlayers()) {
                        adminPlayer.sendMessage(Main.getPrefix() + "§aVerdict for " + player.getName() + " at " + System.currentTimeMillis() + ": §d" + largestKey);
                    }
                }
                for (Player debugPlayer : Main.getDebugPlayers()) {
                    debugPlayer.sendMessage(Main.getPrefixDebugger() + "Verdict for " + player.getName() + " at " + System.currentTimeMillis() + ": §d" + largestKey);
                }
            } else {
                System.out.println("Failed to send data. Response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void recordPlayerData(UUID playerId, Player player) {
        List<MovementData> movementDataList = movementDataMap.get(playerId);

        // Ensure that movementDataList is not null
        if (movementDataList == null) {
            return;
        }

        MovementData movementData = new MovementData(
                System.currentTimeMillis(),
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ(),
                player.getLocation().getPitch(),
                player.getLocation().getYaw()
        );

        if (!movementDataList.isEmpty() && movementDataList.get(movementDataList.size() - 1).equals(movementData)) {
            return;
        }

        movementDataList.add(movementData);
    }

    private String readResponse(HttpURLConnection connection) throws Exception {
        // Read the response from the server
        try (Scanner scanner = new Scanner(connection.getInputStream(), "UTF-8").useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private Map<String, Double> extractKeys(String json, String... keys) {
        // Extract specific keys from the JSON response
        Map<String, Object> jsonObject = new Gson().fromJson(json, Map.class);

        Map<String, Double> extractedData = new HashMap<>();
        for (String key : keys) {
            if (jsonObject.containsKey(key) && jsonObject.get(key) instanceof Number) {
                extractedData.put(key, ((Number) jsonObject.get(key)).doubleValue());
            }
        }

        return extractedData;
    }

    private String findLargestKey(Map<String, Double> data) {
        // Find the key with the largest value in the map
        String largestKey = null;
        double largestValue = Double.MIN_VALUE;

        for (Map.Entry<String, Double> entry : data.entrySet()) {
            if (entry.getValue() > largestValue) {
                largestKey = entry.getKey();
                largestValue = entry.getValue();
            }
        }

        return largestKey;
    }


    private String generateJson(String username, List<MovementData> movementDataList) {
        // Convert the list of movement data to JSON format
        // (You can use a library like Gson for better JSON handling)
        StringBuilder json = new StringBuilder("{\"playerName\": \"" + username + "\",\"headmovement\": [");

        for (MovementData data : movementDataList) {
            json.append("{\"timeSinceStarted\": ").append(data.getTimeSinceStarted())
                    .append(",\"x\": ").append(data.getX())
                    .append(",\"y\": ").append(data.getY())
                    .append(",\"z\": ").append(data.getZ())
                    .append(",\"pitch\": ").append(data.getPitch())
                    .append(",\"yaw\": ").append(data.getYaw())
                    .append("},");
        }

        // Remove the trailing comma
        if (json.charAt(json.length() - 1) == ',') {
            json.deleteCharAt(json.length() - 1);
        }

        json.append("]}");

        return json.toString();
    }

}
