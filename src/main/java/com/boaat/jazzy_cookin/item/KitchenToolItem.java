package com.boaat.jazzy_cookin.item;

import java.util.List;

import com.boaat.jazzy_cookin.kitchen.ToolProfile;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class KitchenToolItem extends Item {
    private final ToolProfile profile;
    private final float qualityBonus;
    private final float speedMultiplier;

    public KitchenToolItem(Properties properties, ToolProfile profile, float qualityBonus, float speedMultiplier) {
        super(properties);
        this.profile = profile;
        this.qualityBonus = qualityBonus;
        this.speedMultiplier = speedMultiplier;
    }

    public ToolProfile profile() {
        return this.profile;
    }

    public float qualityBonus() {
        return this.qualityBonus;
    }

    public float speedMultiplier() {
        return this.speedMultiplier;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.jazzycookin.tool_profile", Component.translatable("tool.jazzycookin." + this.profile.getSerializedName()))
                .withStyle(ChatFormatting.GRAY));
    }
}
