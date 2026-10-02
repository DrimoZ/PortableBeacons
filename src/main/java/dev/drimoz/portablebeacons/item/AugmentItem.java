package dev.drimoz.portablebeacons.item;

import dev.drimoz.portablebeacons.core.AugmentDef;
import dev.drimoz.portablebeacons.core.AugmentInstance;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.registry.BPComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * The single item behind every augment. Its identity, colour and stats all come from the
 * {@code portablebeacons:augment} component pointing into the datapack registry.
 */
public class AugmentItem extends Item {

    public AugmentItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static AugmentInstance instanceOf(ItemStack stack) {
        return stack.get(BPComponents.AUGMENT.get());
    }

    /**
     * The same read, without building an {@link ItemStack} first.
     *
     * <p>Slots are read as resources now, and this runs once per augment slot per tick - turning
     * each one into a stack just to look at one component would allocate for nothing.
     */
    @Nullable
    public static AugmentInstance instanceOf(ItemResource resource) {
        return resource.get(BPComponents.AUGMENT.get());
    }

    /**
     * Names come from the registry key, so a datapack-added augment only needs a matching
     * translation key — no code, no model, no item registration.
     */
    static void appendTooltip(ItemStack stack, TooltipContext context,
                              Consumer<Component> tooltip) {
        AugmentInstance instance = instanceOf(stack);
        if (instance == null || context.registries() == null) {
            return;
        }
        if (!TooltipDetail.expanded()) {
            tooltip.accept(TooltipDetail.HINT);
            return;
        }
        context.registries().lookup(BPRegistryKeys.AUGMENT)
                .flatMap(lookup -> lookup.get(instance.type()))
                .ifPresent(holder -> {
                    for (AugmentDef.Operation op : holder.value().operations()) {
                        tooltip.accept(describe(op, instance.tier(), context.registries()));
                    }
                    tooltip.accept(Component.translatable("portablebeacons.tip.augment_rule")
                            .withStyle(ChatFormatting.DARK_GRAY));
                });
    }

    /** Reads the effect straight off the registry entry, so a datapack augment describes itself. */
    private static Component describe(AugmentDef.Operation op, int tier, HolderLookup.Provider registries) {
        double value = op.valueFor(tier);
        String formatted = value == Math.rint(value)
                ? String.valueOf((int) value)
                : String.format(java.util.Locale.ROOT, "%.2f", value);
        String key = "portablebeacons.op." + op.type().getSerializedName();
        if (op.type() == AugmentDef.Type.UNLOCK_AURA && value < 0) {
            // Recluse lowers the sharing rank, and read as "unlocks wider sharing modes" it claimed
            // the opposite of what it does.
            key = "portablebeacons.op.restrict_aura";
        }
        if (op.effect().isPresent()) {
            // A targeted operation names its effect first, by the game's own name for it.
            Component effect = registries.lookup(BPRegistryKeys.EFFECT)
                    .flatMap(lookup -> lookup.get(op.effect().get()))
                    .<Component>map(holder -> holder.value().effect().value().getDisplayName())
                    .orElse(Component.literal(op.effect().get().identifier().toString()));
            return Component.translatable(key, effect, formatted).withStyle(ChatFormatting.GRAY);
        }
        return Component.translatable(key, formatted).withStyle(ChatFormatting.GRAY);
    }

    @Override
    public Component getName(ItemStack stack) {
        AugmentInstance instance = instanceOf(stack);
        if (instance == null) {
            return super.getName(stack);
        }
        return Component.translatable(
                "augment." + instance.type().identifier().getNamespace()
                        + "." + instance.type().identifier().getPath(),
                Component.translatable("portablebeacons.tier." + instance.tier()));
    }
}
