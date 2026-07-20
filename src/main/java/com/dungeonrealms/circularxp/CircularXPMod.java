package com.dungeonrealms.circularxp;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.input.Keyboard;

/**
 * Entry point for the mod. Client-only: this has no business existing on a
 * dedicated server, and Forge will skip loading it there because of
 * clientSideOnly = true.
 */
@Mod(
        modid = CircularXPMod.MODID,
        name = CircularXPMod.NAME,
        version = CircularXPMod.VERSION,
        clientSideOnly = true,
        acceptableRemoteVersions = "*", // lets you join the DR server even though it doesn't have this mod
        guiFactory = "com.dungeonrealms.circularxp.CircularXPGuiFactory"
)
public class CircularXPMod {

    public static final String MODID = "circularxp";
    public static final String NAME = "Circular XP Bar";
    public static final String VERSION = "1.0.0";

    // Unbound by default (KEY_NONE) — set your own key under Controls > Circular XP Bar.
    public static KeyBinding toggleKeyBinding;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.init(event.getSuggestedConfigurationFile());

        toggleKeyBinding = new KeyBinding(
                "key.circularxp.toggle",
                KeyConflictContext.IN_GAME,
                Keyboard.KEY_NONE,
                "key.categories.circularxp"
        );
        ClientRegistry.registerKeyBinding(toggleKeyBinding);

        MinecraftForge.EVENT_BUS.register(new CircularXPRenderer());
    }
}
