package dev.strataindustria.survey;

import dev.strataindustria.StrataIndustria;
import dev.strataindustria.client.SurveyClient;
import dev.strataindustria.structure.Ledgers;
import dev.strataindustria.structure.StructureContent;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A prospector's notes on a deposit they found (structures spec 5). Right-click to read them; carry them
 * to the deposit and they are marked found.
 */
public class SurveyNotesItem extends Item {
    static final int CHECK_INTERVAL = 20;

    public SurveyNotesItem(Properties properties) {
        super(properties);
    }

    public static SurveyNotes notes(ItemStack stack) {
        return stack.getOrDefault(StructureContent.SURVEY.get(), SurveyNotes.looking());
    }

    /** The ledger text key of a handwritten page, or null for prospector's notes. */
    public static @Nullable String ledger(ItemStack stack) {
        return stack.get(StructureContent.LEDGER.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        return ledger(stack) == null ? super.getName(stack)
                : Component.translatable("item." + StrataIndustria.MOD_ID + ".survey_notes.ledger");
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            SurveyClient.open(stack);
        } else {
            read(stack, (ServerLevel) level, player);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), StructureContent.SURVEY_NOTES_OPEN.get(),
                    SoundSource.PLAYERS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayer player) || player.tickCount % CHECK_INTERVAL != 0) return;
        SurveyNotes notes = read(stack, level, player);
        if (notes.found() || notes.entry().isEmpty()) return;
        if (!Surveyor.atDeposit(notes.entry().get(), player.blockPosition())) return;
        stack.set(StructureContent.SURVEY.get(), notes.markFound());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), StructureContent.SURVEY_NOTES_FOUND.get(),
                SoundSource.PLAYERS, 0.6f, 1.0f);
        player.sendOverlayMessage(Component.translatable("item." + StrataIndustria.MOD_ID + ".survey_notes.found_message"));
    }

    /** Works out where the notes point, the first time anyone holds them. */
    private static SurveyNotes read(ItemStack stack, ServerLevel level, Player player) {
        SurveyNotes notes = notes(stack);
        if (notes.entry().isPresent() || notes.targets().isEmpty()) return notes;
        SurveyNotes read = Surveyor.resolve(level, player.blockPosition(), notes, level.getRandom())
                .map(notes::resolve)
                // Nothing worth finding within reach: the ink has run.
                .orElseGet(() -> new SurveyNotes(List.of(), java.util.Optional.empty(), false));
        stack.set(StructureContent.SURVEY.get(), read);
        return read;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
            TooltipFlag flag) {
        SurveyNotes notes = notes(stack);
        String prefix = "item." + StrataIndustria.MOD_ID + ".survey_notes.";
        String ledger = ledger(stack);
        if (ledger != null) {
            tooltip.accept(Ledgers.place(ledger).copy().withStyle(ChatFormatting.GRAY));
        } else if (notes.entry().isPresent()) {
            SurveyNotes.Entry entry = notes.entry().get();
            tooltip.accept(SurveyText.mineralName(entry.mineral()).copy().withStyle(ChatFormatting.GRAY));
            if (notes.found()) tooltip.accept(Component.translatable(prefix + "found").withStyle(ChatFormatting.DARK_GREEN));
        } else if (notes.targets().isEmpty()) {
            tooltip.accept(Component.translatable(prefix + "blank").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable(prefix + "unread").withStyle(ChatFormatting.GRAY));
        }
    }
}
