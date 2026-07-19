package com.ninefyu.lshiftfix.command;

import com.ninefyu.lshiftfix.LShiftFix;
import com.ninefyu.lshiftfix.config.LShiftFixConfig;
import com.ninefyu.lshiftfix.i18n.L10n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collections;
import java.util.List;

/**
 * In-game command for hot-reloading LShiftFix config without restarting.
 *
 * <p>All user-facing text is localized via {@link L10n}.</p>
 */
public class CommandLShiftFix extends CommandBase {

    private static final String NAME = "lshiftfix";

    @Override
    public String getCommandName() {
        return NAME;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/lshiftfix <status|reload|debug|polling|guard|update> [on|off]";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.emptyList();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sendStatus(sender);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "status":
                sendStatus(sender);
                break;
            case "reload":
                LShiftFixConfig.reload();
                send(sender, EnumChatFormatting.GREEN + L10n.format(L10n.CMD_RELOAD_SUCCESS));
                sendStatus(sender);
                break;
            case "debug":
                if (args.length < 2) throw new WrongUsageException(L10n.format(L10n.CMD_TOGGLE_USAGE, "debug"));
                toggleDebug(sender, args[1]);
                break;
            case "polling":
                if (args.length < 2) throw new WrongUsageException(L10n.format(L10n.CMD_TOGGLE_USAGE, "polling"));
                togglePolling(sender, args[1]);
                break;
            case "guard":
                if (args.length < 2) throw new WrongUsageException(L10n.format(L10n.CMD_TOGGLE_USAGE, "guard"));
                toggleGuard(sender, args[1]);
                break;
            case "update":
                if (args.length < 2) throw new WrongUsageException(L10n.format(L10n.CMD_TOGGLE_USAGE, "update"));
                toggleUpdate(sender, args[1]);
                break;
            default:
                throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "status", "reload", "debug", "polling", "guard", "update");
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("debug") || sub.equals("polling") || sub.equals("guard") || sub.equals("update")) {
                return getListOfStringsMatchingLastWord(args, "on", "off");
            }
        }
        return Collections.emptyList();
    }

    private void sendStatus(ICommandSender sender) {
        send(sender, EnumChatFormatting.AQUA + L10n.format(L10n.CMD_STATUS_HEADER, LShiftFix.VERSION));
        send(sender, EnumChatFormatting.GRAY + L10n.format(L10n.CMD_STATUS_INIT,    bool(LShiftFix.isModInitialized())));
        send(sender, EnumChatFormatting.GRAY + L10n.format(L10n.CMD_STATUS_DEBUG,   bool(LShiftFixConfig.enableDebugLog)));
        send(sender, EnumChatFormatting.GRAY + L10n.format(L10n.CMD_STATUS_POLLING, bool(LShiftFixConfig.enableAllKeyPolling)));
        send(sender, EnumChatFormatting.GRAY + L10n.format(L10n.CMD_STATUS_GUARD,   bool(LShiftFixConfig.enableGuiImeGuard)));
        send(sender, EnumChatFormatting.GRAY + L10n.format(L10n.CMD_STATUS_UPDATE,  bool(LShiftFixConfig.enableUpdateCheck)));
        send(sender, EnumChatFormatting.GRAY + L10n.format(L10n.CMD_STATUS_COOLDOWN, LShiftFixConfig.debugLogCooldownTicks));
    }

    private void toggleDebug(ICommandSender sender, String arg) {
        boolean old = LShiftFixConfig.enableDebugLog;
        boolean val = parseOnOff(arg);
        LShiftFixConfig.enableDebugLog = val;
        sendToggleResult(sender, L10n.format(L10n.GUI_DEBUG), val, old);
    }

    private void togglePolling(ICommandSender sender, String arg) {
        boolean old = LShiftFixConfig.enableAllKeyPolling;
        boolean val = parseOnOff(arg);
        LShiftFixConfig.enableAllKeyPolling = val;
        sendToggleResult(sender, L10n.format(L10n.GUI_POLLING), val, old);
    }

    private void toggleGuard(ICommandSender sender, String arg) {
        boolean old = LShiftFixConfig.enableGuiImeGuard;
        boolean val = parseOnOff(arg);
        LShiftFixConfig.enableGuiImeGuard = val;
        sendToggleResult(sender, L10n.format(L10n.GUI_GUARD), val, old);
    }

    private void toggleUpdate(ICommandSender sender, String arg) {
        boolean old = LShiftFixConfig.enableUpdateCheck;
        boolean val = parseOnOff(arg);
        LShiftFixConfig.enableUpdateCheck = val;
        sendToggleResult(sender, L10n.format(L10n.GUI_UPDATE_CHECK), val, old);
    }

    private boolean parseOnOff(String arg) {
        if (arg.equalsIgnoreCase("on") || arg.equals("1") || arg.equalsIgnoreCase("true")) {
            return true;
        }
        if (arg.equalsIgnoreCase("off") || arg.equals("0") || arg.equalsIgnoreCase("false")) {
            return false;
        }
        throw new WrongUsageException("Expected 'on' or 'off', got: " + arg);
    }

    private void sendToggleResult(ICommandSender sender, String label, boolean newVal, boolean oldVal) {
        String newStr = newVal
            ? (EnumChatFormatting.GREEN + L10n.format(L10n.CMD_TOGGLE_ON))
            : (EnumChatFormatting.RED + L10n.format(L10n.CMD_TOGGLE_OFF));
        String oldStr = oldVal
            ? L10n.format(L10n.CMD_TOGGLE_ON)
            : L10n.format(L10n.CMD_TOGGLE_OFF);
        send(sender, EnumChatFormatting.GREEN + "[LShiftFix] "
            + L10n.format(L10n.CMD_TOGGLE_CHANGED, label, newStr, oldStr));
    }

    private void send(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(message));
    }

    private String bool(boolean v) {
        return v
            ? (EnumChatFormatting.GREEN + L10n.format(L10n.CMD_TOGGLE_ON))
            : (EnumChatFormatting.RED + L10n.format(L10n.CMD_TOGGLE_OFF));
    }
}
