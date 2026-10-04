package dev.strataindustria.automation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What a filter holds (tier 4 spec 13.5): nine entries, each an item or one of its tags, and whether the
 * list lets through (whitelist) or holds back (blacklist), and whether ore grade counts.
 *
 * @param items the item in each entry, air where empty
 * @param tags the tag chosen for each entry, empty for the item itself
 */
public record FilterContents(List<Item> items, List<String> tags, boolean whitelist, boolean matchGrade) {
    public static final int SIZE = 9;

    public static final FilterContents EMPTY = new FilterContents(Collections.nCopies(SIZE, Items.AIR), Collections.nCopies(SIZE, ""), true, false);

    public static final Codec<FilterContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().listOf().fieldOf("items").forGetter(FilterContents::items),
            Codec.STRING.listOf().fieldOf("tags").forGetter(FilterContents::tags),
            Codec.BOOL.optionalFieldOf("whitelist", true).forGetter(FilterContents::whitelist),
            Codec.BOOL.optionalFieldOf("match_grade", false).forGetter(FilterContents::matchGrade)
    ).apply(i, FilterContents::of));

    public static final StreamCodec<RegistryFriendlyByteBuf, FilterContents> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list()), FilterContents::items,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), FilterContents::tags,
            ByteBufCodecs.BOOL, FilterContents::whitelist,
            ByteBufCodecs.BOOL, FilterContents::matchGrade,
            FilterContents::of);

    /** Pads or trims to nine entries, so saved data from any source reads safely. */
    public static FilterContents of(List<Item> items, List<String> tags, boolean whitelist, boolean matchGrade) {
        List<Item> i = new ArrayList<>(SIZE);
        List<String> t = new ArrayList<>(SIZE);
        for (int n = 0; n < SIZE; n++) {
            i.add(n < items.size() ? items.get(n) : Items.AIR);
            t.add(n < tags.size() && i.get(n) != Items.AIR ? tags.get(n) : "");
        }
        return new FilterContents(List.copyOf(i), List.copyOf(t), whitelist, matchGrade);
    }

    public static FilterContents of(ItemStack filter) {
        FilterContents contents = filter.get(dev.strataindustria.registry.Tier4DataComponents.FILTER_CONTENTS.get());
        return contents == null ? EMPTY : contents;
    }

    public boolean isEmpty() {
        for (Item item : items) if (item != Items.AIR) return false;
        return true;
    }

    public Item item(int slot) {
        return items.get(slot);
    }

    public String tag(int slot) {
        return tags.get(slot);
    }

    /** Whether {@code stack} gets through. An empty whitelist lets nothing through; an empty blacklist everything. */
    public boolean test(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (int n = 0; n < SIZE; n++) {
            if (items.get(n) != Items.AIR && entryMatches(n, stack)) return whitelist;
        }
        return !whitelist;
    }

    private boolean entryMatches(int slot, ItemStack stack) {
        String tag = tags.get(slot);
        if (!tag.isEmpty()) {
            Identifier id = Identifier.tryParse(tag);
            return id != null && stack.is(TagKey.create(Registries.ITEM, id));
        }
        Item item = items.get(slot);
        if (stack.is(item)) return true;
        return matchGrade && gradeless(stack.getItem()).equals(gradeless(item));
    }

    /** An item's id with its ore grade taken out, so poor, normal and rich pieces compare equal. */
    static String gradeless(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id.getNamespace() + ":" + id.getPath().replace("poor_", "").replace("rich_", "");
    }

    public FilterContents withEntry(int slot, Item item) {
        List<Item> i = new ArrayList<>(items);
        List<String> t = new ArrayList<>(tags);
        i.set(slot, item);
        t.set(slot, "");
        return new FilterContents(List.copyOf(i), List.copyOf(t), whitelist, matchGrade);
    }

    /** The entry's next tag, then back to the item itself. */
    public FilterContents cycleTag(int slot) {
        Item item = items.get(slot);
        if (item == Items.AIR) return this;
        List<String> all = new ArrayList<>();
        item.builtInRegistryHolder().tags().forEach(key -> all.add(key.location().toString()));
        Collections.sort(all);
        if (all.isEmpty()) return this;
        String current = tags.get(slot);
        int at = all.indexOf(current);
        String next = at + 1 < all.size() ? all.get(at + 1) : "";
        List<String> t = new ArrayList<>(tags);
        t.set(slot, next);
        return new FilterContents(items, List.copyOf(t), whitelist, matchGrade);
    }

    public FilterContents toggleWhitelist() {
        return new FilterContents(items, tags, !whitelist, matchGrade);
    }

    public FilterContents toggleGrade() {
        return new FilterContents(items, tags, whitelist, !matchGrade);
    }
}
