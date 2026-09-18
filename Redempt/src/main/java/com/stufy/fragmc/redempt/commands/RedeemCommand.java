package com.stufy.fragmc.redempt.commands;

import com.stufy.fragmc.redempt.Redempt;
import com.stufy.fragmc.redempt.managers.PromoCodeManager;
import com.stufy.fragmc.redempt.models.PromoCode;
import net.ess3.api.IEssentials;
import net.ess3.api.MaxMoneyException;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RedeemCommand implements CommandExecutor, TabCompleter {

    private final Redempt plugin;
    private final PromoCodeManager promoCodeManager;

    public RedeemCommand(Redempt plugin, PromoCodeManager promoCodeManager) {
        this.plugin = plugin;
        this.promoCodeManager = promoCodeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can redeem promo codes!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /redeem <code>");
            player.sendMessage(ChatColor.YELLOW + "Example: /redeem ABCD-1234-EFGH");
            return true;
        }

        String rawCode = args[0].toUpperCase();
        // Normalize: remove all non-alphanumeric (dashes, spaces) to handle both XXXX-XXXX-XXXX and XXXX-XXXX-XXXX-XXXX
        String normalized = rawCode.replaceAll("[^A-Z0-9]", "");
        // Support both 12-char (3x4) legacy and 16-char (4x4) new codes from shop (shop sends 16-char)
        if (normalized.length() != 12 && normalized.length() != 16) {
            player.sendMessage(ChatColor.RED + "Invalid promo code format! Use XXXX-XXXX-XXXX or XXXX-XXXX-XXXX-XXXX");
            return true;
        }
        // Reconstruct with dashes for DB lookup (DB stores with dashes)
        String code;
        if (normalized.length() == 16) {
            code = normalized.substring(0, 4) + "-" + normalized.substring(4, 8) + "-" + normalized.substring(8, 12) + "-" + normalized.substring(12, 16);
        } else {
            code = normalized.substring(0, 4) + "-" + normalized.substring(4, 8) + "-" + normalized.substring(8, 12);
        }

        try {
            PromoCode promoCode = promoCodeManager.getPromoCode(code);

            if (promoCode == null) {
                player.sendMessage(ChatColor.RED + "Invalid promo code!");
                return true;
            }

            if (promoCodeManager.hasPlayerRedeemed(code, player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You have already redeemed this promo code!");
                return true;
            }

            if (promoCode.isExpired()) {
                player.sendMessage(ChatColor.RED + "This promo code has expired!");
                return true;
            }

            if (promoCode.hasReachedMaxUses()) {
                player.sendMessage(ChatColor.RED + "This promo code has reached its maximum number of uses!");
                return true;
            }

            // Give money using EssentialsX BEFORE recording redemption
            String currencySymbol = plugin.getConfig().getString("currency-symbol", "$");
            try {
                IEssentials essentials = plugin.getEssentials();
                essentials.getUser(player.getUniqueId()).giveMoney(BigDecimal.valueOf(promoCode.getMoney()));

            } catch (MaxMoneyException e) {
                player.sendMessage(ChatColor.RED + "You cannot redeem this code because it would exceed the maximum balance!");
                player.sendMessage(ChatColor.YELLOW + "Your balance is at or near the maximum limit.");
                plugin.getLogger().warning(player.getName() + " couldn't redeem code " + code + " due to max balance limit");
                return true;
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "Error giving money: " + e.getMessage());
                plugin.getLogger().severe("Error giving money to " + player.getName() + ": " + e.getMessage());
                return true;
            }

            // Money was given successfully, now record the redemption
            boolean success = promoCodeManager.redeemCode(code, player.getUniqueId());

            if (!success) {
                player.sendMessage(ChatColor.RED + "Failed to record redemption!");
                plugin.getLogger().severe("Failed to record redemption for " + player.getName() + " - code: " + code);
                return true;
            }

            // Success! Show message
            player.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage(ChatColor.GOLD + "✓ Promo Code Redeemed!");
            player.sendMessage("");
            player.sendMessage(ChatColor.GRAY + "Code: " + ChatColor.WHITE + promoCode.getName());
            player.sendMessage(ChatColor.YELLOW + "You received " + ChatColor.GREEN +
                    ChatColor.BOLD + currencySymbol + promoCode.getMoney() + ChatColor.YELLOW + "!");
            player.sendMessage(ChatColor.GREEN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            plugin.getLogger().info(player.getName() + " redeemed promo code " + code +
                    " (" + promoCode.getName() + ") for " + currencySymbol + promoCode.getMoney());

        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "Database error occurred. Please try again later.");
            plugin.getLogger().severe("Database error during redemption: " + e.getMessage());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}