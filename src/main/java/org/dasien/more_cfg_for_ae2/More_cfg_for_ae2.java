package org.dasien.more_cfg_for_ae2;

import com.mojang.logging.LogUtils;
import dev.toma.configuration.client.ConfigurationClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.dasien.more_cfg_for_ae2.compat.LoadedMods;
import org.slf4j.Logger;

@Mod(More_cfg_for_ae2.MODID)
public class More_cfg_for_ae2 {

    public static final String MODID = "more_cfg_for_ae2";
    private static final Logger LOGGER = LogUtils.getLogger();

    public More_cfg_for_ae2(IEventBus modEventBus) {
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);

        boolean exPatternProviderLoaded = LoadedMods.isExPatternProviderLoaded();
        boolean meRequesterLoaded = LoadedMods.isMERequesterLoaded();

        if (exPatternProviderLoaded) {
            MoreCfgCreativeTabs.TABS.register(modEventBus);
        }

        if (exPatternProviderLoaded && meRequesterLoaded) {
            Config.registerWithExPatternProviderAndMERequester();
        } else if (exPatternProviderLoaded) {
            Config.registerWithExPatternProvider();
        } else if (meRequesterLoaded) {
            Config.registerWithMERequester();
        } else {
            Config.register();
        }

        // 客户端专属：注册配置屏幕扩展点
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(More_cfg_for_ae2::onClientSetup);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Loaded configurable AE2 interface limits");
    }

    public void onServerStarting(ServerStartingEvent event) {
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ModList.get().getModContainerById(MODID).ifPresent(container -> container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> {
                    Screen screen = ConfigurationClient.getConfigScreen(MODID, parent);
                    return screen != null ? screen : parent;
                })));
        LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
