package com.phantyt;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolAuthRegister implements CommandExecutor, Listener, TabCompleter {

    private final KAuth plugin; // Ссылка на основной класс плагина
    private final FileConfiguration config;
    private final Map<Player, String> registrationMap; // Сохраняем игроков, ожидающих ввода Telegram ID
    private final Map<Player, String> tempPlayerData; // Сохраняем данные временно для игроков

    // Конструктор, принимающий основной класс плагина
    public ToolAuthRegister(KAuth plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.registrationMap = new HashMap<>();
        this.tempPlayerData = new HashMap<>();
    }

    public void onEnable() {
        plugin.getCommand("toolauth").setExecutor(this);
        plugin.getCommand("toolauth").setTabCompleter(this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("toolauth")) {
            // Проверка, если команда введена без аргументов
            if (args.length == 0) {
                sender.sendMessage("§x§F§F§B§4§4§0§l|");
                sender.sendMessage("§x§F§F§B§4§4§0§l|");
                sender.sendMessage("§x§F§F§B§4§4§0§l|                     §x§F§F§E§2§5§9§l[ §x§F§F§D§7§5§8§lK§x§F§F§D§2§5§7§lA§x§F§F§C§D§5§6§lᴜ§x§F§F§C§7§5§5§lᴛ§x§F§F§C§2§5§5§lʜ §x§F§F§A§7§5§1§l]");
                sender.sendMessage("§x§F§F§B§4§4§0§l|");
                sender.sendMessage("§x§F§F§B§4§4§0§l|      §cУкажите комманду для использования плагина!");
                sender.sendMessage("§x§F§F§B§4§4§0§l|");
                sender.sendMessage("§x§F§F§B§4§4§0§l|          §cПлагин §eнаходится §cна бета разработке.");
                sender.sendMessage("§x§F§F§B§4§4§0§l|              §cТелеграм создателя: §e@pogostik");
                sender.sendMessage("§x§F§F§B§4§4§0§l|");
                sender.sendMessage("§x§F§F§B§4§4§0§l|");
                return true;
            }

            if (args.length > 0 && args[0].equalsIgnoreCase("register")) {
                // Проверка прав на выполнение команды
                if (!(sender instanceof Player) || !sender.hasPermission("toolauth.register")) {
                    for (int i = 0; i < 20; i++) {
                        sender.sendMessage("");
                    }
                    sender.sendMessage("  §x§F§F§A§4§3§E§lᴀᴜᴛʜ-sʏsᴛᴇᴍ");
                    sender.sendMessage(" §x§F§F§A§4§3§E§l⎛ §fУ вас §c§nнедостаточно§f прав");
                    sender.sendMessage(" §x§F§F§A§4§3§E§l⎝ §fДля §a§nвыполнения§f данной комманды.");
                    sender.sendMessage("");
                    return true;
                }

                if (sender instanceof Player) {
                    Player admin = (Player) sender;
                    if (args.length < 2) {
                        for (int i = 0; i < 20; i++) {
                            admin.sendMessage("");
                        }
                        admin.sendMessage("  §x§F§F§A§4§3§E§lᴀᴜᴛʜ-sʏsᴛᴇᴍ");
                        admin.sendMessage(" §x§F§F§A§4§3§E§l⎛ §fВы §c§nне указали§f ник игрока");
                        admin.sendMessage(" §x§F§F§A§4§3§E§l⎝ §fНа регистрацию §b§nтелеграм айди.");
                        admin.sendMessage("");
                        return true;
                    }

                    String playerName = args[1];
                    Player targetPlayer = Bukkit.getPlayer(playerName);

                    if (targetPlayer != null) {
                        // Настройка для целевого игрока
                        for (int i = 0; i < 20; i++) {
                            targetPlayer.sendMessage("");
                        }
                        targetPlayer.sendMessage("  §x§F§F§A§4§3§E§lᴀᴜᴛʜ-sʏsᴛᴇᴍ");
                        targetPlayer.sendMessage(" §x§F§F§A§4§3§E§l⎛ §fВы §a§nвызваны§f на регистрацию!");
                        targetPlayer.sendMessage(" §x§F§F§A§4§3§E§l⎜ §fВведите свой §b§nтелеграм айди§f в чат.");
                        targetPlayer.sendMessage(" §x§F§F§A§4§3§E§l⎝ §fПолучить айди - §b§nusername_to_id_bot");
                        targetPlayer.sendMessage("");

                        targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
                        targetPlayer.setWalkSpeed(0); // Блокируем движение
                        targetPlayer.setAllowFlight(false); // Запрещаем полет

                        registrationMap.put(targetPlayer, "waiting_for_telegram_id");
                        tempPlayerData.put(targetPlayer, playerName); // Сохраняем имя игрока для дальнейшей обработки
                        for (int i = 0; i < 20; i++) {
                            admin.sendMessage("");
                        }
                        admin.sendMessage("  §x§F§F§A§4§3§E§lᴀᴜᴛʜ-sʏsᴛᴇᴍ");
                        admin.sendMessage(" §x§F§F§A§4§3§E§l⎛ §fВы §a§nвызвали§f игрока §6§n" + playerName);
                        admin.sendMessage(" §x§F§F§A§4§3§E§l⎝ §fНа регистрацию §b§nтелеграм айди.");
                        admin.sendMessage("");
                    } else {
                        admin.sendMessage("  §x§F§F§A§4§3§E§lᴀᴜᴛʜ-sʏsᴛᴇᴍ");
                        admin.sendMessage(" §x§F§F§A§4§3§E§l⎛ §fИгрок §b§n" + playerName + "§f не в сети");
                        admin.sendMessage(" §x§F§F§A§4§3§E§l⎝ §fПоробуйте §a§nвызвать§f его позднее.");
                        admin.sendMessage("");
                    }
                } else {
                    sender.sendMessage("Эта команда доступна только игрокам.");
                }
                return true;
            }
        }
        return false;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("toolauth")) {
            if (args.length == 1) {
                completions.add("register");
            } else if (args.length == 2 && sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("toolauth.register")) {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        completions.add(onlinePlayer.getName());
                    }
                }
            }
        }
        return completions;
    }


    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Проверка, находится ли игрок в процессе регистрации
        if (registrationMap.containsKey(player)) {
            String state = registrationMap.get(player);

            // Если ожидается ввод Telegram ID
            if (state.equals("waiting_for_telegram_id")) {
                String telegramId = event.getMessage();
                String playerName = tempPlayerData.get(player); // Получаем ник игрока

                // Сохраняем Telegram ID в конфигурацию
                config.set("Пользователи." + playerName + ".Телеграмм айди", telegramId);
                plugin.saveConfig(); // Сохраняем изменения в конфигурации

                for (int i = 0; i < 20; i++) {
                    player.sendMessage("");
                }
                player.sendMessage("  §x§F§F§A§4§3§E§lᴀᴜᴛʜ-sʏsᴛᴇᴍ");
                player.sendMessage(" §x§F§F§A§4§3§E§l⎛ §fВы §aуспешно§f зарегестрировались!");
                player.sendMessage(" §x§F§F§A§4§3§E§l⎝ §fВаш §b§nTelegramID§a " + telegramId);
                player.sendMessage("");

                // Убираем эффект слепоты и восстанавливаем возможность двигаться
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                    player.setWalkSpeed(0.2f); // Восстанавливаем скорость
                    player.setAllowFlight(false); // Запрещаем полет

                    registrationMap.remove(player); // Удаляем игрока из процесса регистрации
                    tempPlayerData.remove(player); // Удаляем временные данные
                });

                event.setCancelled(true); // Отменяем событие чата
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (registrationMap.containsKey(player)) {
            event.setCancelled(true); // Блокируем движение
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (registrationMap.containsKey(player)) {
            event.setCancelled(true); // Блокируем телепортацию
        }
    }
}
