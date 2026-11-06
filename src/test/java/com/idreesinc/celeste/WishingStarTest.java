package com.idreesinc.celeste;

import org.bukkit.util.Vector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for the wish geometry shared with the Celestia mod's wish mechanic: a shooting
 * star grants Luck when its path crosses the center of a skyward-looking player's view.
 */
public class WishingStarTest {

    private static final double EPSILON = 1.0E-6;

    private Vector eye(double x, double y, double z) {
        return new Vector(x, y, z);
    }

    @Test
    public void starCrossingViewCenterGrantsWish() {
        // Player at ground level looking up and north, straight at the star's path
        Vector eye = eye(0, 64, 0);
        Vector look = new Vector(0, 26, -60).normalize();
        // Star high up traveling east across the view, crossing the look axis at tick 21
        Vector origin = new Vector(-21, 90, -60);
        Vector velocity = new Vector(1, 0, 0);

        Vector wishPoint = WishingStar.findWishPoint(eye, look, origin, velocity);

        assertNotNull(wishPoint);
        assertEquals(0, wishPoint.getX(), EPSILON);
        assertEquals(90, wishPoint.getY(), EPSILON);
        assertEquals(-60, wishPoint.getZ(), EPSILON);
    }

    @Test
    public void playerLookingDownDoesNotWish() {
        Vector eye = eye(0, 64, 0);
        Vector look = new Vector(0, -26, -30).normalize();
        Vector origin = new Vector(-20, 90, -60);
        Vector velocity = new Vector(1, 0, 0);

        assertNull(WishingStar.findWishPoint(eye, look, origin, velocity));
    }

    @Test
    public void starHeadingStraightForPlayerDoesNotGrantWish() {
        Vector eye = eye(0, 64, 0);
        // Looking up along the direction the star is traveling
        Vector look = new Vector(0, 20, -63).normalize();
        Vector origin = new Vector(0, 84, -63);
        // Star moving away along the view axis instead of crossing it
        Vector velocity = look.clone().multiply(2);

        assertNull(WishingStar.findWishPoint(eye, look, origin, velocity));
    }

    @Test
    public void starFarFromViewCenterDoesNotGrantWish() {
        Vector eye = eye(0, 64, 0);
        Vector look = new Vector(0, 26, -30).normalize();
        // Path crosses 40 blocks east of where the player is looking
        Vector origin = new Vector(40, 90, -60);
        Vector velocity = new Vector(1, 0, 0);

        assertNull(WishingStar.findWishPoint(eye, look, origin, velocity));
    }

    @Test
    public void starTooFarAwayDoesNotGrantWish() {
        Vector eye = eye(0, 64, 0);
        Vector look = new Vector(0, 26, -30).normalize();
        Vector origin = new Vector(0, 300, -400);
        Vector velocity = new Vector(1, 0, 0);

        assertNull(WishingStar.findWishPoint(eye, look, origin, velocity));
    }
}
