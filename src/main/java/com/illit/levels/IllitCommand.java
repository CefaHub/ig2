package com.illit.levels;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IllitCommand implements CommandExecutor, TabCompleter {

    private final IllitLevelsPlugin plugin;
    private final LevelService levelService;

    public IllitCommand(IllitLevelsPlugin plugin, LevelService levelService) {
        this.plugin = plugin;
        this.levelService = levelService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§aIllitLevels §7commands:");
            sender.sendMessage("§e/illit addexp <player> <amount> §7- add exp");
            sender.sendMessage("§e/illit setlevel <player> <level> §7- set level (exp resets)");
            sender.sendMessage("§e/illit reset <player> §7- reset progress");
            sender.sendMessage("§e/illit info [player] §7- show progress");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "addexp": {
                if (args.length < 3) return false;
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                long amount;
                try {
                    amount = Long.parseLong(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cAmount must be a number.");
                    return true;
                }
                LevelUpResult r = levelService.addExp(target.getUniqueId(), amount);
                sender.sendMessage("§aAdded §e" + amount + "§a exp to §e" + target.getName() + "§a. Levels gained: §e" + r.levelsGained());
                if (r.reachedMax()) sender.sendMessage("§6" + target.getName() + " reached MAX level!");
                return true;
            }
            case "setlevel": {
                if (args.length < 3) return false;
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                int level;
                try {
                    level = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cLevel must be a number.");
                    return true;
                }
                levelService.setLevel(target.getUniqueId(), level);
                sender.sendMessage("§aSet level for §e" + target.getName() + "§a to §e" + level + "§a (exp reset).");
                return true;
            }
            case "reset": {
                if (args.length < 2) return false;
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                levelService.reset(target.getUniqueId());
                sender.sendMessage("§aReset progress for §e" + target.getName() + "§a.");
                return true;
            }
            case "info": {
                OfflinePlayer target;
                if (args.length >= 2) target = Bukkit.getOfflinePlayer(args[1]);
                else if (sender instanceof Player p) target = p;
                else {
                    sender.sendMessage("§cUsage: /illit info <player>");
                    return true;
                }

                int lvl = levelService.getLevel(target.getUniqueId());
                long exp = levelService.getExp(target.getUniqueId());
                int next = Math.min(lvl + 1, levelService.getMaxLevel());
                long toNext = levelService.getExpToNext(target.getUniqueId());

                sender.sendMessage("§aIllitLevels §7for §e" + target.getName());
                sender.sendMessage("§7Level: §e" + lvl + "§7 / §e" + levelService.getMaxLevel());
                sender.sendMessage("§7Exp: §e" + exp);
                sender.sendMessage("§7Next level: §e" + next);
                sender.sendMessage("§7Exp to next: §e" + toNext);
                return true;
            }
            default:
                sender.sendMessage("§cUnknown subcommand. Use /illit help");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("illit.admin")) return List.of();

        if (args.length == 1) {
            return filterPrefix(Arrays.asList("help", "addexp", "setlevel", "reset", "info"), args[0]);
        }
        if (args.length == 2 && Arrays.asList("addexp", "setlevel", "reset", "info").contains(args[0].toLowerCase())) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return filterPrefix(names, args[1]);
        }
        return List.of();
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        String p = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(p)) out.add(s);
        }
        return out;
    }
}
