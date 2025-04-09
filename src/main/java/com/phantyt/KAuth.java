package com.phantyt;

import net.md_5.bungee.api.ChatMessageType;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.bukkit.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class KAuth extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private Map<UUID, BossBar> bossBars = new HashMap<>();
    private Map<UUID, Boolean> playerStartStatus = new HashMap<>();
    private Map<UUID, Integer> timerTasks = new HashMap<>();
    private Map<UUID, String> authCodes = new HashMap<>();
    private Map<UUID, Boolean> authenticatedPlayers = new HashMap<>();
    private Map<UUID, Integer> taskIds = new HashMap<>();

    private static final String CHARACTERS = "8073380657:AAHPLuU8mNi4mGzWMnnWdNlAdHtVQZUhdL8";
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        ToolAuthRegister toolAuthRegister = new ToolAuthRegister(this);
        toolAuthRegister.onEnable();

        getServer().getPluginManager().registerEvents(this, this);

        startPermissionsCheckTask();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (authenticatedPlayers.getOrDefault(player.getUniqueId(), true)) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (authenticatedPlayers.getOrDefault(player.getUniqueId(), true)) return;

        event.setCancelled(true);
        player.sendMessage("  §x§8§0§0§0§0§0§lK§x§9§D§1§1§1§1§lA§x§B§A§2§2§2§2§lᴜ§x§D§7§3§3§3§3§lᴛ§x§F§F§4§4§4§4§lʜ");
        player.sendMessage(" §x§F§F§A§4§3§E§l⎛ §fВы §c§nне можете §fиспользовать комманды!");
        player.sendMessage(" §x§F§F§A§4§3§E§l⎝ §fЗавершите §6проверку §fи попробуйте §bещё §fраз.");
        player.sendMessage("");
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!authenticatedPlayers.getOrDefault(player.getUniqueId(), true)) {
            String message = event.getMessage();

            event.setCancelled(true);

            if (message.length() == 15 && authCodes.containsKey(player.getUniqueId())) {
                if (checkAuthCode(player, message)) {
                    authenticatedPlayers.put(player.getUniqueId(), true);
                    for (int i = 0; i < 20; i++) {
                        player.sendMessage("");
                    }
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.0f);
                    player.sendMessage("  §x§8§0§0§0§0§0§lK§x§9§D§1§1§1§1§lA§x§B§A§2§2§2§2§lᴜ§x§D§7§3§3§3§3§lᴛ§x§F§F§4§4§4§4§lʜ");
                    player.sendMessage(" §x§F§F§A§4§3§E§l⎛ §fВы §a§nуспешно§f авторизовались!");
                    player.sendMessage(" §x§F§F§A§4§3§E§l⎝ §bУдачной вам игры.");
                    player.sendMessage("");

                    Bukkit.getScheduler().runTask(this, () -> {
                        player.removePotionEffect(PotionEffectType.BLINDNESS);
                        player.sendTitle("", "", 0, 0, 0);
                    });

                    stopNotificationTask(player);
                    stopTimer(player);
                } else {
                    for (int i = 0; i < 20; i++) {
                        player.sendMessage("");
                    }
                    player.sendMessage("  §x§8§0§0§0§0§0§lK§x§9§D§1§1§1§1§lA§x§B§A§2§2§2§2§lᴜ§x§D§7§3§3§3§3§lᴛ§x§F§F§4§4§4§4§lʜ");
                    player.sendMessage(" §x§F§F§A§4§3§E§l⎛ §fВы §c§nнеудачно §fавторизовались!");
                    player.sendMessage(" §x§F§F§A§4§3§E§l⎝ §fПопробуйте §bещё §fраз.");
                    player.sendMessage("");
                }
            } else {
                for (int i = 0; i < 20; i++) {
                    player.sendMessage("");
                }
                player.sendMessage("  §x§8§0§0§0§0§0§lK§x§9§D§1§1§1§1§lA§x§B§A§2§2§2§2§lᴜ§x§D§7§3§3§3§3§lᴛ§x§F§F§4§4§4§4§lʜ");
                player.sendMessage(" §x§F§F§A§4§3§E§l⎛ §fВы §c§nнеудачно §fавторизовались!");
                player.sendMessage(" §x§F§F§A§4§3§E§l⎝ §fПопробуйте §bещё §fраз.");
                player.sendMessage("");
            }
        }
    }

    private void startPermissionsCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkPermissions(player);
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(this, () -> {
            Location loc = player.getLocation();
            if (!player.isOnGround()) {
                Location groundLoc = findGroundLocation(loc);
                if (groundLoc != null) {
                    player.teleport(groundLoc);
                    player.sendMessage(ChatColor.YELLOW + "Вы были телепортированы на землю для прохождения проверки.");
                }
            }
        });

        checkPermissions(player);
    }

    private Location findGroundLocation(Location startLoc) {
        Location loc = startLoc.clone();
        while (loc.getY() > loc.getWorld().getMinHeight()) {
            loc.subtract(0, 1, 0);
            if (loc.getBlock().getType().isSolid()) {
                loc.add(0, 1, 0);
                return loc;
            }
        }
        return null;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        authenticatedPlayers.remove(player.getUniqueId());
        authCodes.remove(player.getUniqueId());
    }

    private void checkPermissions(Player player) {
        String botToken = config.getString("Токен бота телеграмм");

        if (botToken == null || botToken.isEmpty()) {
            if (!isAdministrator(player) && !player.isOp()) {
                player.sendMessage(ChatColor.RED + "Вам не разрешен доступ к серверу.");
                player.kickPlayer(ChatColor.RED + "Вам не разрешен доступ к серверу.");
                return; // Кик игрока
            }
            return;
        }

        if (getTelegramId(player.getName()) == null) {
            if (isAdministrator(player) || player.isOp()) {
                player.sendMessage(ChatColor.RED + "Вам не разрешен доступ к серверу.");
                player.kickPlayer(ChatColor.RED + "Вам не разрешен доступ к серверу.");
            }
            return;
        }

        if (isAdministrator(player) || player.isOp()) {
            if (!authenticatedPlayers.getOrDefault(player.getUniqueId(), false)) {
                if (!authCodes.containsKey(player.getUniqueId())) {
                    String authCode = generateAuthCode();
                    authCodes.put(player.getUniqueId(), authCode);
                    sendAuthCodeToTelegram(getTelegramId(player.getName()), player, authCode);

                    player.playSound(player.getLocation(), Sound.ENTITY_DOLPHIN_DEATH, 1.0f, 1.0f);

                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));

                    player.sendTitle(ChatColor.RED + "§x§F§F§D§E§5§8☁ §x§F§F§D§A§5§8ᴛ§x§F§F§D§6§5§7ᴇ§x§F§F§D§2§5§7ʟ§x§F§F§C§E§5§6ᴇ§x§F§F§C§A§5§6ɢ§x§F§F§C§6§5§5ʀ§x§F§F§C§3§5§5ᴀ§x§F§F§B§F§5§4ᴍ§x§F§F§B§B§5§4-§x§F§F§B§7§5§3ᴀ§x§F§F§B§3§5§3ᴘ§x§F§F§A§F§5§2ɪ §x§F§F§A§7§5§1☁", ChatColor.YELLOW + "§x§F§F§D§E§5§8К вам в телеграмм пришёл код.", 10, Integer.MAX_VALUE, 0);

                    authenticatedPlayers.put(player.getUniqueId(), false);

                    startNotificationTask(player);

                    startTimer(player);
                }
            }
        } else {
            if (authenticatedPlayers.containsKey(player.getUniqueId())) {
                authenticatedPlayers.remove(player.getUniqueId());
            }
        }
    }


    private void startNotificationTask(Player player) {
        UUID playerId = player.getUniqueId();
        String botLink = config.getString("Ссылка на бота");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!authenticatedPlayers.getOrDefault(playerId, false)) {
                    player.sendTitle(ChatColor.RED + "§x§F§F§D§E§5§8☁ §x§F§F§D§A§5§8ᴛ§x§F§F§D§6§5§7ᴇ§x§F§F§D§2§5§7ʟ§x§F§F§C§E§5§6ᴇ§x§F§F§C§A§5§6ɢ§x§F§F§C§6§5§5ʀ§x§F§F§C§3§5§5ᴀ§x§F§F§B§F§5§4ᴍ§x§F§F§B§B§5§4-§x§F§F§B§7§5§3ᴀ§x§F§F§B§3§5§3ᴘ§x§F§F§A§F§5§2ɪ §x§F§F§A§7§5§1☁", ChatColor.YELLOW + "§x§F§F§D§E§5§8К вам в телеграмм пришёл код.", 10, 40, 10);
                    for (int i = 0; i < 20; i++) {
                        player.sendMessage("");
                    }
                    player.sendMessage(" §x§8§0§0§0§0§0§lK§x§9§D§1§1§1§1§lA§x§B§A§2§2§2§2§lᴜ§x§D§7§3§3§3§3§lᴛ§x§F§F§4§4§4§4§lʜ");
                    player.sendMessage(" §6§l⎛ §fУ вас §b§nбыли замечены§f админ права");
                    player.sendMessage(" §6§l⎜ §fВам в §a§nтелеграмм§f отправлено §6§nсообщение с кодом");
                    if (botLink == null || botLink.isEmpty()) {
                        player.sendMessage(" §6§l⎝ §7§nСсылка на бота:§c Не указана");
                    } else {
                        TextComponent linkMessage = new TextComponent(" §6§l⎝ §7Ссылка на бота: ");
                        linkMessage.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, botLink));
                        linkMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent("Нажмите для перехода")}));
                        TextComponent urlComponent = new TextComponent(botLink);
                        urlComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, botLink));
                        urlComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{new TextComponent("Нажмите для перехода")}));
                        urlComponent.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                        urlComponent.setUnderlined(true);
                        linkMessage.addExtra(urlComponent);
                        player.spigot().sendMessage(linkMessage);
                        player.sendMessage("");
                    }
                } else {
                    cancel();
                }
            }
        }.runTaskTimer(this, 0, 40);
    }

    private void startTimer(Player player) {
        UUID playerId = player.getUniqueId();
        int taskId = new BukkitRunnable() {
            int timeLeft = 60;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    player.kickPlayer(ChatColor.RED + "Вы не успели ввести код аутентификации.");
                    cancel();
                } else {
                    String actionBarMessage =
                            ChatColor.GOLD + "☁ " +
                                    "ᴛɪᴍᴇ - " + timeLeft + "с " +
                                    "☁";

                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBarMessage));
                    timeLeft--;
                }
            }
        }.runTaskTimer(this, 0, 20).getTaskId();

        timerTasks.put(playerId, taskId);
    }

    private void stopTimer(Player player) {
        UUID playerId = player.getUniqueId();
        if (timerTasks.containsKey(playerId)) {
            Bukkit.getScheduler().cancelTask(timerTasks.get(playerId));
            timerTasks.remove(playerId);
        }
        if (bossBars.containsKey(playerId)) {
            bossBars.get(playerId).removePlayer(player);
            bossBars.remove(playerId);
        }
    }

    private void stopNotificationTask(Player player) {
        UUID playerId = player.getUniqueId();
        if (taskIds.containsKey(playerId)) {
            Bukkit.getScheduler().cancelTask(taskIds.get(playerId));
            taskIds.remove(playerId);
        }
    }

    private boolean isAdministrator(Player player) {
        for (String perm : config.getStringList("Права при которых человек считается администратором")) {
            if (player.hasPermission(perm)) {
                return true;
            }
        }
        return false;
    }

    private String getTelegramId(String playerName) {
        if (config.contains("Пользователи." + playerName)) {
            return config.getString("Пользователи." + playerName + ".Телеграмм айди");
        }
        return null; // Если пользователь не найден
    }


    private String generateAuthCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 15; i++) {
            int index = random.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(index));
        }

        return code.toString();
    }

    public boolean checkAuthCode(Player player, String code) {
        String storedCode = authCodes.get(player.getUniqueId());
        if (storedCode != null && storedCode.equals(code)) {
            authCodes.remove(player.getUniqueId());
            return true;
        }
        return false;
    }

    private void sendAuthCodeToTelegram(String telegramId, Player player, String authCode) {
        String botToken = config.getString("Токен бота телеграмм");

        String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        String ipAddress = player.getAddress().getAddress().getHostAddress();

        String message = "🌟 Внимание, администратор! 🌟\n" +
                "🔒 Вы только что выполнили вход с вашего администраторского аккаунта!\n\n" +
                "📅 Дата и время: " + currentDateTime + "\n" +
                "🌐 IP-адрес: " + ipAddress + "\n\n" +
                "🚀 Ваш код для подтверждения: `" + authCode + "`\n\n" +
                "✅ Если это были вы, вы можете продолжать свою работу!\n" +
                "❌ Если это не вы, немедленно свяжитесь с нашей службой поддержки. Безопасность вашего аккаунта — наш приоритет!\n\n" +
                "🛡️ Служба поддержки - @DeepseekIRL\n" +
                "🛡️ Спасибо, что остаетесь с нами!";

        try {
            URL url = new URL(TELEGRAM_API_URL + botToken + "/sendMessage");

            // Создание соединения
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            // Тело запроса
            String jsonPayload = "{\"chat_id\":\"" + telegramId + "\",\"text\":\"" + message + "\",\"parse_mode\":\"Markdown\"}";

            // Отправка запроса
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Проверка ответа
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                getLogger().warning("Ошибка отправки сообщения в Telegram: " + responseCode);
            }

        } catch (Exception e) {
            getLogger().severe("Ошибка при отправке сообщения в Telegram: " + e.getMessage());
        }
    }
}
