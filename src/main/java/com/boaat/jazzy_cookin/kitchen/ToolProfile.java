package com.boaat.jazzy_cookin.kitchen;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.boaat.jazzy_cookin.item.KitchenToolItem;
import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

public enum ToolProfile implements StringRepresentable {
    NONE("none"),
    KNIFE("knife"),
    PARING_KNIFE("paring_knife"),
    CHEF_KNIFE("chef_knife"),
    CLEAVER("cleaver"),
    WHISK("whisk"),
    HEAVY_WHISK("heavy_whisk"),
    ROLLING_PIN("rolling_pin"),
    MORTAR_PESTLE("mortar_pestle"),
    POT("pot"),
    STOCK_POT("stock_pot"),
    PAN("pan"),
    SKILLET("skillet"),
    FRYING_SKILLET("frying_skillet"),
    GRIDDLE("griddle"),
    STRAINER("strainer"),
    FINE_STRAINER("fine_strainer"),
    COARSE_STRAINER("coarse_strainer"),
    STEAMER_BASKET("steamer_basket"),
    LADLE("ladle"),
    TONGS("tongs"),
    SPATULA("spatula"),
    BAKING_TRAY("baking_tray"),
    GLASS_JAR("glass_jar"),
    JAR("jar"),
    PIE_TIN("pie_tin"),
    SAUCEPAN("saucepan"),
    FORK("fork"),
    SPOON("spoon"),
    TABLE_KNIFE("table_knife");

    private static final Map<String, ToolProfile> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(ToolProfile::getSerializedName, Function.identity()));

    public static final Codec<ToolProfile> CODEC = Codec.STRING.xmap(ToolProfile::byName, ToolProfile::getSerializedName);
    public static final StreamCodec<ByteBuf, ToolProfile> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            ToolProfile::byName,
            ToolProfile::getSerializedName
    );

    private final String serializedName;

    ToolProfile(String serializedName) {
        this.serializedName = serializedName;
    }

    public static ToolProfile byName(String name) {
        return BY_NAME.getOrDefault(name, NONE);
    }

    public static ToolProfile fromStack(ItemStack stack) {
        if (stack.getItem() instanceof KitchenToolItem toolItem) {
            return toolItem.profile();
        }
        return NONE;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
