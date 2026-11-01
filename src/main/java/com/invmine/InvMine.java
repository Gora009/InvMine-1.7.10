package com.invmine;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * InvMine — ходьба с открытыми интерфейсами для Minecraft 1.7.10.
 * Мод чисто клиентский: без ASM/Mixin, без пакетов, поэтому его можно ставить
 * только на клиент и он не конфликтует с другими модами.
 */
@Mod(
    modid = InvMine.MODID,
    name = InvMine.NAME,
    version = InvMine.VERSION,
    acceptableRemoteVersions = "*",
    guiFactory = "com.invmine.client.InvMineGuiFactory")
public class InvMine {

    public static final String MODID = "invmine";
    public static final String NAME = "InvMine";
    public static final String VERSION = "1.0.0";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        if (event.getSide().isClient()) {
            com.invmine.client.ClientSetup.preInit(event);
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (event.getSide().isClient()) {
            com.invmine.client.ClientSetup.init();
        }
    }
}
