package com.infrastructuresickos.stealth;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Implements Stealth mechanics:
 *
 *  Mob detection of players is gated by two factors:
 *    - Facing direction: mobs detect players within a forward-facing cone.
 *      Directly behind = nearly undetectable. Directly facing = full range.
 *    - Light level: darker areas reduce detection range.
 *      Darkness (light 0) = 20% of vanilla range. Full light (15) = 100%.
 *
 *  Sneaking further reduces detection by 50%.
 *
 *  Only initial detection is affected (originalTarget == null → mob had no
 *  prior target). Once a mob is already aggro'd to the player it tracks normally.
 *
 * Registered manually on the FORGE bus — do NOT add @Mod.EventBusSubscriber.
 */
public class StealthEventHandler {

    /** Vanilla zombie/skeleton detection range (blocks). */
    private static final double VANILLA_DETECTION_RANGE = 16.0;

    /**
     * Minimum detection range fraction when facing directly away in darkness.
     * At 0.02 a mob can still detect a player within ~0.3 blocks (essentially zero).
     */
    private static final double MIN_ANGLE_FACTOR  = 0.1;
    private static final double MIN_LIGHT_FACTOR  = 0.2;
    private static final double SNEAK_FACTOR      = 0.5;

    @SubscribeEvent
    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
        // Only intercept initial detection (mob had no prior target or target was not the player)
        LivingEntity originalTarget = event.getOriginalTarget();
        if (originalTarget instanceof Player) {
            // Already tracking a player — let the mob re-confirm its target freely
            return;
        }

        // Only care when a mob tries to newly target a player
        if (!(event.getNewTarget() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        // --- Angle factor ---
        // Dot product of mob's look direction and (mob → player) direction.
        // Maps: facing player = 1.0, perpendicular = 0.0, behind = -1.0
        // angleFactor: range [MIN_ANGLE_FACTOR, 1.0]
        Vec3 mobLook   = mob.getLookAngle();
        Vec3 toPlayer  = player.position().subtract(mob.position()).normalize();
        double dot         = mobLook.dot(toPlayer);                            // -1..1
        double angleFactor = MIN_ANGLE_FACTOR + (1.0 - MIN_ANGLE_FACTOR) * ((dot + 1.0) / 2.0);

        // --- Light factor ---
        // Light level at the player's feet position.
        // lightFactor: range [MIN_LIGHT_FACTOR, 1.0]
        // getRawBrightness(pos, 0) returns the combined sky+block light level at the position
        int lightLevel = mob.level().getRawBrightness(BlockPos.containing(player.position()), 0);
        double lightFactor = MIN_LIGHT_FACTOR + (1.0 - MIN_LIGHT_FACTOR) * (lightLevel / 15.0);

        // --- Sneak factor ---
        double sneakMultiplier = player.isCrouching() ? SNEAK_FACTOR : 1.0;

        // --- Effective detection range ---
        double effectiveRange = VANILLA_DETECTION_RANGE * angleFactor * lightFactor * sneakMultiplier;

        double distance = mob.distanceTo(player);
        if (distance > effectiveRange) {
            event.setCanceled(true);
        }
    }
}
