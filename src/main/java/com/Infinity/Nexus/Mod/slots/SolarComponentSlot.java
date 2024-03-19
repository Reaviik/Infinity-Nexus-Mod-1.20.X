package com.Infinity.Nexus.Mod.slots;

import com.Infinity.Nexus.Mod.utils.ModUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class SolarComponentSlot extends SlotItemHandler {
    public SolarComponentSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return ModUtils.isSolarComponent(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}