package com.altnoir.mementoinabyss.twilight;

import com.altnoir.mementoinabyss.api.worldgen.TheAbyssGeneratorSetupEvent;
import com.altnoir.mementoinabyss.twilight.data.TwilightLayerDataProvider;
import com.altnoir.mementoinabyss.twilight.worldgen.AbyssTwilight;
import com.altnoir.mementoinabyss.twilight.worldgen.Layer2ChunkBlanketingPlacement;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.slf4j.Logger;

@Mod(MementoInAbyssTwilight.ID)
public final class MementoInAbyssTwilight {
    public static final String ID = "mementoinabyss_twilight";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MementoInAbyssTwilight(IEventBus modBus) {
        modBus.addListener(MementoInAbyssTwilight::gatherData);
        Layer2ChunkBlanketingPlacement.register(modBus);
        NeoForge.EVENT_BUS.addListener(MementoInAbyssTwilight::setupGenerator);
    }

    private static void setupGenerator(TheAbyssGeneratorSetupEvent event) {
        AbyssTwilight.install(event.level(), event.generator(), event.randomState());
    }

    private static void gatherData(GatherDataEvent.Client event) {
        if (!event.getModContainer().getModId().equals(ID)) return;
        event.createProvider(
                output ->
                        new TwilightLayerDataProvider(
                                output,
                                event.getResourceManager(PackType.SERVER_DATA),
                                event.getLookupProvider()));
    }

    public static Identifier asResource(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }
}
