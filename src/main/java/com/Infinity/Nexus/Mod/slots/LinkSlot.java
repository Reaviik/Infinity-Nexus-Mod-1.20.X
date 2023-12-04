package com.Infinity.Nexus.Mod.slots;

import com.Infinity.Nexus.Mod.item.ModItemsAdditions;
import com.Infinity.Nexus.Mod.utils.ModUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class LinkSlot extends SlotItemHandler {
    public LinkSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return !(ModUtils.isUpgrade(stack) || ModUtils.isComponent(stack)) && stack.is(ModItemsAdditions.LINKING_TOOL.get());
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}