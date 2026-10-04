package dev.strataindustria.metal;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.ceramics.MoldType;
import dev.strataindustria.heat.Heat;
import dev.strataindustria.material.Metal;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A fired mold (spec 7.3). Filled at a crucible; once the metal has set, right-click it to knock the
 * casting out. The casting comes out as hot as the mold, and the mold sometimes cracks.
 */
public class CastMoldItem extends Item {
    private final @Nullable MoldType type;

    public CastMoldItem(@Nullable MoldType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    /** The tool part this mold casts; null for the ingot mold. */
    public @Nullable MoldType type() {
        return type;
    }

    public int units() {
        return type == null ? MetalContent.INGOT_UNITS : type.units();
    }

    /** The metal a cast holds once set: its alloy or single metal, or slag metal for an unknown mix. */
    public static Metal castMetal(Melt contents) {
        return Alloy.resultOf(contents).orElse(Metal.SLAG_METAL);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack mold = player.getItemInHand(hand);
        Melt contents = mold.get(ModDataComponents.CAST_CONTENTS.get());
        if (contents == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        float heat = Heat.get(mold, level);
        Metal metal = castMetal(contents);
        if (heat >= CrucibleBlockEntity.mixMeltingPoint(contents)) {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".mold.still_molten"));
            return InteractionResult.FAIL;
        }

        ItemStack cast = type == null ? new ItemStack(ModItems.ingot(metal)) : new ItemStack(ModItems.head(metal, type));
        if (metal == Metal.SLAG_METAL) cast.set(ModDataComponents.SLAG.get(), contents);
        if (type != null) cast.set(ModDataComponents.QUALITY.get(), new Quality(contents.quality(), Quality.CAST));
        Heat.set(cast, heat, level.getGameTime());

        double breakChance = type == null ? Config.INGOT_MOLD_BREAK.getAsDouble() : Config.TOOL_MOLD_BREAK.getAsDouble();
        boolean broke = level.getRandom().nextDouble() < breakChance;
        ItemStack emptied = ItemStack.EMPTY;
        if (broke) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MOLD_BREAK.get(), SoundSource.PLAYERS, 0.9f, 1.0f);
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".mold.broke"));
        } else {
            emptied = mold.copy();
            emptied.remove(ModDataComponents.CAST_CONTENTS.get());
            emptied.remove(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MOLD_KNOCK.get(), SoundSource.PLAYERS, 0.8f,
                    0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        player.setItemInHand(hand, emptied);
        if (!player.addItem(cast)) net.minecraft.world.level.block.Block.popResource(level, player.blockPosition(), cast);
        return InteractionResult.SUCCESS;
    }

    /** Tooltip line for a filled mold: "Bronze, 100 units". */
    public static Optional<Component> contentsLine(ItemStack stack) {
        Melt contents = stack.get(ModDataComponents.CAST_CONTENTS.get());
        if (contents == null) return Optional.empty();
        Metal metal = castMetal(contents);
        return Optional.of(Component.translatable(StrataIndustria.MOD_ID + ".mold.contents",
                Component.translatable(StrataIndustria.MOD_ID + ".metal." + metal.id()), contents.total()));
    }
}
