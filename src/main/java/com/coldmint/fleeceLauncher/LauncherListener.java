package com.coldmint.fleeceLauncher;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Random;
import java.util.UUID;

public class LauncherListener implements Listener {

    private final HashSet<UUID> tntUuid = new HashSet<>();
    //Used to calculate the probability of obtaining durable enchantments
    //用于计算耐久附魔的概率
    private final Random random = new Random();

    @EventHandler
    void onPlayerLaunchProjectileEvent(EntityShootBowEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof Player player)) {
            return;
        }
        PlayerInventory playerInventory = player.getInventory();
        ItemStack mainHandItem = playerInventory.getItemInMainHand();
        boolean hasInfinity = mainHandItem.containsEnchantment(Enchantment.INFINITY);
        boolean hasMultishot = mainHandItem.containsEnchantment(Enchantment.MULTISHOT);
        ItemStack offHandItem = playerInventory.getItemInOffHand();
        Entity originalProjectile = event.getProjectile();
        if (originalProjectile instanceof Arrow || originalProjectile instanceof SpectralArrow) {
            boolean canLaunch;
            if (hasMultishot) {
                Vector baseVelocity = originalProjectile.getVelocity();
                int successCount = 0;
                if (launchCustomProjectile(player, originalProjectile.getLocation(), baseVelocity.clone().rotateAroundY(0.1), offHandItem)) {
                    successCount++;
                }
                if (launchCustomProjectile(player, originalProjectile.getLocation(), baseVelocity.clone(), offHandItem)) {
                    successCount++;
                }
                if (launchCustomProjectile(player, originalProjectile.getLocation(), baseVelocity.clone().rotateAroundY(-0.1), offHandItem)) {
                    successCount++;
                }
                canLaunch = successCount > 0;
            } else {
                canLaunch = launchCustomProjectile(player, originalProjectile.getLocation(), originalProjectile.getVelocity(), offHandItem);
            }

            if (canLaunch) {
                event.setCancelled(true);
                if (player.getGameMode() != GameMode.CREATIVE) {
                    consumeMainHandDurability(mainHandItem);
                }
                if (!hasInfinity && player.getGameMode() != GameMode.CREATIVE) {
                    offHandItem.setAmount(offHandItem.getAmount() - 1);
                }
            }
        }
    }

    /**
     * 手动消耗主手武器（弓/弩）的耐久度，兼容UNBREAKING附魔
     *
     * @param mainHandItem 主手的弓/弩物品
     */
    private void consumeMainHandDurability(ItemStack mainHandItem) {
        if (!isBowOrCrossbow(mainHandItem.getType())) {
            return;
        }
        int unbreakingLevel = mainHandItem.getEnchantmentLevel(Enchantment.UNBREAKING);
        double breakChance = 1.0 / (unbreakingLevel + 1);
        if (random.nextDouble() <= breakChance) {
            ItemMeta meta = mainHandItem.getItemMeta();
            if (meta instanceof Damageable damageableMeta) {
                int currentDamage = damageableMeta.getDamage();
                damageableMeta.setDamage(currentDamage + 1);
                mainHandItem.setItemMeta(damageableMeta);
                if (currentDamage + 1 >= mainHandItem.getType().getMaxDurability()) {
                    mainHandItem.setAmount(0);
                }
            }
        }
    }

    /**
     * 判断物品类型是否为弓或弩
     *
     * @param material 物品类型
     * @return 是否为弓/弩
     */
    private boolean isBowOrCrossbow(Material material) {
        return material == Material.BOW || material == Material.CROSSBOW;
    }

    /**
     * 封装的自定义投射物生成方法（复用核心逻辑）
     *
     * @param player      发射玩家
     * @param location    生成位置
     * @param velocity    飞行速度（含方向）
     * @param offHandItem 副手物品
     * @return 是否成功生成投射物
     */
    private boolean launchCustomProjectile(Player player, org.bukkit.Location location, Vector velocity, ItemStack offHandItem) {
        Material offHandMaterial = offHandItem.getType();
        boolean success = false;
        if (offHandMaterial.isBlock()) {
            BlockType blockType = offHandMaterial.asBlockType();
            if (blockType == null) {
                return false;
            }
            BlockData blockData = blockType.createBlockData();
            if (offHandMaterial.name().endsWith("WOOL") || offHandMaterial.name().endsWith("CARPET")) {
                FallingBlock fallingBlock = player.getWorld().spawn(location, FallingBlock.class);
                fallingBlock.setBlockData(blockData);
                fallingBlock.setVelocity(velocity);
                success = true;
            } else if (offHandMaterial == Material.TNT) {
                TNTPrimed tntPrimed = player.getWorld().spawn(location, TNTPrimed.class);
                tntPrimed.setBlockData(blockData);
                tntPrimed.setVelocity(velocity);
                tntUuid.add(tntPrimed.getUniqueId());
                success = true;
            }
        }

        // 末影珍珠
        if (offHandMaterial == Material.ENDER_PEARL) {
            EnderPearl enderPearl = player.getWorld().spawn(location, EnderPearl.class);
            enderPearl.setShooter(player);
            enderPearl.setVelocity(velocity);
            success = true;
        }
        // 雪球
        else if (offHandMaterial == Material.SNOWBALL) {
            Snowball snowball = player.getWorld().spawn(location, Snowball.class);
            snowball.setShooter(player);
            snowball.setVelocity(velocity);
            success = true;
        }
        // 滞留药水
        else if (offHandMaterial == Material.LINGERING_POTION) {
            LingeringPotion lingeringPotion = player.getWorld().spawn(location, LingeringPotion.class);
            lingeringPotion.setShooter(player);
            lingeringPotion.setItem(offHandItem);
            lingeringPotion.setVelocity(velocity);
            success = true;
        }
        // 经验瓶
        else if (offHandMaterial == Material.EXPERIENCE_BOTTLE) {
            ThrownExpBottle thrownExpBottle = player.getWorld().spawn(location, ThrownExpBottle.class);
            thrownExpBottle.setShooter(player);
            thrownExpBottle.setItem(offHandItem);
            thrownExpBottle.setVelocity(velocity);
            success = true;
        }
        // 喷溅药水
        else if (offHandMaterial == Material.SPLASH_POTION) {
            SplashPotion splashPotion = player.getWorld().spawn(location, SplashPotion.class);
            splashPotion.setShooter(player);
            splashPotion.setItem(offHandItem);
            splashPotion.setVelocity(velocity);
            success = true;
        }
        // 潜影壳 → 潜影贝子弹
        else if (offHandMaterial == Material.SHULKER_SHELL) {
            Entity target = player.getTargetEntity(120);
            ShulkerBullet shulkerBullet = player.getWorld().spawn(location, ShulkerBullet.class);
            shulkerBullet.setShooter(player);
            if (target != null) {
                shulkerBullet.setTarget(target);
            }
            shulkerBullet.setVelocity(velocity);
            success = true;
        }
        // 龙息 → 龙火球
        else if (offHandMaterial == Material.DRAGON_BREATH) {
            DragonFireball dragonFireball = player.getWorld().spawn(location, DragonFireball.class);
            dragonFireball.setShooter(player);
            dragonFireball.setVelocity(velocity);
            success = true;
        }

        return success;
    }

    @EventHandler
    public void onEntityExplodeEvent(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof TNTPrimed tntPrimed) {
            UUID uuid = tntPrimed.getUniqueId();
            if (tntUuid.contains(uuid)) {
                event.blockList().clear();
                tntUuid.remove(uuid);
            }
        }
    }

}