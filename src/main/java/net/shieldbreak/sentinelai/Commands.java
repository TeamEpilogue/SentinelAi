package net.shieldbreak.sentinelai;

import com.google.gson.Gson;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Commands implements CommandExecutor {

    private final List<MovementData> movementDataList = new ArrayList<>();
    private Player targetPlayer;
    private int dataPointsRecorded = 0;
    private int maxDataPoints = 400;
    private String json;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (command.getName().equalsIgnoreCase("sentinel")) {
            if (strings.length == 0) {
                commandSender.sendMessage(ChatColor.RED + "Usage: /sentinel <subcommand> [options]");
                return true;
            }
            switch (strings[0].toLowerCase()) {
                case "debug":
                    if (!(commandSender instanceof Player)) {
                        commandSender.sendMessage(ChatColor.RED + "Only players can use this command.");
                        return true;
                    }
                    Player player = (Player) commandSender;
                    if (Main.isPlayerInDebug(player)) {
                        Main.removePlayerFromDebug(player);
                        player.sendMessage(Main.prefix + ChatColor.GREEN + "Debug mode disabled.");
                    } else {
                        Main.addPlayerToDebug(player);
                        player.sendMessage(Main.prefix + ChatColor.GREEN + "Debug mode enabled.");
                    }
                    break;
                case "notify":
                    if (!(commandSender instanceof Player)) {
                        commandSender.sendMessage(ChatColor.RED + "Only players can use this command.");
                        return true;
                    }
                    Player notifyPlayer = (Player) commandSender;
                    if (Main.isPlayerInAdminNotifications(notifyPlayer)) {
                        Main.removePlayerFromAdminNotifications(notifyPlayer);
                        notifyPlayer.sendMessage(Main.prefix + ChatColor.GREEN + "Admin notifications disabled.");
                    } else {
                        Main.addPlayerToAdminNotifications(notifyPlayer);
                        notifyPlayer.sendMessage(Main.prefix + ChatColor.GREEN + "Admin notifications enabled.");
                    }
                    break;
                case "analyze":
                    movementDataList.clear();

                    targetPlayer = commandSender.getServer().getPlayer(strings[1]);

                    if (targetPlayer == null) {
                        commandSender.sendMessage("Player not found or not online.");
                        return true;
                    }


                    startRecording(commandSender, targetPlayer.getName(), targetPlayer);


                default:
                    commandSender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /sentinel for usage.");
            }
            return true;
        }


        return false;
    }

    private void startRecording(CommandSender sender, String username, Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (dataPointsRecorded >= maxDataPoints) {
                    stopRecording(username,sender);

                    this.cancel();
                    return;
                }
                String analyzingMessage = "";
                if(showTextRed(dataPointsRecorded)) {
                    analyzingMessage = "§fAnalyzing §x§1§b§d§9§6§e§l§l"+username;
                } else {
                    analyzingMessage = "§cAnalyzing §x§1§b§d§9§6§e§l§l"+username;
                }
                sender.sendMessage("");
                sender.sendMessage("");
                sender.sendMessage("");
                sender.sendMessage("");
                sender.sendMessage("   §aAnalysis in progress §a(" + dataPointsRecorded + "/" + maxDataPoints + ")");
                sender.sendMessage("      " + analyzingMessage);
                sender.sendMessage("");
                sender.sendMessage("");
                sender.sendMessage("");
                sender.sendMessage("");
                recordPlayerData();
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L); // Run every second
    }
    public static boolean showTextRed(int number) {
        int mod = number % 40;
        return mod < 20;
    }

    private void stopRecording(String username,CommandSender sender) {
        // Generate the JSON string
        json = generateJson(username);

        // Make a request with the JSON data instead of saving it to a file
        sendJsonDataToServer(sender);

        // Reset the dataPointsRecorded counter and clear the movementDataList
        dataPointsRecorded = 0;
        movementDataList.clear();
    }

    private void recordPlayerData() {
        if (targetPlayer != null) {
            MovementData movementData = new MovementData(
                    System.currentTimeMillis(),
                    targetPlayer.getLocation().getX(),
                    targetPlayer.getLocation().getY(),
                    targetPlayer.getLocation().getZ(),
                    targetPlayer.getLocation().getPitch(),
                    targetPlayer.getLocation().getYaw()
            );

            // Check if the new movementData is equal to the last one (excluding timeSinceStarted)
            if (!movementDataList.isEmpty() && movementDataList.get(movementDataList.size() - 1).equals(movementData)) {
                return;
            }
            dataPointsRecorded++;
            movementDataList.add(movementData);
        }
    }

    private void sendJsonDataToServer(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                URL url = new URL(Main.getServerBaseUrl() + "production/analyse");

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

                    // Extract specific keys from the JSON response
                    Map<String, Double> extractedData = extractKeys(jsonResponse, "cheating", "legitimate", "baritone","baritone.mining","baritone.walking");

                    String verdictColor = "";
                    // Find the key with the largest value
                    String largestKey = findLargestKey(extractedData);
                    if (!"legitimate".equals(largestKey)) {
                        verdictColor = "§x§d§c§1§4§3§c"; // RED
                    } else {
                        verdictColor = "§x§1§b§d§9§6§e§l"; // SENTINEL GREEN
                    }

                    String finalVerdictColor = verdictColor;
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                        sender.sendMessage("");
                        sender.sendMessage("");
                        sender.sendMessage("");
                        sender.sendMessage("");
                        sender.sendMessage("");
                        sender.sendMessage("");
                        sender.sendMessage("");
                        sender.sendMessage("   §x§1§b§d§9§6§e§lAnalysis Done!");
                        sender.sendMessage("   §aSentinel verdict: "+ finalVerdictColor +largestKey);
                        sender.sendMessage("");
                        sender.sendMessage("");
                        sender.sendMessage("");
                        sender.sendMessage("");
                    });

                } else {
                    System.out.println("Failed to send data. Response code: " + responseCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String readResponse(HttpURLConnection connection) throws Exception {
        // Read the response from the server
        // (You may want to use a proper library like Jackson or Gson for JSON parsing)
        // This is a simple example and assumes the response is a String.
        try (java.util.Scanner scanner = new java.util.Scanner(connection.getInputStream(), "UTF-8").useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private Map<String, Double> extractKeys(String json, String... keys) {
        // Extract specific keys from the JSON response
        // (You may want to use a proper library like Jackson or Gson for JSON parsing)
        // This is a simple example and assumes the response is a JSON object.
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

    private String generateJson(String username) {
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
