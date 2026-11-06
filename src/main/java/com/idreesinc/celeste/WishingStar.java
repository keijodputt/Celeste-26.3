package com.idreesinc.celeste;

import com.idreesinc.celeste.config.CelesteConfig;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Grants a short Luck reward when a shooting star crosses a player's view while they
 * are looking at the night sky. Ported from the Celestia mod's wish mechanic, using
 * the same geometry and limits: the star's path must pass within about 10 degrees of
 * the player's view center, between 8 and 128 blocks away, and the star must cross
 * the view rather than travel straight toward or away from the player.
 */
public class WishingStar {

    private static final double MIN_LOOK_Y = 0.28;
    private static final double MIN_DISTANCE = 8.0;
    private static final double MAX_DISTANCE = 128.0;
    private static final double LOOK_ALIGNMENT_THRESHOLD = 0.985;
    private static final double PATH_ALIGNMENT_THRESHOLD = 0.86;
    private static final int PATH_SAMPLE_TICKS = 42;
    private static final int PATH_SAMPLE_STEP = 3;
    private static final int WISH_COOLDOWN_TICKS = 180;
    private static final int LUCK_AMPLIFIER = 0;
    private static final int WISH_PARTICLE_COUNT = 14;

    private static final Map<UUID, Long> LAST_WISH_DAY_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, Long> NEXT_WISH_GAME_TIME_BY_PLAYER = new HashMap<>();

    /**
     * Check every player in the star's world and grant the Luck reward to those who
     * are wishing on this star. Called each time a shooting star is created.
     */
    public static void tryGrantWishes(Celeste celeste, Location starLocation, Vector direction, double speed) {
        CelesteConfig config = celeste.configManager.getConfigForWorld(starLocation.getWorld().getName());
        if (!config.wishLuckEnabled || config.wishLuckDurationTicks <= 0) {
            return;
        }
        Vector velocity = direction.clone().multiply(speed);
        if (velocity.lengthSquared() < 1.0E-4) {
            return;
        }
        World world = starLocation.getWorld();
        for (Player player : world.getPlayers()) {
            Vector eye = player.getEyeLocation().toVector();
            Vector look = player.getEyeLocation().getDirection();
            Vector wishPoint = findWishPoint(eye, look, starLocation.toVector(), velocity);
            if (wishPoint == null) {
                continue;
            }
            if (!canWishNow(player, world)) {
                continue;
            }
            markWished(player, world);
            grantLuck(player, config);
            playWishEffects(player, wishPoint);
            if (celeste.getConfig().getBoolean("debug")) {
                celeste.getLogger().info(player.getName() + " made a wish on a shooting star and was granted Luck");
            }
        }
    }

    /**
     * Find the point where the star's path best aligns with the player's view, if the
     * star qualifies as wishable. Returns the best point on the path, or null when no
     * wish should be granted.
     */
    public static Vector findWishPoint(Vector eye, Vector look, Vector origin, Vector velocity) {
        if (look.getY() < MIN_LOOK_Y) {
            // The player must be looking up at the sky
            return null;
        }
        Vector travel = velocity.clone().normalize();
        Vector bestPoint = null;
        double bestAlignment = -1.0;
        for (int tick = 0; tick <= PATH_SAMPLE_TICKS; tick += PATH_SAMPLE_STEP) {
            Vector point = origin.clone().add(velocity.clone().multiply(tick));
            Vector toPoint = point.clone().subtract(eye);
            double distance = toPoint.length();
            if (distance > MAX_DISTANCE || distance < MIN_DISTANCE) {
                continue;
            }
            double alignment = look.dot(toPoint.normalize());
            if (alignment > bestAlignment) {
                bestAlignment = alignment;
                bestPoint = point;
            }
        }
        if (bestAlignment < LOOK_ALIGNMENT_THRESHOLD) {
            // The star never passes close enough to the center of the view
            return null;
        }
        if (Math.abs(look.dot(travel)) > PATH_ALIGNMENT_THRESHOLD) {
            // The star is traveling toward or away from the player instead of crossing the view
            return null;
        }
        return bestPoint;
    }

    private static boolean canWishNow(Player player, World world) {
        long currentDay = world.getFullTime() / 24000;
        Long lastWishDay = LAST_WISH_DAY_BY_PLAYER.get(player.getUniqueId());
        if (lastWishDay != null && lastWishDay == currentDay) {
            // One wish per player per Minecraft day
            return false;
        }
        Long nextWishTime = NEXT_WISH_GAME_TIME_BY_PLAYER.get(player.getUniqueId());
        return nextWishTime == null || world.getGameTime() >= nextWishTime;
    }

    private static void markWished(Player player, World world) {
        LAST_WISH_DAY_BY_PLAYER.put(player.getUniqueId(), world.getFullTime() / 24000);
        NEXT_WISH_GAME_TIME_BY_PLAYER.put(player.getUniqueId(), world.getGameTime() + WISH_COOLDOWN_TICKS);
    }

    private static void grantLuck(Player player, CelesteConfig config) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK,
                config.wishLuckDurationTicks, LUCK_AMPLIFIER, false, true, true));
    }

    private static void playWishEffects(Player player, Vector wishPoint) {
        Location point = new Location(player.getWorld(), wishPoint.getX(), wishPoint.getY(), wishPoint.getZ());
        player.spawnParticle(Particle.END_ROD, point, WISH_PARTICLE_COUNT, 0.28, 0.28, 0.28, 0);
        Random random = new Random();
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                0.16f, 1.55f + random.nextFloat() * 0.25f);
    }
}
