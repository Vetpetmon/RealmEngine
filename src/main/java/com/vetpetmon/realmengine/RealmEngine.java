package com.vetpetmon.realmengine;

import com.mojang.logging.LogUtils;
import com.vetpetmon.realmengine.client.ClientProxy;
import com.vetpetmon.realmengine.common.CommonConfig;
import com.vetpetmon.realmengine.common.CommonProxy;
import com.vetpetmon.realmengine.common.armor.ArmorPropertiesData;
import com.vetpetmon.realmengine.common.armor.ArmorPropertiesReloadListener;
import com.vetpetmon.realmengine.common.attribute.ModsetData;
import com.vetpetmon.realmengine.common.attribute.ModsetReloadListener;
import com.vetpetmon.realmengine.common.item.ItemPropertiesData;
import com.vetpetmon.realmengine.common.item.ItemPropertiesReloadListener;
import com.vetpetmon.realmengine.common.metaworld.Metaworld;
import com.vetpetmon.realmengine.common.metaworld.data.QuizDB;
import com.vetpetmon.realmengine.common.networking.ApplyArmorModToSlotPacket;
import com.vetpetmon.realmengine.common.networking.SyncArmorPropertiesPacket;
import com.vetpetmon.realmengine.common.networking.SyncItemPropertiesPacket;
import com.vetpetmon.realmengine.common.networking.SyncModsetsPacket;
import com.vetpetmon.realmengine.common.tiering.loot.LootConditions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.commons.lang3.tuple.Pair;
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

    // Define mod id in a common place for everything to reference
    public static final String MODID = "realmengine";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final String PROTOCOL_VERSION = "4";
    public static CommonProxy PROXY;
    public static final CommonConfig commonConfig;
    public static final ForgeConfigSpec serverConfigSpec;

    static {
        final Pair<CommonConfig, ForgeConfigSpec> specPairCommon = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        serverConfigSpec = specPairCommon.getRight();
        commonConfig = specPairCommon.getLeft();
    }
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(createRL(MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);


    @SuppressWarnings("removal")
    public RealmEngine(FMLJavaModLoadingContext context)
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, serverConfigSpec, "realmengine-common.toml");

        IEventBus modEventBus = context.getModEventBus();
        LOGGER.info("Initializing the Realmfall Engine");

        // Register loot conditions
        LootConditions.register(modEventBus);
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(new ArmorPropertiesData());
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);

        // Set up proxies for server-client sharing and communications
        PROXY = DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);


    }

    @SuppressWarnings("UnusedAssignment")
    private void commonSetup(final FMLCommonSetupEvent event) {

        // Metaworld initialization - this will set up the event bus for metaworld modules to use
        Metaworld.initialize(MinecraftForge.EVENT_BUS);
//        Metaworld.registerModule(new ExampleMetaworldModule());

        int packetID = 0;

        PACKET_HANDLER.registerMessage(packetID++, QuizDB.MessageSyncQuestionDB.class, QuizDB.MessageSyncQuestionDB::write, QuizDB.MessageSyncQuestionDB::new, QuizDB.MessageSyncQuestionDB::handle);
        PACKET_HANDLER.registerMessage(packetID++, ApplyArmorModToSlotPacket.class, ApplyArmorModToSlotPacket::encode, ApplyArmorModToSlotPacket::decode, ApplyArmorModToSlotPacket::handle);
        PACKET_HANDLER.registerMessage(packetID++, SyncArmorPropertiesPacket.class, SyncArmorPropertiesPacket::encode, SyncArmorPropertiesPacket::decode, SyncArmorPropertiesPacket::handle);
        PACKET_HANDLER.registerMessage(packetID++, SyncItemPropertiesPacket.class, SyncItemPropertiesPacket::encode, SyncItemPropertiesPacket::decode, SyncItemPropertiesPacket::handle);
        PACKET_HANDLER.registerMessage(packetID++, SyncModsetsPacket.class, SyncModsetsPacket::encode, SyncModsetsPacket::decode, SyncModsetsPacket::handle);
    }

    // Register reload listeners (datapack-driven articles)
    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ModsetReloadListener());
        event.addListener(new ArmorPropertiesReloadListener());
        event.addListener(new ItemPropertiesReloadListener());
    }

    // Sync armor properties to clients when they log in
    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Send armor properties data to the client
            PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncArmorPropertiesPacket(ArmorPropertiesData.ARMOR_PROPERTIES)
            );
            // Send item properties data to the client
            PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncItemPropertiesPacket(ItemPropertiesData.ITEM_PROPERTIES)
            );
            // Send modsets data to the client
            PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SyncModsetsPacket(ModsetData.MODSETS)
            );
            LOGGER.debug("Synced armor properties, item properties, and modsets to player: {}", player.getName().getString());
        }
        PROXY.setCurrentDayOfWeek(); // Update current day of the week on player login in case the server has been running for a while and the day has changed
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
    public static ResourceLocation createRL(String path) {return ResourceLocation.fromNamespaceAndPath(MODID, path);}
}
