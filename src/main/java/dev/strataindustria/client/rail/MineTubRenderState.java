package dev.strataindustria.client.rail;

import net.minecraft.client.renderer.entity.state.MinecartRenderState;

/** A minecart's render state plus how far the tub has tipped and whether it is coupled. */
public class MineTubRenderState extends MinecartRenderState {
    public float tip;
    public boolean coupled;
    /** A tank wagon's fluid level, 0 to 16. */
    public int gauge;
}
