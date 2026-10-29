package dev.drimoz.portablebeacons.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.drimoz.portablebeacons.core.AugmentDef;
import dev.drimoz.portablebeacons.core.BeaconEffectDef;
import dev.drimoz.portablebeacons.core.FuelDef;
import dev.drimoz.portablebeacons.core.BeaconTierDef;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Parses every JSON the mod ships through the codec that will read it in game.
 *
 * <p>A typo in one of these files makes the entry silently vanish from its registry rather than
 * crash, which is close to impossible to diagnose from inside the game - the symptom is an empty
 * list in the GUI with nothing in the log.
 */
class BuiltinDataTest {

    /** Read from the source tree — see {@link ProjectFiles}. */
    private static final Path DATA = ProjectFiles.resources()
            .resolve(Path.of("data", "portablebeacons", "portablebeacons"));

    // No bootstrap here: ModDevGradle's junit run type already brings Minecraft up, and calling
    // SharedConstants.setVersion a second time throws.

    @Test
    void effectsParse() throws IOException {
        assertAllParse("effect", BeaconEffectDef.CODEC);
    }

    /** The opt-in pack is read by the same codec, but from its own root, which the scans above miss. */
    @Test
    void extendedEffectsParse() throws IOException {
        assertAllParse(ProjectFiles.resources().resolve(Path.of("datapacks", "extended_effects",
                "data", "portablebeacons", "portablebeacons")), "effect", BeaconEffectDef.CODEC);
    }

    @Test
    void augmentsParse() throws IOException {
        assertAllParse("augment", AugmentDef.CODEC);
    }

    @Test
    void tiersParse() throws IOException {
        assertAllParse("tier", BeaconTierDef.CODEC);
    }

    @Test
    void fuelParses() throws IOException {
        assertAllParse("fuel", FuelDef.CODEC);
    }

    private static void assertAllParse(String folder, Codec<?> codec) throws IOException {
        assertAllParse(DATA, folder, codec);
    }

    private static void assertAllParse(Path root, String folder, Codec<?> codec) throws IOException {
        List<Path> files;
        try (Stream<Path> stream = Files.list(root.resolve(folder))) {
            files = stream.filter(path -> path.toString().endsWith(".json")).toList();
        }
        assertFalse(files.isEmpty(), "no built-in entries found for " + folder);

        for (Path file : files) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonElement json = JsonParser.parseReader(reader);
                codec.parse(JsonOps.INSTANCE, json)
                        .getOrThrow(error -> new AssertionError(file + ": " + error));
            }
        }
    }
}
