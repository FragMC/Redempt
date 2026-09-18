package com.stufy.fragmc.redempt.commands;

import com.stufy.fragmc.redempt.Redempt;
import com.stufy.fragmc.redempt.managers.PromoCodeManager;
import com.stufy.fragmc.redempt.models.PromoCode;
import com.stufy.fragmc.redempt.utils.TimeParser;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PromoCommand implements CommandExecutor, TabCompleter {

    private final Redempt plugin;
    private final PromoCodeManager promoCodeManager;

    public PromoCommand(Redempt plugin, PromoCodeManager promoCodeManager) {
        this.plugin = plugin;
        this.promoCodeManager = promoCodeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("redempt.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                return handleCreate(sender, args);
            case "delete":
                return handleDelete(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "list":
                return handleList(sender);
            case "renew":
                return handleRenew(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(ChatColor.RED + "Usage: /promo create <name> <money> <time> <users>");
            sender.sendMessage(ChatColor.YELLOW + "Example: /promo create \"Summer Sale\" 1000 3d 10");
            sender.sendMessage(ChatColor.YELLOW + "Example: /promo create VIPCode 500 inf inf");
            return true;
        }

        try {
            String name = args[1];

            // If name has quotes, extract the full quoted name
            if (name.startsWith("\"")) {
                StringBuilder nameBuilder = new StringBuilder();
                int endIndex = 1;

                for (int i = 1; i < args.length; i++) {
                    if (i > 1) nameBuilder.append(" ");
                    nameBuilder.append(args[i].replace("\"", ""));
                    endIndex = i;
                    if (args[i].endsWith("\"")) {
                        break;
                    }
                }

                name = nameBuilder.toString();

                // Adjust args indices
                String[] newArgs = new String[args.length - (endIndex - 1)];
                newArgs[0] = args[0];
                newArgs[1] = name;
                int newIndex = 2;
                for (int i = endIndex + 1; i < args.length; i++) {
                    newArgs[newIndex++] = args[i];
                }
                args = newArgs;
            }

            if (args.length < 5) {
                sender.sendMessage(ChatColor.RED + "Usage: /promo create <name> <money> <time> <users>");
                return true;
            }

            double money = Double.parseDouble(args[2]);
            if (money <= 0) {
                sender.sendMessage(ChatColor.RED + "Money must be greater than 0!");
                return true;
            }

            long expiryTime;
            try {
                expiryTime = TimeParser.parseTime(args[3]);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Invalid time format! Use formats like: 1d, 2h30m, 1y6d, or 'inf'");
                return true;
            }

            int maxUses;
            if (args[4].equalsIgnoreCase("inf") || args[4].equalsIgnoreCase("infinite")) {
                maxUses = -1;
            } else {
                maxUses = Integer.parseInt(args[4]);
                if (maxUses <= 0) {
                    sender.sendMessage(ChatColor.RED + "Users must be greater than 0 or 'inf'!");
                    return true;
                }
            }

            PromoCode promoCode = promoCodeManager.createPromoCode(name, money, expiryTime, maxUses);

            String currencySymbol = plugin.getConfig().getString("currency-symbol", "$");

            sender.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage(ChatColor.GOLD + "✓ Promo Code Created!");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.WHITE + name);
            sender.sendMessage(ChatColor.YELLOW + "Code: " + ChatColor.WHITE + ChatColor.BOLD + promoCode.getCode());
            sender.sendMessage(ChatColor.YELLOW + "Money: " + ChatColor.WHITE + currencySymbol + money);
            sender.sendMessage(ChatColor.YELLOW + "Expires: " + ChatColor.WHITE +
                    (expiryTime == -1 ? "Never" : TimeParser.formatTime(expiryTime)));
            sender.sendMessage(ChatColor.YELLOW + "Max Uses: " + ChatColor.WHITE +
                    (maxUses == -1 ? "Unlimited" : maxUses));
            sender.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format!");
            return true;
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "Database error: " + e.getMessage());
            plugin.getLogger().severe("Database error while creating promo code: " + e.getMessage());
            return true;
        }

        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /promo delete <code>");
            return true;
        }

        String code = args[1].toUpperCase();

        try {
            PromoCode promoCode = promoCodeManager.getPromoCode(code);
            if (promoCode == null) {
                sender.sendMessage(ChatColor.RED + "Promo code not found!");
                return true;
            }

            promoCodeManager.deletePromoCode(code);
            sender.sendMessage(ChatColor.GREEN + "✓ Promo code " + ChatColor.BOLD + code +
                    ChatColor.GREEN + " (" + promoCode.getName() + ") has been deleted!");

        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "Database error: " + e.getMessage());
            plugin.getLogger().severe("Database error while deleting promo code: " + e.getMessage());
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /promo info <code>");
            return true;
        }

        String code = args[1].toUpperCase();

        try {
            PromoCode promoCode = promoCodeManager.getPromoCode(code);
            if (promoCode == null) {
                sender.sendMessage(ChatColor.RED + "Promo code not found!");
                return true;
            }

            String currencySymbol = plugin.getConfig().getString("currency-symbol", "$");

            sender.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage(ChatColor.GOLD + "Promo Code Information");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.WHITE + promoCode.getName());
            sender.sendMessage(ChatColor.YELLOW + "Code: " + ChatColor.WHITE + promoCode.getCode());
            sender.sendMessage(ChatColor.YELLOW + "Money: " + ChatColor.WHITE + currencySymbol + promoCode.getMoney());
            sender.sendMessage(ChatColor.YELLOW + "Status: " + (promoCode.isValid() ?
                    ChatColor.GREEN + "Active" : ChatColor.RED + "Inactive"));
            sender.sendMessage(ChatColor.YELLOW + "Uses: " + ChatColor.WHITE + promoCode.getCurrentUses() +
                    "/" + (promoCode.getMaxUses() == -1 ? "Unlimited" : promoCode.getMaxUses()));
            sender.sendMessage(ChatColor.YELLOW + "Expires: " + ChatColor.WHITE +
                    TimeParser.formatTime(promoCode.getExpiryTime()));
            sender.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "Database error: " + e.getMessage());
            plugin.getLogger().severe("Database error while fetching promo code info: " + e.getMessage());
        }

        return true;
    }

    private boolean handleList(CommandSender sender) {
        try {
            List<PromoCode> codes = plugin.getDatabaseManager().getAllPromoCodes();

            if (codes.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No promo codes exist.");
                return true;
            }

            String currencySymbol = plugin.getConfig().getString("currency-symbol", "$");

            sender.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage(ChatColor.GOLD + "Active Promo Codes (" + codes.size() + ")");
            sender.sendMessage("");

            for (PromoCode code : codes) {
                String status = code.isValid() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗";
                sender.sendMessage(status + ChatColor.YELLOW + " " + code.getCode() +
                        ChatColor.GRAY + " - " + ChatColor.WHITE + code.getName());
                sender.sendMessage(ChatColor.GRAY + "  " + currencySymbol + code.getMoney() +
                        " • " + code.getCurrentUses() + "/" +
                        (code.getMaxUses() == -1 ? "∞" : code.getMaxUses()) + " uses");
            }

            sender.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "Database error: " + e.getMessage());
            plugin.getLogger().severe("Database error while listing promo codes: " + e.getMessage());
        }

        return true;
    }

    private boolean handleRenew(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /promo renew <code> <time>");
            sender.sendMessage(ChatColor.YELLOW + "Example: /promo renew ABCD-1234-EFGH 7d");
            sender.sendMessage(ChatColor.YELLOW + "Example: /promo renew ABCD-1234-EFGH inf");
            return true;
        }

        String code = args[1].toUpperCase();

        try {
            PromoCode promoCode = promoCodeManager.getPromoCode(code);
            if (promoCode == null) {
                sender.sendMessage(ChatColor.RED + "Promo code not found!");
                return true;
            }

            long newExpiryTime;
            try {
                newExpiryTime = TimeParser.parseTime(args[2]);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Invalid time format! Use formats like: 1d, 2h30m, 1y6d, or 'inf'");
                return true;
            }

            promoCodeManager.renewPromoCode(code, newExpiryTime);

            sender.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage(ChatColor.GOLD + "✓ Promo Code Renewed!");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.WHITE + promoCode.getName());
            sender.sendMessage(ChatColor.YELLOW + "Code: " + ChatColor.WHITE + code);
            sender.sendMessage(ChatColor.YELLOW + "New Expiry: " + ChatColor.WHITE +
                    (newExpiryTime == -1 ? "Never" : TimeParser.formatTime(newExpiryTime)));
            sender.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "Database error: " + e.getMessage());
            plugin.getLogger().severe("Database error while renewing promo code: " + e.getMessage());
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage(ChatColor.GOLD + "Redempt Commands");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "/promo create <name> <money> <time> <users>" +
                ChatColor.GRAY + " - Create a promo code");
        sender.sendMessage(ChatColor.YELLOW + "/promo delete <code>" +
                ChatColor.GRAY + " - Delete a promo code");
        sender.sendMessage(ChatColor.YELLOW + "/promo info <code>" +
                ChatColor.GRAY + " - View promo code info");
        sender.sendMessage(ChatColor.YELLOW + "/promo list" +
                ChatColor.GRAY + " - List all promo codes");
        sender.sendMessage(ChatColor.YELLOW + "/promo renew <code> <time>" +
                ChatColor.GRAY + " - Renew a promo code");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Time format: 1y2d3h4m5s or 'inf'");
        sender.sendMessage(ChatColor.GRAY + "Users: number or 'inf'");
        sender.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("redempt.admin")) {
            return completions;
        }

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "delete", "info", "list", "renew"));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            completions.addAll(Arrays.asList("inf", "1d", "7d", "30d", "1y"));
        } else if (args.length == 5 && args[0].equalsIgnoreCase("create")) {
            completions.addAll(Arrays.asList("inf", "1", "10", "50", "100"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("renew")) {
            completions.addAll(Arrays.asList("inf", "1d", "7d", "30d", "1y"));
        }

        return completions;
    }
}