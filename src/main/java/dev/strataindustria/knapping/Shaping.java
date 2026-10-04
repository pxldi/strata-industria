package dev.strataindustria.knapping;

import dev.strataindustria.Config;
import dev.strataindustria.StrataIndustria;
import dev.strataindustria.cord.CordSounds;
import dev.strataindustria.journal.Journal;
import dev.strataindustria.registry.ModItems;
import dev.strataindustria.registry.ModRecipes;
import dev.strataindustria.registry.ModSounds;
import dev.strataindustria.smithing.Smithing;
import dev.strataindustria.smithing.StrikeNudge;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Shaping by hand in the world (redesign L5): knapping, clay forming and carving. Hold the material, sneak and
 * use (or scroll) to pick the shape, use to strike. It is the anvil's verb with the same feel layer: a held
 * button keeps striking, a click on the rebound glint is a true blow that counts twice, and every blow is the
 * next note of a climbing scale that the last blow resolves. Nothing can be ruined; the material is spent on
 * the last blow, so a piece put down half-done costs nothing.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID)
public final class Shaping {
    private Shaping() {}

    /** The scroll wheel while sneaking with the material: next (+1) or previous (-1) shape. */
    public record Cycle(int delta) implements CustomPacketPayload {
        public static final Type<Cycle> TYPE = new Type<>(StrataIndustria.id("cycle_shape"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Cycle> CODEC = ByteBufCodecs.VAR_INT.map(Cycle::new, Cycle::delta).cast();

        @Override
        public Type<Cycle> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(Cycle.TYPE, Cycle.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isSecondaryUseActive()) return;
            InteractionHand hand = Knapping.isKnappable(player.getMainHandItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            cycle(player, hand, payload.delta() > 0 ? 1 : -1);
        }));
    }

    /** A click with the material: strikes, or picks the next shape while sneaking. */
    public static boolean use(ServerPlayer player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!Knapping.isKnappable(held)) return false;
        if (player.isSecondaryUseActive()) cycle(player, hand, 1);
        else strike(player, hand, player.level().getGameTime());
        return true;
    }

    /** The shapes the held material can become, quick ones first, in a fixed order. */
    public static List<RecipeHolder<KnappingRecipe>> shapes(ServerLevel level, ItemStack held) {
        return level.recipeAccess().recipeMap()
                .getRecipesFor(ModRecipes.KNAPPING.get(), new SingleRecipeInput(held.copyWithCount(1)), level)
                .sorted(Comparator.<RecipeHolder<KnappingRecipe>>comparingInt(h -> h.value().blows()).thenComparing(h -> h.id().identifier().toString()))
                .toList();
    }

    /** What the player has set the held material to: the shape picked, else the last one used, else the first. */
    public static Optional<RecipeHolder<KnappingRecipe>> current(ServerPlayer player, ItemStack held) {
        List<RecipeHolder<KnappingRecipe>> shapes = shapes((ServerLevel) player.level(), held);
        if (shapes.isEmpty()) return Optional.empty();
        HandShaping state = HandShaping.of(player);
        String kind = Knapping.kind(held);
        Identifier wanted = state.material.equals(kind) && state.shape != null ? state.shape : state.lastShape(kind);
        if (wanted != null) for (RecipeHolder<KnappingRecipe> shape : shapes) if (shape.id().identifier().equals(wanted)) return Optional.of(shape);
        return Optional.of(shapes.get(0));
    }

    /** Blows on the piece the player is shaping now. */
    public static int blows(ServerPlayer player) {
        return HandShaping.of(player).blows;
    }

    /** Material items the shape costs: the recipe's own count, at least the material's base cost. */
    public static int cost(KnappingRecipe recipe, ItemStack held) {
        return Math.max(recipe.consume(), Knapping.openingCost(held));
    }

    /** Sets the held material to {@code shape}, dropping any blows on another shape. */
    private static void select(HandShaping state, String kind, RecipeHolder<KnappingRecipe> shape) {
        Identifier id = shape.id().identifier();
        if (!state.material.equals(kind) || !id.equals(state.shape)) state.reset();
        state.material = kind;
        state.shape = id;
        state.remember(kind, id);
    }

    /** Sneak + use, or the scroll wheel: the next (or previous) shape, and a line saying what it is. */
    public static void cycle(ServerPlayer player, InteractionHand hand, int delta) {
        ItemStack held = player.getItemInHand(hand);
        if (!Knapping.isKnappable(held) || !(player.level() instanceof ServerLevel level)) return;
        List<RecipeHolder<KnappingRecipe>> shapes = shapes(level, held);
        if (shapes.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".shaping.nothing", held.getHoverName()));
            return;
        }
        int now = 0;
        Optional<RecipeHolder<KnappingRecipe>> current = current(player, held);
        for (int i = 0; i < shapes.size(); i++) if (current.isPresent() && shapes.get(i).id().equals(current.get().id())) now = i;
        RecipeHolder<KnappingRecipe> next = shapes.get(Math.floorMod(now + delta, shapes.size()));
        HandShaping state = HandShaping.of(player);
        select(state, Knapping.kind(held), next);
        state.spot = spot(player);
        ShapingView.show(level, state, held, next.value().assemble(new SingleRecipeInput(held.copyWithCount(1))), 0, next.value().blows(), 0.0f, level.getGameTime());
        level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.3f, 1.4f);
        player.sendOverlayMessage(shapeLine(next, held));
    }

    /** "Stone axe head · 4 blows". */
    public static Component shapeLine(RecipeHolder<KnappingRecipe> shape, ItemStack held) {
        KnappingRecipe recipe = shape.value();
        String key = StrataIndustria.MOD_ID + (recipe.bound() ? ".shaping.shape_bound" : ".shaping.shape");
        return Component.translatable(key, recipe.result().create().getHoverName(), recipe.blows(), cost(recipe, held), held.getHoverName());
    }

    /**
     * One click. Clicks that are too close are ignored; one on the glint is a true blow that counts twice; a
     * held button repeats at a steady pace and never lands on the glint. {@code now} is explicit so tests can space their blows.
     */
    public static void strike(ServerPlayer player, InteractionHand hand, long now) {
        if (!(player.level() instanceof ServerLevel level)) return;
        ItemStack held = player.getItemInHand(hand);
        Optional<RecipeHolder<KnappingRecipe>> found = current(player, held);
        if (found.isEmpty()) {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".shaping.nothing", held.getHoverName()));
            return;
        }
        HandShaping state = HandShaping.of(player);
        String kind = Knapping.kind(held);
        RecipeHolder<KnappingRecipe> shape = found.get();
        select(state, kind, shape);
        KnappingRecipe recipe = shape.value();

        boolean heldDown = state.lastClick != Long.MIN_VALUE && now - state.lastClick <= Smithing.HOLD_REPEAT_TICKS;
        state.lastClick = now;
        long since = state.lastBlow == Long.MIN_VALUE ? Long.MAX_VALUE : now - state.lastBlow;
        if (since < (heldDown ? Smithing.HOLD_GAP_TICKS : Smithing.MIN_GAP_TICKS)) return;

        int cost = cost(recipe, held);
        if (!player.hasInfiniteMaterials() && held.getCount() < cost) {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".knapping.need_more", cost, held.getHoverName()));
            return;
        }
        if (recipe.bound() && !Knapping.hasBinding(player)) {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".shaping.need_binding", recipe.result().create().getHoverName()));
            return;
        }
        if (!state.hinted && state.blows == 0) {
            state.hinted = true;
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".shaping.hint"));
        }

        boolean inRhythm = since <= Smithing.RHYTHM_TIMEOUT_TICKS;
        boolean trueBlow = !heldDown && inRhythm && Math.abs(since - Smithing.BEAT_TICKS) <= Config.SMITHING_BEAT_WINDOW.get();
        state.groove = trueBlow ? state.groove + 1 : 0;
        int total = recipe.blows();
        int before = state.blows;
        state.blows = Math.min(total, before + (trueBlow ? 2 : 1));
        boolean done = state.blows >= total;
        state.spot = spot(player);
        state.lastBlow = now;
        state.glintPending = !done;

        ItemStack result = recipe.assemble(new SingleRecipeInput(held.copyWithCount(1)));
        feedback(level, player, held, state, before, total, trueBlow, done, recipe.bound());
        if (done) ShapingView.clear(level, state);
        else ShapingView.show(level, state, held, result, state.blows, total, 1.0f, now);
        if (done) {
            if (!player.hasInfiniteMaterials()) held.shrink(cost);
            if (recipe.bound()) Knapping.spendBinding(player);
            pop(level, state.spot, result);
            if (!kind.equals(Knapping.KIND_WOOD)) Journal.award(player, Knapping.isClayKind(kind) ? Journal.CLAY_FORMING : Journal.KNAP);
            state.reset();
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".shaping.done", result.getHoverName()));
        } else {
            player.sendOverlayMessage(Component.translatable(StrataIndustria.MOD_ID + ".shaping.progress", result.getHoverName(), state.blows, total));
        }
    }

    /** Where the chips fly from: the face the player is looking at, else just in front of the hands. */
    private static Vec3 spot(ServerPlayer player) {
        HitResult hit = player.pick(4.5, 1.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) return hit.getLocation();
        return player.getEyePosition().add(player.getLookAngle().scale(0.9)).add(0.0, -0.3, 0.0);
    }

    private static void feedback(ServerLevel level, ServerPlayer player, ItemStack held, HandShaping state, int before, int total,
                                 boolean trueBlow, boolean done, boolean bound) {
        Vec3 at = state.spot;
        var random = player.getRandom();
        float vary = 0.97f + random.nextFloat() * 0.06f;
        float note = Smithing.notePitch(before, total, done, true);
        float base = Knapping.grainOf(held).map(Grain::strikePitch).orElse(1.0f) * (Knapping.isFlint(held) ? 0.8f : 1.0f);
        float pitch = Mth.clamp(base * note * vary, 0.5f, 2.0f);
        level.playSound(null, at.x, at.y, at.z, Knapping.strikeSound(held), SoundSource.PLAYERS, 0.8f, pitch);
        if (Knapping.isFlint(held)) {
            level.playSound(null, at.x, at.y, at.z, ModSounds.SHAPING_CHIME.get(), SoundSource.PLAYERS, 0.45f, Mth.clamp(note * 1.1f, 0.5f, 2.0f));
        }
        if (trueBlow) level.playSound(null, at.x, at.y, at.z, ModSounds.SHAPING_TRUE_BLOW.get(), SoundSource.PLAYERS, 0.8f, 0.95f + random.nextFloat() * 0.1f);
        if (state.groove >= 3) level.playSound(null, at.x, at.y, at.z, ModSounds.SHAPING_CHIME.get(), SoundSource.PLAYERS, 0.3f, Mth.clamp(note * 1.5f, 0.5f, 2.0f));

        int factor = Knapping.grainOf(held).map(Grain::chipFactor).orElse(1);
        int chips = (trueBlow ? 14 : 6) * factor + (done ? 10 : 0);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, held.getItem()), at.x, at.y + 0.05, at.z, chips, 0.12, 0.08, 0.12, 0.1);
        if (Knapping.grainOf(held).isPresent()) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.05, at.z, trueBlow || done ? 6 : 2, 0.1, 0.05, 0.1, 0.2);
        }
        if (done) {
            level.playSound(null, at.x, at.y, at.z, Knapping.finishSound(held), SoundSource.PLAYERS, 0.9f, 1.0f);
            level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.1, at.z, 4, 0.1, 0.05, 0.1, 0.01);
            level.sendParticles(ParticleTypes.WAX_ON, at.x, at.y + 0.2, at.z, 8, 0.2, 0.15, 0.2, 0.0);
            if (bound) {
                // The head is seated, wrapped and knotted: loose cord ends spring free over a glint.
                level.playSound(null, at.x, at.y, at.z, CordSounds.BIND.get(), SoundSource.PLAYERS, 1.0f, 0.95f + random.nextFloat() * 0.1f);
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ModItems.CORD.get()), at.x, at.y + 0.2, at.z, 10, 0.12, 0.1, 0.12, 0.08);
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y + 0.25, at.z, 8, 0.15, 0.1, 0.15, 0.25);
            }
        }
        if (!(player instanceof FakePlayer) && Config.SMITHING_SCREEN_NUDGE.get() && (trueBlow || done)) {
            StrikeNudge.send(player, done ? 1.0f : 0.6f);
        }
    }

    /** The finished piece hops up off the surface. */
    private static void pop(ServerLevel level, Vec3 at, ItemStack result) {
        ItemEntity entity = new ItemEntity(level, at.x, at.y + 0.15, at.z, result);
        entity.setDeltaMovement(0.0, 0.25, 0.0);
        entity.setPickUpDelay(12);
        level.addFreshEntity(entity);
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        HandShaping state = HandShaping.existing(player);
        if (state != null) ShapingView.clear(level, state);
    }

    /** The glint at the top of the rebound, a soft tick to strike on. */
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        HandShaping state = HandShaping.existing(player);
        if (state == null) return;
        long now = player.level().getGameTime();
        if (state.workpiece != null || state.ghost != null) {
            if (!(player.level() instanceof ServerLevel view)) return;
            boolean away = !Knapping.isKnappable(player.getMainHandItem()) && !Knapping.isKnappable(player.getOffhandItem());
            if (away || now - state.lastActive > ShapingView.LINGER_TICKS) ShapingView.clear(view, state);
            else if (state.relaxAt != Long.MIN_VALUE && now >= state.relaxAt) ShapingView.relax(view, state, now);
        }
        if (!state.glintPending) return;
        if (now - state.lastBlow < Smithing.BEAT_TICKS) return;
        state.glintPending = false;
        if (!(player.level() instanceof ServerLevel level)) return;
        level.playSound(null, state.spot.x, state.spot.y, state.spot.z, ModSounds.SHAPING_GLINT.get(), SoundSource.PLAYERS, 0.6f, 1.0f);
        level.sendParticles(ParticleTypes.END_ROD, state.spot.x, state.spot.y + 0.3, state.spot.z, 1, 0.0, 0.0, 0.0, 0.0);
    }
}
