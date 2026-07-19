package com.ninefyu.lshiftfix.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

/**
 * Forge Mods-menu GUI factory for LShiftFix.
 *
 * <p>Registers a Config button in the Mods → LShiftFix entry. Clicking it
 * opens {@link GuiConfigLShiftFix}.</p>
 */
public class GuiFactoryLShiftFix implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {
        // No-op. Resources can be cached here if needed.
    }

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return GuiConfigLShiftFix.class;
    }

    @Override
    public java.util.Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }
}
