package dev.drimoz.portablebeacons.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

/**
 * The contents of one effect slot in the GUI.
 *
 * <p>{@code enabled} is per effect and separate from the beacon's master switch: keeping Strength
 * configured but switched off, then turning it on before a fight, is the intended use — it avoids
 * both paying fuel for nothing and reconfiguring every time.
 *
 * @param effect    entry of the {@code portablebeacons:effect} registry
 * @param amplifier 0 = level I
 */
public record EffectSlotConfig(
        ResourceKey<BeaconEffectDef> effect,
        int amplifier,
        boolean enabled,
        AuraMode aura
) {
    public static final Codec<EffectSlotConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(BPRegistryKeys.EFFECT).fieldOf("effect")
                    .forGetter(EffectSlotConfig::effect),
            Codec.intRange(0, BeaconStats.MAX_AMPLIFIER).optionalFieldOf("amplifier", 0)
                    .forGetter(EffectSlotConfig::amplifier),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(EffectSlotConfig::enabled),
            AuraMode.CODEC.optionalFieldOf("aura", AuraMode.SELF).forGetter(EffectSlotConfig::aura)
    ).apply(i, EffectSlotConfig::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EffectSlotConfig> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceKey.streamCodec(BPRegistryKeys.EFFECT), EffectSlotConfig::effect,
                    ByteBufCodecs.VAR_INT, EffectSlotConfig::amplifier,
                    ByteBufCodecs.BOOL, EffectSlotConfig::enabled,
                    ByteBufCodecs.idMapper(id -> AuraMode.values()[id], Enum::ordinal),
                    EffectSlotConfig::aura,
                    EffectSlotConfig::new);

    public EffectSlotConfig withAmplifier(int newAmplifier) {
        return new EffectSlotConfig(effect, newAmplifier, enabled, aura);
    }

    public EffectSlotConfig withEnabled(boolean newEnabled) {
        return new EffectSlotConfig(effect, amplifier, newEnabled, aura);
    }

    public EffectSlotConfig withAura(AuraMode newAura) {
        return new EffectSlotConfig(effect, amplifier, enabled, newAura);
    }
}
