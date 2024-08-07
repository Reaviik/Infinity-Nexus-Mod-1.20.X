package com.Infinity.Nexus.Mod.block.entity;

import com.Infinity.Nexus.Mod.block.custom.Miner;
import com.Infinity.Nexus.Mod.block.entity.common.SetMachineLevel;
import com.Infinity.Nexus.Mod.block.entity.common.SetUpgradeLevel;
import com.Infinity.Nexus.Mod.config.ConfigUtils;
import com.Infinity.Nexus.Mod.fakePlayer.IFFakePlayer;
import com.Infinity.Nexus.Mod.item.ModItemsAdditions;
import com.Infinity.Nexus.Mod.item.custom.ComponentItem;
import com.Infinity.Nexus.Mod.screen.miner.MinerMenu;
import com.Infinity.Nexus.Mod.utils.MinerTierStructure;
import com.Infinity.Nexus.Mod.utils.ModEnergyStorage;
import com.Infinity.Nexus.Mod.utils.ModUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MinerBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(17) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot == getComponentSlot()) {
                makeStructure();
                verify = maxVerify;
                progress = maxProgress;
            }
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0, 1, 2, 3, 4, 5, 6, 7, 8 -> !ModUtils.isUpgrade(stack) || !ModUtils.isComponent(stack);
                case 9, 10, 11, 12 -> ModUtils.isUpgrade(stack);
                case 13 -> ModUtils.isComponent(stack);
                //TODO
                case 14 -> true;
                case 15 -> stack.is(ModItemsAdditions.LINKING_TOOL.get().asItem());
                case 16 -> true;
                default -> super.isItemValid(slot, stack);
            };
        }
    };
    private static final int[] OUTPUT_SLOT = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] UPGRADE_SLOTS = {9, 10, 11, 12};
    private static final int COMPONENT_SLOT = 13;
    private static final int LINK_SLOT = 15;
    private static final int FORTUNE_SLOT = 14;
    private static final int STRUCTURE_SLOT = 16;
    private static final int ENERGY_STORAGE_CAPACITY = ConfigUtils.miner_energy_storage_capacity;
    private static final int ENERGY_TRANSFER_RATE = ConfigUtils.miner_energy_transfer_rate;

    private final ModEnergyStorage ENERGY_STORAGE = createEnergyStorage();


    private ModEnergyStorage createEnergyStorage() {
        return new ModEnergyStorage(ENERGY_STORAGE_CAPACITY, ENERGY_TRANSFER_RATE) {
            @Override
            public void onEnergyChanged() {
                setChanged();
                getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 4);
            }
        };
    }

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyStorage = LazyOptional.empty();


    private final Map<Direction, LazyOptional<WrappedHandler>> directionWrappedHandlerMap = Map.of(Direction.UP, LazyOptional.of(() -> new WrappedHandler(itemHandler, (i) -> i < 9, (i, s) -> !(ModUtils.isComponent(s) || ModUtils.isUpgrade(s)))), Direction.DOWN, LazyOptional.of(() -> new WrappedHandler(itemHandler, (i) -> i < 9, (i, s) -> !(ModUtils.isComponent(s) || ModUtils.isUpgrade(s)))), Direction.NORTH, LazyOptional.of(() -> new WrappedHandler(itemHandler, (i) -> i < 9, (i, s) -> !(ModUtils.isComponent(s) || ModUtils.isUpgrade(s)))), Direction.SOUTH, LazyOptional.of(() -> new WrappedHandler(itemHandler, (i) -> i < 9, (i, s) -> !(ModUtils.isComponent(s) || ModUtils.isUpgrade(s)))), Direction.EAST, LazyOptional.of(() -> new WrappedHandler(itemHandler, (i) -> i < 9, (i, s) -> !(ModUtils.isComponent(s) || ModUtils.isUpgrade(s)))), Direction.WEST, LazyOptional.of(() -> new WrappedHandler(itemHandler, (i) -> i < 9, (i, s) -> !(ModUtils.isComponent(s) || ModUtils.isUpgrade(s)))));

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 80;
    private int verify = 0;
    private int maxVerify = 15;
    private int structure = 0;

    private int hasRedstoneSignal = 0;
    private int stillCrafting = 0;
    private int hasSlotFree = 0;
    private int hasComponent = 0;
    private int hasEnoughEnergy = 0;
    private int hasRecipe = 0;

    private int linkx = 0;
    private int linky = 0;
    private int linkz = 0;
    private int linkFace = 0;


    public MinerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.MINER_BE.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> MinerBlockEntity.this.progress;
                    case 1 -> MinerBlockEntity.this.maxProgress;
                    case 2 -> MinerBlockEntity.this.verify;
                    case 3 -> MinerBlockEntity.this.maxVerify;
                    case 4 -> MinerBlockEntity.this.structure;

                    case 5 -> MinerBlockEntity.this.hasRedstoneSignal;
                    case 6 -> MinerBlockEntity.this.stillCrafting;
                    case 7 -> MinerBlockEntity.this.hasSlotFree;
                    case 8 -> MinerBlockEntity.this.hasComponent;
                    case 9 -> MinerBlockEntity.this.hasEnoughEnergy;
                    case 10 -> MinerBlockEntity.this.hasRecipe;

                    case 11 -> MinerBlockEntity.this.linkx;
                    case 12 -> MinerBlockEntity.this.linky;
                    case 13 -> MinerBlockEntity.this.linkz;
                    case 14 -> MinerBlockEntity.this.linkFace;

                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> MinerBlockEntity.this.progress = pValue;
                    case 1 -> MinerBlockEntity.this.maxProgress = pValue;
                    case 2 -> MinerBlockEntity.this.verify = pValue;
                    case 3 -> MinerBlockEntity.this.maxVerify = pValue;
                    case 4 -> MinerBlockEntity.this.structure = pValue;

                    case 5 -> MinerBlockEntity.this.hasRedstoneSignal = pValue;
                    case 6 -> MinerBlockEntity.this.stillCrafting = pValue;
                    case 7 -> MinerBlockEntity.this.hasSlotFree = pValue;
                    case 8 -> MinerBlockEntity.this.hasComponent = pValue;
                    case 9 -> MinerBlockEntity.this.hasEnoughEnergy = pValue;
                    case 10 -> MinerBlockEntity.this.hasRecipe = pValue;

                    case 11 -> MinerBlockEntity.this.linkx = pValue;
                    case 12 -> MinerBlockEntity.this.linky = pValue;
                    case 13 -> MinerBlockEntity.this.linkz = pValue;
                    case 14 -> MinerBlockEntity.this.linkFace = pValue;
                }
            }

            @Override
            public int getCount() {
                return 15;
            }
        };

    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyEnergyStorage.cast();
        }

        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == null) {
                return lazyItemHandler.cast();
            }

            if (directionWrappedHandlerMap.containsKey(side)) {
                Direction localDir = this.getBlockState().getValue(Miner.FACING);

                if (side == Direction.UP || side == Direction.DOWN) {
                    return directionWrappedHandlerMap.get(side).cast();
                }

                return switch (localDir) {
                    default -> directionWrappedHandlerMap.get(side.getOpposite()).cast();
                    case EAST -> directionWrappedHandlerMap.get(side.getClockWise()).cast();
                    case SOUTH -> directionWrappedHandlerMap.get(side).cast();
                    case WEST -> directionWrappedHandlerMap.get(side.getCounterClockWise()).cast();
                };
            }
        }


        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyEnergyStorage = LazyOptional.of(() -> ENERGY_STORAGE);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyStorage.invalidate();
    }


    public static int getComponentSlot() {
        return COMPONENT_SLOT;
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.infinity_nexus_mod.miner").append(" LV " + getMachineLevel());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new MinerMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    public IEnergyStorage getEnergyStorage() {
        return ENERGY_STORAGE;
    }
    public int[] getDisplayInfo() {
        return new int[] { data.get(5), data.get(8), data.get(9), data.get(4), data.get(7), data.get(10)};
    }

    public void setEnergyLevel(int energy) {
        this.ENERGY_STORAGE.setEnergy(energy);
    }

    public int getHasEnoughEnergy() {
        return data.get(9);
    }

    public int getHasRecipe() {
        return data.get(10);
    }

    public String getHasLink() {
        if (this.data.get(11) != 0 || this.data.get(12) != 0 || this.data.get(13) != 0) {
            return "X: " + this.data.get(11) + ", Y: " + this.data.get(12) + ", Z: " + this.data.get(13);
        } else {
            return "[Unlinked]";

        }
    }

    public ItemStack getLikedBlock() {
        return new ItemStack(level.getBlockState(new BlockPos(this.data.get(11), this.data.get(12), this.data.get(13))).getBlock().asItem());
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("miner.progress", progress);
        pTag.putInt("miner.energy", ENERGY_STORAGE.getEnergyStored());

        pTag.putInt("miner.hasStructure", data.get(4));

        pTag.putInt("miner.hasRedstoneSignal", data.get(5));
        pTag.putInt("miner.stillCrafting", data.get(6));
        pTag.putInt("miner.hasSlotFree", data.get(7));
        pTag.putInt("miner.hasComponent", data.get(8));
        pTag.putInt("miner.hasEnoughEnergy", data.get(9));
        pTag.putInt("miner.hasRecipe", data.get(10));

        pTag.putInt("miner.linkx", data.get(11));
        pTag.putInt("miner.linky", data.get(12));
        pTag.putInt("miner.linkz", data.get(13));
        pTag.putInt("miner.linkFace", data.get(14));

        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        progress = pTag.getInt("miner.progress");
        ENERGY_STORAGE.setEnergy(pTag.getInt("miner.energy"));

        structure = pTag.getInt("miner.hasStructure");

        hasRedstoneSignal = pTag.getInt("miner.hasRedstoneSignal");
        stillCrafting = pTag.getInt("miner.stillCrafting");
        hasSlotFree = pTag.getInt("miner.hasSlotFree");
        hasComponent = pTag.getInt("miner.hasComponent");
        hasEnoughEnergy = pTag.getInt("miner.hasEnoughEnergy");
        hasRecipe = pTag.getInt("miner.hasRecipe");

        linkx = pTag.getInt("miner.linkx");
        linky = pTag.getInt("miner.linky");
        linkz = pTag.getInt("miner.linkz");
        linkFace = pTag.getInt("miner.linkFace");
    }


    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {

        if (pLevel.isClientSide) {
            return;
        }
        int machineLevel = getMachineLevel() - 1 <= 0 ? 0 : getMachineLevel() - 1;
        if (structure == 0) {
            pLevel.setBlock(pPos, pState.setValue(Miner.LIT, machineLevel), 3);
        }

        if (isRedstonePowered(pPos)) {
            this.data.set(5, 1);
            pLevel.setBlock(pPos, pState.setValue(Miner.LIT, machineLevel), 3);
            return;
        }
        this.data.set(5, 0);

        if (!hasProgressFinished()) {
            this.data.set(6, 0);
            increaseCraftingProgress();
            return;
        }
        this.data.set(6, 1);

        if (!hasEmptySlot()) {
            this.data.set(7, 0);
            pLevel.setBlock(pPos, pState.setValue(Miner.LIT, machineLevel), 3);
            return;
        }
        this.data.set(7, 1);

        resetProgress();
        if (!hasComponent()) {
            this.data.set(8, 0);
            pLevel.setBlock(pPos, pState.setValue(Miner.LIT, machineLevel), 3);
            return;
        }
        this.data.set(8, 1);

        setMaxProgress(machineLevel);
        if (!hasEnoughEnergy(machineLevel)) {
            this.data.set(9, 0);
            pLevel.setBlock(pPos, pState.setValue(Miner.LIT, machineLevel), 3);
            return;
        }
        this.data.set(9, 1);
        if (hasRecipe(pPos, machineLevel)) {
            this.data.set(10, 1);
            pLevel.setBlock(pPos, pState.setValue(Miner.LIT, machineLevel + 9), 3);
            craftItem(pPos, machineLevel);
            extractEnergy(this, machineLevel);
            setChanged(pLevel, pPos, pState);
        }
        this.data.set(10, 0);
    }

    private boolean hasEmptySlot() {
        boolean hasFreeSpace = false;
        for (int slot : OUTPUT_SLOT) {
            if (itemHandler.getStackInSlot(slot).isEmpty()) {
                hasFreeSpace = true;
                break;
            }
        }
        return hasFreeSpace;
    }

    private boolean hasComponent() {
        return itemHandler.getStackInSlot(COMPONENT_SLOT).getItem() instanceof ComponentItem;
    }

    private void extractEnergy(MinerBlockEntity minerBlockEntity, int machineLevel) {
        int energy = ((machineLevel + 1) * 1000);
        int speed = Math.max(ModUtils.getSpeed(itemHandler, UPGRADE_SLOTS), 2) + machineLevel;
        int strength = (ModUtils.getStrength(itemHandler, UPGRADE_SLOTS) * 10);

        int var1 = energy * speed;

        int extractEnergy = var1 + strength;
        minerBlockEntity.ENERGY_STORAGE.extractEnergy(Math.max(extractEnergy, 1), false);
    }

    private boolean hasEnoughEnergy(int machineLevel) {

        int energy = ((machineLevel + 1) * 1000);
        int speed = Math.max(ModUtils.getSpeed(itemHandler, UPGRADE_SLOTS), 2) + machineLevel;
        int strength = (ModUtils.getStrength(itemHandler, UPGRADE_SLOTS) * 10);

        int var1 = energy * speed;

        int extractEnergy = var1 + strength;
        return ENERGY_STORAGE.getEnergyStored() >= extractEnergy;
    }

    private void resetProgress() {
        progress = 0;
    }

    private boolean isOre(ItemStack stack) {
        List<TagKey<Item>> tags = stack.getTags().toList();

        return tags.toString().contains("forge:ores");
    }

    private ItemStack getDrop(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem)) {
            return null;
        }
        IFFakePlayer player = new IFFakePlayer((ServerLevel) this.level);
        Block block = ((BlockItem) stack.getItem()).getBlock();
        AtomicReference<ItemStack> dropItem = new AtomicReference<ItemStack>(ItemStack.EMPTY);
        ItemStack pickaxe = getPickaxe();

        List<ItemStack> drops = new ArrayList<>(Block.getDrops(block.defaultBlockState(), (ServerLevel) level, this.getBlockPos(), null, player, pickaxe));

        drops.forEach(stack1 -> {
            int drop = new Random().nextInt(drops.size());
            dropItem.set(drops.get(drop).copy());

        });
        return dropItem.get();
    }

    private ItemStack getPickaxe() {
        ItemStack enchantedItem = itemHandler.getStackInSlot(FORTUNE_SLOT);
        ItemStack pickaxe = new ItemStack(Items.NETHERITE_PICKAXE);
        Map<Enchantment, Integer> enchantments = enchantedItem.getAllEnchantments();
        if(enchantedItem.getItem() instanceof EnchantedBookItem){
            Tag enchantedBook = enchantedItem.getTag();
            if(enchantedBook != null && !enchantedBook.toString().contains("silk_touch")){
                pickaxe.enchant(Enchantments.BLOCK_FORTUNE, getFortuneLevel());
            }
        }else{
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                if (entry.getKey() != Enchantments.SILK_TOUCH) {
                    pickaxe.enchant(entry.getKey(), entry.getValue());
                }
            }
        }
        return pickaxe;
    }

    private void craftItem(BlockPos pos, int machineLevel) {

        ItemStack component = this.itemHandler.getStackInSlot(COMPONENT_SLOT);

        ModUtils.useComponent(component, level, this.getBlockPos());

        int fortune = getFortuneLevel();
        ItemStack output = getOutputItem(pos, fortune, machineLevel);

        if ((output.getItem() == ModItemsAdditions.RAW_INFINITY.get())) {
            if (machineLevel < 6) {
                return;
            }
            output = new ItemStack(ModItemsAdditions.RAW_INFINITY.get());
        }
        if ((output.getItem() == Blocks.ANCIENT_DEBRIS.asItem())) {
            if (machineLevel < 4) {
                return;
            }
            if (machineLevel < 6) {
                output = new ItemStack(Items.NETHERITE_SCRAP);
            }
        }
        Random random = new Random();
        int randomTier = random.nextInt(ConfigUtils.miner_tier_crystal_chance * (machineLevel + 1));
        int random1 = random.nextInt(ConfigUtils.miner_old_tier_crystal_chance * Math.max(machineLevel, 1));

        //if (random1 < 10) {
        //    int[] chance = {100, 61, 38, 23, 14, 9, 5, 3, 1};
        //    for (int i = 1; i < machineLevel + 2; i++) {
        //        if (randomTier < chance[chance.length - i]) {
        //            insertItemOnInventory(new ItemStack(ModUtils.getCrystalType(chance.length - i).getItem()));
        //            break;
        //        }
        //    }
        //}
        for(int i = 1; i < (machineLevel +1); i++){
            if(random1 < i){
                insertItemOnInventory(new ItemStack(ModUtils.getCrystalType(Math.min(i, 7)).getItem()));
                break;
            }
        }

        insertItemOnInventory(randomTier <= 1 ? new ItemStack(ModUtils.getCrystalType(Math.min(machineLevel + 1, 7)).getItem()) : output);
        level.playSound(null, this.getBlockPos(), SoundEvents.BEE_HURT, SoundSource.BLOCKS, 0.1f, 1.0f);


    }

    private boolean hasRecipe(BlockPos pos, int machineLevel) {

        if (this.verify >= maxVerify) {
            this.structure = MinerTierStructure.hasStructure(machineLevel + 1, pos, level) ? 1 : 0;
            this.data.set(4, structure);
            this.verify = 0;
        }
        this.verify++;
        return this.structure > 0;
    }

    private ItemStack getOutputItem(BlockPos pos, int fortune, int machineLevel) {
        List<ItemStack> drops = new ArrayList<>();
        int radio = ((int) Math.floor((double) machineLevel / 2) + 1);

        int startX = pos.getX() - radio;
        ;
        int startY = pos.getY() - ((int) Math.floor((double) (machineLevel + 4) / 2) * 2);
        int startZ = pos.getZ() - radio;

        int endX = pos.getX() + radio;
        int endY = pos.getY() - 2;
        int endZ = pos.getZ() + radio;

        int randomX = startX + new Random().nextInt(endX - (startX - 1));
        int randomY = startY + new Random().nextInt(endY - (startY - 1));
        int randomZ = startZ + new Random().nextInt(endZ - (startZ - 1));

        BlockPos novoBlockPos = new BlockPos(randomX, randomY, randomZ);

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                for (int z = startZ; z <= endZ; z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    if (blockPos.equals(novoBlockPos)) {
                        BlockState blockState = level.getBlockState(blockPos);
                        ItemStack blockStack = new ItemStack(blockState.getBlock().asItem());
                        if (blockState.isAir() || isOre(blockStack)) {
                            ItemStack drop = getDrop(blockStack);
                            drops.add(Objects.requireNonNullElse(drop, ItemStack.EMPTY));
                        }
                    }
                }
            }
        }
        if (drops.isEmpty()) {
            drops.add(ItemStack.EMPTY);
        }
        return drops.get(new Random().nextInt(drops.size()));
    }

    private int getFortuneLevel() {
        ItemStack bookItem = itemHandler.getStackInSlot(FORTUNE_SLOT);
        int enchantmentLevel = 0;
        CompoundTag bookTag = bookItem.getTag();
        if (bookTag != null && bookTag.contains("StoredEnchantments")) {
            ListTag enchantmentsTag = bookTag.getList("StoredEnchantments", 10);
            for (int i = 0; i < enchantmentsTag.size(); i++) {
                CompoundTag enchantmentTag = enchantmentsTag.getCompound(i);
                String enchantmentId = enchantmentTag.getString("id");
                if (enchantmentId.equals("minecraft:fortune")) {
                    enchantmentLevel = enchantmentTag.getShort("lvl");
                    break;
                }
            }
        }
        //{StoredEnchantments:[{id:"minecraft:fortune",lvl:3s}]}
        return enchantmentLevel;

    }

    private void insertItemOnInventory(ItemStack itemStack) {
        try {
            if (itemHandler.getStackInSlot(LINK_SLOT).is(ModItemsAdditions.LINKING_TOOL.get())) {
                ItemStack linkingTool = itemHandler.getStackInSlot(LINK_SLOT).copy();
                AtomicBoolean success = new AtomicBoolean(false);
                String name = linkingTool.getDisplayName().getString();
                this.data.set(11, 0);
                this.data.set(12, 0);
                this.data.set(13, 0);
                if (linkingTool.hasCustomHoverName()) {
                    String[] parts = name.substring(1, name.length() - 1).split(",");
                    int xl = 0;
                    int yl = 0;
                    int zl = 0;
                    String facel = "up";

                    for (String part : parts) {
                        String[] keyValue = part.split("=");
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();

                        if (key.equals("x")) {
                            xl = Integer.parseInt(value);
                            this.data.set(11, xl);
                        } else if (key.equals("y")) {
                            yl = Integer.parseInt(value);
                            this.data.set(12, yl);
                        } else if (key.equals("z")) {
                            zl = Integer.parseInt(value);
                            this.data.set(13, zl);
                        } else if (key.equals("face")) {
                            facel = value;
                        }
                    }
                    BlockEntity blockEntity = this.level.getBlockEntity(new BlockPos(xl, yl, zl));
                    if (blockEntity != null && canLink(blockEntity)) {
                        blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, getLinkedSide(facel)).ifPresent(iItemHandler -> {
                            for (int slot = 0; slot < iItemHandler.getSlots(); slot++) {
                                if (ModUtils.canPlaceItemInContainer(itemStack.copy(), slot, iItemHandler) && iItemHandler.isItemValid(slot, itemStack.copy())) {
                                    iItemHandler.insertItem(slot, itemStack.copy(), false);
                                    success.set(true);
                                    break;
                                }
                            }

                            for (int slot = 0; slot < iItemHandler.getSlots(); slot++) {
                                for (int outputSlot : OUTPUT_SLOT) {
                                    if (!itemHandler.getStackInSlot(outputSlot).isEmpty() && iItemHandler.isItemValid(slot, itemStack.copy()) && ModUtils.canPlaceItemInContainer(itemHandler.getStackInSlot(outputSlot).copy(), slot, iItemHandler)) {
                                        iItemHandler.insertItem(slot, itemHandler.getStackInSlot(outputSlot).copy(), false);
                                        itemHandler.extractItem(outputSlot, itemHandler.getStackInSlot(outputSlot).getCount(), false);
                                        success.set(true);
                                        break;
                                    }
                                }
                            }
                        });
                    }
                }
                if (!success.get()) {
                    insertItemOnSelfInventory(itemStack);
                }
            } else {
                insertItemOnSelfInventory(itemStack);
            }

        } catch (Exception e) {
            System.out.println("§f[INM§f]§c: Failed to insert item in: " + this.getBlockPos());
        }
    }

    private Direction getLinkedSide(String side) {
        switch (side) {
            case "up":
                return Direction.UP;
            case "down":
                return Direction.DOWN;
            case "north":
                return Direction.NORTH;
            case "south":
                return Direction.SOUTH;
            case "west":
                return Direction.WEST;
            case "east":
                return Direction.EAST;
            default:
                return Direction.UP;
        }
    }

    private boolean canLink(BlockEntity blockEntity) {
        return (int) Math.sqrt(this.getBlockPos().distSqr(blockEntity.getBlockPos())) < 100;
    }

    private void insertItemOnSelfInventory(ItemStack itemStack) {
        for (int slot : OUTPUT_SLOT) {
            if (ModUtils.canPlaceItemInContainer(itemStack, slot, this.itemHandler)) {
                this.itemHandler.insertItem(slot, itemStack, false);
                break;
            }
        }
    }

    private int getMachineLevel() {
        return ModUtils.getComponentLevel(this.itemHandler.getStackInSlot(COMPONENT_SLOT));
    }

    private boolean isRedstonePowered(BlockPos pPos) {
        return this.level.hasNeighborSignal(pPos);
    }

    private boolean hasProgressFinished() {
        return progress >= maxProgress;
    }

    private void increaseCraftingProgress() {
        progress++;
    }

    private void setMaxProgress(int machineLevel) {
        int duration = 130;
        int speed = ModUtils.getSpeed(itemHandler, UPGRADE_SLOTS);

        duration = duration - (machineLevel * Math.max(speed, 1));
        maxProgress = Math.max(duration, ConfigUtils.miner_minimum_tick);
    }

    public static int getInputSlot() {
        return 0;
    }

    public static int getOutputSlot() {
        return 0;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithFullMetadata();
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
    }

    private void makeStructure() {
        MinerTierStructure.hasStructure(getMachineLevel(), this.getBlockPos(), this.level);
    }
    public void resetVerify() {
        this.data.set(2, this.data.get(3));
    }
    public void setMachineLevel(ItemStack itemStack, Player player) {
        SetMachineLevel.setMachineLevel(itemStack, player, this, COMPONENT_SLOT, this.itemHandler);
        verify = maxVerify;
        progress = maxProgress;
    }
    public void setUpgradeLevel(ItemStack itemStack, Player player) {
        SetUpgradeLevel.setUpgradeLevel(itemStack, player, this, UPGRADE_SLOTS, this.itemHandler);
    }
}