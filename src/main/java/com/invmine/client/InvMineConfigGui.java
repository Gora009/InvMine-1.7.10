package com.invmine.client;

import com.invmine.InvMine;
import cpw.mods.fml.client.config.GuiConfig;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;

public class InvMineConfigGui extends GuiConfig {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public InvMineConfigGui(GuiScreen parent) {
        super(parent,
            new ConfigElement(InvMineConfig.config.getCategory(InvMineConfig.CATEGORY)).getChildElements(),
            InvMine.MODID, false, false, InvMine.NAME);
    }
}
