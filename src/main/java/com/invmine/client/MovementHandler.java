package com.invmine.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentTranslation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

/**
 * Пока открыт интерфейс, ванилла не обновляет состояние клавиш движения
 * (а при открытии вообще "отпускает" все клавиши). Здесь в начале каждого
 * клиентского тика мы выставляем клавишам движения их реальное физическое
 * состояние — дальше обычный MovementInputFromOptions двигает игрока сам.
 *
 * Никаких изменений байткода: только событие тика и поле KeyBinding.pressed,
 * поэтому мод совместим с любыми другими модами.
 */
public class MovementHandler {

    private static final Logger LOG = LogManager.getLogger("InvMine");

    private static final Field PRESSED = ReflectionHelper.findField(KeyBinding.class, "field_74513_e", "pressed");

    /** true, если в прошлом тике мы управляли клавишами. */
    private boolean controlling;
    private GuiScreen lastScreen;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen screen = mc.currentScreen;

        if (screen != lastScreen) {
            lastScreen = screen;
            if (screen != null && InvMineConfig.logScreenClass) {
                LOG.info("Opened GUI: " + screen.getClass().getName());
            }
        }

        boolean allow = screen != null
            && mc.thePlayer != null
            && mc.theWorld != null
            && InvMineConfig.enabled
            && canMoveIn(screen);

        GameSettings gs = mc.gameSettings;
        if (allow) {
            controlling = true;
            syncMovementKeys(gs, true, true);
        } else if (controlling) {
            controlling = false;
            // Интерфейс закрыли — синхронизируем с реальными клавишами, чтобы отпущенные
            // в интерфейсе не "залипли". Перешли в окно, где двигаться нельзя — отпускаем всё.
            syncMovementKeys(gs, screen == null, screen != null);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (ClientSetup.toggleKey != null && ClientSetup.toggleKey.isPressed()) {
            InvMineConfig.config.get(InvMineConfig.CATEGORY, "enabled", true).set(!InvMineConfig.enabled);
            InvMineConfig.sync();
            InvMineConfig.config.save();
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentTranslation(
                    InvMineConfig.enabled ? "invmine.message.enabled" : "invmine.message.disabled"));
            }
        }
    }

    private static boolean canMoveIn(GuiScreen screen) {
        if (screen instanceof GuiContainer) {
            if (!InvMineConfig.moveInContainers) {
                return false;
            }
        } else {
            if (!InvMineConfig.moveInOtherScreens) {
                return false;
            }
            // Окна ввода текста (чат, табличка, книга, окна модов) почти всегда включают автоповтор.
            if (InvMineConfig.blockWhenKeyRepeatEnabled && Keyboard.areRepeatEventsEnabled()) {
                return false;
            }
        }
        if (isBlacklisted(screen.getClass())) {
            return false;
        }
        if (InvMineConfig.blockWhenTextFieldFocused && TextInputDetector.isTyping(screen)) {
            return false;
        }
        return true;
    }

    /** Совпадение по точному имени класса (включая суперклассы) или по префиксу, оканчивающемуся на '.' или '*'. */
    private static boolean isBlacklisted(Class<?> cls) {
        for (Class<?> c = cls; c != null && c != GuiScreen.class && c != Object.class; c = c.getSuperclass()) {
            String name = c.getName();
            for (String entry : InvMineConfig.blacklistPrefixes) {
                if (name.equals(entry)) {
                    return true;
                }
                if (entry.endsWith(".") || entry.endsWith("*")) {
                    String prefix = entry.endsWith("*") ? entry.substring(0, entry.length() - 1) : entry;
                    if (name.startsWith(prefix)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void syncMovementKeys(GameSettings gs, boolean physical, boolean inGui) {
        set(gs.keyBindForward, physical);
        set(gs.keyBindBack, physical);
        set(gs.keyBindLeft, physical);
        set(gs.keyBindRight, physical);
        set(gs.keyBindJump, physical && (InvMineConfig.allowJump || !inGui));
        set(gs.keyBindSprint, physical && (InvMineConfig.allowSprint || !inGui));
        set(gs.keyBindSneak, physical && (InvMineConfig.allowSneak || !inGui));
    }

    private static void set(KeyBinding kb, boolean usePhysical) {
        boolean down = usePhysical && isPhysicallyDown(kb.getKeyCode());
        try {
            PRESSED.setBoolean(kb, down);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isPhysicallyDown(int keyCode) {
        // Клавиши, назначенные на кнопки мыши (код < 0), в интерфейсе не учитываем —
        // клики там предназначены для предметов.
        return keyCode > 0 && keyCode < Keyboard.KEYBOARD_SIZE && Keyboard.isKeyDown(keyCode);
    }
}
