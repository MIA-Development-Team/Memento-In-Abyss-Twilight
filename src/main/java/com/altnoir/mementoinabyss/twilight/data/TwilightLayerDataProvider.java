package com.altnoir.mementoinabyss.twilight.data;

import com.altnoir.mementoinabyss.twilight.MementoInAbyssTwilight;
import com.altnoir.mementoinabyss.twilight.worldgen.TwilightBiomeDefinitions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class TwilightLayerDataProvider implements DataProvider {
    private static final String MOD_ID = MementoInAbyssTwilight.ID;
    private static final String TWILIGHT = "twilightforest";
    private static final int VERTICAL_OFFSET = -112;
    private static final Set<String> BIOME_FAMILIES = TwilightBiomeDefinitions.families();
    private static final Set<String> HEIGHT_MODIFIERS =
            Set.of(
                    "minecraft:heightmap",
                    "minecraft:height_range",
                    "minecraft:heightmap_spread_double",
                    "minecraft:water_depth");
    private static final Set<String> TF_TOP_HEIGHT_MODIFIERS =
            Set.of("twilightforest:chunk_blanketing");
    private static final Set<String> REMOVED_TF_MODIFIERS = Set.of("twilightforest:avoid_landmark");
    private static final String SURFACE_MODIFIER = "mementoinabyss:layer2_surface";

    private final ResourceManager resources;
    private final PackOutput packOutput;
    private final CompletableFuture<HolderLookup.Provider> lookupProvider;
    private HolderLookup.Provider registries;
    private final PackOutput.PathProvider biomes;
    private final PackOutput.PathProvider configuredFeatures;
    private final PackOutput.PathProvider placedFeatures;
    private final PackOutput.PathProvider biomeTags;
    private final PackOutput.PathProvider structureSets;

    public TwilightLayerDataProvider(
            PackOutput output,
            ResourceManager resources,
            CompletableFuture<HolderLookup.Provider> lookupProvider) {
        this.resources = resources;
        this.packOutput = output;
        this.lookupProvider = lookupProvider;
        this.structureSets =
                output.createPathProvider(PackOutput.Target.DATA_PACK, "worldgen/structure_set");
        this.biomes = output.createPathProvider(PackOutput.Target.DATA_PACK, "worldgen/biome");
        this.configuredFeatures =
                output.createPathProvider(
                        PackOutput.Target.DATA_PACK, "worldgen/configured_feature");
        this.placedFeatures =
                output.createPathProvider(PackOutput.Target.DATA_PACK, "worldgen/placed_feature");
        this.biomeTags =
                output.createPathProvider(PackOutput.Target.DATA_PACK, "tags/worldgen/biome");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return lookupProvider.thenCompose(
                lookup -> {
                    registries = lookup;
                    List<CompletableFuture<?>> saves = new ArrayList<>();
                    try {
                        generate(cache, saves);
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                    return CompletableFuture.allOf(saves.toArray(CompletableFuture<?>[]::new));
                });
    }

    @Override
    public String getName() {
        return "Memento In Abyss twilight layer data";
    }

    private void generate(CachedOutput cache, List<CompletableFuture<?>> saves) throws IOException {
        Map<Identifier, JsonObject> selectedBiomes = selectBiomes();
        if (selectedBiomes.isEmpty()) return;

        Set<Identifier> placedIds = new LinkedHashSet<>();
        Set<Identifier> configuredIds = new LinkedHashSet<>();
        Set<Identifier> biomeRoots = new LinkedHashSet<>();
        Set<Identifier> surfaceRoots = new LinkedHashSet<>();
        collectFeatures(selectedBiomes, placedIds, configuredIds, biomeRoots, surfaceRoots);

        for (Identifier id : configuredIds)
            saveConfigured(cache, saves, id, configuredIds, placedIds);
        for (Identifier id : placedIds)
            savePlaced(cache, saves, id, configuredIds, placedIds, biomeRoots, surfaceRoots);
        for (Map.Entry<Identifier, JsonObject> entry : selectedBiomes.entrySet())
            saveBiome(cache, saves, entry.getKey(), entry.getValue(), placedIds, configuredIds);
        saveTags(cache, saves, selectedBiomes.keySet());
        saveStructureSets(cache, saves);
        saveMapColors(cache, saves, selectedBiomes.keySet());
    }

    private void saveMapColors(
            CachedOutput cache, List<CompletableFuture<?>> saves, Set<Identifier> selected)
            throws IOException {
        Identifier id =
                Identifier.fromNamespaceAndPath(
                        TWILIGHT, "data_maps/worldgen/biome/magic_map_color.json");
        JsonObject source = read(id);
        if (source == null) return;
        JsonObject values = source.getAsJsonObject("values");
        if (values == null) return;
        JsonObject copied = new JsonObject(), mapped = new JsonObject();
        for (Identifier biome : selected) {
            JsonElement color = values.get(biome.toString());
            if (color == null) continue;
            JsonObject entry = new JsonObject();
            entry.add("value", color.deepCopy());
            withCondition(entry);
            mapped.add(publicCopyId(biome).toString(), entry);
        }
        copied.add("values", mapped);
        var paths =
                packOutput.createPathProvider(
                        PackOutput.Target.DATA_PACK, "data_maps/worldgen/biome");
        saves.add(
                DataProvider.saveStable(
                        cache,
                        copied,
                        paths.json(Identifier.fromNamespaceAndPath(TWILIGHT, "magic_map_color"))));
    }

    private void saveStructureSets(CachedOutput cache, List<CompletableFuture<?>> saves) {
        var primary =
                TwilightBiomeDefinitions.groups()
                        .map(TwilightBiomeDefinitions.Group::structure)
                        .distinct()
                        .toList();
        var satellites =
                Stream.concat(
                                TwilightBiomeDefinitions.groups()
                                        .map(TwilightBiomeDefinitions.Group::satellite),
                                Stream.of("giant_house"))
                        .distinct()
                        .toList();
        for (String structure : Stream.concat(primary.stream(), satellites.stream()).toList()) {
            JsonObject data = new JsonObject(),
                    entry = new JsonObject(),
                    placement = new JsonObject();
            entry.addProperty("structure", "twilightforest:" + structure);
            entry.addProperty("weight", 1);
            JsonArray structures = new JsonArray();
            structures.add(entry);
            data.add("structures", structures);
            placement.addProperty("type", "mementoinabyss:layer2_landmark");
            placement.addProperty("primary", primary.contains(structure));
            if (structure.equals("giant_house")) placement.addProperty("salt", 1);
            data.add("placement", placement);
            withCondition(data);
            saves.add(
                    DataProvider.saveStable(
                            cache,
                            data,
                            structureSets.json(
                                    Identifier.fromNamespaceAndPath(MOD_ID, structure))));
        }
        JsonObject hills = new JsonObject(), placement = new JsonObject();
        JsonArray choices = new JsonArray();
        String[] sizes = {"small", "medium", "large"};
        for (int index = 0; index < sizes.length; index++) {
            JsonObject entry = new JsonObject();
            entry.addProperty("structure", TWILIGHT + ":" + sizes[index] + "_hollow_hill");
            entry.addProperty("weight", 3 - index);
            choices.add(entry);
        }
        hills.add("structures", choices);
        placement.addProperty("type", "minecraft:random_spread");
        placement.addProperty("spacing", 32);
        placement.addProperty("separation", 16);
        placement.addProperty("salt", 47268129);
        placement.addProperty("frequency", .25);
        JsonObject exclusion = new JsonObject();
        // All primary sets use the same native anchor placement, regardless of their biome.
        exclusion.addProperty("other_set", MOD_ID + ":lich_tower");
        exclusion.addProperty("chunk_count", 12);
        placement.add("exclusion_zone", exclusion);
        hills.add("placement", placement);
        withCondition(hills);
        saves.add(
                DataProvider.saveStable(
                        cache,
                        hills,
                        structureSets.json(
                                Identifier.fromNamespaceAndPath(MOD_ID, "hollow_hills"))));
    }

    private Map<Identifier, JsonObject> selectBiomes() throws IOException {
        Map<Identifier, JsonObject> result = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : list("worldgen/biome").entrySet()) {
            Identifier id = registryId(entry.getKey(), "worldgen/biome");
            if (id == null || !BIOME_FAMILIES.contains(family(id))) continue;
            result.put(id, read(entry.getValue()));
        }
        return result;
    }

    private void collectFeatures(
            Map<Identifier, JsonObject> selectedBiomes,
            Set<Identifier> placedIds,
            Set<Identifier> configuredIds,
            Set<Identifier> biomeRoots,
            Set<Identifier> surfaceRoots)
            throws IOException {
        Deque<Identifier> placedQueue = new ArrayDeque<>();
        Deque<Identifier> configuredQueue = new ArrayDeque<>();

        for (JsonObject biome : selectedBiomes.values()) {
            JsonElement features = biome.get("features");
            if (features == null || !features.isJsonArray()) continue;
            JsonArray layers = features.getAsJsonArray();
            for (int step = 0; step < layers.size(); step++) {
                if (step == 1) continue;
                JsonElement layer = layers.get(step);
                if (!layer.isJsonArray()) continue;
                for (JsonElement entry : layer.getAsJsonArray()) {
                    Identifier id = featureId(entry);
                    if (id != null) {
                        biomeRoots.add(id);
                        if (surfaceStep(step)) surfaceRoots.add(id);
                    }
                    collectPlacedHolder(
                            entry, placedIds, configuredIds, placedQueue, configuredQueue);
                }
            }
        }

        while (!placedQueue.isEmpty() || !configuredQueue.isEmpty()) {
            while (!placedQueue.isEmpty()) {
                Identifier id = placedQueue.poll();
                JsonObject placed = readFeature("worldgen/placed_feature", id);
                if (placed != null)
                    collectConfiguredHolder(
                            placed.get("feature"),
                            placedIds,
                            configuredIds,
                            placedQueue,
                            configuredQueue);
            }
            while (!configuredQueue.isEmpty()) {
                Identifier id = configuredQueue.poll();
                JsonObject configured = readFeature("worldgen/configured_feature", id);
                if (configured != null)
                    collectConfiguredRefs(
                            configured, placedIds, configuredIds, placedQueue, configuredQueue);
            }
        }
    }

    private void collectConfiguredRefs(
            JsonObject configured,
            Set<Identifier> placedIds,
            Set<Identifier> configuredIds,
            Deque<Identifier> placedQueue,
            Deque<Identifier> configuredQueue) {
        mapPlacedReferences(
                configured,
                holder -> {
                    collectPlacedHolder(
                            holder, placedIds, configuredIds, placedQueue, configuredQueue);
                    return holder;
                });
    }

    private void collectPlacedHolder(
            JsonElement holder,
            Set<Identifier> placedIds,
            Set<Identifier> configuredIds,
            Deque<Identifier> placedQueue,
            Deque<Identifier> configuredQueue) {
        Identifier id = featureId(holder);
        if (id != null) enqueuePlaced(id, placedIds, placedQueue);
        else if (holder != null && holder.isJsonObject())
            collectConfiguredHolder(
                    holder.getAsJsonObject().get("feature"),
                    placedIds,
                    configuredIds,
                    placedQueue,
                    configuredQueue);
    }

    private void collectConfiguredHolder(
            JsonElement holder,
            Set<Identifier> placedIds,
            Set<Identifier> configuredIds,
            Deque<Identifier> placedQueue,
            Deque<Identifier> configuredQueue) {
        Identifier id = featureId(holder);
        if (id != null) enqueueConfigured(id, configuredIds, configuredQueue);
        else if (holder != null && holder.isJsonObject())
            collectConfiguredRefs(
                    holder.getAsJsonObject(),
                    placedIds,
                    configuredIds,
                    placedQueue,
                    configuredQueue);
    }

    private void enqueuePlaced(Identifier id, Set<Identifier> ids, Deque<Identifier> queue) {
        if (!copyable(id) || ids.contains(id)) return;
        if (!exists("worldgen/placed_feature", id)) return;
        ids.add(id);
        queue.add(id);
    }

    private void enqueueConfigured(Identifier id, Set<Identifier> ids, Deque<Identifier> queue) {
        if (!copyable(id) || ids.contains(id)) return;
        if (!exists("worldgen/configured_feature", id)) return;
        ids.add(id);
        queue.add(id);
    }

    private void saveBiome(
            CachedOutput cache,
            List<CompletableFuture<?>> saves,
            Identifier original,
            JsonObject source,
            Set<Identifier> placedIds,
            Set<Identifier> configuredIds)
            throws IOException {
        JsonObject copy = source.deepCopy();
        copy.add("carvers", new JsonArray());

        JsonElement features = copy.get("features");
        if (features != null && features.isJsonArray()) {
            JsonArray layers = features.getAsJsonArray();
            for (int step = 0; step < layers.size(); step++) {
                if (step == 1) {
                    layers.set(step, new JsonArray());
                    continue;
                }
                JsonElement layer = layers.get(step);
                if (!layer.isJsonArray()) continue;
                JsonArray values = layer.getAsJsonArray();
                for (int i = 0; i < values.size(); i++) {
                    JsonElement holder = values.get(i);
                    if (holder.isJsonObject()) holder = offsetAnchors(holder);
                    values.set(
                            i,
                            rewritePlacedHolder(
                                    holder, configuredIds, placedIds, surfaceStep(step), false));
                }
            }
        }

        withCondition(copy);
        saves.add(DataProvider.saveStable(cache, copy, biomes.json(publicCopyId(original))));
    }

    private void saveConfigured(
            CachedOutput cache,
            List<CompletableFuture<?>> saves,
            Identifier original,
            Set<Identifier> configuredIds,
            Set<Identifier> placedIds)
            throws IOException {
        JsonObject source = readFeature("worldgen/configured_feature", original);
        if (source == null) return;

        JsonObject raw = offsetAnchors(source.deepCopy()).getAsJsonObject();
        rewriteFeatureRefs(raw, configuredIds, placedIds);
        withCondition(raw);
        saves.add(
                DataProvider.saveStable(cache, raw, configuredFeatures.json(rawCopyId(original))));

        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("type", "mementoinabyss:layer2_feature");
        JsonObject config = new JsonObject();
        config.addProperty("feature", rawCopyId(original).toString());
        String type = string(source.get("type"));
        if ("twilightforest:simple_well".equals(type) || "twilightforest:fancy_well".equals(type))
            config.addProperty("allow_fluids", true);
        wrapper.add("config", config);
        withCondition(wrapper);
        saves.add(
                DataProvider.saveStable(
                        cache, wrapper, configuredFeatures.json(publicCopyId(original))));
    }

    private void savePlaced(
            CachedOutput cache,
            List<CompletableFuture<?>> saves,
            Identifier original,
            Set<Identifier> configuredIds,
            Set<Identifier> placedIds,
            Set<Identifier> biomeRoots,
            Set<Identifier> surfaceRoots)
            throws IOException {
        JsonObject source = readFeature("worldgen/placed_feature", original);
        if (source == null) return;

        JsonObject copy = offsetAnchors(source.deepCopy()).getAsJsonObject();
        JsonObject result =
                rewritePlacedHolder(
                                copy,
                                configuredIds,
                                placedIds,
                                surfaceRoots.contains(original),
                                !biomeRoots.contains(original))
                        .getAsJsonObject();
        withCondition(result);
        saves.add(
                DataProvider.saveStable(
                        cache, result, placedFeatures.json(publicCopyId(original))));
    }

    private static JsonArray surfacePlacement(JsonArray original, boolean scan) {
        JsonArray kept = new JsonArray();
        int insertAt = -1;
        String heightmap = null;
        for (JsonElement element : original) {
            if (!element.isJsonObject()) {
                kept.add(element.deepCopy());
                continue;
            }
            JsonObject modifier = element.getAsJsonObject();
            String type = string(modifier.get("type"));
            if (type == null) {
                kept.add(modifier.deepCopy());
                continue;
            }
            if (REMOVED_TF_MODIFIERS.contains(type)) continue;
            if (scan
                    && (type.equals(SURFACE_MODIFIER)
                            || HEIGHT_MODIFIERS.contains(type)
                            || TF_TOP_HEIGHT_MODIFIERS.contains(type))) {
                if (heightmap == null && modifier.has("heightmap"))
                    heightmap = string(modifier.get("heightmap"));
                if (insertAt < 0) insertAt = kept.size();
                continue;
            }
            kept.add(modifier.deepCopy());
        }

        if (!scan) return kept;

        // Keep the biome filter after the scan so it sees each candidate surface.
        JsonObject scanner = new JsonObject();
        scanner.addProperty("type", SURFACE_MODIFIER);
        if (heightmap != null) scanner.addProperty("heightmap", heightmap);
        if (insertAt < 0) {
            insertAt = kept.size();
            for (int i = 0; i < kept.size(); i++) {
                JsonElement element = kept.get(i);
                if (element.isJsonObject()
                        && "minecraft:biome"
                                .equals(string(element.getAsJsonObject().get("type")))) {
                    insertAt = i;
                    break;
                }
            }
        }

        JsonArray result = new JsonArray();
        for (int i = 0; i < insertAt; i++) result.add(kept.get(i));
        result.add(scanner);
        for (int i = insertAt; i < kept.size(); i++) result.add(kept.get(i));
        return result;
    }

    private void saveTags(
            CachedOutput cache, List<CompletableFuture<?>> saves, Set<Identifier> selected)
            throws IOException {
        List<Identifier> ordered = new ArrayList<>(selected);
        ordered.sort(Comparator.comparing(Identifier::getPath));

        for (Map.Entry<Identifier, Resource> entry : list("tags/worldgen/biome").entrySet()) {
            Identifier tagId = registryId(entry.getKey(), "tags/worldgen/biome");
            if (tagId == null) continue;
            JsonObject tag = read(entry.getValue());
            JsonArray values = tag.getAsJsonArray("values");
            if (values == null) continue;

            Set<Identifier> present = new LinkedHashSet<>();
            for (JsonElement value : values) {
                Identifier biome = tagBiomeId(value);
                if (biome != null && selected.contains(biome)) present.add(biome);
            }
            if (present.isEmpty()) continue;

            JsonObject merged = new JsonObject();
            merged.addProperty("replace", false);
            JsonArray mergedValues = new JsonArray();
            merged.add("values", mergedValues);
            Set<String> existing = new LinkedHashSet<>();
            for (Identifier biome : ordered) {
                if (!present.contains(biome)) continue;
                String copy = publicCopyId(biome).toString();
                if (existing.contains(copy)) continue;
                mergedValues.add(optionalEntry(copy));
                existing.add(copy);
            }
            saves.add(DataProvider.saveStable(cache, merged, biomeTags.json(tagId)));
        }

        JsonObject own = new JsonObject();
        JsonArray values = new JsonArray();
        for (Identifier biome : ordered) values.add(optionalEntry(publicCopyId(biome).toString()));
        own.add("values", values);
        saves.add(
                DataProvider.saveStable(
                        cache,
                        own,
                        biomeTags.json(Identifier.fromNamespaceAndPath(MOD_ID, "layer2"))));
    }

    private static JsonObject optionalEntry(String id) {
        JsonObject entry = new JsonObject();
        entry.addProperty("id", id);
        entry.addProperty("required", false);
        return entry;
    }

    private void rewriteFeatureRefs(
            JsonObject configured, Set<Identifier> configuredIds, Set<Identifier> placedIds) {
        mapPlacedReferences(
                configured,
                holder -> rewritePlacedHolder(holder, configuredIds, placedIds, false, true));
    }

    private JsonElement rewritePlacedHolder(
            JsonElement holder,
            Set<Identifier> configuredIds,
            Set<Identifier> placedIds,
            boolean surfaceRoot,
            boolean nested) {
        Identifier id = featureId(holder);
        if (id != null)
            return placedIds.contains(id) ? new JsonPrimitive(publicCopyId(id).toString()) : holder;
        if (holder == null || !holder.isJsonObject()) return holder;
        JsonObject copy = holder.getAsJsonObject().deepCopy();
        JsonElement feature = copy.get("feature");
        Identifier configured = featureId(feature);
        if (configured != null && configuredIds.contains(configured))
            copy.addProperty("feature", publicCopyId(configured).toString());
        else if (feature != null && feature.isJsonObject())
            rewriteFeatureRefs(feature.getAsJsonObject(), configuredIds, placedIds);
        JsonArray placement =
                copy.has("placement") && copy.get("placement").isJsonArray()
                        ? copy.getAsJsonArray("placement")
                        : new JsonArray();
        copy.add(
                "placement",
                surfacePlacement(placement, surfaceRoot || nested && groundHeightmap(placement)));
        return copy;
    }

    // Only these codec fields contain feature holders. Block states and item names may share their
    // IDs.
    private static void mapPlacedReferences(
            JsonObject configured, UnaryOperator<JsonElement> mapper) {
        String type = string(configured.get("type"));
        JsonElement config = configured.get("config");
        if (type == null || config == null) return;
        if (type.equals("twilightforest:weighted_list") && config.isJsonArray()) {
            for (JsonElement entry : config.getAsJsonArray()) {
                if (entry.isJsonObject()) mapField(entry.getAsJsonObject(), "data", mapper);
            }
            return;
        }
        if (!config.isJsonObject()) return;
        JsonObject object = config.getAsJsonObject();
        switch (type) {
            case "minecraft:random_selector" -> {
                mapField(object, "default", mapper);
                JsonArray features = object.getAsJsonArray("features");
                if (features != null)
                    for (JsonElement entry : features) {
                        if (entry.isJsonObject())
                            mapField(entry.getAsJsonObject(), "feature", mapper);
                    }
            }
            case "minecraft:simple_random_selector" -> {
                JsonElement features = object.get("features");
                if (features != null && features.isJsonArray()) {
                    JsonArray array = features.getAsJsonArray();
                    for (int i = 0; i < array.size(); i++) array.set(i, mapper.apply(array.get(i)));
                }
            }
            case "minecraft:random_boolean_selector" -> {
                mapField(object, "feature_true", mapper);
                mapField(object, "feature_false", mapper);
            }
            case "minecraft:random_patch",
                    "minecraft:flower",
                    "minecraft:no_bonemeal_flower",
                    "minecraft:root_system" ->
                    mapField(object, "feature", mapper);
            case "minecraft:vegetation_patch", "minecraft:waterlogged_vegetation_patch" ->
                    mapField(object, "vegetation_feature", mapper);
            default -> {}
        }
    }

    private static void mapField(JsonObject object, String key, UnaryOperator<JsonElement> mapper) {
        if (object.has(key)) object.add(key, mapper.apply(object.get(key)));
    }

    private static boolean groundHeightmap(JsonArray placement) {
        for (JsonElement entry : placement) {
            if (!entry.isJsonObject()) continue;
            String type = string(entry.getAsJsonObject().get("type"));
            if ("minecraft:heightmap".equals(type)
                    || "minecraft:heightmap_spread_double".equals(type)
                    || type != null && TF_TOP_HEIGHT_MODIFIERS.contains(type)) return true;
        }
        return false;
    }

    private static boolean surfaceStep(int step) {
        return step == 4 || step == 9 || step == 10;
    }

    private static boolean copyable(Identifier id) {
        return id != null
                && (TWILIGHT.equals(id.getNamespace()) || "minecraft".equals(id.getNamespace()));
    }

    private static JsonElement offsetAnchors(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.size() == 1) {
                JsonElement absolute = object.get("absolute");
                if (absolute != null
                        && absolute.isJsonPrimitive()
                        && absolute.getAsJsonPrimitive().isNumber()) {
                    object.addProperty("absolute", absolute.getAsInt() + VERTICAL_OFFSET);
                    return object;
                }
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet())
                entry.setValue(offsetAnchors(entry.getValue()));
            return object;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) array.set(i, offsetAnchors(array.get(i)));
            return array;
        }
        return element;
    }

    private static void withCondition(JsonObject json) {
        JsonArray conditions;
        JsonElement existing = json.get("neoforge:conditions");
        if (existing != null && existing.isJsonArray()) conditions = existing.getAsJsonArray();
        else {
            conditions = new JsonArray();
            json.add("neoforge:conditions", conditions);
        }
        for (JsonElement condition : conditions) {
            if (!condition.isJsonObject()) continue;
            JsonObject object = condition.getAsJsonObject();
            if ("neoforge:mod_loaded".equals(string(object.get("type")))
                    && TWILIGHT.equals(string(object.get("modid")))) return;
        }
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "neoforge:mod_loaded");
        condition.addProperty("modid", TWILIGHT);
        conditions.add(condition);
    }

    private Map<Identifier, Resource> list(String directory) {
        return resources.listResources(
                directory,
                id -> id.getNamespace().equals(TWILIGHT) && id.getPath().endsWith(".json"));
    }

    private boolean exists(String directory, Identifier id) {
        if (resources.getResource(resourceId(directory, id)).isPresent()) return true;
        if (!"minecraft".equals(id.getNamespace())) return false;
        return switch (directory) {
            case "worldgen/placed_feature" ->
                    registries
                            .lookupOrThrow(Registries.PLACED_FEATURE)
                            .get(ResourceKey.create(Registries.PLACED_FEATURE, id))
                            .isPresent();
            case "worldgen/configured_feature" ->
                    registries
                            .lookupOrThrow(Registries.CONFIGURED_FEATURE)
                            .get(ResourceKey.create(Registries.CONFIGURED_FEATURE, id))
                            .isPresent();
            default -> false;
        };
    }

    private JsonObject readFeature(String directory, Identifier id) throws IOException {
        JsonObject resource = read(resourceId(directory, id));
        if (resource != null || !"minecraft".equals(id.getNamespace())) return resource;
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        if (directory.equals("worldgen/placed_feature")) {
            var holder =
                    registries
                            .lookupOrThrow(Registries.PLACED_FEATURE)
                            .get(ResourceKey.create(Registries.PLACED_FEATURE, id));
            return holder.isEmpty()
                    ? null
                    : PlacedFeature.DIRECT_CODEC
                            .encodeStart(ops, holder.get().value())
                            .getOrThrow()
                            .getAsJsonObject();
        }
        if (directory.equals("worldgen/configured_feature")) {
            var holder =
                    registries
                            .lookupOrThrow(Registries.CONFIGURED_FEATURE)
                            .get(ResourceKey.create(Registries.CONFIGURED_FEATURE, id));
            return holder.isEmpty()
                    ? null
                    : ConfiguredFeature.DIRECT_CODEC
                            .encodeStart(ops, holder.get().value())
                            .getOrThrow()
                            .getAsJsonObject();
        }
        return null;
    }

    private JsonObject read(Identifier id) throws IOException {
        Resource resource = resources.getResource(id).orElse(null);
        return resource == null ? null : read(resource);
    }

    private static JsonObject read(Resource resource) throws IOException {
        try (InputStream input = resource.open()) {
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    private static Identifier registryId(Identifier resourceId, String directory) {
        String prefix = directory + "/";
        String path = resourceId.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(".json")) return null;
        return Identifier.fromNamespaceAndPath(
                resourceId.getNamespace(), path.substring(prefix.length(), path.length() - 5));
    }

    private static Identifier resourceId(String directory, Identifier id) {
        return Identifier.fromNamespaceAndPath(
                id.getNamespace(), directory + "/" + id.getPath() + ".json");
    }

    private static Identifier publicCopyId(Identifier original) {
        return Identifier.fromNamespaceAndPath(
                MOD_ID,
                ("minecraft".equals(original.getNamespace()) ? "vanilla/" : "")
                        + original.getPath());
    }

    private static Identifier rawCopyId(Identifier original) {
        return Identifier.fromNamespaceAndPath(
                MOD_ID,
                "raw/"
                        + ("minecraft".equals(original.getNamespace()) ? "vanilla/" : "")
                        + original.getPath());
    }

    private static String family(Identifier id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private static Identifier featureId(JsonElement element) {
        String value = string(element);
        return value == null ? null : parseId(value);
    }

    private static Identifier tagBiomeId(JsonElement value) {
        String id = string(value);
        if (id != null) return parseId(stripHash(id));
        if (!value.isJsonObject()) return null;
        return parseId(stripHash(string(value.getAsJsonObject().get("id"))));
    }

    private static Identifier parseId(String value) {
        if (value == null) return null;
        int colon = value.indexOf(':');
        if (colon <= 0 || colon == value.length() - 1) return null;
        try {
            return Identifier.fromNamespaceAndPath(
                    value.substring(0, colon), value.substring(colon + 1));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String stripHash(String value) {
        return value != null && value.startsWith("#") ? value.substring(1) : value;
    }

    private static String string(JsonElement element) {
        if (element == null
                || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isString()) return null;
        return element.getAsString();
    }
}
