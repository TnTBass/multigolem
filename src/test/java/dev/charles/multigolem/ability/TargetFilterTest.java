package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.ability.TargetFilter.DiamondTargetPredicate;
import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TargetFilterTest {

    @BeforeAll
    static void bootstrap() { MinecraftBootstrap.ensure(); }

    @Test
    void ignored_creepers_excludesCreepers() {
        TargetFilter f = TargetFilter.fromIgnoredList(List.of("CREEPERS"));
        assertTrue(f.isExcludedClass(Creeper.class));
        assertFalse(f.isExcludedClass(Zombie.class));
    }

    @Test
    void ignored_endermen_excludesEndermen() {
        TargetFilter f = TargetFilter.fromIgnoredList(List.of("ENDERMEN"));
        assertTrue(f.isExcludedClass(EnderMan.class));
        assertFalse(f.isExcludedClass(Zombie.class));
    }

    @Test
    void ignored_players_excludesPlayers() {
        TargetFilter f = TargetFilter.fromIgnoredList(List.of("PLAYERS"));
        assertTrue(f.isExcludedClass(Player.class));
    }

    @Test
    void ignored_allBosses_excludesBosses() {
        TargetFilter f = TargetFilter.fromIgnoredList(List.of("ALL_BOSSES"));
        assertTrue(f.isExcludedClass(WitherBoss.class));
        assertTrue(f.isExcludedClass(EnderDragon.class));
        assertTrue(f.isExcludedClass(Warden.class));
        assertFalse(f.isExcludedClass(Skeleton.class));
    }

    @Test
    void ignored_allBosses_excludesEnderDragonPart() {
        TargetFilter f = TargetFilter.fromIgnoredList(List.of("ALL_BOSSES"));
        assertTrue(f.isExcludedClass(EnderDragonPart.class));
    }

    @Test
    void ignored_empty_excludesNothing() {
        TargetFilter f = TargetFilter.fromIgnoredList(List.of());
        assertFalse(f.isExcludedClass(Creeper.class));
        assertFalse(f.isExcludedClass(EnderMan.class));
    }

    @Test
    void diamondMode_allHostileMobs_matchesEnemies() {
        DiamondTargetPredicate p = DiamondTargetPredicate.of("ALL_HOSTILE_MOBS");
        assertTrue(p.matchesClass(Zombie.class));
        assertTrue(p.matchesClass(Creeper.class));
        assertFalse(p.matchesClass(Player.class));
    }

    @Test
    void diamondMode_allHostileMobs_matchesZombieGolemIdentityOnly() {
        DiamondTargetPredicate p = DiamondTargetPredicate.of("ALL_HOSTILE_MOBS");
        assertTrue(p.matchesGolemVariant(GolemVariant.ZOMBIE));
        assertFalse(p.matchesGolemVariant(GolemVariant.DIAMOND));
    }

    @Test
    void diamondMode_allHostileMobsAndPlayers_includesPlayers() {
        DiamondTargetPredicate p = DiamondTargetPredicate.of("ALL_HOSTILE_MOBS_AND_PLAYERS");
        assertTrue(p.matchesClass(Zombie.class));
        assertTrue(p.matchesClass(Player.class));
    }

    @Test
    void diamondMode_allHostileMobsAndPlayers_matchesZombieGolemIdentityOnly() {
        DiamondTargetPredicate p = DiamondTargetPredicate.of("ALL_HOSTILE_MOBS_AND_PLAYERS");
        assertTrue(p.matchesGolemVariant(GolemVariant.ZOMBIE));
        assertFalse(p.matchesGolemVariant(GolemVariant.GOLD));
    }

    @Test
    void diamondMode_bossesOnly_matchesBossesOnly() {
        DiamondTargetPredicate p = DiamondTargetPredicate.of("BOSSES_ONLY");
        assertTrue(p.matchesClass(WitherBoss.class));
        assertTrue(p.matchesClass(EnderDragon.class));
        assertTrue(p.matchesClass(EnderDragonPart.class));
        assertTrue(p.matchesClass(Warden.class));
        assertFalse(p.matchesClass(Zombie.class));
    }

    @Test
    void diamondMode_none_matchesNothing() {
        DiamondTargetPredicate p = DiamondTargetPredicate.of("NONE");
        assertFalse(p.matchesClass(Zombie.class));
        assertFalse(p.matchesClass(WitherBoss.class));
        assertFalse(p.matchesGolemVariant(GolemVariant.ZOMBIE));
    }
}
