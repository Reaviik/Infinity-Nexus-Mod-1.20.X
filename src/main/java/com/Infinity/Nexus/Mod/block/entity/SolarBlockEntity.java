package com.Infinity.Nexus.Mod.block.entity;

import com.Infinity.Nexus.Mod.block.custom.Solar;
import com.Infinity.Nexus.Mod.item.ModItemsAdditions;
import com.Infinity.Nexus.Mod.screen.solar.solar.SolarMenu;
import com.Infinity.Nexus.Mod.utils.ModEnergyStorage;
import com.Infinity.Nexus.Mod.utils.ModUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DaylightDetectorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class SolarBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return super.isItemValid(slot, stack);
        }
    };
    private static final int COMPONENT_SLOT = 0;
    private static final int GENERATED = 8;
    private static final int TRANSFER = 64000;
    private static final int CAPACITY = 512000;
    private static int GEM = 0;

    private final ModEnergyStorage ENERGY_STORAGE = createEnergyStorage();


    private ModEnergyStorage createEnergyStorage() {
        return new ModEnergyStorage(CAPACITY, TRANSFER) {
            @Override
            public void onEnergyChanged() {
                setChanged();
                getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 4);
            }
        };
    }

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyStorage = LazyOptional.empty();


    private final Map<Direction, LazyOptional<WrappedHandler>> directionWrappedHandlerMap =
            Map.of(
                    Direction.UP, LazyOptional.of(() -> new WrappedHandler(itemHandler, (i) -> false, (i, s) -> !(ModUtils.isSolarComponent(s)))),
                    Direction.DOWN, LazyOptional.of(() -> new WrappedHandler(itemHandler, (i) -> false, (i, s) -> !(ModUtils.isSolarComponent(s)))),
                    Direction.NORTH, LazyOptional.of(() -> new WrappedHandler(itemHandler, (i) -> false, (i, s) -> !(ModUtils.isSolarComponent(s)))),
                    Direction.SOUTH, LazyOptional.of(() -> new WrappedHandler(itemHandler, (i) -> false, (i, s) -> !(ModUtils.isSolarComponent(s)))),
                    Direction.EAST, LazyOptional.of(() -> new WrappedHandler(itemHandler, (i) -> false, (i, s) -> !(ModUtils.isSolarComponent(s)))),
                    Direction.WEST, LazyOptional.of(() -> new WrappedHandler(itemHandler, (i) -> false, (i, s) -> !(ModUtils.isSolarComponent(s)))));

    protected final ContainerData data;
    public SolarBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.SOLAR_BE.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return GEM;
            }

            @Override
            public void set(int pIndex, int pValue) {
                 data.set(0, pValue);
            }

            @Override
            public int getCount() {
                return 1;
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
                Direction localDir = this.getBlockState().getValue(Solar.FACING);

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
    public boolean getTime(){
            return this.getLevel().getLevelData().getDayTime() % 24000 < 12000;
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

        return switch (getSolarLevel()) {
            case 0 -> Component.translatable("block.infinity_nexus_mod.solar");
            case 1 -> Component.translatable("item.infinity_nexus_mod.solar_pane");
            case 2 -> Component.translatable("item.infinity_nexus_mod.solar_pane_advanced");
            case 3 -> Component.translatable("item.infinity_nexus_mod.solar_pane_ultimate");
            case 4 -> Component.translatable("item.infinity_nexus_mod.solar_pane_quantum");
            default -> throw new IllegalStateException("Unexpected value: " + getSolarLevel());
        };
    }
    private int getSolarLevel() {
        int level = 0;
        ItemStack stack = itemHandler.getStackInSlot(COMPONENT_SLOT);
        if (stack.getItem() == ModItemsAdditions.SOLAR_PANE.get()) {
            level = 1;
        }else
        if (stack.getItem() == ModItemsAdditions.SOLAR_PANE_ADVANCED.get()) {
            level = 2;
        }else
        if (stack.getItem() == ModItemsAdditions.SOLAR_PANE_ULTIMATE.get()) {
            level = 3;
        }else
        if (stack.getItem() == ModItemsAdditions.SOLAR_PANE_QUANTUM.get()) {
            level = 4;
        }
        return level;
    }
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new SolarMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    public IEnergyStorage getEnergyStorage() {
        return ENERGY_STORAGE;
    }

    public void setEnergyLevel(int energy) {
        this.ENERGY_STORAGE.setEnergy(energy);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("solar.energy", ENERGY_STORAGE.getEnergyStored());

        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        ENERGY_STORAGE.setEnergy(pTag.getInt("solar.energy"));
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {

        if (pLevel.isClientSide) {
            return;
        }
        int level = getSolarLevel();
        pLevel.setBlock(pPos, pState.setValue(Solar.LIT, 0), 3);

        if(!hasUpgrade()){
            return;
        }

        if (isRedstonePowered(pPos)) {
            return;
        }

        if (!hasRecipe()) {
            GEM = 0;
            return;
        }
        GEM = 1;
        if (!hasEnoughEnergySpace()) {
            return;
        }
        generateEnergy();
        pLevel.setBlock(pPos, pState.setValue(Solar.LIT, level), 3);
        setChanged(pLevel, pPos, pState);
    }

    private boolean hasUpgrade() {
        return getSolarLevel() > 0;
    }

    private void generateEnergy() {
        ENERGY_STORAGE.receiveEnergy(getGenerationRate(),false);
    }

    public  int getGenerationRate() {
        int level = getSolarLevel();
        int[] energy = {0, 8, 72, 648, 5832};
        return getTime() ? energy[level] : energy[level] / 8;
    }

    private boolean hasEnoughEnergySpace() {
        return ENERGY_STORAGE.getEnergyStored() < ENERGY_STORAGE.getMaxEnergyStored();
    }

    private boolean hasRecipe() {
        return this.getLevel().canSeeSky(this.getBlockPos().above());
    }

    private boolean isRedstonePowered(BlockPos pPos) {
        return this.level.hasNeighborSignal(pPos);
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

    public void setMachineLevel(ItemStack itemStack, Player player) {
        if (this.itemHandler.getStackInSlot(COMPONENT_SLOT).isEmpty()) {
            this.itemHandler.setStackInSlot(COMPONENT_SLOT, itemStack.copy());
            player.getMainHandItem().shrink(1);
            this.setChanged();
        }else{
            ItemStack component = this.itemHandler.getStackInSlot(COMPONENT_SLOT);
            this.itemHandler.setStackInSlot(COMPONENT_SLOT, itemStack.copy());
            ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), component);
            if (!player.isCreative()) {
                player.getMainHandItem().shrink(1);
                this.level.addFreshEntity(itemEntity);
            }
            this.setChanged();
        }
        assert level != null;
        level.playSound(null, this.getBlockPos(), SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.BLOCKS, 1.0f, 1.0f);
    }
}