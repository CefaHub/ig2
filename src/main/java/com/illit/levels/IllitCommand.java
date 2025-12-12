package com.illit.levels;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IllitCommand implements CommandExecutor, TabCompleter {

    private final LevelService levelService;

    public IllitCommand(IllitLevelsPlugin plugin, LevelService levelService) {
        this.levelService = levelService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String group = args[0].toLowerCase();

        if (group.equals("info")) return handleInfo(sender, args);
        if (group.equals("reset")) return handleReset(sender, args);

        if (args.length < 4) {
            sendHelp(sender);
            return true;
        }

        String action = args[1].toLowerCase();
        String nick = args[2];
        String amountRaw = args[3];

        OfflinePlayer target = Bukkit.getOfflinePlayer(nick);

        long amountLong;
        int amountInt;
        try {
            amountLong = Long.parseLong(amountRaw);
            amountInt = Integer.parseInt(amountRaw);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cAmount must be a number.");
            return true;
        }

        if (amountLong <= 0) {
            sender.sendMessage("§cAmount must be > 0.");
            return true;
        }

        return switch (group) {
            case "exp" -> handleExp(sender, target, action, amountLong);
            case "lvl", "level" -> handleLevel(sender, target, action, amountInt);
            default -> { sendHelp(sender); yield true; }
        };
    }

    private boolean handleExp(CommandSender sender, OfflinePlayer target, String action, long amount) {
        return switch (action) {
            case "give" -> {
                LevelUpResult r = levelService.addExp(target.getUniqueId(), amount);
                sender.sendMessage("§a[IllitLevels] Added §e" + amount + "§a exp to §e" + safeName(target) + "§a. Levels gained: §e" + r.levelsGained());
                if (r.reachedMax()) sender.sendMessage("§6[IllitLevels] " + safeName(target) + " reached MAX level!");
                yield true;
            }
            case "remove" -> {
                LevelDownResult r = levelService.removeExp(target.getUniqueId(), amount);
                sender.sendMessage("§a[IllitLevels] Removed §e" + amount + "§a exp from §e" + safeName(target) + "§a. Levels lost: §e" + r.levelsLost());
                if (r.hitMinLevel()) sender.sendMessage("§6[IllitLevels] " + safeName(target) + " is at MIN level.");
                yield true;
            }
            default -> {
                sender.sendMessage("§cUsage: /illit exp <give|remove> <nick> <amount>");
                yield true;
            }
        };
    }

    private boolean handleLevel(CommandSender sender, OfflinePlayer target, String action, int amount) {
        return switch (action) {
            case "give" -> {
                LevelChangeResult r = levelService.addLevels(target.getUniqueId(), amount);
                sender.sendMessage("§a[IllitLevels] Added §e" + amount + "§a levels to §e" + safeName(target) + "§a.");
                if (r.hitLimit()) sender.sendMessage("§6[IllitLevels] " + safeName(target) + " reached MAX level!");
                yield true;
            }
            case "remove" -> {
                LevelChangeResult r = levelService.removeLevels(target.getUniqueId(), amount);
                sender.sendMessage("§a[IllitLevels] Removed §e" + amount + "§a levels from §e" + safeName(target) + "§a.");
                if (r.hitLimit()) sender.sendMessage("§6[IllitLevels] " + safeName(target) + " is at MIN level.");
                yield true;
            }
            default -> {
                sender.sendMessage("§cUsage: /illit lvl <give|remove> <nick> <amount>");
                yield true;
            }
        };
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length >= 2) target = Bukkit.getOfflinePlayer(args[1]);
        else if (sender instanceof Player p) target = p;
        else {
            sender.sendMessage("§cUsage: /illit info <nick>");
            return true;
        }

        int lvl = levelService.getLevel(target.getUniqueId());
        long exp = levelService.getExp(target.getUniqueId());
        int next = Math.min(lvl + 1, levelService.getMaxLevel());
        long toNext = levelService.getExpToNext(target.getUniqueId());

        sender.sendMessage("§a[IllitLevels] §7Player: §e" + safeName(target));
        sender.sendMessage("§7Level: §e" + lvl + "§7 / §e" + levelService.getMaxLevel());
        sender.sendMessage("§7Exp: §e" + exp);
        sender.sendMessage("§7Next level: §e" + next);
        sender.sendMessage("§7Exp to next: §e" + toNext);
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /illit reset <nick>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        levelService.reset(target.getUniqueId());
        sender.sendMessage("§a[IllitLevels] Reset progress for §e" + safeName(target) + "§a.");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§aIllitLevels §7commands:");
        sender.sendMessage("§e/illit lvl give <nick> <amount> §7- add levels");
        sender.sendMessage("§e/illit lvl remove <nick> <amount> §7- remove levels (min " + levelService.getMinLevel() + ")");
        sender.sendMessage("§e/illit exp give <nick> <amount> §7- add exp (levels up automatically)");
        sender.sendMessage("§e/illit exp remove <nick> <amount> §7- remove exp (levels down automatically, min " + levelService.getMinLevel() + ")");
        sender.sendMessage("§e/illit info [nick] §7- show progress");
        sender.sendMessage("§e/illit reset <nick> §7- reset to min level");
        sender.sendMessage("§7Placeholders: §f%illit_level% §7%illit_exp% §7%illit_level_next% §7%illit_exp_next%");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("illit.admin")) return List.of();

        if (args.length == 1) {
            return filterPrefix(Arrays.asList("help", "lvl", "level", "exp", "info", "reset"), args[0]);
        }
        if (args.length == 2) {
            String g = args[0].toLowerCase();
            if (g.equals("lvl") || g.equals("level") || g.equals("exp")) {
                return filterPrefix(Arrays.asList("give", "remove"), args[1]);
            }
        }
        if (args.length == 3) {
            String g = args[0].toLowerCase();
            if (g.equals("lvl") || g.equals("level") || g.equals("exp") || g.equals("info") || g.equals("reset")) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
                return filterPrefix(names, args[2]);
            }
        }
        return List.of();
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        String p = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String s : list) if (s.toLowerCase().startsWith(p)) out.add(s);
        return out;
    }

    private String safeName(OfflinePlayer p) {
        return (p != null && p.getName() != null) ? p.getName() : "unknown";
    }
}
