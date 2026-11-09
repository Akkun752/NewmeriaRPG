package fr.akkun.newmeriarpg;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import fr.akkun.newmeriarpg.NewmeriaRPG;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * How many RPG points each kill, mined block and fishing catch is worth, read from two files in
 * {@code data/newmeriarpg/rpg/} (reloaded by {@code /reload}):
 * <pre>
 * mobs.json:   { "fishing": 2, "mobs": { "minecraft:zombie": 1, "minecraft:cow": 0, ... } }
 * blocks.json: { "blocks": { "minecraft:diamond_ore": 3, "minecraft:oak_log": 0.25, ... } }
 * </pre>
 * A value may have decimals: 0.25 is one chance in four of 1 point, 2.5 is 2 points plus one
 * chance in two of a third (see {@link #roll}).
 * <p>
 * A mob missing from the list (one added by another mod, say) is worth 1 point if it is hostile
 * and nothing otherwise; a block missing from the list is worth nothing. Players are not in the
 * files: see {@link RpgEvents}.
 */
@EventBusSubscriber(modid = NewmeriaRPG.MOD_ID)
public class RpgPoints extends SimplePreparableReloadListener<RpgPoints.Files> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier MOBS_FILE = Identifier.fromNamespaceAndPath(NewmeriaRPG.MOD_ID, "rpg/mobs.json");
    private static final Identifier BLOCKS_FILE = Identifier.fromNamespaceAndPath(NewmeriaRPG.MOD_ID, "rpg/blocks.json");
    private static final double DEFAULT_FISHING = 2;

    private static Map<Identifier, Double> mobs = Map.of();
    private static Map<Identifier, Double> blocks = Map.of();
    private static double fishing = DEFAULT_FISHING;

    protected record Files(JsonObject mobs, JsonObject blocks) {
    }

    public static double forKill(LivingEntity victim) {
        Double points = mobs.get(BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()));
        if (points != null) {
            return points;
        }
        return victim instanceof Enemy ? 1 : 0;
    }

    public static double forBlock(BlockState state) {
        return blocks.getOrDefault(BuiltInRegistries.BLOCK.getKey(state.getBlock()), 0.0);
    }

    public static double forFishing() {
        return fishing;
    }

    /** Turns a value from the files into whole points: its decimal part is the chance of one more. */
    public static int roll(double points, RandomSource random) {
        int whole = (int) Math.floor(points);
        return random.nextDouble() < points - whole ? whole + 1 : whole;
    }

    @SubscribeEvent
    static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(NewmeriaRPG.MOD_ID, "rpg_points"), new RpgPoints());
    }

    @Override
    protected Files prepare(ResourceManager manager, ProfilerFiller profiler) {
        return new Files(read(manager, MOBS_FILE), read(manager, BLOCKS_FILE));
    }

    private static JsonObject read(ResourceManager manager, Identifier file) {
        try (Reader reader = manager.openAsReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.error("Could not read {}, the default points are used instead", file, e);
            return new JsonObject();
        }
    }

    @Override
    protected void apply(Files files, ResourceManager manager, ProfilerFiller profiler) {
        double loadedFishing = DEFAULT_FISHING;
        try {
            if (files.mobs().has("fishing")) {
                loadedFishing = Math.max(0, files.mobs().get("fishing").getAsDouble());
            }
        } catch (RuntimeException e) {
            LOGGER.error("{}: \"fishing\" is not a number", MOBS_FILE, e);
        }
        fishing = loadedFishing;
        mobs = readList(files.mobs(), "mobs", MOBS_FILE, BuiltInRegistries.ENTITY_TYPE);
        blocks = readList(files.blocks(), "blocks", BLOCKS_FILE, BuiltInRegistries.BLOCK);
    }

    private static Map<Identifier, Double> readList(JsonObject json, String key, Identifier file, Registry<?> registry) {
        Map<Identifier, Double> loaded = new HashMap<>();
        try {
            if (json.has(key)) {
                for (var entry : json.getAsJsonObject(key).entrySet()) {
                    Identifier id = Identifier.tryParse(entry.getKey());
                    if (id == null || !registry.containsKey(id)) {
                        LOGGER.warn("{}: unknown entry '{}', ignored", file, entry.getKey());
                        continue;
                    }
                    loaded.put(id, Math.max(0, entry.getValue().getAsDouble()));
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("{} is malformed, what could be read of it is used", file, e);
        }
        return loaded;
    }
}
