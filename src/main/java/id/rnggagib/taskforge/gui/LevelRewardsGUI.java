package id.rnggagib.taskforge.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import id.rnggagib.taskforge.TaskForgePlugin;
import id.rnggagib.taskforge.jobs.Job;
import id.rnggagib.taskforge.jobs.JobReward;

/**
 * GUI dedicated to showing level rewards with dynamic progression
 */
public class LevelRewardsGUI implements Listener {
    
    private final TaskForgePlugin plugin;
    private final Player player;
    private final Job job;
    private final JobDetailGUI parentGUI;
    private final Inventory inventory;
    
    public LevelRewardsGUI(TaskForgePlugin plugin, Player player, Job job, JobDetailGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.job = job;
        this.parentGUI = parentGUI;
        
        String title = plugin.getConfigManager().translateColorCodes(
            "&6" + job.getDisplayName() + " &8- &dLevel Rewards");
        
        this.inventory = Bukkit.createInventory(null, 54, title);
        
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        setupGUI();
    }
    
    /**
     * Setup the level rewards GUI
     */
    private void setupGUI() {
        // Clear inventory
        inventory.clear();
        
        // Fill background
        fillBackground();
        
        // Title item
        setupTitle();
        
        // Display rewards
        setupRewards();
        
        // Navigation
        setupNavigation();
    }
    
    /**
     * Fill background with glass panes
     */
    private void fillBackground() {
        ItemStack background = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = background.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            background.setItemMeta(meta);
        }
        
        // Fill border slots
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            inventory.setItem(slot, background);
        }
    }
    
    /**
     * Setup title section
     */
    private void setupTitle() {
        ItemStack title = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = title.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&d&l✨ " + job.getDisplayName() + " LEVEL REWARDS"));
            
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().translateColorCodes("&7Rewards you get for reaching new levels"));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e&lDynamic Rewards System:"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7• Every 10 levels: &6🗝 Crate Key"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7• Special milestones: &dExclusive rewards"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7• Level 100: &c&lULTIMATE REWARD"));
            lore.add("");
            
            int playerLevel = plugin.getPlayerDataManager().getJobLevel(player.getUniqueId(), job.getName());
            lore.add(plugin.getConfigManager().translateColorCodes("&7Your current level: &e" + playerLevel));
            int nextReward = ((playerLevel / 10) + 1) * 10;
            if (nextReward <= 100) {
                lore.add(plugin.getConfigManager().translateColorCodes("&7Next reward at: &a" + nextReward));
            }
            
            meta.setLore(lore);
            title.setItemMeta(meta);
        }
        inventory.setItem(4, title);
    }
    
    /**
     * Setup rewards display
     */
    private void setupRewards() {
        int playerLevel = plugin.getPlayerDataManager().getJobLevel(player.getUniqueId(), job.getName());
        
        // Static rewards from config
        Map<Integer, JobReward> staticRewards = job.getLevelRewards();
        
        // Display rewards in a nice layout
        int[] rewardSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int slotIndex = 0;
        
        // Show dynamic rewards (every 10 levels)
        for (int level = 10; level <= 100; level += 10) {
            if (slotIndex >= rewardSlots.length) break;
            
            ItemStack rewardItem = createDynamicRewardItem(level, playerLevel);
            inventory.setItem(rewardSlots[slotIndex], rewardItem);
            slotIndex++;
        }
        
        // Show static rewards from config (if any don't conflict with dynamic ones)
        for (Map.Entry<Integer, JobReward> entry : staticRewards.entrySet()) {
            int level = entry.getKey();
            JobReward reward = entry.getValue();
            
            // Skip if it's a multiple of 10 (already covered by dynamic rewards)
            if (level % 10 == 0) continue;
            if (slotIndex >= rewardSlots.length) break;
            
            ItemStack rewardItem = createStaticRewardItem(level, reward, playerLevel);
            inventory.setItem(rewardSlots[slotIndex], rewardItem);
            slotIndex++;
        }
    }
    
    /**
     * Create dynamic reward item (every 10 levels)
     */
    private ItemStack createDynamicRewardItem(int level, int playerLevel) {
        Material material;
        String rewardName;
        String rewardDescription;
        double moneyAmount = 0;
        
        if (level == 100) {
            material = Material.NETHER_STAR;
            rewardName = "&c&l🌟 ULTIMATE MASTER REWARD";
            rewardDescription = "Legendary achievement for reaching max level!";
            moneyAmount = 10000;
        } else if (level == 90) {
            material = Material.BEACON;
            rewardName = "&d&l🔮 LEGENDARY CRATE KEY";
            rewardDescription = "Exclusive legendary crate with rare items";
            moneyAmount = 2500;
        } else if (level == 80) {
            material = Material.TOTEM_OF_UNDYING;
            rewardName = "&6&l⚡ EPIC CRATE KEY";
            rewardDescription = "Epic crate with powerful rewards";
            moneyAmount = 2000;
        } else if (level == 70) {
            material = Material.DRAGON_HEAD;
            rewardName = "&5&l🐉 RARE CRATE KEY";
            rewardDescription = "Rare crate with special items";
            moneyAmount = 1500;
        } else if (level >= 50) {
            material = Material.CHEST;
            rewardName = "&e&l🗝 PREMIUM CRATE KEY";
            rewardDescription = "Premium crate with valuable items";
            moneyAmount = 1000 + (level - 50) * 50;
        } else {
            material = Material.TRIPWIRE_HOOK;
            rewardName = "&a&l🗝 BASIC CRATE KEY";
            rewardDescription = "Basic crate with useful items";
            moneyAmount = 100 + (level * 10);
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&6Level " + level + " - " + rewardName));
            
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().translateColorCodes("&7" + rewardDescription));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e&lRewards Include:"));
            lore.add(plugin.getConfigManager().translateColorCodes("&a💰 Money: &f$" + String.format("%.0f", moneyAmount)));
            lore.add(plugin.getConfigManager().translateColorCodes("&b🗝 Crate Key: &f1x " + getCrateTypeName(level)));
            lore.add(plugin.getConfigManager().translateColorCodes("&d✨ Special Items: &fRandom rewards"));
            lore.add("");
            
            // Status
            if (playerLevel >= level) {
                lore.add(plugin.getConfigManager().translateColorCodes("&a&l✓ UNLOCKED"));
                lore.add(plugin.getConfigManager().translateColorCodes("&7You have reached this level!"));
            } else {
                lore.add(plugin.getConfigManager().translateColorCodes("&c&l🔒 LOCKED"));
                lore.add(plugin.getConfigManager().translateColorCodes("&7Reach level " + level + " to unlock"));
                int levelsNeeded = level - playerLevel;
                lore.add(plugin.getConfigManager().translateColorCodes("&7" + levelsNeeded + " levels to go"));
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Create static reward item from config
     */
    private ItemStack createStaticRewardItem(int level, JobReward reward, int playerLevel) {
        Material material;
        switch (reward.getType()) {
            case MONEY: material = Material.GOLD_INGOT; break;
            case COMMAND: material = Material.COMMAND_BLOCK; break;
            case EFFECT: material = Material.FIREWORK_ROCKET; break;
            case TITLE: material = Material.NAME_TAG; break;
            case ITEM: material = Material.SHULKER_BOX; break;
            default: material = Material.EMERALD;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&6Level " + level + " - &e" + reward.getType().name() + " Reward"));
            
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().translateColorCodes("&7Special milestone reward"));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&e&lReward Details:"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7Type: &f" + reward.getType().name()));
            lore.add(plugin.getConfigManager().translateColorCodes("&7Value: &f" + reward.getValue()));
            lore.add("");
            lore.add(plugin.getConfigManager().translateColorCodes("&7Message:"));
            lore.add(plugin.getConfigManager().translateColorCodes("&f" + reward.getMessage()));
            lore.add("");
            
            // Status
            if (playerLevel >= level) {
                lore.add(plugin.getConfigManager().translateColorCodes("&a&l✓ UNLOCKED"));
            } else {
                lore.add(plugin.getConfigManager().translateColorCodes("&c&l🔒 LOCKED"));
                lore.add(plugin.getConfigManager().translateColorCodes("&7Reach level " + level + " to unlock"));
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Get crate type name based on level
     */
    private String getCrateTypeName(int level) {
        if (level == 100) return "Ultimate Crate";
        if (level == 90) return "Legendary Crate";
        if (level == 80) return "Epic Crate";
        if (level == 70) return "Rare Crate";
        if (level >= 50) return "Premium Crate";
        return "Basic Crate";
    }
    
    /**
     * Setup navigation
     */
    private void setupNavigation() {
        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta meta = backItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().translateColorCodes("&7&l← Back to Job Details"));
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().translateColorCodes("&7Return to " + job.getDisplayName() + " details"));
            meta.setLore(lore);
            backItem.setItemMeta(meta);
        }
        inventory.setItem(45, backItem);
        
        // Objectives button
        ItemStack objectivesItem = new ItemStack(Material.TARGET);
        ItemMeta objMeta = objectivesItem.getItemMeta();
        if (objMeta != null) {
            objMeta.setDisplayName(plugin.getConfigManager().translateColorCodes("&e&l🎯 Job Objectives"));
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().translateColorCodes("&7View all objectives for this job"));
            lore.add(plugin.getConfigManager().translateColorCodes("&7See how to earn experience and money"));
            objMeta.setLore(lore);
            objectivesItem.setItemMeta(objMeta);
        }
        inventory.setItem(47, objectivesItem);
        
        // Progress info
        ItemStack progressItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta progMeta = progressItem.getItemMeta();
        if (progMeta != null) {
            progMeta.setDisplayName(plugin.getConfigManager().translateColorCodes("&b&l📈 Your Progress"));
            
            List<String> lore = new ArrayList<>();
            int playerLevel = plugin.getPlayerDataManager().getJobLevel(player.getUniqueId(), job.getName());
            double progress = plugin.getPlayerDataManager().getLevelProgress(player.getUniqueId(), job.getName());
            
            lore.add(plugin.getConfigManager().translateColorCodes("&7Current Level: &e" + playerLevel));
            lore.add(plugin.getConfigManager().translateColorCodes("&7Progress: &a" + String.format("%.1f%%", progress * 100)));
            lore.add("");
            
            // Next dynamic reward
            int nextDynamicReward = ((playerLevel / 10) + 1) * 10;
            if (nextDynamicReward <= 100) {
                lore.add(plugin.getConfigManager().translateColorCodes("&7Next crate key at: &6Level " + nextDynamicReward));
                int levelsNeeded = nextDynamicReward - playerLevel;
                lore.add(plugin.getConfigManager().translateColorCodes("&7" + levelsNeeded + " levels to go"));
            } else {
                lore.add(plugin.getConfigManager().translateColorCodes("&a&l🎉 All rewards unlocked!"));
            }
            
            progMeta.setLore(lore);
            progressItem.setItemMeta(progMeta);
        }
        inventory.setItem(49, progressItem);
    }
    
    /**
     * Open the GUI for the player
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * Handle inventory clicks
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        event.setCancelled(true); // Prevent item movement
        
        Player clicker = (Player) event.getWhoClicked();
        if (!clicker.equals(player)) return;
        
        int slot = event.getSlot();
        
        switch (slot) {
            case 45: // Back button
                player.closeInventory();
                if (parentGUI != null) {
                    parentGUI.open();
                }
                break;
            case 47: // Objectives button
                player.closeInventory();
                new ObjectivesGUI(plugin, player, job, parentGUI).open();
                break;
        }
    }
}
