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
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import net.neoforged.fml.ModContainer;

// Recognize this as its own mod
@Mod(RealmEngine.MODID)
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

    public static final ModConfigSpec serverConfigSpec;

    static {
        final Pair<CommonConfig, ModConfigSpec> specPairCommon = new ModConfigSpec.Builder().configure(CommonConfig::new);
        serverConfigSpec = specPairCommon.getRight();
        commonConfig = specPairCommon.getLeft();
    }

    // FIXME: Update to Neoforge 1.21.1 networking system
//    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(createRL(MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    // Why did you have to rewrite the packet system??? There was nothing to fix, why??? Why did you touch the packet code??? It worked just fine???

    //    public static final GameRules.Key<GameRules.BooleanValue> TNT_BREAKS_BLOCKS = GameRules.register("tntBreaksBlocks", GameRules.Category.MISC, GameRules.BooleanValue.create(true));
    @SuppressWarnings({"removal", "this-escape"})
    public RealmEngine(IEventBus modEventBus, ModContainer modContainer) {
        MixinExtrasBootstrap.init();
        modContainer.registerConfig(ModConfig.Type.COMMON, serverConfigSpec, "realmengine-common.toml");
                LOGGER.info("Initializing the Realmfall Engine");
        // Register loot conditions
        LootConditions.register(modEventBus);
        // Register ourselves for server and other game events we are interested in
modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(new ArmorPropertiesData());
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        // Set up proxies for server-client sharing and communications
//        PROXY = DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
        PROXY = new CommonProxy();
    }

    @SuppressWarnings("UnusedAssignment")
    private void commonSetup(final FMLCommonSetupEvent event) {
        // Metaworld initialization - this will set up the event bus for metaworld modules to use
        Metaworld.initialize(NeoForge.EVENT_BUS);
        //        Metaworld.registerModule(new ExampleMetaworldModule());
        int packetID = 0;
//        PACKET_HANDLER.registerMessage(packetID++, QuizDB.MessageSyncQuestionDB.class, QuizDB.MessageSyncQuestionDB::write, QuizDB.MessageSyncQuestionDB::new, QuizDB.MessageSyncQuestionDB::handle);
//        PACKET_HANDLER.registerMessage(packetID++, ApplyArmorModToSlotPacket.class, ApplyArmorModToSlotPacket::encode, ApplyArmorModToSlotPacket::decode, ApplyArmorModToSlotPacket::handle);
//        PACKET_HANDLER.registerMessage(packetID++, SyncArmorPropertiesPacket.class, SyncArmorPropertiesPacket::encode, SyncArmorPropertiesPacket::decode, SyncArmorPropertiesPacket::handle);
//        PACKET_HANDLER.registerMessage(packetID++, SyncItemPropertiesPacket.class, SyncItemPropertiesPacket::encode, SyncItemPropertiesPacket::decode, SyncItemPropertiesPacket::handle);
//        PACKET_HANDLER.registerMessage(packetID++, SyncModsetsPacket.class, SyncModsetsPacket::encode, SyncModsetsPacket::decode, SyncModsetsPacket::handle);
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
            // FIXME: Update to Neoforge 1.21.1 networking system
//            // Send armor properties data to the client
//            PacketDistributor.sendToPlayer(player, new SyncArmorPropertiesPacket(ArmorPropertiesData.ARMOR_PROPERTIES));
//            // Send item properties data to the client
//            PacketDistributor.sendToPlayer(player, new SyncItemPropertiesPacket(ItemPropertiesData.ITEM_PROPERTIES));
//            // Send modsets data to the client
//            PacketDistributor.sendToPlayer(player, new SyncModsetsPacket(ModsetData.MODSETS));
//            LOGGER.debug("Synced armor properties, item properties, and modsets to player: {}", player.getName().getString());
        }
        // Update current day of the week on player login in case the server has been running for a while and the day has changed
        PROXY.setCurrentDayOfWeek();
    }

    public void onServerStarting(ServerStartingEvent event) {
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
