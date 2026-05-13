package com.boaat.jazzy_cookin.item;

import java.util.List;

import com.boaat.jazzy_cookin.tutorial.KitchenBlockTutorial;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class KitchenTutorialBlockItem extends BlockItem {
    public KitchenTutorialBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        KitchenBlockTutorial.forBlock(this.getBlock()).ifPresent(tutorial -> {
            tooltipComponents.add(Component.translatable("tooltip.jazzycookin.block_tutorial", tutorial.category())
                    .withStyle(ChatFormatting.AQUA));
            tooltipComponents.add(Component.translatable("tooltip.jazzycookin.block_tutorial_key")
                    .withStyle(ChatFormatting.GRAY));
        });
    }
}
