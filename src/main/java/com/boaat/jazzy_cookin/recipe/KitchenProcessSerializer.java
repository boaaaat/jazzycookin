package com.boaat.jazzy_cookin.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.boaat.jazzy_cookin.kitchen.HeatLevel;
import com.boaat.jazzy_cookin.kitchen.KitchenMethod;
import com.boaat.jazzy_cookin.kitchen.ProcessMode;
import com.boaat.jazzy_cookin.kitchen.StationType;
import com.boaat.jazzy_cookin.kitchen.ToolProfile;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class KitchenProcessSerializer implements RecipeSerializer<KitchenProcessRecipe> {
    private record BasicRecipeData(
            StationType station,
            List<KitchenInputRequirement> inputs,
            KitchenMethod method,
            List<KitchenProcessOutcome> outcomes,
            KitchenProcessOutput output
    ) {
        private static final MapCodec<BasicRecipeData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                StationType.CODEC.fieldOf("station").forGetter(BasicRecipeData::station),
                KitchenInputRequirement.CODEC.listOf().fieldOf("inputs").forGetter(BasicRecipeData::inputs),
                KitchenMethod.CODEC.optionalFieldOf("method", KitchenMethod.NONE).forGetter(BasicRecipeData::method),
                KitchenProcessOutcome.CODEC.listOf().optionalFieldOf("outcomes", List.of()).forGetter(BasicRecipeData::outcomes),
                KitchenProcessOutput.CODEC.fieldOf("output").forGetter(BasicRecipeData::output)
        ).apply(instance, BasicRecipeData::new));

        private static BasicRecipeData from(KitchenProcessRecipe recipe) {
            return new BasicRecipeData(recipe.station(), recipe.inputs(), recipe.method(), recipe.outcomes(), recipe.output());
        }
    }

    private record ToolConfig(Optional<ToolProfile> preferredTool, List<ToolProfile> allowedTools, boolean toolRequired) {
        private static final MapCodec<ToolConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ToolProfile.CODEC.optionalFieldOf("preferred_tool").forGetter(ToolConfig::preferredTool),
                ToolProfile.CODEC.listOf().optionalFieldOf("allowed_tools", List.of()).forGetter(ToolConfig::allowedTools),
                Codec.BOOL.optionalFieldOf("tool_required", false).forGetter(ToolConfig::toolRequired)
        ).apply(instance, ToolConfig::new));

        private static ToolConfig from(KitchenProcessRecipe recipe) {
            return new ToolConfig(recipe.preferredTool(), recipe.allowedTools(), recipe.toolRequired());
        }
    }

    private record ProcessConfig(
            int duration,
            int passiveDurationTicks,
            HeatLevel preferredHeat,
            HeatLevel minimumHeat,
            HeatLevel maximumHeat,
            boolean requiresPreheat,
            ProcessMode mode,
            KitchenEnvironmentRequirements environmentRequirements,
            KitchenRecipeGuideData guide,
            KitchenRecipeBookData recipeBook
    ) {
        private static final MapCodec<ProcessConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.optionalFieldOf("duration", 60).forGetter(ProcessConfig::duration),
                Codec.INT.optionalFieldOf("passive_duration_ticks", 0).forGetter(ProcessConfig::passiveDurationTicks),
                HeatLevel.CODEC.optionalFieldOf("preferred_heat", HeatLevel.OFF).forGetter(ProcessConfig::preferredHeat),
                HeatLevel.CODEC.optionalFieldOf("minimum_heat", HeatLevel.OFF).forGetter(ProcessConfig::minimumHeat),
                HeatLevel.CODEC.optionalFieldOf("maximum_heat", HeatLevel.OFF).forGetter(ProcessConfig::maximumHeat),
                Codec.BOOL.optionalFieldOf("requires_preheat", false).forGetter(ProcessConfig::requiresPreheat),
                ProcessMode.CODEC.optionalFieldOf("mode", ProcessMode.ACTIVE).forGetter(ProcessConfig::mode),
                KitchenEnvironmentRequirements.CODEC.optionalFieldOf("environment_requirements", KitchenEnvironmentRequirements.NONE)
                        .forGetter(ProcessConfig::environmentRequirements),
                KitchenRecipeGuideData.CODEC.optionalFieldOf("guide", KitchenRecipeGuideData.DEFAULT).forGetter(ProcessConfig::guide),
                KitchenRecipeBookData.CODEC.fieldOf("recipe_book").forGetter(ProcessConfig::recipeBook)
        ).apply(instance, ProcessConfig::new));

        private static ProcessConfig from(KitchenProcessRecipe recipe) {
            return new ProcessConfig(
                    recipe.duration(),
                    recipe.passiveDurationTicks(),
                    recipe.preferredHeat(),
                    recipe.minimumHeat(),
                    recipe.maximumHeat(),
                    recipe.requiresPreheat(),
                    recipe.mode(),
                    recipe.environmentRequirements(),
                    recipe.guide(),
                    recipe.recipeBook()
            );
        }
    }

    public static final MapCodec<KitchenProcessRecipe> CODEC = Codec.mapPair(
            BasicRecipeData.CODEC,
            Codec.mapPair(ToolConfig.CODEC, ProcessConfig.CODEC)
    ).xmap(
            data -> {
                BasicRecipeData basic = data.getFirst();
                ToolConfig toolConfig = data.getSecond().getFirst();
                ProcessConfig processConfig = data.getSecond().getSecond();
                return new KitchenProcessRecipe(
                        basic.station(),
                        basic.inputs(),
                        basic.method() == KitchenMethod.NONE
                                ? KitchenMethod.infer(
                                basic.station(),
                                processConfig.preferredHeat(),
                                toolConfig.preferredTool().orElse(ToolProfile.NONE),
                                basic.output().state(),
                                processConfig.mode(),
                                processConfig.environmentRequirements().nearbyWater(),
                                processConfig.requiresPreheat())
                                : basic.method(),
                        toolConfig.preferredTool(),
                        toolConfig.allowedTools().isEmpty() && toolConfig.preferredTool().isPresent() ? List.of(toolConfig.preferredTool().get()) : toolConfig.allowedTools(),
                        toolConfig.toolRequired(),
                        processConfig.duration(),
                        processConfig.passiveDurationTicks(),
                        processConfig.preferredHeat(),
                        processConfig.minimumHeat() == HeatLevel.OFF && processConfig.preferredHeat() != HeatLevel.OFF
                                ? processConfig.preferredHeat()
                                : processConfig.minimumHeat(),
                        processConfig.maximumHeat() == HeatLevel.OFF && processConfig.preferredHeat() != HeatLevel.OFF
                                ? processConfig.preferredHeat()
                                : processConfig.maximumHeat(),
                        processConfig.requiresPreheat(),
                        processConfig.mode(),
                        processConfig.environmentRequirements(),
                        processConfig.guide(),
                        processConfig.recipeBook(),
                        basic.outcomes(),
                        basic.output()
                );
            },
            recipe -> Pair.of(
                    BasicRecipeData.from(recipe),
                    Pair.of(ToolConfig.from(recipe), ProcessConfig.from(recipe))
            )
    );

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
            KitchenRecipeGuideData guide = KitchenRecipeGuideData.STREAM_CODEC.decode(buffer);
            KitchenRecipeBookData recipeBook = KitchenRecipeBookData.STREAM_CODEC.decode(buffer);
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
                    guide,
                    recipeBook,
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
            KitchenRecipeGuideData.STREAM_CODEC.encode(buffer, value.guide());
            KitchenRecipeBookData.STREAM_CODEC.encode(buffer, value.recipeBook());
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
