package dev.strataindustria.client.rail;

import net.minecraft.client.renderer.entity.state.MinecartRenderState;

/** A minecart's render state plus how far the tub has tipped and whether it is coupled. */
public class MineTubRenderState extends MinecartRenderState {
    public float tip;
    public boolean coupled;
    /** A tank wagon's fluid level, 0 to 16. */
    public int gauge;
    /** For vehicles with a front: the way they head, in degrees, and whether that counts. */
    public float headingYaw;
    public boolean directional;
    /** How far the coupling rods have turned, in radians. */
    public float wheelPhase;
    /** The tram's trolley pole lean and bell swing, in radians. */
    public float poleAngle, bellSwing;
}
