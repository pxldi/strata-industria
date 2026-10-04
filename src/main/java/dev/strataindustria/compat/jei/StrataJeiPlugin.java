package dev.strataindustria.compat.jei;

import dev.strataindustria.StrataIndustria;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.Identifier;

/** Recipe pages for JEI. Loaded by JEI only; nothing else in the mod refers to this package. */
@JeiPlugin
public final class StrataJeiPlugin implements IModPlugin {
    private static final Identifier UID = StrataIndustria.id("jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }
}
