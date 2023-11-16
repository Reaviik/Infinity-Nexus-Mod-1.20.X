package com.Infinity.Nexus.Mod.datagen;

import com.Infinity.Nexus.Mod.InfinityNexusMod;
import com.Infinity.Nexus.Mod.block.ModBlocksAdditions;
import com.Infinity.Nexus.Mod.block.ModBlocksProgression;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, InfinityNexusMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {

        blockWithItem(ModBlocksAdditions.INFINITY_BLOCK);
        blockWithItem(ModBlocksAdditions.LEAD_BLOCK);
        blockWithItem(ModBlocksAdditions.ALUMINUM_BLOCK);
        blockWithItem(ModBlocksAdditions.RAW_INFINITY_BLOCK);
        blockWithItem(ModBlocksAdditions.RAW_LEAD_BLOCK);
        blockWithItem(ModBlocksAdditions.RAW_ALUMINUM_BLOCK);
        blockWithItem(ModBlocksAdditions.INFINITY_ORE);
        blockWithItem(ModBlocksAdditions.DEEPSLATE_INFINITY_ORE);
        blockWithItem(ModBlocksAdditions.LEAD_ORE);
        blockWithItem(ModBlocksAdditions.DEEPSLATE_LEAD_ORE);
        blockWithItem(ModBlocksAdditions.ALUMINUM_ORE);
        blockWithItem(ModBlocksAdditions.DEEPSLATE_ALUMINUM_ORE);

        blockWithItem(ModBlocksAdditions.ASPHALT);



    }

    private void blockWithItem(RegistryObject<Block> blockRegistryObject){
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));
    }
}