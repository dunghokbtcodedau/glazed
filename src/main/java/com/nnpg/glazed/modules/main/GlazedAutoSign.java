package com.nnpg.glazed.modules.main;

import com.nnpg.glazed.GlazedAddon;
import com.nnpg.glazed.utils.glazed.AutoFlowCoordinator;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

/* JADX INFO: loaded from: grunt-1.21.4-n-16.1.jar:com/nnpg/glazed/modules/main/GlazedAutoSign.class */
public class GlazedAutoSign extends Module {
    private final SettingGroup sgGeneral;
    private final SettingGroup sgTiming;
    private final SettingGroup sgDebug;
    private final Setting<String> password;
    private final Setting<String> loginCommand;
    private final Setting<Boolean> sendLogin;
    private final Setting<Boolean> openMenu;
    private final Setting<Integer> serverSlot;
    private final Setting<Boolean> disableAfterClick;
    private final Setting<Integer> loginDelay;
    private final Setting<Integer> menuDelay;
    private final Setting<Integer> clickDelay;
    private final Setting<Integer> menuTimeout;
    private final Setting<Boolean> debug;
    private Stage stage;
    private long stageStartedAt;
    private long menuOpenedAt;
    private boolean menuOpened;

    /* JADX INFO: loaded from: grunt-1.21.4-n-16.1.jar:com/nnpg/glazed/modules/main/GlazedAutoSign$Stage.class */
    private enum Stage {
        IDLE,
        WAIT_LOGIN,
        WAIT_MENU_COMMAND,
        WAIT_MENU_SCREEN,
        WAIT_CLICK_SLOT,
        DONE
    }

    public GlazedAutoSign() {
        super(GlazedAddon.CATEGORY, "grunt-auto-sign", "Logs in with a password, opens /menu, then clicks the selected server slot.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.sgTiming = this.settings.createGroup("Timing");
        this.sgDebug = this.settings.createGroup("Debug");
        this.password = this.sgGeneral.add(((StringSetting.Builder) ((StringSetting.Builder) ((StringSetting.Builder) new StringSetting.Builder().name("password")).description("Password used for /login. The value is never printed in debug output.")).defaultValue("")).build());
        this.loginCommand = this.sgGeneral.add(((StringSetting.Builder) ((StringSetting.Builder) ((StringSetting.Builder) new StringSetting.Builder().name("login-command")).description("Login command without slash.")).defaultValue("login")).build());
        this.sendLogin = this.sgGeneral.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder().name("send-login")).description("Send the login command after joining.")).defaultValue(true)).build());
        this.openMenu = this.sgGeneral.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder().name("open-menu")).description("Send /menu after login.")).defaultValue(true)).build());
        this.serverSlot = this.sgGeneral.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("server-slot")).description("Menu slot to click after /menu opens.")).defaultValue(24)).min(0).max(53).sliderMax(53).build());
        this.disableAfterClick = this.sgGeneral.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder().name("disable-after-enter-server")).description("Toggle this module off after the server slot is clicked. Keep this off if you want auto login after kick/reconnect.")).defaultValue(false)).build());
        this.loginDelay = this.sgTiming.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("login-delay-ms")).description("Delay after joining before sending /login.")).defaultValue(1500)).min(0).max(15000).sliderMax(10000).build());
        this.menuDelay = this.sgTiming.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("menu-delay-ms")).description("Delay after login before sending /menu.")).defaultValue(3500)).min(0).max(30000).sliderMax(15000).build());
        this.clickDelay = this.sgTiming.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("click-delay-ms")).description("Delay after the menu GUI opens before clicking the server slot.")).defaultValue(400)).min(0).max(5000).sliderMax(3000).build());
        this.menuTimeout = this.sgTiming.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("menu-timeout-ms")).description("How long to wait for the /menu GUI before retrying /menu.")).defaultValue(8000)).min(1000).max(30000).sliderMax(15000).build());
        this.debug = this.sgDebug.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder().name("debug")).description("Print clear status messages for each step.")).defaultValue(true)).build());
        this.stage = Stage.IDLE;
        this.stageStartedAt = 0L;
        this.menuOpenedAt = 0L;
        this.menuOpened = false;
        this.runInMainMenu = true;
    }

    public void onActivate() {
        reset();
        if (this.mc.player != null && this.mc.world != null) {
            startFlow("module activated while already in game");
        } else {
            debug("Đang chờ vào server để tự đăng nhập.");
        }
    }

    public void onDeactivate() {
        debug("Đã tắt AutoSign.");
        reset();
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        AutoFlowCoordinator.resetHubSignal();
        startFlow("joined game");
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        reset();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (this.mc.player == null || this.mc.world == null || this.mc.interactionManager == null) {
            return;
        }
        long now = System.currentTimeMillis();
        switch (this.stage) {
            case WAIT_LOGIN:
                if (elapsed(now) >= ((Integer) this.loginDelay.get()).intValue()) {
                    sendLoginCommand();
                    setStage(((Boolean) this.openMenu.get()).booleanValue() ? Stage.WAIT_MENU_COMMAND : Stage.DONE);
                }
                break;
            case WAIT_MENU_COMMAND:
                if (elapsed(now) >= ((Integer) this.menuDelay.get()).intValue()) {
                    sendMenuCommand();
                    setStage(Stage.WAIT_MENU_SCREEN);
                }
                break;
            case WAIT_MENU_SCREEN:
                if (this.mc.currentScreen instanceof HandledScreen) {
                    this.menuOpened = true;
                    this.menuOpenedAt = now;
                    setStage(Stage.WAIT_CLICK_SLOT);
                } else if (elapsed(now) >= ((Integer) this.menuTimeout.get()).intValue()) {
                    debug("Menu chưa mở, gửi lại /menu.");
                    sendMenuCommand();
                    this.stageStartedAt = now;
                }
                break;
            case WAIT_CLICK_SLOT:
                Screen ScreenVar = this.mc.currentScreen;
                if (ScreenVar instanceof HandledScreen) {
                    HandledScreen<?> screen = (HandledScreen) ScreenVar;
                    if (now - this.menuOpenedAt >= ((Integer) this.clickDelay.get()).intValue()) {
                        if (!clickServerSlot(screen)) {
                            setStage(Stage.IDLE);
                        } else {
                            setStage(Stage.DONE);
                            AutoFlowCoordinator.markHubEntered();
                            debug("Đã click server slot " + String.valueOf(this.serverSlot.get()) + ". AutoOrder có thể bắt đầu.");
                            if (((Boolean) this.disableAfterClick.get()).booleanValue()) {
                                debug("Tự tắt AutoSign theo setting.");
                                toggle();
                            }
                        }
                    }
                    break;
                } else if (this.menuOpened && elapsed(now) >= ((Integer) this.menuTimeout.get()).intValue()) {
                    debug("Menu screen closed before slot click. Sending /menu again.");
                    this.menuOpened = false;
                    sendMenuCommand();
                    setStage(Stage.WAIT_MENU_SCREEN);
                    break;
                }
                break;
        }
    }

    private void startFlow(String reason) {
        if (((Boolean) this.sendLogin.get()).booleanValue()) {
            if (((String) this.password.get()).isBlank()) {
                warning("Password is empty. Set password first.", new Object[0]);
                debug("Cannot start login flow because password is empty.");
                setStage(Stage.IDLE);
                return;
            } else {
                debug("Chuẩn bị đăng nhập sau " + String.valueOf(this.loginDelay.get()) + "ms.");
                setStage(Stage.WAIT_LOGIN);
                return;
            }
        }
        if (((Boolean) this.openMenu.get()).booleanValue()) {
            debug("Login đang tắt, sẽ mở /menu sau " + String.valueOf(this.menuDelay.get()) + "ms.");
            setStage(Stage.WAIT_MENU_COMMAND);
        } else {
            debug("Login và /menu đều đang tắt.");
            setStage(Stage.DONE);
        }
    }

    private void sendLoginCommand() {
        String command = ((String) this.loginCommand.get()).trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        debug("Gửi /" + command + " <password>.");
        ChatUtils.sendPlayerMsg("/" + command + " " + ((String) this.password.get()));
    }

    private void sendMenuCommand() {
        debug("Gửi /menu.");
        ChatUtils.sendPlayerMsg("/menu");
    }

    private boolean clickServerSlot(HandledScreen<?> screen) {
        ScreenHandler handler = screen.getScreenHandler();
        int slot = ((Integer) this.serverSlot.get()).intValue();
        if (slot < 0 || slot >= handler.slots.size()) {
            error("Slot " + slot + " is outside this menu. Menu has " + handler.slots.size() + " slots.", new Object[0]);
            return false;
        }
        debug("Click slot server " + slot + ".");
        this.mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, this.mc.player);
        return true;
    }

    private long elapsed(long now) {
        return now - this.stageStartedAt;
    }

    private void setStage(Stage next) {
        this.stage = next;
        this.stageStartedAt = System.currentTimeMillis();
    }

    private void reset() {
        this.stage = Stage.IDLE;
        this.stageStartedAt = 0L;
        this.menuOpenedAt = 0L;
        this.menuOpened = false;
    }

    private void debug(String message) {
        if (((Boolean) this.debug.get()).booleanValue()) {
            info("[AutoSign] " + message, new Object[0]);
        }
    }

    public String getInfoString() {
        if (this.stage == Stage.IDLE) {
            return null;
        }
        return this.stage.name();
    }
}
