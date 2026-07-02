package com.vetpetmon.realmengine;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import com.mojang.logging.LogUtils;
import com.vetpetmon.realmengine.common.CommonConfig;
import com.vetpetmon.realmengine.common.CommonProxy;
import com.vetpetmon.realmengine.common.armor.ArmorPropertiesData;
import com.vetpetmon.realmengine.common.armor.ArmorPropertiesReloadListener;
import com.vetpetmon.realmengine.common.attribute.ModsetReloadListener;
import com.vetpetmon.realmengine.common.item.ItemPropertiesReloadListener;
import com.vetpetmon.realmengine.common.metaworld.Metaworld;
import com.vetpetmon.realmengine.common.tiering.loot.LootConditions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@Mod(RealmEngine.MODID) // Recognize this as its own mod
public class RealmEngine {

    public enum Weekday {
        SUNDAY,
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY,
        NONE
    }
    public static final String MODID = "realmengine";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public static CommonProxy PROXY;

    // Store the mod event bus for use in event handlers
    private final IEventBus modEventBus;

//    public static final GameRules.Key<GameRules.BooleanValue> TNT_BREAKS_BLOCKS = GameRules.register("tntBreaksBlocks", GameRules.Category.MISC, GameRules.BooleanValue.create(true));

    @SuppressWarnings("unused")
    public RealmEngine(IEventBus modEventBus, ModContainer modContainer)
    {
        this.modEventBus = modEventBus;

        MixinExtrasBootstrap.init();

        LOGGER.info("Initializing the Realmfall Engine");

        // Register loot conditions
        LootConditions.register(modEventBus);

        // Register mod lifecycle events
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onAddReloadListeners);
        modEventBus.addListener(this::onPlayerLoggedIn);
        modEventBus.addListener(this::onServerStarting);

        // Register ArmorPropertiesData as event handler
        modEventBus.register(new ArmorPropertiesData());

        // Set up proxies for server-client sharing and communications
        PROXY = new CommonProxy();  // Default to server-side proxy for now

        modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Metaworld initialization - this will set up the event bus for metaworld modules to use
        Metaworld.initialize(modEventBus);
    }

    // Register reload listeners (datapack-driven articles)
    private void onAddReloadListeners(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        event.addListener(new ModsetReloadListener());
        event.addListener(new ArmorPropertiesReloadListener());
        event.addListener(new ItemPropertiesReloadListener());
    }

    // Sync armor properties to clients when they log in
    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Send armor properties data to the client
            // TODO: Update packet sending for NeoForge 1.21.1 networking
            // PACKET_HANDLER.send(
            //     PacketDistributor.PLAYER.with(() -> player),
            //     new SyncArmorPropertiesPacket(ArmorPropertiesData.ARMOR_PROPERTIES)
            // );
            // Send item properties data to the client
            // PACKET_HANDLER.send(
            //     PacketDistributor.PLAYER.with(() -> player),
            //     new SyncItemPropertiesPacket(ItemPropertiesData.ITEM_PROPERTIES)
            // );
            // Send modsets data to the client
            // PACKET_HANDLER.send(
            //     PacketDistributor.PLAYER.with(() -> player),
            //     new SyncModsetsPacket(ModsetData.MODSETS)
            // );
            LOGGER.debug("Synced armor properties, item properties, and modsets to player: {}", player.getName().getString());
        }
        PROXY.setCurrentDayOfWeek(); // Update current day of the week on player login in case the server has been running for a while and the day has changed
    }

    @SubscribeEvent // on the mod event bus
    public static void register(RegisterPayloadHandlersEvent event) {
        // Sets the current network version
        final PayloadRegistrar registrar = event.registrar("1");
    }

    public void onServerStarting(ServerStartingEvent event)
    {
        PROXY.setCurrentDayOfWeek();
        LOGGER.info("Current day of the week set to: {}", PROXY.getCurrentDayOfWeek().toString());
    }

    /**
     * Helper to create ResourceLocations with modid
     * @param path Filepath
     * @return ResourceLocation with modID:path
     */
    public static ResourceLocation createRL(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
