package com.coldmint.fleeceLauncher;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashSet;
import java.util.UUID;

public class LauncherListener implements Listener {

    private final HashSet<UUID> tntUuid = new HashSet<>();

    @EventHandler
    void onPlayerLaunchProjectileEvent(EntityShootBowEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof Player player)) {
            return;
        }
        PlayerInventory playerInventory = player.getInventory();
        ItemStack itemStack = playerInventory.getItemInOffHand();
        Material material = itemStack.getType();
        Entity projectile = event.getProjectile();
        boolean success = false;
        if (projectile instanceof Arrow || projectile instanceof SpectralArrow) {
            player.sendMessage(material.name());
            if (material.isBlock()) {
                BlockType blockType = material.asBlockType();
                if (blockType == null) {
                    return;
                }
                BlockData blockData = blockType.createBlockData();
                if (material.name().endsWith("WOOL") || material.name().endsWith("CARPET")) {
                    //兼容羊毛和地毯。
                    FallingBlock fallingBlock = player.getWorld().spawn(projectile.getLocation(), FallingBlock.class);
                    fallingBlock.setBlockData(blockData);
                    fallingBlock.setVelocity(projectile.getVelocity());
                    success = true;
                    event.setCancelled(true);
                }
                if (material == Material.TNT) {
                    TNTPrimed tntPrimed = player.getWorld().spawn(projectile.getLocation(), TNTPrimed.class);
                    tntPrimed.setBlockData(blockData);
                    tntPrimed.setVelocity(projectile.getVelocity());
                    tntUuid.add(tntPrimed.getUniqueId());
                    success = true;
                    event.setCancelled(true);
                }
            }
            if (material == Material.ENDER_PEARL) {
                EnderPearl enderPearl = player.getWorld().spawn(projectile.getLocation(), EnderPearl.class);
                enderPearl.setShooter(player);
                enderPearl.setVelocity(projectile.getVelocity());
                success = true;
                event.setCancelled(true);
            }
            if (material == Material.SNOWBALL) {
                Snowball snowball = player.getWorld().spawn(projectile.getLocation(), Snowball.class);
                snowball.setShooter(player);
                snowball.setVelocity(projectile.getVelocity());
                success = true;
                event.setCancelled(true);
            }
            if (material == Material.LINGERING_POTION) {
                LingeringPotion lingeringPotion = player.getWorld().spawn(projectile.getLocation(), LingeringPotion.class);
                lingeringPotion.setShooter(player);
                lingeringPotion.setItem(itemStack);
                lingeringPotion.setVelocity(projectile.getVelocity());
                success = true;
                event.setCancelled(true);
            }
            if (material == Material.EXPERIENCE_BOTTLE) {
                ThrownExpBottle thrownExpBottle = player.getWorld().spawn(projectile.getLocation(), ThrownExpBottle.class);
                thrownExpBottle.setShooter(player);
                thrownExpBottle.setItem(itemStack);
                thrownExpBottle.setVelocity(projectile.getVelocity());
                success = true;
                event.setCancelled(true);
            }
            if (material == Material.SPLASH_POTION) {
                SplashPotion splashPotion = player.getWorld().spawn(projectile.getLocation(), SplashPotion.class);
                splashPotion.setShooter(player);
                splashPotion.setItem(itemStack);
                splashPotion.setVelocity(projectile.getVelocity());
                success = true;
                event.setCancelled(true);
            }
            if (material == Material.SHULKER_SHELL) {
                Entity entity = player.getTargetEntity(120);
                ShulkerBullet shulkerBullet = player.getWorld().spawn(projectile.getLocation(), ShulkerBullet.class);
                shulkerBullet.setShooter(player);
                if (entity != null) {
                    shulkerBullet.setTarget(entity);
                }
                shulkerBullet.setVelocity(projectile.getVelocity());
                success = true;
                event.setCancelled(true);
            }
            if (material == Material.DRAGON_BREATH) {
                DragonFireball dragonFireball = player.getWorld().spawn(projectile.getLocation(), DragonFireball.class);
                dragonFireball.setShooter(player);
                dragonFireball.setVelocity(projectile.getVelocity());
                success = true;
                event.setCancelled(true);
            }
            if (success && player.getGameMode() != GameMode.CREATIVE) {
                itemStack.setAmount(itemStack.getAmount() - 1);
            }
        }

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
