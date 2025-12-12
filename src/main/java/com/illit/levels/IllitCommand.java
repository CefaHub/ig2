package com.illit.levels;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IllitCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "§7[§f§lILLIT LVL§7]§r ";

    private final LevelService levelService;
    private final TopService topService;

    public IllitCommand(LevelService levelService, TopService topService) {
        this.levelService = levelService;
        this.topService = topService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("помощь")) {
            sendHelp(sender);
            return true;
        }

        String group = args[0].toLowerCase();

        if (group.equals("info") || group.equals("инфо")) return handleInfo(sender, args);
        if (group.equals("reset") || group.equals("сброс")) return handleReset(sender, args);
        if (group.equals("top") || group.equals("топ")) return handleTop(sender);

        if (args.length < 4) {
            sendHelp(sender);
            return true;
        }

        String action = args[1].toLowerCase();
        String nick = args[2];
        String amountRaw = args[3];

        OfflinePlayer target = Bukkit.getOfflinePlayer(nick);
        if (target != null && target.getName() != null) levelService.setName(target.getUniqueId(), target.getName());

        long amountLong;
        int amountInt;
        try {
            amountLong = Long.parseLong(amountRaw);
            amountInt = Integer.parseInt(amountRaw);
        } catch (NumberFormatException e) {
            sender.sendMessage(PREFIX + "§cЗначение должно быть числом.");
            return true;
        }

        if (action.equals("set") || action.equals("установить")) {
            if (amountLong < 0) {
                sender.sendMessage(PREFIX + "§cЗначение должно быть >= 0.");
                return true;
            }
            return switch (group) {
                case "exp", "опыт" -> {
                    levelService.setExp(target.getUniqueId(), amountLong);
                    sender.sendMessage(PREFIX + "§aОпыт игрока §e" + safeName(target) + "§a установлен на §e" + amountLong + "§a.");
                    yield true;
                }
                case "lvl", "level", "уровень", "лвл" -> {
                    levelService.setLevel(target.getUniqueId(), (int) amountLong);
                    sender.sendMessage(PREFIX + "§aУровень игрока §e" + safeName(target) + "§a установлен на §e" + amountLong + "§a (опыт сброшен).");
                    yield true;
                }
                default -> { sendHelp(sender); yield true; }
            };
        }

        if (amountLong <= 0) {
            sender.sendMessage(PREFIX + "§cЗначение должно быть > 0.");
            return true;
        }

        return switch (group) {
            case "exp", "опыт" -> handleExp(sender, target, action, amountLong);
            case "lvl", "level", "уровень", "лвл" -> handleLevel(sender, target, action, amountInt);
            default -> { sendHelp(sender); yield true; }
        };
    }

    private boolean handleExp(CommandSender sender, OfflinePlayer target, String action, long amount) {
        return switch (action) {
            case "give", "выдать", "add", "добавить" -> {
                LevelUpResult r = levelService.addExp(target.getUniqueId(), amount);
                sender.sendMessage(PREFIX + "§aДобавлено §e" + amount + "§a опыта игроку §e" + safeName(target) + "§a. Получено уровней: §e" + r.levelsGained());
                if (r.reachedMax()) sender.sendMessage(PREFIX + "§6Игрок §e" + safeName(target) + "§6 достиг максимального уровня!");
                yield true;
            }
            case "remove", "снять", "del", "удалить" -> {
                LevelDownResult r = levelService.removeExp(target.getUniqueId(), amount);
                sender.sendMessage(PREFIX + "§aСнято §e" + amount + "§a опыта у игрока §e" + safeName(target) + "§a. Потеряно уровней: §e" + r.levelsLost());
                if (r.hitMinLevel()) sender.sendMessage(PREFIX + "§6Игрок §e" + safeName(target) + "§6 на минимальном уровне.");
                yield true;
            }
            default -> {
                sender.sendMessage(PREFIX + "§cИспользование: /illit exp <give|remove|set> <ник> <значение>");
                yield true;
            }
        };
    }

    private boolean handleLevel(CommandSender sender, OfflinePlayer target, String action, int amount) {
        return switch (action) {
            case "give", "выдать", "add", "добавить" -> {
                LevelChangeResult r = levelService.addLevels(target.getUniqueId(), amount);
                sender.sendMessage(PREFIX + "§aДобавлено §e" + amount + "§a уровней игроку §e" + safeName(target) + "§a.");
                if (r.hitLimit()) sender.sendMessage(PREFIX + "§6Игрок §e" + safeName(target) + "§6 достиг максимального уровня!");
                yield true;
            }
            case "remove", "снять", "del", "удалить" -> {
                LevelChangeResult r = levelService.removeLevels(target.getUniqueId(), amount);
                sender.sendMessage(PREFIX + "§aСнято §e" + amount + "§a уровней у игрока §e" + safeName(target) + "§a.");
                if (r.hitLimit()) sender.sendMessage(PREFIX + "§6Игрок §e" + safeName(target) + "§6 на минимальном уровне.");
                yield true;
            }
            default -> {
                sender.sendMessage(PREFIX + "§cИспользование: /illit lvl <give|remove|set> <ник> <значение>");
                yield true;
            }
        };
    }

    private boolean handleTop(CommandSender sender) {
        List<TopService.TopEntry> top = topService.top10();
        sender.sendMessage(PREFIX + "§7Топ-10 по уровню:");
        if (top.isEmpty()) {
            sender.sendMessage(PREFIX + "§7(пока нет данных)");
            return true;
        }
        for (int i = 0; i < top.size(); i++) {
            TopService.TopEntry e = top.get(i);
            sender.sendMessage("§e" + (i + 1) + "§7. §f" + e.name() + " §7- §e" + e.level() + "§7 ур.");
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length >= 2) target = Bukkit.getOfflinePlayer(args[1]);
        else if (sender instanceof Player p) target = p;
        else {
            sender.sendMessage(PREFIX + "§cИспользование: /illit info <ник>");
            return true;
        }

        if (target != null && target.getName() != null) levelService.setName(target.getUniqueId(), target.getName());

        int lvl = levelService.getLevel(target.getUniqueId());
        long exp = levelService.getExp(target.getUniqueId());
        int next = Math.min(lvl + 1, levelService.getMaxLevel());
        long toNext = levelService.getExpToNext(target.getUniqueId());

        sender.sendMessage(PREFIX + "§7Игрок: §e" + safeName(target));
        sender.sendMessage("§7Уровень: §e" + lvl + "§7 / §e" + levelService.getMaxLevel());
        sender.sendMessage("§7Опыт: §e" + exp);
        sender.sendMessage("§7Следующий уровень: §e" + next);
        sender.sendMessage("§7Опыта до следующего: §e" + toNext);
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(PREFIX + "§cИспользование: /illit reset <ник>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target != null && target.getName() != null) levelService.setName(target.getUniqueId(), target.getName());
        levelService.reset(target.getUniqueId());
        sender.sendMessage(PREFIX + "§aПрогресс игрока §e" + safeName(target) + "§a сброшен.");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(PREFIX + "§7Команды:");
        sender.sendMessage("§e/illit lvl give <ник> <кол-во> §7— добавить уровни");
        sender.sendMessage("§e/illit lvl remove <ник> <кол-во> §7— снять уровни");
        sender.sendMessage("§e/illit lvl set <ник> <уровень> §7— установить уровень (опыт сбросится)");
        sender.sendMessage("§e/illit exp give <ник> <кол-во> §7— добавить опыт (уровни растут автоматически)");
        sender.sendMessage("§e/illit exp remove <ник> <кол-во> §7— снять опыт (уровни падают автоматически)");
        sender.sendMessage("§e/illit exp set <ник> <значение> §7— установить опыт в текущем уровне");
        sender.sendMessage("§e/illit top §7— топ-10 по уровню");
        sender.sendMessage("§e/illit info [ник] §7— информация");
        sender.sendMessage("§e/illit reset <ник> §7— сброс");

        sender.sendMessage(PREFIX + "§7Плейсхолдеры:");
        sender.sendMessage("§f%illit_level% §7%illit_exp% §7%illit_level_next% §7%illit_exp_next%");
        sender.sendMessage("§f%illit_level_format% §7%illit_exp_format% §7%illit_progress_percent% §7%illit_progress_bar%");
        sender.sendMessage("§f%illit_top_1_name%§7/§f%illit_top_1_level% §7... §f%illit_top_10_name%§7/§f%illit_top_10_level%");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("illit.admin")) return List.of();

        if (args.length == 1) {
            return filterPrefix(Arrays.asList("help","помощь","lvl","level","лвл","уровень","exp","опыт","info","инфо","reset","сброс","top","топ"), args[0]);
        }
        if (args.length == 2) {
            String g = args[0].toLowerCase();
            if (g.equals("lvl") || g.equals("level") || g.equals("лвл") || g.equals("уровень") || g.equals("exp") || g.equals("опыт")) {
                return filterPrefix(Arrays.asList("give","remove","set","выдать","снять","установить","добавить","удалить"), args[1]);
            }
        }
        if (args.length == 3) {
            String g = args[0].toLowerCase();
            if (g.equals("lvl") || g.equals("level") || g.equals("лвл") || g.equals("уровень") ||
                g.equals("exp") || g.equals("опыт") || g.equals("info") || g.equals("инфо") || g.equals("reset") || g.equals("сброс")) {
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
