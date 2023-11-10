package com.Infinity.Nexus.Mod.utils;

import com.Infinity.Nexus.Mod.block.entity.WrappedHandler;
import com.Infinity.Nexus.Mod.item.ModItemsAdditions;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Map;

public class ModUtils {

    public static int getComponentLevel(ItemStack stack) {
        if(stack.getItem() == ModItemsAdditions.REDSTONE_COMPONENT.get()) {return 1;}
        if(stack.getItem() == ModItemsAdditions.BASIC_COMPONENT.get()) {return 2;}
        if(stack.getItem() == ModItemsAdditions.REINFORCED_COMPONENT.get()) {return 3;}
        if(stack.getItem() == ModItemsAdditions.LOGIC_COMPONENT.get()) {return 4;}
        if(stack.getItem() == ModItemsAdditions.ADVANCED_COMPONENT.get()) {return 5;}
        if(stack.getItem() == ModItemsAdditions.REFINED_COMPONENT.get()) {return 6;}
        if(stack.getItem() == ModItemsAdditions.INTEGRAL_COMPONENT.get()) {return 7;}
        if(stack.getItem() == ModItemsAdditions.INFINITY_COMPONENT.get()) {return 8;}
        return 0;
    }
    public static boolean canInsert(int slots, ItemStack stack,ItemStackHandler itemHandler ) {
        return  itemHandler.getStackInSlot(slots).getCount() < 1
                && !(stack.getItem() == ModItemsAdditions.SPEED_UPGRADE.get())
                && !(stack.getItem() == ModItemsAdditions.STRENGTH_UPGRADE.get());
    }
    public static int getSpeed(ItemStackHandler itemHandler, int upgradeSlots) {
        int speed = 0;
        for (int i = upgradeSlots; i <= upgradeSlots+4; i++){
            if(itemHandler.getStackInSlot(i).getItem() == ModItemsAdditions.SPEED_UPGRADE.get()) {
                speed++;
            }
        }
        return speed;
    }
}