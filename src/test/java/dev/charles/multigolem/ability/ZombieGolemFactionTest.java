package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombieGolemFactionTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void zombieGolemTargetsPlayersCiviliansAndNonZombieGolems() {
        assertTrue(ZombieGolemFaction.zombieGolemCanTargetClass(Player.class, GolemVariant.IRON));
        assertTrue(ZombieGolemFaction.zombieGolemCanTargetClass(Villager.class, GolemVariant.IRON));
        assertTrue(ZombieGolemFaction.zombieGolemCanTargetClass(WanderingTrader.class, GolemVariant.IRON));
        assertTrue(ZombieGolemFaction.zombieGolemCanTargetClass(IronGolem.class, GolemVariant.IRON));
        assertTrue(ZombieGolemFaction.zombieGolemCanTargetClass(IronGolem.class, GolemVariant.GOLD));
    }

    @Test
    void zombieGolemIgnoresZombieFamilyAndZombieGolems() {
        assertFalse(ZombieGolemFaction.zombieGolemCanTargetClass(Zombie.class, GolemVariant.IRON));
        assertFalse(ZombieGolemFaction.zombieGolemCanTargetClass(ZombieVillager.class, GolemVariant.IRON));
        assertFalse(ZombieGolemFaction.zombieGolemCanTargetClass(IronGolem.class, GolemVariant.ZOMBIE));
    }

    @Test
    void zombieFamilyDoesNotTargetZombieGolems() {
        assertFalse(ZombieGolemFaction.zombieFamilyCanTargetGolem(Zombie.class, GolemVariant.ZOMBIE));
        assertFalse(ZombieGolemFaction.zombieFamilyCanTargetGolem(ZombieVillager.class, GolemVariant.ZOMBIE));
        assertTrue(ZombieGolemFaction.zombieFamilyCanTargetGolem(Zombie.class, GolemVariant.IRON));
        assertTrue(ZombieGolemFaction.zombieFamilyCanTargetGolem(ZombieVillager.class, GolemVariant.GOLD));
        assertTrue(ZombieGolemFaction.zombieFamilyCanTargetGolem(Skeleton.class, GolemVariant.ZOMBIE));
    }

    @Test
    void convertedZombieVillagersCannotKeepFightsAliveWithZombieGolems() {
        assertFalse(ZombieGolemFaction.zombieFamilyCanTargetGolem(ZombieVillager.class, GolemVariant.ZOMBIE));
        assertFalse(ZombieGolemFaction.zombieGolemCanTargetClass(ZombieVillager.class, GolemVariant.IRON, true));
    }

    @Test
    void nonZombieGolemsTreatZombieGolemsAsTargets() {
        assertTrue(ZombieGolemFaction.nonZombieGolemCanTarget(GolemVariant.IRON, GolemVariant.ZOMBIE));
        assertTrue(ZombieGolemFaction.nonZombieGolemCanTarget(GolemVariant.DIAMOND, GolemVariant.ZOMBIE));
        assertFalse(ZombieGolemFaction.nonZombieGolemCanTarget(GolemVariant.ZOMBIE, GolemVariant.ZOMBIE));
        assertFalse(ZombieGolemFaction.nonZombieGolemCanTarget(GolemVariant.GOLD, GolemVariant.IRON));
    }

    @Test
    void goalPredicatesCoverZombieAndDefenderTargets() {
        assertTrue(ZombieGolemFaction.zombieGoalCanTargetClass(Player.class, GolemVariant.IRON));
        assertTrue(ZombieGolemFaction.zombieGoalCanTargetClass(Villager.class, GolemVariant.IRON));
        assertTrue(ZombieGolemFaction.zombieGoalCanTargetClass(WanderingTrader.class, GolemVariant.IRON));
        assertTrue(ZombieGolemFaction.zombieGoalCanTargetClass(IronGolem.class, GolemVariant.GOLD));
        assertFalse(ZombieGolemFaction.zombieGoalCanTargetClass(IronGolem.class, GolemVariant.ZOMBIE));
        assertTrue(ZombieGolemFaction.defenderGoalCanTarget(GolemVariant.GOLD, GolemVariant.ZOMBIE));
    }

    @Test
    void zombieGolemSelfDefenseStillIgnoresZombieFamily() {
        // Zombie golems and zombie-family mobs are one faction by design, so retaliation should not
        // restart fights after conversion or incidental zombie damage.
        assertFalse(ZombieGolemFaction.zombieGolemCanTargetClass(Zombie.class, GolemVariant.IRON, true));
        assertFalse(ZombieGolemFaction.zombieGolemCanTargetClass(ZombieVillager.class, GolemVariant.IRON, true));
        assertTrue(ZombieGolemFaction.zombieGolemCanTargetClass(Player.class, GolemVariant.IRON, true));
    }
}
