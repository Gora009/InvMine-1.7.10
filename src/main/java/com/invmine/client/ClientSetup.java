package com.invmine.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public final class ClientSetup {

    public static KeyBinding toggleKey;

    private ClientSetup() {}

    public static void preInit(FMLPreInitializationEvent event) {
        InvMineConfig.load(event.getSuggestedConfigurationFile());
    }

    public static void init() {
        toggleKey = new KeyBinding("key.invmine.toggle", Keyboard.KEY_NONE, "key.categories.invmine");
        ClientRegistry.registerKeyBinding(toggleKey);

        // Тики и ввод приходят по шине FML, изменение конфига — тоже.
        FMLCommonHandler.instance().bus().register(new MovementHandler());
        FMLCommonHandler.instance().bus().register(new InvMineConfig.ChangeListener());
    }
}
