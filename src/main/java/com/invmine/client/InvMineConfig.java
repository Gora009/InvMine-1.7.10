package com.invmine.client;

import com.invmine.InvMine;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.common.config.Configuration;

public final class InvMineConfig {

    public static final String CATEGORY = Configuration.CATEGORY_GENERAL;

    public static Configuration config;

    public static boolean enabled = true;
    public static boolean moveInContainers = true;
    public static boolean moveInOtherScreens = true;
    public static boolean allowJump = true;
    public static boolean allowSprint = true;
    public static boolean allowSneak = false;
    public static boolean blockWhenTextFieldFocused = true;
    public static boolean blockWhenKeyRepeatEnabled = true;
    public static boolean logScreenClass = false;
    public static String[] blacklist = new String[0];

    private static final String[] DEFAULT_BLACKLIST = {
        // Окна, где клавиши используются для ввода / назначения
        "net.minecraft.client.gui.GuiChat",
        "net.minecraft.client.gui.GuiSleepMP",
        "net.minecraft.client.gui.inventory.GuiEditSign",
        "net.minecraft.client.gui.GuiScreenBook",
        "net.minecraft.client.gui.GuiCommandBlock",
        "net.minecraft.client.gui.GuiControls",
        "net.minecraft.client.gui.GuiIngameMenu",
        "net.minecraft.client.gui.GuiGameOver",
        "net.minecraft.client.gui.GuiDownloadTerrain",
        "net.minecraft.client.gui.GuiMainMenu",
        "cpw.mods.fml.client.config.GuiConfig",
        "cpw.mods.fml.client.GuiModList",
    };

    /** Префиксы имён классов, в которых двигаться нельзя (вычисляется из blacklist). */
    static List<String> blacklistPrefixes = new ArrayList<String>();

    private InvMineConfig() {}

    public static void load(File file) {
        config = new Configuration(file);
        sync();
    }

    public static void sync() {
        config.setCategoryComment(CATEGORY, "InvMine: movement while GUIs are open / ходьба в открытых интерфейсах");

        enabled = config.getBoolean("enabled", CATEGORY, true,
            "Enable movement in GUIs. / Включить ходьбу в интерфейсах.");
        moveInContainers = config.getBoolean("moveInContainers", CATEGORY, true,
            "Allow movement in inventories / chests / machines (GuiContainer). / Ходьба в инвентарях, сундуках, механизмах.");
        moveInOtherScreens = config.getBoolean("moveInOtherScreens", CATEGORY, true,
            "Allow movement in other screens (books of quests, maps, mod menus...). / Ходьба в прочих интерфейсах модов.");
        allowJump = config.getBoolean("allowJump", CATEGORY, true,
            "Allow jumping / swimming up. / Разрешить прыжок.");
        allowSprint = config.getBoolean("allowSprint", CATEGORY, true,
            "Allow sprinting. / Разрешить бег.");
        allowSneak = config.getBoolean("allowSneak", CATEGORY, false,
            "Allow sneaking. Off by default because Shift is used for shift-click. / Разрешить приседание (по умолчанию выкл., т.к. Shift нужен для shift-клика).");
        blockWhenTextFieldFocused = config.getBoolean("blockWhenTextFieldFocused", CATEGORY, true,
            "Stop movement while a text field (search, anvil name, NEI...) is focused. / Не двигаться, пока активно текстовое поле.");
        blockWhenKeyRepeatEnabled = config.getBoolean("blockWhenKeyRepeatEnabled", CATEGORY, true,
            "Treat non-container screens with keyboard repeat enabled as text input screens. / Считать окна с автоповтором клавиш окнами ввода текста.");
        blacklist = config.getStringList("blacklist", CATEGORY, DEFAULT_BLACKLIST,
            "GUI class names (or package prefixes) where movement is disabled. Enable debug logging via 'logScreenClass' to find names. / Классы интерфейсов (или префиксы пакетов), где ходьба отключена.");
        logScreenClass = config.getBoolean("logScreenClass", CATEGORY, false,
            "Log the class name of every opened GUI (for filling the blacklist). / Писать в лог класс каждого открытого интерфейса.");

        List<String> prefixes = new ArrayList<String>();
        for (String s : blacklist) {
            if (s != null && !s.trim().isEmpty()) {
                prefixes.add(s.trim());
            }
        }
        blacklistPrefixes = prefixes;

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static class ChangeListener {
        @SubscribeEvent
        public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (InvMine.MODID.equals(event.modID)) {
                sync();
            }
        }
    }
}
