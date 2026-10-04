package dev.strataindustria.client.capture;

import com.mojang.logging.LogUtils;
import dev.strataindustria.StrataIndustria;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.slf4j.Logger;

/**
 * Dev-only scene recorder: start the client with {@code -Dstrata.capture=<scene file>} (see tools/capture/README.md).
 * It creates a flat creative world, runs the scene script and saves a screenshot per step into
 * {@code -Dstrata.capture.out} (default run/capture). Software rendering is slow, so animation is captured by
 * freezing the game and stepping it one tick per frame, never in real time. Does nothing without the property.
 *
 * Scene lines: {@code cmd <server command>}, {@code wait <client ticks>}, {@code shot <name>},
 * {@code frames <name> <count>} (step one game tick, save a frame, repeat), {@code quit}. '#' starts a comment.
 */
@EventBusSubscriber(modid = StrataIndustria.MOD_ID, value = Dist.CLIENT)
public final class CaptureClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SCENE = System.getProperty("strata.capture");
    private static final Path OUT = Path.of(System.getProperty("strata.capture.out", "capture"));
    private static ArrayDeque<String> script;
    private static boolean worldRequested;
    private static int waitTicks;
    private static int ready; // client ticks spent in the world
    private static String pendingShot; // saved on the next rendered frame
    private static int pendingFrames, frameIndex;
    private static String frameName;
    private static boolean stepRequested;
    private static int shotsInFlight;

    private CaptureClient() {}

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        if (SCENE == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (script == null) {
            try {
                script = new ArrayDeque<>(Files.readAllLines(Path.of(SCENE)).stream()
                        .map(String::strip).filter(l -> !l.isEmpty() && !l.startsWith("#")).toList());
            } catch (Exception e) {
                LOGGER.error("capture: cannot read scene {}", SCENE, e);
                mc.stop();
                return;
            }
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) {
            if (mc.gui.screen() instanceof TitleScreen && !worldRequested) {
                worldRequested = true;
                createWorld(mc);
            }
            return;
        }
        if (ready++ == 0 && !mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        if (ready < 60 || shotsInFlight > 0 || pendingShot != null || stepRequested) return; // let chunks load and frames land
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        if (pendingFrames > 0) {
            // advance exactly one game tick, then the next rendered frame is saved
            runCommand(mc, "tick step 1");
            stepRequested = true;
            pendingShot = String.format("%s_%03d.png", frameName, frameIndex++);
            pendingFrames--;
            return;
        }
        String line = script.poll();
        if (line == null) {
            mc.stop();
            return;
        }
        String[] parts = line.split("\\s+", 2);
        switch (parts[0]) {
            case "cmd" -> runCommand(mc, parts[1]);
            case "wait" -> waitTicks = Integer.parseInt(parts[1].strip());
            case "shot" -> pendingShot = parts[1].strip() + ".png";
            case "frames" -> {
                String[] f = parts[1].split("\\s+");
                if (!f[0].equals(frameName)) frameIndex = 0; // repeated "frames" lines with one name continue the series
                frameName = f[0];
                pendingFrames = Integer.parseInt(f[1]);
            }
            case "quit" -> mc.stop();
            default -> LOGGER.warn("capture: unknown scene line '{}'", line);
        }
    }

    @SubscribeEvent
    static void onFrame(RenderFrameEvent.Post event) {
        if (SCENE == null || pendingShot == null) return;
        if (stepRequested) { // give the stepped tick one frame to show up
            stepRequested = false;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        File dir = OUT.toAbsolutePath().toFile();
        dir.mkdirs();
        String name = pendingShot;
        pendingShot = null;
        shotsInFlight++;
        Screenshot.grab(dir, name, mc.gameRenderer.mainRenderTarget(), 1, msg -> shotsInFlight--);
    }

    private static void createWorld(Minecraft mc) {
        String name = "capture_" + System.currentTimeMillis();
        mc.createWorldOpenFlows().createFreshLevel(name,
                new LevelSettings(name, GameType.CREATIVE, new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, true),
                        true, WorldDataConfiguration.DEFAULT),
                WorldOptions.defaultWithRandomSeed(),
                registries -> registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                new TitleScreen());
    }

    private static void runCommand(Minecraft mc, String command) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withSuppressedOutput(), command));
    }

    /** Frame lists for tests and docs. */
    static List<String> commands() {
        return List.copyOf(script == null ? List.of() : script);
    }
}
