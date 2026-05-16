package dev.charles.multigolem.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record HasGolemVariantLootCondition(GolemVariant variant) implements LootItemCondition {

    public static final MapCodec<HasGolemVariantLootCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            GolemVariant.CODEC.fieldOf("variant").forGetter(HasGolemVariantLootCondition::variant)
        ).apply(instance, HasGolemVariantLootCondition::new)
    );

    @Override
    public MapCodec<HasGolemVariantLootCondition> codec() { return MAP_CODEC; }

    @Override
    public boolean test(LootContext ctx) {
        Entity e = ctx.getOptionalParameter(LootContextParams.THIS_ENTITY);
        return e != null && GolemVariantAttachment.get(e) == variant;
    }
}
