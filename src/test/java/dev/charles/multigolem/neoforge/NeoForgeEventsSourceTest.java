package dev.charles.multigolem.neoforge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeEventsSourceTest {
    @Test
    void neoforgeAbilityAdapterOwnsNeoForgeTickAndDamageRegistration() throws IOException {
        String source = Files.readString(Path.of(
            "src/neoforge/java/dev/charles/multigolem/neoforge/ability/NeoForgeAbilityEvents.java"));

        assertTrue(source.contains("NeoForge.EVENT_BUS.addListener(NeoForgeAbilityEvents::onLevelTick);"));
        assertTrue(source.contains("NeoForge.EVENT_BUS.addListener(NeoForgeAbilityEvents::onLivingIncomingDamage);"));
        assertTrue(source.contains("GoldAbility.onTick(level);"));
        assertTrue(source.contains("EmeraldAbility.onTick(level);"));
        assertTrue(source.contains("DiamondAbility.onTick(level);"));
        assertTrue(source.contains("CopperAbility.allowDamage(entity, source, amount)"));
        assertTrue(source.contains("NetheriteAbility.allowDamage(entity, source, amount)"));
        assertTrue(source.contains("DiamondAbility.allowDamage(entity, source, amount)"));
        assertTrue(source.contains("boolean cancelDamage = !CopperAbility.allowDamage(entity, source, amount)"));
        assertTrue(source.contains("| !NetheriteAbility.allowDamage(entity, source, amount)"));
        assertTrue(source.contains("| !DiamondAbility.allowDamage(entity, source, amount)"));
        assertFalse(source.contains("net.fabricmc"));
    }

    @Test
    void neoforgeEventAdapterOwnsLootCreativeAndEntityLoadRegistration() throws IOException {
        String source = Files.readString(Path.of(
            "src/neoforge/java/dev/charles/multigolem/neoforge/event/NeoForgeMultiGolemEvents.java"));

        assertTrue(source.contains("DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, MultiGolem.MOD_ID)"));
        assertTrue(source.contains("HasGolemVariantLootCondition.MAP_CODEC"));
        assertTrue(source.contains("modBus.addListener(NeoForgeMultiGolemEvents::buildCreativeTabs);"));
        assertTrue(source.contains("NeoForge.EVENT_BUS.addListener(NeoForgeMultiGolemEvents::onEntityJoinLevel);"));
        assertTrue(source.contains("NeoForge.EVENT_BUS.addListener(NeoForgeMultiGolemEvents::onLootTableLoad);"));
        assertTrue(source.contains("SpawnEggStacks.create(variant)"));
        assertTrue(source.contains("VariantAttributes.apply(golem);"));
        assertTrue(source.contains("table.addPool(variantPool(variant, drop.item(), drop.min(), drop.max()));"));
        assertTrue(source.contains("HasGolemVariantLootCondition.builder(variant)"));
        assertFalse(source.contains("net.fabricmc"));
    }

    @Test
    void neoforgeMetadataDeclaresCommonGameplayMixins() throws IOException {
        String metadata = Files.readString(Path.of("src/neoforge/resources/META-INF/neoforge.mods.toml"));
        String mixins = Files.readString(Path.of("src/neoforge/resources/multigolem.neoforge.mixins.json"));

        assertTrue(metadata.contains("\"multigolem.neoforge.mixins.json\""));
        assertTrue(metadata.contains("\"multigolem.neoforge.client.mixins.json\""));
        assertTrue(mixins.contains("\"package\": \"dev.charles.multigolem.mixin\""));
        assertTrue(mixins.contains("\"EntityMixin\""));
        assertTrue(mixins.contains("\"IronGolemMixin\""));
        assertTrue(mixins.contains("\"CarvedPumpkinBlockMixin\""));
        assertTrue(mixins.contains("\"BlockItemMixin\""));
        assertTrue(mixins.contains("\"LivingEntityMixin\""));
        assertTrue(mixins.contains("\"GolemTargetingMixin\""));
        assertTrue(mixins.contains("\"IronGolemRegisterGoalsMixin\""));
        assertTrue(mixins.contains("\"IronGolemAttackMixin\""));
        assertTrue(mixins.contains("\"VillagerMixin\""));
        assertTrue(mixins.contains("\"BaseSpawnerAccessor\""));
        assertTrue(mixins.contains("\"BaseSpawnerMixin\""));
        assertTrue(mixins.contains("\"SpawnEggItemMixin\""));
    }
}
