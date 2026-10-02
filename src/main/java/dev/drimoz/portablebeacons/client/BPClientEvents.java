package dev.drimoz.portablebeacons.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.client.model.AugmentLook;
import dev.drimoz.portablebeacons.net.OpenBeaconPayload;
import dev.drimoz.portablebeacons.registry.BPItems;
import dev.drimoz.portablebeacons.registry.BPMenus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class BPClientEvents {

    /**
     * Opens the active beacon without having to hold it.
     *
     * <p>Its own category rather than the vanilla "Inventory" one: filed there it sat among twenty
     * vanilla binds and was effectively impossible to find, which reads as the bind not existing.
     */
    public static final KeyMapping OPEN_PACK = new KeyMapping(
            "key.portablebeacons.open_beacon",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.portablebeacons");

    @EventBusSubscriber(modid = PortableBeacons.MOD_ID,
            value = Dist.CLIENT)
    public static final class ModBus {

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(BPMenus.BEACON.get(), PortableBeaconScreen::new);
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_PACK);
        }

        /** Tints the augment's screen layer from its registry entry - see {@link AugmentLook}. */
        @SubscribeEvent
        public static void registerColours(RegisterColorHandlersEvent.Item event) {
            event.register(AugmentLook::tint, BPItems.AUGMENT.get());
        }

        /** The glyph-and-tier value the augment's model overrides select on. */
        @SubscribeEvent
        public static void registerModelProperties(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ItemProperties.register(BPItems.AUGMENT.get(), AugmentLook.PROPERTY,
                    (stack, level, entity, seed) -> AugmentLook.look(stack)));
        }

        private ModBus() {}
    }

    @EventBusSubscriber(modid = PortableBeacons.MOD_ID,
            value = Dist.CLIENT)
    public static final class GameBus {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            while (OPEN_PACK.consumeClick()) {
                // No slot index: the server finds the beacon itself, so a crafted packet cannot
                // point the menu at an arbitrary stack.
                PacketDistributor.sendToServer(new OpenBeaconPayload(OpenBeaconPayload.FIND_ANY));
            }
        }

        private GameBus() {}
    }

    private BPClientEvents() {}
}
