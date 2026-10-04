package dev.strataindustria.mark;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.registry.ModDataComponents;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModMenus;
import dev.strataindustria.registry.ModSounds;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Maker's marks (uniqueness 2.2): the stamp on a piece, its tally of blocks mined, the editor and the punch that opens it. */
public final class MarkRegistry {
    /** The mark and maker's name a part carries. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MakerStamp>> STAMP =
            ModDataComponents.COMPONENTS.registerComponentType("maker_stamp", b -> b
                    .persistent(MakerStamp.CODEC)
                    .networkSynchronized(MakerStamp.STREAM_CODEC));

    /** Blocks a stamped tool has broken. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BLOCKS_MINED =
            ModDataComponents.COMPONENTS.registerComponentType("blocks_mined", b -> b
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<MenuType<?>, MenuType<MarkMenu>> MENU =
            ModMenus.MENUS.register("makers_mark", () -> IMenuTypeExtension.create((id, inventory, buf) -> new MarkMenu(id)));

    /** A small bronze punch; right-click to cut a new mark. */
    public static final DeferredItem<MakerPunchItem> MAKER_PUNCH = ModItems.ITEMS.registerItem("maker_punch", MakerPunchItem::new,
            p -> p.stacksTo(1));

    /** The punch struck into a piece. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STAMP_STRIKE = sound("mark.stamp");
    /** A cell cut or filled in the editor. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MARK_CUT = sound("mark.cut");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return ModSounds.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StrataIndustria.id(name)));
    }

    public static void init() {}

    private MarkRegistry() {}
}
