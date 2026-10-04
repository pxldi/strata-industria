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
import net.minecraft.util.RandomSource;
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
    /** Tier 4 spec 3: clay molds take only metal that melts at or below this. */
    public static final int CLAY_MOLD_LIMIT = 1300;

    private final @Nullable MoldType type;
    private final boolean gear;
    private final boolean refractory;
    private final boolean sand;
    private final boolean bell;

    public CastMoldItem(@Nullable MoldType type, Properties properties) {
        this(type, false, false, properties);
    }

    /** A gear mold casts {@code <metal>_gear}; a refractory mold takes any metal, iron and steel included. */
    public CastMoldItem(@Nullable MoldType type, boolean gear, boolean refractory, Properties properties) {
        this(type, gear, refractory, false, properties);
    }

    /** A sand mold is pressed from a pattern, takes any metal and cracks apart after one casting. */
    public CastMoldItem(@Nullable MoldType type, boolean gear, boolean refractory, boolean sand, Properties properties) {
        this(type, gear, refractory, sand, false, properties);
    }

    /** The bell mold (uniqueness 4.2): casts a {@link dev.strataindustria.bronze.BellBlock} item from any bronze, brass or copper. */
    public static CastMoldItem bell(Properties properties) {
        return new CastMoldItem(null, false, false, false, true, properties);
    }

    private CastMoldItem(@Nullable MoldType type, boolean gear, boolean refractory, boolean sand, boolean bell, Properties properties) {
        super(properties);
        this.bell = bell;
        this.sand = sand;
        this.type = type;
        this.gear = gear;
        this.refractory = refractory;
    }

    /** The tool part this mold casts; null for ingot and gear molds. */
    public @Nullable MoldType type() {
        return type;
    }

    public boolean isGear() {
        return gear;
    }

    public boolean isBell() {
        return bell;
    }

    /** Whether a bell can be cast from this metal: the bronzes, brass and plain copper. */
    public static boolean ringsAsBell(Metal metal) {
        return metal.isBronze() || metal == Metal.BRASS || metal == Metal.COPPER;
    }

    public boolean isRefractory() {
        return refractory;
    }

    /** Whether the mold can take metal that melts at {@code meltingPoint}. */
    public boolean takes(int meltingPoint) {
        return refractory || meltingPoint <= CLAY_MOLD_LIMIT;
    }

    public int units() {
        if (bell) return dev.strataindustria.bronze.BronzeRegistry.BELL_UNITS;
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
        if (heat >= CrucibleBlockEntity.mixMeltingPoint(contents)) {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".mold.still_molten"));
            return InteractionResult.FAIL;
        }

        ItemStack cast = castOf(mold, heat, level.getGameTime());
        dev.strataindustria.mark.MakerMarks.stampCasting(mold, cast, player);
        boolean broke = breaks(mold, level.getRandom());
        ItemStack emptied = ItemStack.EMPTY;
        if (broke) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MOLD_BREAK.get(), SoundSource.PLAYERS, 0.9f, 1.0f);
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".mold.broke"));
        } else {
            emptied = emptied(mold);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MOLD_KNOCK.get(), SoundSource.PLAYERS, 0.8f,
                    0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        player.setItemInHand(hand, emptied);
        if (!player.addItem(cast)) net.minecraft.world.level.block.Block.popResource(level, player.blockPosition(), cast);
        return InteractionResult.SUCCESS;
    }

    /** What a filled mold gives when knocked out at {@code heat}: an ingot, tool head or gear as hot as the mold. */
    public static ItemStack castOf(ItemStack mold, float heat, long now) {
        CastMoldItem item = (CastMoldItem) mold.getItem();
        Melt contents = mold.getOrDefault(ModDataComponents.CAST_CONTENTS.get(), Melt.EMPTY);
        Metal metal = castMetal(contents);
        ItemStack cast;
        if (item.bell) {
            cast = new ItemStack(dev.strataindustria.bronze.BronzeRegistry.BELL_ITEM.get());
            Quality quality = new Quality(Quality.CAST);
            cast.set(ModDataComponents.QUALITY.get(), quality);
            cast.set(dev.strataindustria.bronze.BronzeRegistry.BELL_TONE.get(), dev.strataindustria.bronze.BellTone.of(contents));
            Heat.set(cast, heat, now);
            return cast;
        }
        if (item.gear) cast = ModItems.GEARS.containsKey(metal) ? new ItemStack(ModItems.GEARS.get(metal).get()) : new ItemStack(ModItems.ingot(Metal.SLAG_METAL));
        else cast = item.type == null ? new ItemStack(ModItems.ingot(metal)) : new ItemStack(ModItems.head(metal, item.type));
        if (cast.is(ModItems.ingot(Metal.SLAG_METAL))) cast.set(ModDataComponents.SLAG.get(), contents);
        if (item.type != null) cast.set(ModDataComponents.QUALITY.get(), new Quality(Quality.CAST));
        Heat.set(cast, heat, now);
        return cast;
    }

    /** Whether the mold cracks as the casting is knocked out of it. */
    public static boolean breaks(ItemStack mold, RandomSource random) {
        CastMoldItem item = (CastMoldItem) mold.getItem();
        if (item.sand) return true;
        double breakChance = item.refractory ? Config.REFRACTORY_MOLD_BREAK.getAsDouble()
                : item.type == null && !item.gear && !item.bell ? Config.INGOT_MOLD_BREAK.getAsDouble() : Config.TOOL_MOLD_BREAK.getAsDouble();
        return random.nextDouble() < breakChance;
    }

    /** The mold with its casting knocked out, ready to fill again. */
    public static ItemStack emptied(ItemStack mold) {
        ItemStack emptied = mold.copy();
        emptied.remove(ModDataComponents.CAST_CONTENTS.get());
        emptied.remove(net.minecraft.core.component.DataComponents.MAX_STACK_SIZE);
        return emptied;
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
