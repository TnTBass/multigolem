package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Set;

public final class TargetFilter {

    private final boolean excludeCreepers;
    private final boolean excludeEndermen;
    private final boolean excludePlayers;
    private final boolean excludeAllBosses;

    private TargetFilter(Set<String> names) {
        this.excludeCreepers  = names.contains("CREEPERS");
        this.excludeEndermen  = names.contains("ENDERMEN");
        this.excludePlayers   = names.contains("PLAYERS");
        this.excludeAllBosses = names.contains("ALL_BOSSES");
    }

    public static TargetFilter fromIgnoredList(List<String> ignoredTypes) {
        return new TargetFilter(Set.copyOf(ignoredTypes));
    }

    /** Production use: check a live entity. */
    public boolean isExcluded(Entity entity) {
        return isExcludedClass(entity.getClass());
    }

    /** Test-friendly: check by class token without needing an entity instance. */
    public boolean isExcludedClass(Class<?> type) {
        if (excludeCreepers  && Creeper.class.isAssignableFrom(type))    return true;
        if (excludeEndermen  && EnderMan.class.isAssignableFrom(type))   return true;
        if (excludePlayers   && Player.class.isAssignableFrom(type))     return true;
        if (excludeAllBosses && isBossClass(type))                       return true;
        return false;
    }

    private static boolean isBossClass(Class<?> type) {
        return WitherBoss.class.isAssignableFrom(type)
            || EnderDragon.class.isAssignableFrom(type)
            || EnderDragonPart.class.isAssignableFrom(type)
            || Warden.class.isAssignableFrom(type);
    }

    public interface DiamondTargetPredicate {
        boolean matches(Entity entity);

        /** Test-friendly: check by class token without needing an entity instance. */
        boolean matchesClass(Class<?> type);

        /** Test-friendly: check a MultiGolem identity carried by a vanilla IronGolem entity. */
        boolean matchesGolemVariant(GolemVariant variant);

        static DiamondTargetPredicate of(String mode) {
            return switch (mode) {
                case "ALL_HOSTILE_MOBS"             -> new ClassCheckingPredicate(
                    e -> e instanceof Enemy,
                    t -> Enemy.class.isAssignableFrom(t),
                    TargetFilter::isZombieGolemVariant);
                case "ALL_HOSTILE_MOBS_AND_PLAYERS" -> new ClassCheckingPredicate(
                    e -> e instanceof Enemy || e instanceof Player,
                    t -> Enemy.class.isAssignableFrom(t) || Player.class.isAssignableFrom(t),
                    TargetFilter::isZombieGolemVariant);
                case "BOSSES_ONLY"                  -> new ClassCheckingPredicate(
                    e -> isBossClass(e.getClass()),
                    t -> isBossClass(t),
                    v -> false);
                case "NONE"                         -> new ClassCheckingPredicate(
                    e -> false,
                    t -> false,
                    v -> false);
                default                             -> new ClassCheckingPredicate(
                    e -> e instanceof Enemy,
                    t -> Enemy.class.isAssignableFrom(t),
                    TargetFilter::isZombieGolemVariant);
            };
        }
    }

    private static boolean isZombieGolemVariant(GolemVariant variant) {
        return variant == GolemVariant.ZOMBIE;
    }

    private record ClassCheckingPredicate(
        java.util.function.Predicate<Entity> entityTest,
        java.util.function.Predicate<Class<?>> classTest,
        java.util.function.Predicate<GolemVariant> golemVariantTest
    ) implements DiamondTargetPredicate {
        public boolean matches(Entity entity) { return entityTest.test(entity); }
        public boolean matchesClass(Class<?> type) { return classTest.test(type); }
        public boolean matchesGolemVariant(GolemVariant variant) { return golemVariantTest.test(variant); }
    }
}
