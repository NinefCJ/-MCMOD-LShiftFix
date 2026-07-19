package com.ninefyu.lshiftfix.gui;

import com.ninefyu.lshiftfix.LShiftFix;
import com.ninefyu.lshiftfix.config.LShiftFixConfig;
import com.ninefyu.lshiftfix.i18n.L10n;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;

import java.io.IOException;

/**
 * In-game config GUI for LShiftFix.
 *
 * <p>All text is localized via {@link L10n} — follows the player's
 * Minecraft language setting (supports zh_CN / en_US / ja_JP / ko_KR).</p>
 */
public class GuiConfigLShiftFix extends GuiScreen {

    private static final int BTN_DEBUG   = 0;
    private static final int BTN_POLLING = 1;
    private static final int BTN_GUARD   = 2;
    private static final int BTN_UPDATE  = 3;
    private static final int BTN_RELOAD  = 4;
    private static final int BTN_SAVE    = 5;
    private static final int BTN_DONE    = 6;

    private final GuiScreen parent;

    public GuiConfigLShiftFix(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        int cx = width / 2;
        int cy = height / 2;
        int btnW = 220;
        int btnH = 20;
        int gap  = 24;

        buttonList.clear();
        buttonList.add(new GuiButton(BTN_DEBUG,   cx - btnW / 2, cy - gap * 2, btnW, btnH,
            toggleLabel(L10n.format(L10n.GUI_DEBUG), LShiftFixConfig.enableDebugLog)));
        buttonList.add(new GuiButton(BTN_POLLING, cx - btnW / 2, cy - gap,     btnW, btnH,
            toggleLabel(L10n.format(L10n.GUI_POLLING), LShiftFixConfig.enableAllKeyPolling)));
        buttonList.add(new GuiButton(BTN_GUARD,   cx - btnW / 2, cy,            btnW, btnH,
            toggleLabel(L10n.format(L10n.GUI_GUARD), LShiftFixConfig.enableGuiImeGuard)));
        buttonList.add(new GuiButton(BTN_UPDATE,  cx - btnW / 2, cy + gap,     btnW, btnH,
            toggleLabel(L10n.format(L10n.GUI_UPDATE_CHECK), LShiftFixConfig.enableUpdateCheck)));
        buttonList.add(new GuiButton(BTN_RELOAD,  cx - btnW / 2, cy + gap * 2, btnW, btnH,
            EnumChatFormatting.YELLOW + L10n.format(L10n.GUI_RELOAD)));
        buttonList.add(new GuiButton(BTN_SAVE,    cx - btnW / 2, cy + gap * 3, btnW, btnH,
            EnumChatFormatting.GREEN + L10n.format(L10n.GUI_SAVE)));
        buttonList.add(new GuiButton(BTN_DONE,    cx - btnW / 2, cy + gap * 4, btnW, btnH,
            L10n.format(L10n.GUI_DONE)));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_DEBUG:
                LShiftFixConfig.enableDebugLog = !LShiftFixConfig.enableDebugLog;
                button.displayString = toggleLabel(L10n.format(L10n.GUI_DEBUG), LShiftFixConfig.enableDebugLog);
                break;
            case BTN_POLLING:
                LShiftFixConfig.enableAllKeyPolling = !LShiftFixConfig.enableAllKeyPolling;
                button.displayString = toggleLabel(L10n.format(L10n.GUI_POLLING), LShiftFixConfig.enableAllKeyPolling);
                break;
            case BTN_GUARD:
                LShiftFixConfig.enableGuiImeGuard = !LShiftFixConfig.enableGuiImeGuard;
                button.displayString = toggleLabel(L10n.format(L10n.GUI_GUARD), LShiftFixConfig.enableGuiImeGuard);
                break;
            case BTN_UPDATE:
                LShiftFixConfig.enableUpdateCheck = !LShiftFixConfig.enableUpdateCheck;
                button.displayString = toggleLabel(L10n.format(L10n.GUI_UPDATE_CHECK), LShiftFixConfig.enableUpdateCheck);
                break;
            case BTN_RELOAD:
                LShiftFixConfig.reload();
                initGui();
                break;
            case BTN_SAVE:
                LShiftFixConfig.save();
                mc.displayGuiScreen(parent);
                break;
            case BTN_DONE:
                mc.displayGuiScreen(parent);
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj,
            EnumChatFormatting.AQUA + L10n.format(L10n.GUI_TITLE),
            width / 2, height / 2 - 70, 0xFFFFFF);
        drawCenteredString(fontRendererObj,
            EnumChatFormatting.GRAY + L10n.format(L10n.GUI_VERSION, LShiftFix.VERSION),
            width / 2, height / 2 - 58, 0xAAAAAA);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private static String toggleLabel(String name, boolean value) {
        String state = value
            ? (EnumChatFormatting.GREEN + L10n.format(L10n.GUI_ON))
            : (EnumChatFormatting.RED + L10n.format(L10n.GUI_OFF));
        return EnumChatFormatting.WHITE + name + ": " + state;
    }
}
