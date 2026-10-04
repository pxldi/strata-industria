package dev.strataindustria.transport.outpost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

/**
 * One posted charter. {@code brokenAt} is the game time the post was taken down, or -1 while it stands; a
 * taken-down charter keeps its links and name for a while so the post can be moved a few blocks.
 */
public record Charter(UUID id, BlockPos pos, UUID owner, String ownerName, String name, long brokenAt) {
    public static final Codec<Charter> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Charter::id),
            BlockPos.CODEC.fieldOf("pos").forGetter(Charter::pos),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(Charter::owner),
            Codec.STRING.fieldOf("owner_name").forGetter(Charter::ownerName),
            Codec.STRING.fieldOf("name").forGetter(Charter::name),
            Codec.LONG.optionalFieldOf("broken_at", -1L).forGetter(Charter::brokenAt)
    ).apply(i, Charter::new));

    public boolean active() {
        return brokenAt < 0;
    }

    public Charter withName(String newName) {
        return new Charter(id, pos, owner, ownerName, newName, brokenAt);
    }

    public Charter brokenAt(long time) {
        return new Charter(id, pos, owner, ownerName, name, time);
    }

    public Charter movedTo(BlockPos newPos, String newName) {
        return new Charter(id, newPos, owner, ownerName, newName, -1);
    }
}
