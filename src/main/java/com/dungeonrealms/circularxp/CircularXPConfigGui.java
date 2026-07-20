package com.dungeonrealms.circularxp;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.List;

public class CircularXPConfigGui extends GuiConfig {

    public CircularXPConfigGui(GuiScreen parent) {
        super(parent, getConfigElements(), CircularXPMod.MODID, false, false,
                "Circular XP Bar Settings");
    }

    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        list.addAll(new ConfigElement(ModConfig.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements());
        return list;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        // Re-sync our cached static fields from whatever was just edited/saved.
        ModConfig.load();
    }
}
