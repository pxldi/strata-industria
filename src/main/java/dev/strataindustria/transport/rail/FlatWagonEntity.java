package dev.strataindustria.transport.rail;

import dev.strataindustria.StrataIndustria;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The flat wagon (outposts spec 7.2): a deck that carries one block, with its block entity, from one outpost to the
 * next. {@code FlatWagonEvents} picks a block up and sets it down; the wagon draws it as the minecart shows a block.
 */
public class FlatWagonEntity extends MineTubEntity {
    private @Nullable CompoundTag loadData;

    public FlatWagonEntity(EntityType<? extends FlatWagonEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDropItem() {
        return RailwayRegistry.FLAT_WAGON.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(RailwayRegistry.FLAT_WAGON.get());
    }

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        throw new IllegalStateException("A flat wagon has no slots");
    }

    @Override
    protected boolean tips() {
        return false;
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 6;
    }

    // ---------------------------------------------------------------- the load

    /** The block on the deck, or air. */
    public BlockState load() {
        return getDisplayBlockState();
    }

    public boolean loaded() {
        return !load().isAir();
    }

    /** The saved block entity of the load, if it had one. */
    public @Nullable CompoundTag loadData() {
        return loadData;
    }

    /** Sets a block on the deck, with the block entity it came with. */
    public void putLoad(BlockState state, @Nullable CompoundTag data) {
        setCustomDisplayBlockState(Optional.of(state));
        loadData = data;
    }

    /** Takes the block off the deck. */
    public void clearLoad() {
        setCustomDisplayBlockState(Optional.empty());
        loadData = null;
    }

    /** Sets the load down at {@code pos}, with its block entity. The caller has checked the place is free. */
    public void placeLoad(ServerLevel level, BlockPos pos) {
        BlockState state = load();
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (loadData != null && state.hasBlockEntity()) {
            BlockEntity entity = BlockEntity.loadStatic(pos, state, loadData, level.registryAccess());
            if (entity != null) level.setBlockEntity(entity);
        }
        clearLoad();
    }

    /** A wagon carrying a block is never "empty"; for the stop rules it is a full load. */
    @Override
    public boolean isEmpty() {
        return !loaded();
    }

    @Override
    public boolean cannotTakeMore(ServerLevel server) {
        return loaded();
    }

    @Override
    public int contentSignature() {
        return loaded() ? BuiltInRegistries.BLOCK.getId(load().getBlock()) + 1 : 0;
    }

    @Override
    public double fillAmount() {
        return loaded() ? 9 : 0;
    }

    @Override
    public int fillCapacity() {
        return 9;
    }

    /** Sneak-use with an empty hand sets the load down beside the wagon, on the player's side if there is room. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!player.isSecondaryUseActive() || !player.getItemInHand(hand).isEmpty() || !loaded()) return super.interact(player, hand, location);
        if (level() instanceof ServerLevel server) {
            BlockPos spot = freeSpotBeside(player);
            if (spot == null) {
                player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".flat_wagon.no_room"));
            } else {
                BlockState state = load();
                placeLoad(server, spot);
                FlatWagonEvents.puff(server, spot, state, true);
                player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".flat_wagon.set_down"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The first of the four sides, nearest the player first, where the block can be set down. */
    private @Nullable BlockPos freeSpotBeside(Player player) {
        Direction first = Direction.getApproximateNearest(player.getX() - getX(), 0, player.getZ() - getZ());
        BlockPos here = blockPosition();
        for (int i = 0; i < 4; i++) {
            Direction side = first;
            for (int turn = 0; turn < i; turn++) side = side.getClockWise();
            BlockPos pos = here.relative(side);
            BlockState there = level().getBlockState(pos);
            if (there.canBeReplaced() && !BaseRailBlock.isRail(there) && load().canSurvive(level(), pos)) return pos;
        }
        return null;
    }

    /** A use on the deck does nothing more than couple. */
    @Override
    protected InteractionResult openLoad(Player player, InteractionHand hand, Vec3 location) {
        return InteractionResult.PASS;
    }

    /** A broken wagon sets its load down where it stood, or drops the block. */
    @Override
    public void destroy(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        if (loaded()) {
            BlockPos at = blockPosition();
            if (level.getBlockState(at).canBeReplaced() && !BaseRailBlock.isRail(level, at)) placeLoad(level, at);
            else spawnAtLocation(level, new ItemStack(load().getBlock()));
        }
        super.destroy(level, source);
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (loadData != null) output.store("LoadData", CompoundTag.CODEC, loadData);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        loadData = input.read("LoadData", CompoundTag.CODEC).orElse(null);
    }
}
