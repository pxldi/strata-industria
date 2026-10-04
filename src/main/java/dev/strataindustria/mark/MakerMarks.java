package dev.strataindustria.mark;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.metal.CastMoldItem;
import dev.strataindustria.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jspecify.annotations.Nullable;

/** Each player's own mark, kept on the player and copied on death, and how it gets onto the things they make. */
public final class MakerMarks {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, StrataIndustria.MOD_ID);

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<MakersMark>> TYPE = ATTACHMENTS.register("makers_mark",
            () -> AttachmentType.builder(() -> MakersMark.BLANK)
                    .serialize(MakersMark.CODEC.fieldOf("bits"))
                    .copyOnDeath()
                    .build());

    private MakerMarks() {}

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }

    public static MakersMark of(Player player) {
        return player.getData(TYPE);
    }

    public static void set(Player player, MakersMark mark) {
        player.setData(TYPE, mark);
    }

    /** Puts the player's mark and name on a part. Does nothing until they have cut a mark. */
    public static boolean stamp(ItemStack stack, Player player) {
        MakersMark mark = of(player);
        if (mark.isBlank() || stack.isEmpty()) return false;
        stack.set(MarkRegistry.STAMP.get(), new MakerStamp(mark, player.getName().getString()));
        return true;
    }

    /** Stamps a casting knocked out of a mold: heads and gears carry the mark, plain ingots do not. */
    public static void stampCasting(ItemStack mold, ItemStack cast, Player player) {
        if (!(mold.getItem() instanceof CastMoldItem item) || (item.type() == null && !item.isGear())) return;
        if (cast.has(ModDataComponents.SLAG.get())) return;
        stamp(cast, player);
    }

    /** A finished smithing piece takes the player's mark; a player with no mark yet is asked to cut one, and the piece is stamped once they do. */
    public static void stampOrAsk(ItemStack piece, ServerPlayer player, BlockPos anvil) {
        if (stamp(piece, player)) return;
        player.level().getServer().execute(() -> {
            if (of(player).isBlank() && player.isAlive()) openEditor(player, anvil);
        });
    }

    public static void openEditor(ServerPlayer player, @Nullable BlockPos anvil) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new MarkMenu(id, of(p), anvil),
                Component.translatable("container." + StrataIndustria.MOD_ID + ".makers_mark")));
    }

    /** The tally a stamped tool keeps of the blocks it has broken. */
    public static void countBlock(ItemStack tool) {
        if (!tool.has(MarkRegistry.STAMP.get())) return;
        tool.set(MarkRegistry.BLOCKS_MINED.get(), tool.getOrDefault(MarkRegistry.BLOCKS_MINED.get(), 0) + 1);
    }
}
