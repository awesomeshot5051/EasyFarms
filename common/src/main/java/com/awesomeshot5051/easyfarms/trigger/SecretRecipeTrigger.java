package com.awesomeshot5051.easyfarms.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** Fires when a player first fills a farm with the inputs of a secret recipe. Condition: {@code "recipe": "<recipe id>"} (optional). */
public class SecretRecipeTrigger extends SimpleCriterionTrigger<SecretRecipeTrigger.Instance> {
    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, Identifier recipe) {
        trigger(player, instance -> instance.recipe().map(recipe::equals).orElse(true));
    }

    public record Instance(Optional<Holder<LootItemCondition>> player, Optional<Identifier> recipe) implements SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                LootItemCondition.CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Identifier.CODEC.optionalFieldOf("recipe").forGetter(Instance::recipe)
        ).apply(i, Instance::new));
    }
}
