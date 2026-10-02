package dev.drimoz.portablebeacons.gametest;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.core.AugmentInstance;
import dev.drimoz.portablebeacons.core.AuraMode;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.core.BeaconEffectDef;
import dev.drimoz.portablebeacons.core.BeaconState;
import dev.drimoz.portablebeacons.core.EffectSlotConfig;
import dev.drimoz.portablebeacons.item.PortableBeaconItem;
import dev.drimoz.portablebeacons.menu.BeaconMenuOpener;
import dev.drimoz.portablebeacons.registry.BPComponents;
import dev.drimoz.portablebeacons.registry.BPItems;
import dev.drimoz.portablebeacons.registry.BPLookups;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.PackResources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Captures the beacon screen in its states, so it can be looked at without playing.
 *
 * <p>The same tool FactoryIO has, for the same reason: a screen is not done until someone has
 * looked at it, and nothing else in this repo looks. Does nothing unless the environment says so:
 * <pre>
 *   BEACON_GUI_PREVIEW=1 ./gradlew runClient
 * </pre>
 * The client loads a <b>copy</b> of the {@code 26_1} dev world (never the original), hands the
 * player a configured Beacon IV, opens its screen, writes {@code run/screenshots/preview/*.png}
 * and quits.
 */
@EventBusSubscriber(modid = PortableBeacons.MOD_ID, value = Dist.CLIENT)
public final class GuiPreview {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = "1".equals(System.getenv("BEACON_GUI_PREVIEW"));
    private static final String SOURCE_WORLD = "26_1";
    private static final String WORLD = "GuiPreview";

    private record Step(int delay, Consumer<Minecraft> action) {}

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static boolean started;
    private static int wait;
    private static String pendingShot;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!started && minecraft.screen instanceof TitleScreen) {
            started = true;
            minecraft.options.pauseOnLostFocus = false;
            copyWorld(minecraft);
            script();
            minecraft.createWorldOpenFlows().openWorld(WORLD, () -> {});
            return;
        }
        if (!started || minecraft.player == null || minecraft.getSingleplayerServer() == null
                || pendingShot != null || STEPS.isEmpty() || wait-- > 0) {
            return;
        }
        STEPS.poll().action().accept(minecraft);
        wait = STEPS.isEmpty() ? 0 : STEPS.peek().delay();
    }

    /** Taken once the frame is fully drawn, not halfway through a tick. */
    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Post event) {
        if (!ENABLED || pendingShot == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        File file = new File(minecraft.gameDirectory, "screenshots/preview/" + pendingShot + ".png");
        file.getParentFile().mkdirs();
        pendingShot = null;
        Screenshot.takeScreenshot(minecraft.getMainRenderTarget(), image -> {
            try (NativeImage owned = image) {
                owned.writeToFile(file);
                LOGGER.info("GUI preview: {}", file);
            } catch (IOException e) {
                LOGGER.error("GUI preview failed: {}", file, e);
            }
        });
    }

    // ------------------------------------------------------------------ script

    /**
     * What a player would do, in the screen's own coordinates. Written out rather than read from
     * the screen: a screen that moved its controls should make this script visibly wrong, not
     * silently follow it.
     */
    private static void script() {
        step(60, mc -> onServer(mc, GuiPreview::equip));
        step(20, mc -> onServer(mc, player -> BeaconMenuOpener.open(player, 0)));
        step(30, mc -> mc.getToastManager().clear());
        step(5, mc -> hover(mc, 0.0, 0.0));
        step(5, mc -> shot("beacon_main"));

        // The info tab, left; its header is the 22-pixel square at the window's top-left corner.
        step(5, mc -> clickGui(mc, -11, 15));
        step(15, mc -> shot("beacon_info"));
        step(5, mc -> clickGui(mc, -11, 15));
        // Both tabs shut: the augment tab's header sits against the right edge.
        step(5, mc -> clickGui(mc, 176 + 11, 15));
        step(15, mc -> shot("beacon_tabs_shut"));
        step(5, mc -> clickGui(mc, 176 + 11, 15));

        // Tooltips: the fuel gauge, then the first row's audience icon.
        step(15, mc -> hoverGui(mc, 15, 40));
        step(10, mc -> shot("beacon_fuel_tooltip"));
        step(5, mc -> hoverGui(mc, 141, 28));
        step(10, mc -> shot("beacon_control_tooltip"));

        // The picker, from the first row - which holds an effect, so the remove cell shows - hovering its second cell.
        step(5, mc -> clickGui(mc, 80, 28));
        step(5, mc -> hoverGui(mc, 34, 45));
        step(10, mc -> shot("beacon_selector"));
        step(5, mc -> key(mc, GLFW.GLFW_KEY_ESCAPE));

        step(5, mc -> mc.setScreen(null));
        step(10, Minecraft::stop);
    }

    /** A Beacon IV with two effects, two augments and fuel in the slot - the busy case. */
    private static void equip(ServerPlayer player) {
        ItemStack beacon = new ItemStack(BPItems.BEACON_IV.get());
        BeaconState state = new BeaconState(List.of(
                new EffectSlotConfig(effect("strength"), 1, true, AuraMode.SELF),
                new EffectSlotConfig(effect("resistance"), 0, true, AuraMode.TEAM)),
                21000, true, 36000);
        ResourceHandler<ItemResource> slots = BPLookups.handlerOf(beacon);
        try (Transaction transaction = Transaction.openRoot()) {
            slots.insert(PortableBeaconItem.FUEL_SLOT, ItemResource.of(new ItemStack(Items.GOLD_INGOT)),
                    24, transaction);
            slots.insert(PortableBeaconItem.augmentSlot(0), ItemResource.of(augment("range", 2)), 1,
                    transaction);
            slots.insert(PortableBeaconItem.augmentSlot(1), ItemResource.of(augment("wayfarer", 1)), 1,
                    transaction);
            transaction.commit();
        }
        PortableBeaconItem.setState(beacon, state);
        player.getInventory().setItem(0, beacon);
        player.getInventory().setItem(1, new ItemStack(Items.IRON_INGOT, 32));
        player.getInventory().setItem(2, augment("capacity", 3));
        // From the preview datapack written by copyWorld: an augment no code knows about, which
        // must still show its glyph and colour.
        ItemStack sprinter = new ItemStack(BPItems.AUGMENT.get());
        sprinter.set(BPComponents.AUGMENT.get(), new AugmentInstance(ResourceKey.create(BPRegistryKeys.AUGMENT,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("previewpack", "sprinter")), 1));
        player.getInventory().setItem(3, sprinter);
        // Every augment and every beacon, so the item art is in the shot too. Tiers cycle 1-3 so the
        // casing pips can be checked against the tooltip.
        String[] augments = {"range", "focus", "amplification", "efficiency", "capacity", "attunement",
                "discretion", "communion", "wellspring", "wayfarer", "sentinel", "vanguard", "prism", "recluse"};
        for (int i = 0; i < augments.length; i++) {
            player.getInventory().setItem(9 + i, augment(augments[i], 1 + i % 3));
        }
        var beacons = BPItems.beacons();
        for (int i = 0; i < beacons.size(); i++) {
            player.getInventory().setItem(27 + i, new ItemStack(beacons.get(i).get()));
        }
    }

    private static ResourceKey<BeaconEffectDef> effect(String name) {
        return ResourceKey.create(BPRegistryKeys.EFFECT, BPRegistryKeys.id(name));
    }

    private static ItemStack augment(String name, int tier) {
        ItemStack stack = new ItemStack(BPItems.AUGMENT.get());
        stack.set(BPComponents.AUGMENT.get(), new AugmentInstance(
                ResourceKey.create(BPRegistryKeys.AUGMENT, BPRegistryKeys.id(name)), tier));
        return stack;
    }

    // ------------------------------------------------------------------ plumbing

    private static void step(int delay, Consumer<Minecraft> action) {
        STEPS.add(new Step(delay, action));
    }

    private static void shot(String name) {
        pendingShot = name;
    }

    private static void onServer(Minecraft minecraft, Consumer<ServerPlayer> action) {
        var server = minecraft.getSingleplayerServer();
        var id = minecraft.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                action.accept(player);
            }
        });
    }

    /** Clicks a point of the open screen, in its own coordinates - side tabs included. */
    private static void clickGui(Minecraft minecraft, int x, int y) {
        if (minecraft.screen instanceof AbstractContainerScreen<?> screen) {
            screen.mouseClicked(new MouseButtonEvent(screen.getLeftPos() + x, screen.getTopPos() + y,
                    new MouseButtonInfo(0, 0)), false);
        }
    }

    private static void key(Minecraft minecraft, int key) {
        if (minecraft.screen != null) {
            minecraft.screen.keyPressed(new KeyEvent(key, 0, 0));
        }
    }

    /** Puts the cursor on a point of the open screen, in its own coordinates. */
    private static void hoverGui(Minecraft minecraft, int x, int y) {
        if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        double scale = minecraft.getWindow().getGuiScale();
        hover(minecraft, (screen.getLeftPos() + x + 0.5) * scale / minecraft.getWindow().getScreenWidth(),
                (screen.getTopPos() + y + 0.5) * scale / minecraft.getWindow().getScreenHeight());
    }

    /**
     * Moves the real cursor, as a fraction of the window.
     *
     * <p>Through GLFW rather than by writing the mouse handler's position: the game re-reads the
     * cursor every frame, so a written position held for one frame - enough to leave a sticky
     * highlight, never enough for a tooltip, which is why the first previews showed none.
     */
    private static void hover(Minecraft minecraft, double fx, double fy) {
        GLFW.glfwSetCursorPos(minecraft.getWindow().handle(),
                minecraft.getWindow().getScreenWidth() * fx, minecraft.getWindow().getScreenHeight() * fy);
    }

    private static void copyWorld(Minecraft minecraft) {
        Path saves = minecraft.gameDirectory.toPath().resolve("saves");
        Path source = saves.resolve(SOURCE_WORLD);
        Path target = saves.resolve(WORLD);
        try {
            if (Files.exists(target)) {
                try (Stream<Path> walk = Files.walk(target)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
                }
            }
            try (Stream<Path> walk = Files.walk(source)) {
                for (Path path : (Iterable<Path>) walk::iterator) {
                    // The running world's lock, which the copy must not inherit.
                    if (path.getFileName().toString().endsWith(".lock")) {
                        continue;
                    }
                    Path copy = target.resolve(source.relativize(path).toString());
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(copy);
                    } else {
                        Files.copy(path, copy);
                    }
                }
            }
            writePreviewPack(target.resolve("datapacks").resolve("previewpack"));
        } catch (IOException e) {
            throw new IllegalStateException("GUI preview: could not copy the dev world", e);
        }
    }

    /**
     * A datapack augment, into the copy only: the README's own example, so the preview shows that
     * what the docs promise - a datapack augment drawing a shipped glyph - actually renders.
     */
    private static void writePreviewPack(Path pack) throws IOException {
        Path augment = pack.resolve("data/previewpack/portablebeacons/augment/sprinter.json");
        Files.createDirectories(augment.getParent());
        Files.writeString(pack.resolve(PackResources.PACK_META),
                "{ \"pack\": { \"description\": \"GUI preview\", \"min_format\": 101, \"max_format\": 101 } }");
        Files.writeString(augment, """
                { "max_tier": 1, "color": 5636095, "glyph": "bolt",
                  "operations": [
                    { "type": "add_effect_amplifier", "effect": "portablebeacons:speed", "values": [1] },
                    { "type": "mul_effect_cost", "effect": "portablebeacons:speed", "values": [0.6] } ] }
                """);
    }

    private GuiPreview() {}
}
