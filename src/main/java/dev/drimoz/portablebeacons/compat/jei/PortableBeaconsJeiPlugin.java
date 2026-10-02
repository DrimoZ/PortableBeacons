package dev.drimoz.portablebeacons.compat.jei;

import dev.drimoz.portablebeacons.core.AugmentInstance;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.core.FuelDef;
import dev.drimoz.portablebeacons.item.AugmentItem;
import dev.drimoz.portablebeacons.registry.BPItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import dev.drimoz.portablebeacons.client.PortableBeaconScreen;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import net.minecraft.client.renderer.Rect2i;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * JEI integration.
 *
 * <p>Nothing outside this package references it and JEI is compiled against but never required, so
 * this class is only ever loaded by JEI itself - which is what keeps JEI optional without a load
 * check of the kind the Curios integration needs.
 */
@JeiPlugin
public class PortableBeaconsJeiPlugin implements IModPlugin {

    private static final Identifier UID = BPRegistryKeys.id("jei");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    /**
     * Tells JEI where the side tabs are. They sit outside the window, which is exactly where JEI
     * lays its item list - and without this the list covered them, augment slots included.
     */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(PortableBeaconScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(PortableBeaconScreen screen) {
                return screen.extraAreas();
            }
        });
    }

    /**
     * Tells JEI that two augments are different items.
     *
     * <p>Without this, all fifteen augments collapse into one entry: they share a single registered
     * item and differ only by a data component, which JEI has no way to guess is meaningful. The
     * visible symptom is fourteen missing items and fifteen recipes that all produce an
     * indistinguishable "Augment".
     */
    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(BPItems.AUGMENT.get(),
                new ISubtypeInterpreter<ItemStack>() {
                    @Override
                    public Object getSubtypeData(ItemStack stack, UidContext context) {
                        // The component itself is the identity, so JEI can compare augments without
                        // going through a string at all.
                        return AugmentItem.instanceOf(stack);
                    }
                });
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new FuelCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(FuelCategory.TYPE, fuelEntries());
    }

    /**
     * Read from the datapack registry rather than hardcoded, so a beacon's own fuels appear here on
     * their own - the same reason the creative tab enumerates augments instead of listing them.
     */
    private static List<FuelEntry> fuelEntries() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            // JEI builds its lists on world load, so this should not happen; returning empty beats
            // throwing and taking the whole plugin down with it.
            return List.of();
        }
        RegistryAccess access = minecraft.level.registryAccess();

        List<FuelEntry> entries = new ArrayList<>();
        for (FuelDef def : access.lookupOrThrow(BPRegistryKeys.FUEL)) {
            // One unit is one second of one basic effect kept to yourself - the scale the whole
            // GUI is written in, so the two agree by construction.
            List<ItemStack> items = def.item()
                    .map(holder -> List.of(new ItemStack(holder.value())))
                    .orElseGet(() -> itemsIn(def));
            if (!items.isEmpty()) {
                entries.add(new FuelEntry(items, def.units(), def.units()));
            }
        }
        entries.sort(Comparator.comparingInt(FuelEntry::units));
        return entries;
    }

    /**
     * Every item currently in a tag-keyed entry's tag.
     *
     * <p>One row per tag, not per item: the slot cycles through them, which is how JEI shows a set
     * that means the same thing. An empty tag - the mod it belongs to is not installed - is dropped
     * rather than listed as a blank row.
     */
    private static List<ItemStack> itemsIn(FuelDef def) {
        return def.tag()
                .map(tag -> java.util.stream.StreamSupport.stream(
                                net.minecraft.core.registries.BuiltInRegistries.ITEM
                                        .getTagOrEmpty(tag).spliterator(), false)
                        .map(holder -> new ItemStack(holder.value()))
                        .toList())
                .orElse(List.of());
    }
}
