package dev.strataindustria.client.foot;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** What the renderer needs of a handcart: its heading, whether the shafts are up, and how far the wheels have turned. */
public class HandcartRenderState extends EntityRenderState {
    public float yRot;
    public boolean pulled;
    public float wheelAngle;
}
