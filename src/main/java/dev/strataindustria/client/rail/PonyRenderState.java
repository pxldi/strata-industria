package dev.strataindustria.client.rail;

import net.minecraft.client.renderer.entity.state.DonkeyRenderState;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;

/** A minecart's render state plus what the horse inside the harness looks like and how it walks. */
public class PonyRenderState extends MinecartRenderState {
    public int kind;
    public int look;
    public float heading;
    /** The state the horse model reads; the pony fills it from its own. */
    public final DonkeyRenderState equine = new DonkeyRenderState();
}
