package com.foxsrv.id;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ID extends JavaPlugin implements Listener, TabExecutor {
    
    private FileConfiguration usersConfig;
    private File usersFile;
    private FileConfiguration config;
    
    private boolean hasPAPI = false;
    private PlaceholderAPIHook papiHook;
    
    private class PlaceholderAPIHook {
        public String setPlaceholders(Player player, String placeholder) {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                try {
                    Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                    Object result = papiClass.getMethod("setPlaceholders", Player.class, String.class)
                                            .invoke(null, player, placeholder);
                    return result.toString();
                } catch (Exception e) {
                    return placeholder;
                }
            }
            return placeholder;
        }
    }
    
    @Override
    public void onEnable() {
        hasPAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        
        if (hasPAPI) {
            papiHook = new PlaceholderAPIHook();
            getLogger().info("PlaceholderAPI found! Placeholder support enabled.");
        } else {
            getLogger().info("PlaceholderAPI not found. Placeholder support disabled.");
        }
        
        saveDefaultConfig();
        config = getConfig();
        loadUsersConfig();
        getServer().getPluginManager().registerEvents(this, this);
        
        getLogger().info("ID Plugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        saveUsersConfig();
        getLogger().info("ID Plugin has been disabled!");
    }
    
    private void loadUsersConfig() {
        usersFile = new File(getDataFolder(), "users.yml");
        if (!usersFile.exists()) {
            saveResource("users.yml", false);
        }
        usersConfig = YamlConfiguration.loadConfiguration(usersFile);
    }
    
    private void saveUsersConfig() {
        try {
            usersConfig.save(usersFile);
        } catch (IOException e) {
            getLogger().severe("Could not save users.yml: " + e.getMessage());
        }
    }
    
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;
        
        Player clicker = event.getPlayer();
        Player target = (Player) event.getRightClicked();
        
        if (clicker.isSneaking()) {
            event.setCancelled(true);
            if (clicker.hasPermission("id.view")) {
                showID(clicker, target.getName());
                clicker.sendMessage(getMessage("click-info", "&7&o(Shift + Right Click to view player ID)"));
            }
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("not-player", "&cOnly players can use this command!"));
                return true;
            }
            
            Player player = (Player) sender;
            if (!player.hasPermission("id.view")) {
                sendMessage(sender, "no-permission");
                return true;
            }
            
            showID(player, player.getName());
            return true;
        }
        
        // Primeiro verifica se é um subcomando admin
        if (args.length >= 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "name":
                    if (!sender.hasPermission("id.admin")) {
                        sendMessage(sender, "no-permission");
                        return true;
                    }
                    
                    if (args.length < 3) {
                        sender.sendMessage(getMessage("usage-name", "&cUsage: /id name <player> <name>"));
                        return true;
                    }
                    
                    String targetPlayer = args[1];
                    String newName = args[2];
                    
                    setUserData(targetPlayer, "name", newName);
                    sender.sendMessage(getMessage("name-set", "&aName set to %name% for %player%")
                            .replace("%name%", newName)
                            .replace("%player%", targetPlayer));
                    return true;
                    
                case "port":
                    if (!sender.hasPermission("id.admin")) {
                        sendMessage(sender, "no-permission");
                        return true;
                    }
                    
                    if (args.length < 3) {
                        sender.sendMessage(getMessage("usage-port", "&cUsage: /id port <player> <port>"));
                        return true;
                    }
                    
                    String portPlayer = args[1];
                    String port = args[2];
                    
                    setUserData(portPlayer, "port", port);
                    sender.sendMessage(getMessage("port-set", "&aPort set to %port% for %player%")
                            .replace("%port%", port)
                            .replace("%player%", portPlayer));
                    return true;

                case "license":
                    if (!sender.hasPermission("id.admin")) {
                        sendMessage(sender, "no-permission");
                        return true;
                    }
                    
                    if (args.length < 3) {
                        sender.sendMessage(getMessage("usage-license", "&cUsage: /id license <player> <license>"));
                        return true;
                    }
                    
                    String licensePlayer = args[1];
                    String license = args[2];
                    
                    setUserData(licensePlayer, "license", license);
                    sender.sendMessage(getMessage("license-set", "&aPort set to %license% for %player%")
                            .replace("%license%", license)
                            .replace("%player%", licensePlayer));
                    return true;
                    
                case "born":
                    if (!sender.hasPermission("id.admin")) {
                        sendMessage(sender, "no-permission");
                        return true;
                    }
                    
                    if (args.length < 3) {
                        sender.sendMessage(getMessage("usage-born", "&cUsage: /id born <player> <dd/mm/yyyy>"));
                        return true;
                    }
                    
                    String bornPlayer = args[1];
                    String date = args[2];
                    
                    if (!isValidDate(date)) {
                        sendMessage(sender, "invalid-date");
                        return true;
                    }
                    
                    setUserData(bornPlayer, "born", date);
                    sender.sendMessage(getMessage("born-set", "&aBirth date set to %date% for %player%")
                            .replace("%date%", date)
                            .replace("%player%", bornPlayer));
                    return true;
                    
                case "id":
                    if (!sender.hasPermission("id.admin")) {
                        sendMessage(sender, "no-permission");
                        return true;
                    }
                    
                    if (args.length < 3) {
                        sender.sendMessage(getMessage("usage-id", "&cUsage: /id id <player> <number>"));
                        return true;
                    }
                    
                    String idPlayer = args[1];
                    String idNum = args[2];
                    
                    setUserData(idPlayer, "id", idNum);
                    sender.sendMessage(getMessage("id-set", "&aID set to %id% for %player%")
                            .replace("%id%", idNum)
                            .replace("%player%", idPlayer));
                    return true;
                    
                case "reload":
                    if (!sender.hasPermission("id.reload")) {
                        sendMessage(sender, "no-permission");
                        return true;
                    }
                    
                    reloadConfig();
                    config = getConfig();
                    loadUsersConfig();
                    sendMessage(sender, "reloaded");
                    return true;
            }
        }
        
        // Verifica se é comando de descrição
        if (args[0].equalsIgnoreCase("desc")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("not-player", "&cOnly players can use this command!"));
                return true;
            }
            
            Player player = (Player) sender;
            if (!player.hasPermission("id.desc")) {
                sendMessage(sender, "no-permission");
                return true;
            }
            
            if (args.length < 2) {
                player.sendMessage(getMessage("usage-desc", "&cUsage: /id desc <text>"));
                return true;
            }
            
            String desc = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            if (desc.length() > 256) {
                sendMessage(sender, "desc-too-long");
                return true;
            }
            
            setUserData(player.getName(), "desc", desc);
            sendMessage(sender, "desc-set");
            return true;
        }
        
        // Se não for nenhum dos acima, assume que é para mostrar ID de jogador
        if (!sender.hasPermission("id.view")) {
            sendMessage(sender, "no-permission");
            return true;
        }
        
        showID(sender, args[0]);
        return true;
    }
    
    private void showID(CommandSender sender, String playerName) {
        ConfigurationSection userSection = usersConfig.getConfigurationSection("users." + playerName);
        
        if (userSection == null) {
            userSection = usersConfig.createSection("users." + playerName);
            userSection.set("name", playerName);
            userSection.set("port", "");
            userSection.set("license", "");
            userSection.set("born", "");
            userSection.set("id", "");
            userSection.set("desc", "");
            saveUsersConfig();
        }
        
        String name = userSection.getString("name", playerName);
        String port = userSection.getString("port", "");
        String license = userSection.getString("license", "");
        String born = userSection.getString("born", "");
        String id = userSection.getString("id", "");
        String desc = userSection.getString("desc", "");
        
        Player targetPlayer = Bukkit.getPlayer(playerName);
        
        List<String> lines = new ArrayList<>();
        
        lines.add(getMessage("id-format.header", "&6&l=========== User Identity ==========="));
        
        // Nome - sempre mostra
        addFormattedLine(lines, "Name:", !name.isEmpty() ? name : "--");
        
        // Data de nascimento - sempre mostra
        addFormattedLine(lines, "Born date:", !born.isEmpty() ? born : "--");
        
        // Porte - sempre mostra
        addFormattedLine(lines, "Port:", !port.isEmpty() ? port : "--");

        // Licença - sempre mostra
        addFormattedLine(lines, "License:", !license.isEmpty() ? license : "--");
        
        // ID - sempre mostra
        addFormattedLine(lines, "ID:", !id.isEmpty() ? id : "--");
        
        // Placeholders extras da config (se habilitados)
        if (config.getBoolean("Placeholders", true) && hasPAPI && targetPlayer != null) {
            ConfigurationSection papiList = config.getConfigurationSection("PAPI_LIST");
            if (papiList != null) {
                for (String key : papiList.getKeys(false)) {
                    String text = papiList.getString(key + ".text", "");
                    String placeholder = papiList.getString(key + ".placeholder", "");
                    
                    if (!text.isEmpty() && !placeholder.isEmpty()) {
                        String value = placeholder;
                        if (hasPAPI && papiHook != null) {
                            value = papiHook.setPlaceholders(targetPlayer, placeholder);
                        }
                        if (!value.equals(placeholder)) { // Só mostra se placeholder foi resolvido
                            addFormattedLine(lines, text, value);
                        }
                    }
                }
            }
        }
        
        // Descrição
        lines.add(getMessage("id-format.desc-header", "&6&l============ Description ==========="));
        
        if (!desc.isEmpty()) {
            if (desc.length() > 50) {
                String[] words = desc.split(" ");
                StringBuilder currentLine = new StringBuilder();
                for (String word : words) {
                    if (currentLine.length() + word.length() + 1 > 50) {
                        lines.add(ChatColor.WHITE + currentLine.toString());
                        currentLine = new StringBuilder(word);
                    } else {
                        if (currentLine.length() > 0) {
                            currentLine.append(" ");
                        }
                        currentLine.append(word);
                    }
                }
                if (currentLine.length() > 0) {
                    lines.add(ChatColor.WHITE + currentLine.toString());
                }
            } else {
                lines.add(ChatColor.WHITE + desc);
            }
        } else {
            lines.add(ChatColor.GRAY + "No description set.");
        }
        
        lines.add(getMessage("id-format.footer", "&6&l========================================"));
        
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }
    
    private void addFormattedLine(List<String> lines, String text, String value) {
        String format = config.getString("id-format.line-format", "&e%text% &f%value%");
        String line = ChatColor.translateAlternateColorCodes('&', format)
            .replace("%text%", text)
            .replace("%value%", value);
        lines.add(line);
    }
    
    private void setUserData(String playerName, String key, String value) {
        String path = "users." + playerName + "." + key;
        usersConfig.set(path, value);
        saveUsersConfig();
    }
    
    private String getMessage(String path, String defaultValue) {
        String message = config.getString(path, defaultValue);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    private void sendMessage(CommandSender sender, String messagePath) {
        Map<String, String> defaultMessages = new HashMap<>();
        defaultMessages.put("no-permission", "&cYou don't have permission to do that!");
        defaultMessages.put("player-not-found", "&cPlayer not found!");
        defaultMessages.put("invalid-date", "&cInvalid date format! Use DD/MM/YYYY");
        defaultMessages.put("desc-too-long", "&cDescription too long! Max 256 characters.");
        defaultMessages.put("desc-set", "&aDescription set successfully!");
        defaultMessages.put("name-set", "&aName set to %name% for %player%");
        defaultMessages.put("port-set", "&aPort set to %port% for %player%");
        defaultMessages.put("license-set", "&aLicense set to %license% for %player%");
        defaultMessages.put("born-set", "&aBirth date set to %date% for %player%");
        defaultMessages.put("id-set", "&aID set to %id% for %player%");
        defaultMessages.put("reloaded", "&aConfiguration reloaded!");
        defaultMessages.put("click-info", "&7&o(Shift + Right Click to view player ID)");
        defaultMessages.put("not-player", "&cOnly players can use this command!");
        defaultMessages.put("usage-desc", "&cUsage: /id desc <text>");
        defaultMessages.put("usage-name", "&cUsage: /id name <player> <name>");
        defaultMessages.put("usage-port", "&cUsage: /id port <player> <port>");
        defaultMessages.put("usage-license", "&cUsage: /id license <player> <license>");
        defaultMessages.put("usage-born", "&cUsage: /id born <player> <dd/mm/yyyy>");
        defaultMessages.put("usage-id", "&cUsage: /id id <player> <number>");
        
        String message = config.getString("messages." + messagePath);
        if (message == null) {
            message = defaultMessages.getOrDefault(messagePath, "&cError: Message not found!");
        }
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    private boolean isValidDate(String date) {
        try {
            // Verifica formato básico
            if (!date.matches("\\d{2}/\\d{2}/\\d{4}")) {
                return false;
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            sdf.setLenient(false);
            Date parsed = sdf.parse(date);
            
            // Verifica se a data é válida (não é futura demais, etc)
            Calendar cal = Calendar.getInstance();
            cal.setTime(parsed);
            int year = cal.get(Calendar.YEAR);
            
            // Validações básicas (ano entre 1900-2100)
            return year >= -10000 && year <= 10000;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            
            // Mostra jogadores online primeiro
            completions.addAll(getOnlinePlayerNames());
            
            // Adiciona subcomandos baseado nas permissões
            if (sender.hasPermission("id.desc")) {
                if ("desc".startsWith(input)) completions.add("desc");
            }
            
            if (sender.hasPermission("id.admin")) {
                String[] adminCommands = {"name", "port", "license", "born", "id", "reload"};
                for (String cmd : adminCommands) {
                    if (cmd.startsWith(input)) completions.add(cmd);
                }
            }
        } 
        else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "name":
                case "port":
                case "license":
                case "born":
                case "id":
                    if (sender.hasPermission("id.admin")) {
                        completions.addAll(getAllPlayerNames());
                    }
                    break;
                case "desc":
                    completions.add("<description>");
                    break;
                default:
                    // Para mostrar ID de outro jogador
                    completions.addAll(getAllPlayerNames());
                    break;
            }
        }
        else if (args.length == 3) {
            if (sender.hasPermission("id.admin")) {
                switch (args[0].toLowerCase()) {
                    case "name":
                        completions.add("<new_name>");
                        break;
                    case "port":
                        completions.add("<port_text>");
                        break;
                    case "license":
                        completions.add("<license_text>");
                        break;
                    case "born":
                        completions.add("<dd/mm/yyyy>");
                        break;
                    case "id":
                        completions.add("<id_number>");
                        break;
                }
            }
        }
        
        String currentArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(currentArg))
                .sorted()
                .collect(Collectors.toList());
    }
    
    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }
    
    private List<String> getAllPlayerNames() {
        List<String> names = new ArrayList<>();
        names.addAll(getOnlinePlayerNames());
        
        ConfigurationSection usersSection = usersConfig.getConfigurationSection("users");
        if (usersSection != null) {
            names.addAll(usersSection.getKeys(false));
        }
        
        return names.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}