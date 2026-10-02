package dev.drimoz.portablebeacons.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.net.OpenBeaconPayload;
import dev.drimoz.portablebeacons.registry.BPItems;
import dev.drimoz.portablebeacons.registry.BPMenus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import dev.drimoz.portablebeacons.client.model.AugmentLook;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class BPClientEvents {

    /**
     * Opens the active beacon without having to hold it.
     *
     * <p>Its own category rather than the vanilla "Inventory" one: filed there it sat among twenty
     * vanilla binds and was effectively impossible to find, which reads as the bind not existing.
     */
    /** Categories are objects now, not the free-form string the bind used to carry. */
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(BPRegistryKeys.id("main"));

    public static final KeyMapping OPEN_PACK = new KeyMapping(
            "key.portablebeacons.open_beacon",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_B),
            CATEGORY);

    @EventBusSubscriber(modid = PortableBeacons.MOD_ID,
            value = Dist.CLIENT)
    public static final class ModBus {

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(BPMenus.BEACON.get(), PortableBeaconScreen::new);
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.registerCategory(CATEGORY);
            event.register(OPEN_PACK);
        }

        /**
         * Tints the shared augment texture from its registry entry, which is what lets a
         * datapack-added augment look distinct without shipping a model or a texture.
         */
        @SubscribeEvent
        public static void registerTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
            event.register(BPRegistryKeys.id("augment_colour"), AugmentLook.Tint.CODEC);
        }

        /**
         * The augment glyph is chosen by the model, keyed on the augment's registry key.
         *
         * <p>See {@link AugmentLook} for why that replaced the model_data integer.
         */
        @SubscribeEvent
        public static void registerModelProperties(RegisterSelectItemModelPropertyEvent event) {
            event.register(BPRegistryKeys.id("augment_glyph"), AugmentLook.GlyphProperty.TYPE);
            event.register(BPRegistryKeys.id("augment_tier"), AugmentLook.TierProperty.TYPE);
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
                ClientPacketDistributor.sendToServer(new OpenBeaconPayload(OpenBeaconPayload.FIND_ANY));
            }
        }

        private GameBus() {}
    }

    private BPClientEvents() {}
}
