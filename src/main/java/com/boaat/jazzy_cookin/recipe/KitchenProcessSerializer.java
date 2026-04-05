package com.boaat.jazzy_cookin.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.ProcessMode;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class KitchenProcessSerializer implements RecipeSerializer<KitchenProcessRecipe> {
    public static final MapCodec<KitchenProcessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StationType.CODEC.fieldOf("station").forGetter(KitchenProcessRecipe::station),
            KitchenInputRequirement.CODEC.listOf().fieldOf("inputs").forGetter(KitchenProcessRecipe::inputs),
            KitchenMethod.CODEC.optionalFieldOf("method", KitchenMethod.NONE).forGetter(KitchenProcessRecipe::method),
            ToolProfile.CODEC.optionalFieldOf("preferred_tool").forGetter(KitchenProcessRecipe::preferredTool),
            ToolProfile.CODEC.listOf().optionalFieldOf("allowed_tools", List.of()).forGetter(KitchenProcessRecipe::allowedTools),
            Codec.BOOL.optionalFieldOf("tool_required", false).forGetter(KitchenProcessRecipe::toolRequired),
            Codec.INT.optionalFieldOf("duration", 60).forGetter(KitchenProcessRecipe::duration),
            Codec.INT.optionalFieldOf("passive_duration_ticks", 0).forGetter(KitchenProcessRecipe::passiveDurationTicks),
            HeatLevel.CODEC.optionalFieldOf("preferred_heat", HeatLevel.OFF).forGetter(KitchenProcessRecipe::preferredHeat),
            HeatLevel.CODEC.optionalFieldOf("minimum_heat", HeatLevel.OFF).forGetter(KitchenProcessRecipe::minimumHeat),
            HeatLevel.CODEC.optionalFieldOf("maximum_heat", HeatLevel.OFF).forGetter(KitchenProcessRecipe::maximumHeat),
            Codec.BOOL.optionalFieldOf("requires_preheat", false).forGetter(KitchenProcessRecipe::requiresPreheat),
            ProcessMode.CODEC.optionalFieldOf("mode", ProcessMode.ACTIVE).forGetter(KitchenProcessRecipe::mode),
            KitchenEnvironmentRequirements.CODEC.optionalFieldOf("environment_requirements", KitchenEnvironmentRequirements.NONE)
                    .forGetter(KitchenProcessRecipe::environmentRequirements),
            KitchenProcessOutcome.CODEC.listOf().optionalFieldOf("outcomes", List.of()).forGetter(KitchenProcessRecipe::outcomes),
            KitchenProcessOutput.CODEC.fieldOf("output").forGetter(KitchenProcessRecipe::output)
    ).apply(instance, (station, inputs, method, preferredTool, allowedTools, toolRequired, duration, passiveDurationTicks, preferredHeat, minimumHeat, maximumHeat,
            requiresPreheat, mode, environmentRequirements, outcomes, output) ->
            new KitchenProcessRecipe(
                    station,
                    inputs,
                    method == KitchenMethod.NONE
                            ? KitchenMethod.infer(station, preferredHeat, preferredTool.orElse(ToolProfile.NONE), output.state(), mode, environmentRequirements.nearbyWater(), requiresPreheat)
                            : method,
                    preferredTool,
                    allowedTools.isEmpty() && preferredTool.isPresent() ? List.of(preferredTool.get()) : allowedTools,
                    toolRequired,
                    duration,
                    passiveDurationTicks,
                    preferredHeat,
                    minimumHeat == HeatLevel.OFF && preferredHeat != HeatLevel.OFF ? preferredHeat : minimumHeat,
                    maximumHeat == HeatLevel.OFF && preferredHeat != HeatLevel.OFF ? preferredHeat : maximumHeat,
                    requiresPreheat,
                    mode,
                    environmentRequirements,
                    outcomes,
                    output
            )));

    public static final StreamCodec<RegistryFriendlyByteBuf, KitchenProcessRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public KitchenProcessRecipe decode(RegistryFriendlyByteBuf buffer) {
            StationType station = StationType.STREAM_CODEC.decode(buffer);
            List<KitchenInputRequirement> inputs = new ArrayList<>();
            int inputCount = buffer.readVarInt();
            for (int i = 0; i < inputCount; i++) {
                inputs.add(KitchenInputRequirement.STREAM_CODEC.decode(buffer));
            }
            KitchenMethod method = KitchenMethod.STREAM_CODEC.decode(buffer);
            Optional<ToolProfile> preferredTool = buffer.readBoolean()
                    ? Optional.of(ToolProfile.STREAM_CODEC.decode(buffer))
                    : Optional.empty();
            List<ToolProfile> allowedTools = new ArrayList<>();
            int allowedToolCount = buffer.readVarInt();
            for (int i = 0; i < allowedToolCount; i++) {
                allowedTools.add(ToolProfile.STREAM_CODEC.decode(buffer));
            }
            boolean toolRequired = buffer.readBoolean();
            int duration = buffer.readVarInt();
            int passiveDurationTicks = buffer.readVarInt();
            HeatLevel preferredHeat = HeatLevel.STREAM_CODEC.decode(buffer);
            HeatLevel minimumHeat = HeatLevel.STREAM_CODEC.decode(buffer);
            HeatLevel maximumHeat = HeatLevel.STREAM_CODEC.decode(buffer);
            boolean requiresPreheat = buffer.readBoolean();
            ProcessMode mode = ProcessMode.STREAM_CODEC.decode(buffer);
            KitchenEnvironmentRequirements environmentRequirements = KitchenEnvironmentRequirements.STREAM_CODEC.decode(buffer);
            List<KitchenProcessOutcome> outcomes = new ArrayList<>();
            int outcomeCount = buffer.readVarInt();
            for (int i = 0; i < outcomeCount; i++) {
                outcomes.add(KitchenProcessOutcome.STREAM_CODEC.decode(buffer));
            }
            KitchenProcessOutput output = KitchenProcessOutput.STREAM_CODEC.decode(buffer);
            return new KitchenProcessRecipe(
                    station,
                    inputs,
                    method,
                    preferredTool,
                    allowedTools,
                    toolRequired,
                    duration,
                    passiveDurationTicks,
                    preferredHeat,
                    minimumHeat,
                    maximumHeat,
                    requiresPreheat,
                    mode,
                    environmentRequirements,
                    outcomes,
                    output
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, KitchenProcessRecipe value) {
            StationType.STREAM_CODEC.encode(buffer, value.station());
            buffer.writeVarInt(value.inputs().size());
            value.inputs().forEach(input -> KitchenInputRequirement.STREAM_CODEC.encode(buffer, input));
            KitchenMethod.STREAM_CODEC.encode(buffer, value.method());
            buffer.writeBoolean(value.preferredTool().isPresent());
            value.preferredTool().ifPresent(tool -> ToolProfile.STREAM_CODEC.encode(buffer, tool));
            buffer.writeVarInt(value.allowedTools().size());
            value.allowedTools().forEach(tool -> ToolProfile.STREAM_CODEC.encode(buffer, tool));
            buffer.writeBoolean(value.toolRequired());
            buffer.writeVarInt(value.duration());
            buffer.writeVarInt(value.passiveDurationTicks());
            HeatLevel.STREAM_CODEC.encode(buffer, value.preferredHeat());
            HeatLevel.STREAM_CODEC.encode(buffer, value.minimumHeat());
            HeatLevel.STREAM_CODEC.encode(buffer, value.maximumHeat());
            buffer.writeBoolean(value.requiresPreheat());
            ProcessMode.STREAM_CODEC.encode(buffer, value.mode());
            KitchenEnvironmentRequirements.STREAM_CODEC.encode(buffer, value.environmentRequirements());
            buffer.writeVarInt(value.outcomes().size());
            value.outcomes().forEach(outcome -> KitchenProcessOutcome.STREAM_CODEC.encode(buffer, outcome));
            KitchenProcessOutput.STREAM_CODEC.encode(buffer, value.output());
        }
    };

    @Override
    public MapCodec<KitchenProcessRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, KitchenProcessRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
