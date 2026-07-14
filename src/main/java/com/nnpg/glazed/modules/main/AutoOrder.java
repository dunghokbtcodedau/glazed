package com.nnpg.glazed.modules.main;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import com.nnpg.glazed.GlazedAddon;
import com.nnpg.glazed.utils.glazed.AutoFlowCoordinator;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType; 
import java.lang.runtime.ObjectMethods;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerInteractionManager; // ClientPlayerInteractionManager
import net.minecraft.screen.ScreenHandler;                          // ScreenHandler
import net.minecraft.screen.slot.SlotActionType;                    // SlotActionType
import net.minecraft.screen.slot.Slot;                              // Slot
import net.minecraft.item.Item;                                     // Item
import net.minecraft.item.ItemStack;                                // ItemStack
import net.minecraft.item.Items;                                    // Items
import net.minecraft.enchantment.EnchantmentHelper;                 // EnchantmentHelper
import net.minecraft.util.math.BlockPos;                            // BlockPos
import net.minecraft.util.math.Direction;                           // Direction
import net.minecraft.client.gui.screen.Screen;                      // Screen
import net.minecraft.util.math.Vec3d;                               // Vec3d
import net.minecraft.block.SpawnerBlock;                            // SpawnerBlock
import net.minecraft.text.Text;                                     // Text
import net.minecraft.util.math.MathHelper;                          // MathHelper
import net.minecraft.util.hit.BlockHitResult;                       // BlockHitResult
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen; // GenericContainerScreen

/* JADX INFO: loaded from: grunt-1.21.4-n-16.1.jar:com/nnpg/glazed/modules/main/AutoOrder.class */
public class AutoOrder extends Module {
    private final SettingGroup sgGeneral;
    private final SettingGroup sgTarget;
    private final SettingGroup sgSpawner;
    private final SettingGroup sgOrder;
    private final SettingGroup sgFlow;
    private final SettingGroup sgRender;
    private final Setting<String> targetItemName;
    private final Setting<Item> targetItem;
    private final Setting<List<String>> spawnerItems;
    private final Setting<Integer> actionDelay;
    private final Setting<Boolean> debug;
    private final Setting<Integer> moveBatchSize;
    private final Setting<Boolean> waitForAutoSign;
    private final Setting<Integer> startAfterSignDelay;
    private final Setting<Integer> spawnerRadius;
    private final Setting<Boolean> rotateWhileSearching;
    private final Setting<Boolean> rotateToSpawner;
    private final Setting<Integer> rotateSpeed;
    private final Setting<Integer> collectSlot;
    private final Setting<Integer> closeAfterCollectTicks;
    private final Setting<Integer> spawnerInteractRange;
    private final Setting<Integer> spawnerRestockWaitSeconds;
    private final Setting<Integer> switchSpawnerAfterCycles;
    private final Setting<String> orderCommand;
    private final Setting<Integer> maxOrderSlots;
    private final Setting<Integer> confirmSlot;
    private final Setting<String> sellCommand;
    private final Setting<Boolean> showSpawnerEsp;
    private final Setting<SettingColor> spawnerEspColor;
    private static final Pattern PRICE_PATTERN = Pattern.compile("(?:\\$|price|gia|giÃ¡)\\s*[:\\-]?\\s*\\$?\\s*([0-9][0-9,._kKmMbB]*)", 2);
    private static final Pattern TRUE_PRICE_PATTERN = Pattern.compile("\\$\\s*([0-9][0-9,._]*\\s*[kKmMbB]?)", 2);
    private static final Pattern ANY_PRICE_NUMBER_PATTERN = Pattern.compile("([0-9][0-9,._]*\\s*[kKmMbB]?)");
    private Stage stage;
    private int ticks;
    private int inventorySlot;
    private int cycles;
    private long startReadyAt;
    private long spawnerRestockUntil;
    private boolean waitingLogged;
    private boolean waitingForSpawnerGui;
    private boolean spawnerGuiReady;
    private int closeOrderClicks;
    private int closeSellClicks;
    private int orderGuiWaitAttempts;
    private int confirmGuiWaitAttempts;
    private int sellGuiWaitAttempts;
    private int openSpawnerLookTicks;
    private int openSpawnerAttempts;
    private int openSpawnerRecoveries;
    private int activeSpawnerIndex;
    private int cyclesOnActiveSpawner;
    private long stageStartedAt;
    private long lastWatchdogLogAt;
    private BlockPos targetSpawner;
    private BlockPos lastSpawner;
    private OrderCandidate selectedOrder;
    private final List<OrderCandidate> rejectedOrders;
    private final Map<BlockPos, Long> spawnerRestockUntilByPos;
    private String currentTarget;

    /* JADX INFO: loaded from: grunt-1.21.4-n-16.1.jar:com/nnpg/glazed/modules/main/AutoOrder$Stage.class */
    private enum Stage {
        IDLE,
        WAIT_FOR_AUTOSIGN,
        FIND_SPAWNER,
        OPEN_SPAWNER,
        COLLECT_SPAWNER,
        WAIT_AFTER_COLLECT,
        WAIT_SPAWNER_RESTOCK,
        CHECK_INVENTORY,
        OPEN_SELL,
        DEPOSIT_SELL,
        CLOSE_SELL,
        OPEN_ORDER,
        SELECT_BEST_ORDER,
        DEPOSIT_ITEMS,
        WAIT_CONFIRM,
        CONFIRM_ORDER,
        CLOSE_ORDER,
        WAIT_NEXT_COLLECT
    }

    public AutoOrder() {
        super(GlazedAddon.CATEGORY, "auto-order", "Collects spawner loot, then sells it to the highest priced order when inventory is full.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.sgTarget = this.settings.createGroup("Target");
        this.sgSpawner = this.settings.createGroup("Spawner");
        this.sgOrder = this.settings.createGroup("Order");
        this.sgFlow = this.settings.createGroup("AutoSign Flow");
        this.sgRender = this.settings.createGroup("Spawner ESP");
        this.targetItemName = this.sgTarget.add(((StringSetting.Builder) ((StringSetting.Builder) ((StringSetting.Builder) new StringSetting.Builder().name("item-name")).description("Item name used for /order and inventory matching.")).defaultValue("porkchop")).build());
        this.targetItem = this.sgTarget.add(((ItemSetting.Builder) ((ItemSetting.Builder) ((ItemSetting.Builder) new ItemSetting.Builder().name("target-item")).description("Exact target item. Leave as air to match by item-name.")).defaultValue(Items.AIR)).build());
        this.spawnerItems = this.sgTarget.add(((StringListSetting.Builder) ((StringListSetting.Builder) ((StringListSetting.Builder) new StringListSetting.Builder().name("spawner-items")).description("Order item for each spawner index. Example: porkchop, bones.")).defaultValue(Arrays.asList("porkchop"))).build());
        this.actionDelay = this.sgGeneral.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("action-delay-ticks")).description("Delay between actions.")).defaultValue(12)).min(1).max(100).sliderMax(40).build());
        this.debug = this.sgGeneral.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder().name("debug")).description("Hiển thị debug chi tiết khi cần kiểm tra lỗi.")).defaultValue(false)).build());
        this.moveBatchSize = this.sgGeneral.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("move-batch-size")).description("Số stack item được giao trong một tick khi đang ở GUI order.")).defaultValue(6)).min(1).max(12).sliderMax(12).build());
        this.waitForAutoSign = this.sgFlow.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder().name("wait-for-autosign")).description("Chờ AutoSign vào hub/server rồi AutoOrder mới bắt đầu.")).defaultValue(true)).build());
        SettingGroup settingGroup = this.sgFlow;
        IntSetting.Builder builderSliderMax = ((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("start-after-sign-delay-ms")).description("Delay sau khi AutoSign click server slot rồi mới bắt đầu AutoOrder.")).defaultValue(3500)).min(0).max(30000).sliderMax(15000);
        Setting<Boolean> setting = this.waitForAutoSign;
        Objects.requireNonNull(setting);
        this.startAfterSignDelay = settingGroup.add(((IntSetting.Builder) builderSliderMax.visible(setting::get)).build());
        this.spawnerRadius = this.sgSpawner.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("spawner-radius")).description("Radius to find nearby spawners.")).defaultValue(6)).min(1).max(12).sliderMax(10).build());
        this.rotateWhileSearching = this.sgSpawner.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder().name("rotate-while-searching")).description("Tự quay 360 độ khi chưa tìm thấy spawner.")).defaultValue(true)).build());
        this.rotateToSpawner = this.sgSpawner.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder().name("rotate-to-spawner")).description("Tự xoay góc nhìn vào spawner mục tiêu trước khi mở.")).defaultValue(true)).build());
        this.rotateSpeed = this.sgSpawner.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("rotate-speed")).description("Tốc độ xoay khi scan hoặc target spawner.")).defaultValue(18)).min(1).max(90).sliderMax(45).build());
        this.collectSlot = this.sgSpawner.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("collect-slot")).description("Spawner GUI slot to click for collecting items.")).defaultValue(48)).min(0).max(53).sliderMax(53).build());
        this.closeAfterCollectTicks = this.sgSpawner.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("close-after-collect-ticks")).description("Ticks to wait after collecting from spawner before checking inventory.")).defaultValue(10)).min(1).max(100).sliderMax(40).build());
        this.spawnerInteractRange = this.sgSpawner.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("spawner-interact-range")).description("Maximum distance used when choosing a spawner to open.")).defaultValue(4)).min(2).max(6).sliderMax(6).build());
        this.spawnerRestockWaitSeconds = this.sgSpawner.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("spawner-restock-wait-seconds")).description("Wait time when the spawner collect slot is empty.")).defaultValue(90)).min(10).max(180).sliderMax(180).build());
        this.switchSpawnerAfterCycles = this.sgSpawner.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("switch-spawner-after-cycles")).description("Number of completed order cycles before switching to the next nearby spawner.")).defaultValue(10)).min(1).max(100).sliderMax(30).build());
        this.orderCommand = this.sgOrder.add(((StringSetting.Builder) ((StringSetting.Builder) ((StringSetting.Builder) new StringSetting.Builder().name("order-command")).description("Order command without slash.")).defaultValue("order")).build());
        this.maxOrderSlots = this.sgOrder.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("max-order-slots")).description("How many top GUI slots to scan for orders.")).defaultValue(45)).min(1).max(54).sliderMax(54).build());
        this.confirmSlot = this.sgOrder.add(((IntSetting.Builder) ((IntSetting.Builder) ((IntSetting.Builder) new IntSetting.Builder().name("confirm-slot")).description("Confirm GUI slot.")).defaultValue(16)).min(0).max(53).sliderMax(53).build());
        this.sellCommand = this.sgOrder.add(((StringSetting.Builder) ((StringSetting.Builder) ((StringSetting.Builder) new StringSetting.Builder().name("sell-command")).description("Sell command without slash. Used to sell arrows from spawner.")).defaultValue("sell")).build());
        this.showSpawnerEsp = this.sgRender.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) new BoolSetting.Builder().name("show-spawner-esp")).description("Vẽ ESP quanh spawner đang target.")).defaultValue(true)).build());
        SettingGroup settingGroup2 = this.sgRender;
        ColorSetting.Builder builderDefaultValue = ((ColorSetting.Builder) ((ColorSetting.Builder) new ColorSetting.Builder().name("spawner-esp-color")).description("Màu ESP của spawner target.")).defaultValue(new SettingColor(0, 255, 120, 120));
        Setting<Boolean> setting2 = this.showSpawnerEsp;
        Objects.requireNonNull(setting2);
        this.spawnerEspColor = settingGroup2.add(((ColorSetting.Builder) builderDefaultValue.visible(setting2::get)).build());
        this.stage = Stage.IDLE;
        this.ticks = 0;
        this.inventorySlot = 0;
        this.cycles = 0;
        this.startReadyAt = 0L;
        this.spawnerRestockUntil = 0L;
        this.waitingLogged = false;
        this.waitingForSpawnerGui = false;
        this.spawnerGuiReady = false;
        this.closeOrderClicks = 0;
        this.closeSellClicks = 0;
        this.orderGuiWaitAttempts = 0;
        this.confirmGuiWaitAttempts = 0;
        this.sellGuiWaitAttempts = 0;
        this.openSpawnerLookTicks = 0;
        this.openSpawnerAttempts = 0;
        this.openSpawnerRecoveries = 0;
        this.activeSpawnerIndex = 0;
        this.cyclesOnActiveSpawner = 0;
        this.stageStartedAt = 0L;
        this.lastWatchdogLogAt = 0L;
        this.rejectedOrders = new ArrayList();
        this.spawnerRestockUntilByPos = new HashMap();
        this.currentTarget = "";
        this.runInMainMenu = true;
    }

    public void onActivate() {
        this.currentTarget = getConfiguredTargetForSpawner(0);
        this.cycles = 0;
        this.inventorySlot = 0;
        this.targetSpawner = null;
        this.lastSpawner = null;
        this.activeSpawnerIndex = 0;
        this.cyclesOnActiveSpawner = 0;
        this.startReadyAt = 0L;
        this.spawnerRestockUntil = 0L;
        this.waitingLogged = false;
        resetSpawnerGuiState();
        this.selectedOrder = null;
        this.rejectedOrders.clear();
        this.spawnerRestockUntilByPos.clear();
        this.closeOrderClicks = 0;
        this.closeSellClicks = 0;
        this.orderGuiWaitAttempts = 0;
        this.confirmGuiWaitAttempts = 0;
        this.sellGuiWaitAttempts = 0;
        if (this.mc.player == null || this.mc.world == null) {
            setStage(Stage.WAIT_FOR_AUTOSIGN);
            log("Ä\u0090Ã£ báº\u00adt ngoÃ i game. Ä\u0090ang chá»\u009d vÃ o server/hub.");
        } else {
            prepareStart();
        }
    }

    public void onDeactivate() {
        if (this.mc.player != null && this.mc.currentScreen != null) {
            this.mc.player.closeHandledScreen();
        }
        this.stage = Stage.IDLE;
        this.ticks = 0;
        this.inventorySlot = 0;
        this.targetSpawner = null;
        this.lastSpawner = null;
        this.activeSpawnerIndex = 0;
        this.cyclesOnActiveSpawner = 0;
        this.startReadyAt = 0L;
        this.spawnerRestockUntil = 0L;
        this.waitingLogged = false;
        resetSpawnerGuiState();
        this.selectedOrder = null;
        this.rejectedOrders.clear();
        this.spawnerRestockUntilByPos.clear();
        this.closeOrderClicks = 0;
        this.closeSellClicks = 0;
        this.orderGuiWaitAttempts = 0;
        this.confirmGuiWaitAttempts = 0;
        this.sellGuiWaitAttempts = 0;
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        if (((Boolean) this.waitForAutoSign.get()).booleanValue()) {
            setStage(Stage.WAIT_FOR_AUTOSIGN);
            this.startReadyAt = 0L;
            this.waitingLogged = false;
            resetSpawnerGuiState();
            log("Ä\u0090Ã£ vÃ o server. Chá»\u009d AutoSign Ä‘Äƒng nháº\u00adp vÃ  vÃ o hub/server.");
            return;
        }
        prepareStart();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        this.stage = Stage.WAIT_FOR_AUTOSIGN;
        this.ticks = 0;
        this.startReadyAt = 0L;
        this.spawnerRestockUntil = 0L;
        this.targetSpawner = null;
        this.lastSpawner = null;
        this.activeSpawnerIndex = 0;
        this.cyclesOnActiveSpawner = 0;
        this.waitingLogged = false;
        resetSpawnerGuiState();
        this.selectedOrder = null;
        this.rejectedOrders.clear();
        this.spawnerRestockUntilByPos.clear();
        this.closeOrderClicks = 0;
        this.closeSellClicks = 0;
        this.orderGuiWaitAttempts = 0;
        this.confirmGuiWaitAttempts = 0;
        this.sellGuiWaitAttempts = 0;
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (this.selectedOrder == null || !isSellingStage()) {
            return;
        }
        String message = event.getMessage().getString();
        if (isOrderAmountExceededMessage(message)) {
            this.rejectedOrders.add(this.selectedOrder);
            logImportant("Order của " + this.selectedOrder.customerName + " đã hết số lượng. Tìm order khác.");
            this.selectedOrder = null;
            this.inventorySlot = 0;
            this.closeOrderClicks = 0;
            this.closeSellClicks = 0;
            this.orderGuiWaitAttempts = 0;
            this.confirmGuiWaitAttempts = 0;
            this.sellGuiWaitAttempts = 0;
            resetSpawnerGuiState();
            if (this.mc.player != null && this.mc.currentScreen != null) {
                this.mc.player.closeHandledScreen();
            }
            setStage(Stage.OPEN_ORDER);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (this.mc.player == null || this.mc.world == null || this.mc.interactionManager == null) {
            return;
        }
        watchdog();
        if (this.stage == Stage.WAIT_AFTER_COLLECT) {
            this.ticks++;
            if (this.ticks >= ((Integer) this.closeAfterCollectTicks.get()).intValue()) {
                this.ticks = 0;
                waitAfterCollect();
            }
            return;
        }
        if (this.stage == Stage.WAIT_SPAWNER_RESTOCK) {
            waitSpawnerRestock();
            return;
        }
        this.ticks++;
        if (this.ticks < Math.max(((Integer) this.actionDelay.get()).intValue(), 8)) {
            return;
        }
        this.ticks = 0;
        switch (this.stage) {
            case WAIT_FOR_AUTOSIGN:
                waitForAutoSignSignal();
                break;
            case FIND_SPAWNER:
                findSpawner();
                break;
            case OPEN_SPAWNER:
                openSpawner();
                break;
            case COLLECT_SPAWNER:
                collectFromSpawner();
                break;
            case WAIT_AFTER_COLLECT:
                waitAfterCollect();
                break;
            case WAIT_SPAWNER_RESTOCK:
                waitSpawnerRestock();
                break;
            case CHECK_INVENTORY:
                checkInventory();
                break;
            case OPEN_SELL:
                openSell();
                break;
            case DEPOSIT_SELL:
                depositSellItems();
                break;
            case CLOSE_SELL:
                closeSell();
                break;
            case OPEN_ORDER:
                openOrder();
                break;
            case SELECT_BEST_ORDER:
                selectBestOrder();
                break;
            case DEPOSIT_ITEMS:
                depositItems();
                break;
            case WAIT_CONFIRM:
                waitConfirm();
                break;
            case CONFIRM_ORDER:
                confirmOrder();
                break;
            case CLOSE_ORDER:
                closeOrder();
                break;
        }
    }

    private void waitForAutoSignSignal() {
        if (!((Boolean) this.waitForAutoSign.get()).booleanValue()) {
            prepareStart();
            return;
        }
        if (!this.waitingLogged) {
            log("Ä\u0090ang chá»\u009d AutoSign vÃ o hub/server...");
            this.waitingLogged = true;
        }
        if (AutoFlowCoordinator.wasHubEnteredWithin(120000L)) {
            if (this.startReadyAt == 0) {
                this.startReadyAt = System.currentTimeMillis() + ((long) ((Integer) this.startAfterSignDelay.get()).intValue());
                log("Ä\u0090Ã£ nháº\u00adn tÃ\u00adn hiá»‡u tá»« AutoSign. AutoOrder sáº½ báº¯t Ä‘áº§u sau " + String.valueOf(this.startAfterSignDelay.get()) + "ms.");
            }
            if (System.currentTimeMillis() >= this.startReadyAt) {
                prepareStart();
            }
        }
    }

    private void prepareStart() {
        if (this.mc.player == null || this.mc.world == null) {
            setStage(Stage.WAIT_FOR_AUTOSIGN);
            return;
        }
        this.inventorySlot = 0;
        this.targetSpawner = null;
        this.lastSpawner = null;
        this.activeSpawnerIndex = 0;
        this.cyclesOnActiveSpawner = 0;
        this.currentTarget = getConfiguredTargetForSpawner(this.activeSpawnerIndex);
        this.spawnerRestockUntil = 0L;
        this.waitingLogged = false;
        resetSpawnerGuiState();
        this.selectedOrder = null;
        this.rejectedOrders.clear();
        this.spawnerRestockUntilByPos.clear();
        this.closeSellClicks = 0;
        this.orderGuiWaitAttempts = 0;
        this.confirmGuiWaitAttempts = 0;
        this.sellGuiWaitAttempts = 0;
        setStage(Stage.FIND_SPAWNER);
        log("Báº¯t Ä‘áº§u AutoOrder. Item má»¥c tiÃªu: " + getTargetLabel() + ".");
    }

    private void findSpawner() {
        this.targetSpawner = findNearbySpawner();
        if (this.targetSpawner == null) {
            rotateSearch();
            return;
        }
        log("Ä\u0090Ã£ tÃ¬m tháº¥y spawner gáº§n nháº¥t táº¡i " + this.targetSpawner.toShortString() + ".");
        this.lastSpawner = this.targetSpawner;
        setStage(Stage.OPEN_SPAWNER);
    }

    private void openSpawner() {
        if (this.mc.currentScreen instanceof GenericContainerScreen) {
            if (this.waitingForSpawnerGui) {
                this.waitingForSpawnerGui = false;
                this.spawnerGuiReady = true;
                this.openSpawnerLookTicks = 0;
                this.openSpawnerAttempts = 0;
                this.openSpawnerRecoveries = 0;
                setStage(Stage.COLLECT_SPAWNER);
                return;
            }
            this.mc.player.closeHandledScreen();
            return;
        }
        if (this.targetSpawner == null) {
            resetSpawnerGuiState();
            setStage(Stage.FIND_SPAWNER);
            return;
        }
        if (!canReachSpawner(this.targetSpawner)) {
            logImportant("Spawner ngoài tầm click: " + this.targetSpawner.toShortString() + ". Đang tìm spawner gần hơn.");
            this.targetSpawner = null;
            resetSpawnerGuiState();
            setStage(Stage.FIND_SPAWNER);
            return;
        }
        rotateToward(this.targetSpawner);
        if (this.waitingForSpawnerGui) {
            this.openSpawnerAttempts++;
            if (this.openSpawnerAttempts < 4) {
                return;
            }
            this.waitingForSpawnerGui = false;
            this.openSpawnerAttempts = 0;
        }
        if (this.openSpawnerLookTicks < 3) {
            this.openSpawnerLookTicks++;
            return;
        }
        BlockHitResult hitResult = makeSpawnerHitResult(this.targetSpawner);
        this.waitingForSpawnerGui = true;
        this.spawnerGuiReady = false;
        this.openSpawnerLookTicks = 0;
        this.mc.interactionManager.interactBlock(this.mc.player, Hand.MAIN_HAND, hitResult);
        this.mc.player.swingHand(Hand.MAIN_HAND);
    }

    private BlockHitResult makeSpawnerHitResult(BlockPos pos) {
        BlockHitResult BlockHitResultVar = (BlockHitResult) this.mc.crosshairTarget;
        if (BlockHitResultVar instanceof BlockHitResult) {
            BlockHitResult hitResult = BlockHitResultVar;
            if (hitResult.getBlockPos().equals(pos) && hitResult.getType() == HitResult.Type.BLOCK) {
                return hitResult;
            }
        }
        return new BlockHitResult(Vec3d.ofCenter(pos), getClosestSpawnerSide(pos), pos, false);
    }

    private Direction getClosestSpawnerSide(BlockPos pos) {
        Vec3d center = Vec3d.ofCenter(pos);
        Vec3d eyes = this.mc.player.getEyePos();
        double dx = eyes.x - center.x;
        double dy = eyes.y - center.y;
        double dz = eyes.z - center.z;
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);
        return (ay < ax || ay < az) ? ax >= az ? dx > 0.0d ? Direction.EAST : Direction.WEST : dz > 0.0d ? Direction.NORTH : Direction.SOUTH : dy > 0.0d ? Direction.UP : Direction.DOWN;
    }

    private void rotateSearch() {
        if (!((Boolean) this.rotateWhileSearching.get()).booleanValue() || this.mc.player == null) {
            return;
        }
        this.mc.player.setYaw(this.mc.player.getYaw() + ((Integer) this.rotateSpeed.get()).intValue());
    }

    private void rotateToward(BlockPos pos) {
        if (!((Boolean) this.rotateToSpawner.get()).booleanValue() || this.mc.player == null || pos == null) {
            return;
        }
        Vec3d eyes = this.mc.player.getEyePos();
        Vec3d target = Vec3d.ofCenter(pos);
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double horizontal = Math.sqrt((dx * dx) + (dz * dz));
        float targetYaw = ((float) Math.toDegrees(Math.atan2(dz, dx))) - 90.0f;
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
        float yawDelta = MathHelper.wrapDegrees(targetYaw - this.mc.player.getYaw());
        float pitchDelta = targetPitch - this.mc.player.getPitch();
        float speed = ((Integer) this.rotateSpeed.get()).intValue();
        this.mc.player.setYaw(this.mc.player.getYaw() + MathHelper.clamp(yawDelta, -speed, speed));
        this.mc.player.setPitch(MathHelper.clamp(this.mc.player.getPitch() + MathHelper.clamp(pitchDelta, -speed, speed), -90.0f, 90.0f));
    }

    private void collectFromSpawner() {
        //(GenericContainerScreen) this.mc.currentScreen instanceof GenericContainerScreen ? (GenericContainerScreen);
        if (!(this.mc.currentScreen instanceof GenericContainerScreen)) {
            this.spawnerGuiReady = false;
            setStage(Stage.OPEN_SPAWNER);
            return;
        }
        GenericContainerScreen screen = (GenericContainerScreen) this.mc.currentScreen;
        if (!this.spawnerGuiReady) {
            this.mc.player.closeHandledScreen();
            setStage(Stage.OPEN_SPAWNER);
            return;
        }
        ScreenHandler handler = screen.getScreenHandler();
        int slot = ((Integer) this.collectSlot.get()).intValue();
        if (slot >= handler.slots.size()) {
            logImportant("GUI spawner không đúng hoặc slot " + slot + " không tồn tại. Đóng GUI và tìm spawner lại.");
            if (this.mc.currentScreen != null) {
                this.mc.player.closeHandledScreen();
            }
            this.targetSpawner = null;
            resetSpawnerGuiState();
            setStage(Stage.FIND_SPAWNER);
            return;
        }
        ItemStack collectStack = handler.getSlot(slot).getStack();
        if (!isSpawnerCollectReady(collectStack)) {
            markSpawnerRestocking(this.targetSpawner);
            this.spawnerGuiReady = false;
            this.waitingForSpawnerGui = false;
            if (this.mc.currentScreen != null) {
                this.mc.player.closeHandledScreen();
            }
            logImportant("Spawner chưa đủ item (" + getStackCountLabel(collectStack) + "). Bỏ qua spawner này và farm spawner khác.");
            resetSpawnerGuiState();
            this.targetSpawner = selectNextAvailableSpawner();
            if (this.targetSpawner != null) {
                this.lastSpawner = this.targetSpawner;
                setStage(Stage.OPEN_SPAWNER);
                return;
            } else {
                this.spawnerRestockUntil = getEarliestSpawnerRestockUntil();
                setStage(Stage.WAIT_SPAWNER_RESTOCK);
                return;
            }
        }
        this.mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, this.mc.player);
        this.lastSpawner = this.targetSpawner;
        this.spawnerGuiReady = false;
        setStage(Stage.WAIT_AFTER_COLLECT);
    }

    private void waitSpawnerRestock() {
        if (this.mc.currentScreen != null) {
            this.mc.player.closeHandledScreen();
        }
        BlockPos next = selectNextAvailableSpawner();
        if (next != null) {
            this.spawnerRestockUntil = 0L;
            this.targetSpawner = next;
            this.lastSpawner = next;
            setStage(Stage.OPEN_SPAWNER);
            return;
        }
        if (System.currentTimeMillis() < this.spawnerRestockUntil) {
            rotateSearch();
            return;
        }
        this.spawnerRestockUntil = 0L;
        resetSpawnerGuiState();
        this.targetSpawner = selectActiveSpawner();
        if (this.targetSpawner != null && (this.mc.world.getBlockState(this.targetSpawner).getBlock() instanceof SpawnerBlock)) {
            this.lastSpawner = this.targetSpawner;
            setStage(Stage.OPEN_SPAWNER);
        } else {
            this.targetSpawner = null;
            setStage(Stage.FIND_SPAWNER);
        }
    }

    private void waitAfterCollect() {
        if (this.mc.currentScreen != null) {
            this.mc.player.closeHandledScreen();
        }
        setStage(Stage.CHECK_INVENTORY);
    }

    private void checkInventory() {
        updateCurrentTargetFromInventory();
        int targetStacks = countTargetStacks();
        int arrowStacks = countArrowStacks();
        if (arrowStacks > 0) {
            logImportant("Phát hiện " + arrowStacks + " stack arrow. Chuyển sang /sell.");
            setStage(Stage.OPEN_SELL);
        } else if (targetStacks > 0) {
            log("TÃºi Ä‘á»“ Ä‘Ã£ Ä‘áº§y vá»›i " + targetStacks + " stack item má»¥c tiÃªu. Chuyá»ƒn sang giao order.");
            setStage(Stage.OPEN_ORDER);
        } else {
            setStage(Stage.FIND_SPAWNER);
        }
    }

    private void openSell() {
        resetSpawnerGuiState();
        this.sellGuiWaitAttempts = 0;
        if (this.mc.currentScreen != null) {
            this.mc.player.closeHandledScreen();
            return;
        }
        String command = ((String) this.sellCommand.get()).trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        log("Gửi lệnh /" + command + " để bán arrow.");
        ChatUtils.sendPlayerMsg("/" + command);
        this.inventorySlot = 0;
        setStage(Stage.DEPOSIT_SELL);
    }

    private void depositSellItems() {
        int handlerSlot;
        resetSpawnerGuiState();
        if (!(this.mc.currentScreen instanceof GenericContainerScreen)) {
            this.sellGuiWaitAttempts++;
            if (this.sellGuiWaitAttempts >= 8) {
                this.sellGuiWaitAttempts = 0;
                setStage(Stage.OPEN_SELL);
                return;
            }
            return;
        }
        GenericContainerScreen screen = (GenericContainerScreen) this.mc.currentScreen;
        this.sellGuiWaitAttempts = 0;
        ScreenHandler handler = screen.getScreenHandler();
        int moved = 0;
        int batchLimit = Math.min(((Integer) this.moveBatchSize.get()).intValue(), 6);
        while (this.inventorySlot < this.mc.player.getInventory().size() && moved < batchLimit) {
            int current = this.inventorySlot;
            this.inventorySlot = current + 1;
            ItemStack stack = this.mc.player.getInventory().getStack(current);
            if (!stack.isEmpty() && isArrowItem(stack) && (handlerSlot = inventoryIndexToHandlerSlot(handler, current)) >= 0) {
                this.mc.interactionManager.clickSlot(handler.syncId, handlerSlot, 0, SlotActionType.QUICK_MOVE, this.mc.player);
                moved++;
            }
        }
        if (this.inventorySlot < this.mc.player.getInventory().size()) {
            return;
        }
        logImportant("Đã chuyển arrow vào GUI sell.");
        this.closeSellClicks = 0;
        setStage(Stage.CLOSE_SELL);
    }

    private void closeSell() {
        resetSpawnerGuiState();
        if (this.closeSellClicks < 2) {
            if (this.mc.currentScreen != null) {
                this.mc.player.closeHandledScreen();
            }
            this.closeSellClicks++;
        } else {
            if (this.mc.currentScreen != null) {
                this.mc.player.closeHandledScreen();
                return;
            }
            this.closeSellClicks = 0;
            this.sellGuiWaitAttempts = 0;
            this.inventorySlot = 0;
            log("Hoàn tất sell arrow. Quay lại kiểm tra inventory.");
            setStage(Stage.CHECK_INVENTORY);
        }
    }

    private void openOrder() {
        resetSpawnerGuiState();
        updateCurrentTargetFromInventory();
        this.selectedOrder = null;
        this.orderGuiWaitAttempts = 0;
        this.confirmGuiWaitAttempts = 0;
        if (this.mc.currentScreen != null) {
            this.mc.player.closeHandledScreen();
            return;
        }
        String command = ((String) this.orderCommand.get()).trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        log("Gá»\u00adi lá»‡nh /" + command + " " + this.currentTarget + ".");
        ChatUtils.sendPlayerMsg("/" + command + " " + this.currentTarget);
        setStage(Stage.SELECT_BEST_ORDER);
    }

    private void selectBestOrder() {
        resetSpawnerGuiState();
        if (!(this.mc.currentScreen instanceof GenericContainerScreen)) {
            this.orderGuiWaitAttempts++;
            if (this.orderGuiWaitAttempts >= 8) {
                this.orderGuiWaitAttempts = 0;
                setStage(Stage.OPEN_ORDER);
                return;
            }
            return;
        }
        GenericContainerScreen screen = (GenericContainerScreen) this.mc.currentScreen;
        this.orderGuiWaitAttempts = 0;
        ScreenHandler handler = screen.getScreenHandler();
        OrderCandidate best = findBestOrder(handler);
        if (best == null) {
            log("KhÃ´ng tÃ¬m tháº¥y order phÃ¹ há»£p. Quay láº¡i láº¥y item.");
            this.mc.player.closeHandledScreen();
            setStage(Stage.OPEN_SPAWNER);
        } else {
            this.selectedOrder = best;
            logImportant("Chọn order giá cao nhất: slot " + best.slotId + " | giao cho: " + best.customerName + " | giá mỗi item: $" + formatPrice(best.price));
            this.mc.interactionManager.clickSlot(handler.syncId, best.slotId, 0, SlotActionType.PICKUP, this.mc.player);
            this.inventorySlot = 0;
            setStage(Stage.DEPOSIT_ITEMS);
        }
    }

    private void depositItems() {
        int handlerSlot;
        resetSpawnerGuiState();
        if (!(this.mc.currentScreen instanceof GenericContainerScreen)) {
            return;
        }
        GenericContainerScreen screen = (GenericContainerScreen) this.mc.currentScreen;
        ScreenHandler handler = screen.getScreenHandler();
        int moved = 0;
        int batchLimit = Math.min(((Integer) this.moveBatchSize.get()).intValue(), 6);
        while (this.inventorySlot < this.mc.player.getInventory().size() && moved < batchLimit) {
            int current = this.inventorySlot;
            this.inventorySlot = current + 1;
            ItemStack stack = this.mc.player.getInventory().getStack(current);
            if (!stack.isEmpty() && isTargetItem(stack) && (handlerSlot = inventoryIndexToHandlerSlot(handler, current)) >= 0) {
                this.mc.interactionManager.clickSlot(handler.syncId, handlerSlot, 0, SlotActionType.QUICK_MOVE, this.mc.player);
                moved++;
            }
        }
        if (this.inventorySlot < this.mc.player.getInventory().size()) {
            return;
        }
        if (this.selectedOrder != null) {
            logImportant("Đã giao item cho " + this.selectedOrder.customerName + " | giá mỗi item: $" + formatPrice(this.selectedOrder.price) + ".");
        } else {
            logImportant("Đã giao xong item vào order.");
        }
        if (this.mc.currentScreen != null) {
            this.mc.player.closeHandledScreen();
        }
        setStage(Stage.WAIT_CONFIRM);
    }

    private void waitConfirm() {
        if (this.mc.currentScreen instanceof GenericContainerScreen) {
            this.confirmGuiWaitAttempts = 0;
            setStage(Stage.CONFIRM_ORDER);
            return;
        }
        this.confirmGuiWaitAttempts++;
        if (this.confirmGuiWaitAttempts < 10) {
            return;
        }
        this.confirmGuiWaitAttempts = 0;
        this.closeOrderClicks = 0;
        if (countTargetStacks() > 0) {
            setStage(Stage.OPEN_ORDER);
        } else {
            setStage(Stage.CLOSE_ORDER);
        }
    }

    private void confirmOrder() {
        resetSpawnerGuiState();
        if (!(this.mc.currentScreen instanceof GenericContainerScreen)) {
            this.closeOrderClicks = 0;
            setStage(Stage.CLOSE_ORDER);
            return;
        }
        GenericContainerScreen screen = (GenericContainerScreen) this.mc.currentScreen;
        ScreenHandler handler = screen.getScreenHandler();
        int slot = ((Integer) this.confirmSlot.get()).intValue();
        if (slot >= 0 && slot < handler.slots.size()) {
            this.mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, this.mc.player);
        } else {
            Slot confirm = findConfirmSlot(handler);
            if (confirm != null) {
                this.mc.interactionManager.clickSlot(handler.syncId, confirm.id, 0, SlotActionType.PICKUP, this.mc.player);
            } else {
                log("KhÃ´ng tÃ¬m tháº¥y nÃºt xÃ¡c nháº\u00adn trong GUI.");
            }
        }
        this.closeOrderClicks = 0;
        setStage(Stage.CLOSE_ORDER);
    }

    private void closeOrder() {
        resetSpawnerGuiState();
        if (this.closeOrderClicks < 2) {
            if (this.mc.currentScreen != null) {
                this.mc.player.closeHandledScreen();
            }
            this.closeOrderClicks++;
            return;
        }
        if (this.mc.currentScreen != null) {
            this.mc.player.closeHandledScreen();
            return;
        }
        this.cycles++;
        this.cyclesOnActiveSpawner++;
        this.closeOrderClicks = 0;
        this.orderGuiWaitAttempts = 0;
        this.confirmGuiWaitAttempts = 0;
        this.inventorySlot = 0;
        this.selectedOrder = null;
        this.rejectedOrders.clear();
        switchSpawnerIfNeeded();
        this.targetSpawner = selectActiveSpawner();
        log("Hoan tat mot luot giao order. Quay lai target spawner.");
        if (this.targetSpawner != null && (this.mc.world.getBlockState(this.targetSpawner).getBlock() instanceof SpawnerBlock)) {
            this.lastSpawner = this.targetSpawner;
            setStage(Stage.OPEN_SPAWNER);
        } else {
            this.targetSpawner = null;
            setStage(Stage.FIND_SPAWNER);
        }
    }

    private void resetSpawnerGuiState() {
        this.waitingForSpawnerGui = false;
        this.spawnerGuiReady = false;
        this.openSpawnerLookTicks = 0;
        this.openSpawnerAttempts = 0;
        this.openSpawnerRecoveries = 0;
    }

    private void watchdog() {
        if (this.stage == Stage.IDLE || this.stage == Stage.WAIT_NEXT_COLLECT) {
            logImportant("AutoOrder rơi vào stage dừng: " + getStageName(this.stage) + ". Tự khôi phục loop.");
            if (this.mc.currentScreen != null) {
                this.mc.player.closeHandledScreen();
            }
            this.targetSpawner = null;
            this.selectedOrder = null;
            resetSpawnerGuiState();
            setStage(Stage.FIND_SPAWNER);
            return;
        }
        if (this.stage == Stage.WAIT_FOR_AUTOSIGN || this.stage == Stage.WAIT_SPAWNER_RESTOCK || this.stageStartedAt <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long stuckMs = now - this.stageStartedAt;
        if (stuckMs < 20000 || now - this.lastWatchdogLogAt < 10000) {
            return;
        }
        this.lastWatchdogLogAt = now;
        if (this.stage == Stage.OPEN_SPAWNER) {
            this.openSpawnerRecoveries++;
            logImportant("AutoOrder kẹt khi mở spawner. Stage: " + getStageName(this.stage) + ", target: " + getTargetSpawnerLabel() + ", GUI: " + getCurrentGuiLabel() + ", retry: " + this.openSpawnerRecoveries + ".");
            if (this.mc.currentScreen != null) {
                this.mc.player.closeHandledScreen();
            }
            this.waitingForSpawnerGui = false;
            this.spawnerGuiReady = false;
            this.openSpawnerLookTicks = 0;
            this.openSpawnerAttempts = 0;
            if (this.targetSpawner != null && (this.mc.world.getBlockState(this.targetSpawner).getBlock() instanceof SpawnerBlock) && this.openSpawnerRecoveries < 4) {
                setStage(Stage.OPEN_SPAWNER);
                return;
            }
            this.targetSpawner = null;
            resetSpawnerGuiState();
            setStage(Stage.FIND_SPAWNER);
            return;
        }
        if (this.stage == Stage.SELECT_BEST_ORDER || this.stage == Stage.WAIT_CONFIRM || this.stage == Stage.CLOSE_ORDER || this.stage == Stage.DEPOSIT_SELL || this.stage == Stage.CLOSE_SELL) {
            logImportant("AutoOrder đang chờ lâu. Stage: " + getStageName(this.stage) + ", GUI: " + getCurrentGuiLabel() + ", item: " + countTargetStacks() + " stack.");
        }
    }

    private String getTargetSpawnerLabel() {
        return this.targetSpawner == null ? "none" : this.targetSpawner.toShortString();
    }

    private String getCurrentGuiLabel() {
        return this.mc.currentScreen == null ? "none" : this.mc.currentScreen.getClass().getSimpleName();
    }

    private BlockPos findNearbySpawner() {
        return selectActiveSpawner();
    }

    private void switchSpawnerIfNeeded() {
        List<BlockPos> spawners = findReachableSpawners();
        if (spawners.size() >= 2 && this.cyclesOnActiveSpawner >= ((Integer) this.switchSpawnerAfterCycles.get()).intValue()) {
            this.activeSpawnerIndex = (this.activeSpawnerIndex + 1) % spawners.size();
            this.cyclesOnActiveSpawner = 0;
            updateCurrentTargetForActiveSpawner();
            BlockPos next = spawners.get(this.activeSpawnerIndex);
            this.targetSpawner = next;
            this.lastSpawner = next;
            logImportant("Chuyển sang spawner " + (this.activeSpawnerIndex + 1) + "/" + spawners.size() + " | item order: " + this.currentTarget + ".");
        }
    }

    private BlockPos selectActiveSpawner() {
        List<BlockPos> spawners = findReachableSpawners();
        if (spawners.isEmpty()) {
            return null;
        }
        if (this.activeSpawnerIndex >= spawners.size()) {
            this.activeSpawnerIndex = 0;
            this.cyclesOnActiveSpawner = 0;
        }
        int startIndex = this.activeSpawnerIndex;
        for (int i = 0; i < spawners.size(); i++) {
            int index = (startIndex + i) % spawners.size();
            BlockPos pos = spawners.get(index);
            if (!isSpawnerCoolingDown(pos)) {
                this.activeSpawnerIndex = index;
                updateCurrentTargetForActiveSpawner();
                return pos;
            }
        }
        updateCurrentTargetForActiveSpawner();
        return spawners.get(this.activeSpawnerIndex);
    }

    private BlockPos selectNextAvailableSpawner() {
        List<BlockPos> spawners = findReachableSpawners();
        if (spawners.isEmpty()) {
            return null;
        }
        int startIndex = spawners.isEmpty() ? 0 : (this.activeSpawnerIndex + 1) % spawners.size();
        for (int i = 0; i < spawners.size(); i++) {
            int index = (startIndex + i) % spawners.size();
            BlockPos pos = spawners.get(index);
            if (!isSpawnerCoolingDown(pos)) {
                this.activeSpawnerIndex = index;
                this.cyclesOnActiveSpawner = 0;
                updateCurrentTargetForActiveSpawner();
                return pos;
            }
        }
        return null;
    }

    private List<BlockPos> findReachableSpawners() {
        clearExpiredSpawnerCooldowns();
        BlockPos playerPos = this.mc.player.getBlockPos();
        int radius = ((Integer) this.spawnerRadius.get()).intValue();
        List<BlockPos> found = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if ((this.mc.world.getBlockState(pos).getBlock() instanceof SpawnerBlock) && canReachSpawner(pos)) {
                        found.add(pos);
                    }
                }
            }
        }
        found.sort(Comparator.<BlockPos, Double>comparing(pos2 -> {
            return Vec3d.ofCenter(pos2).squaredDistanceTo(this.mc.player.getPos());
        }).thenComparingInt((BlockPos v0) -> {
            return v0.getX();
        }).thenComparingInt((BlockPos v0) -> {
            return v0.getY();
        }).thenComparingInt((BlockPos v0) -> {
            return v0.getZ();
        }));
        return found;
    }

    private boolean isSpawnerCollectReady(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() >= stack.getMaxCount();
    }

    private String getStackCountLabel(ItemStack stack) {
        return stack.isEmpty() ? "0" : stack.getCount() + "/" + stack.getMaxCount();
    }

    private void markSpawnerRestocking(BlockPos pos) {
        if (pos == null) {
            return;
        }
        this.spawnerRestockUntilByPos.put(pos, Long.valueOf(System.currentTimeMillis() + (((long) ((Integer) this.spawnerRestockWaitSeconds.get()).intValue()) * 1000)));
    }

    private boolean isSpawnerCoolingDown(BlockPos pos) {
        Long until = this.spawnerRestockUntilByPos.get(pos);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() < until.longValue()) {
            return true;
        }
        this.spawnerRestockUntilByPos.remove(pos);
        return false;
    }

    private void clearExpiredSpawnerCooldowns() {
        long now = System.currentTimeMillis();
        this.spawnerRestockUntilByPos.entrySet().removeIf(entry -> {
            return ((Long) entry.getValue()).longValue() <= now;
        });
    }

    private long getEarliestSpawnerRestockUntil() {
        clearExpiredSpawnerCooldowns();
        long earliest = Long.MAX_VALUE;
        Iterator<Long> it = this.spawnerRestockUntilByPos.values().iterator();
        while (it.hasNext()) {
            long until = it.next().longValue();
            if (until < earliest) {
                earliest = until;
            }
        }
        return earliest == Long.MAX_VALUE ? System.currentTimeMillis() + (((long) ((Integer) this.spawnerRestockWaitSeconds.get()).intValue()) * 1000) : earliest;
    }

    private boolean canReachSpawner(BlockPos pos) {
        if (this.mc.player == null || pos == null) {
            return false;
        }
        double range = ((double) ((Integer) this.spawnerInteractRange.get()).intValue()) + 0.75d;
        return this.mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(pos)) <= range * range;
    }

    private OrderCandidate findBestOrder(ScreenHandler handler) {
        OrderCandidate best = null;
        int limit = Math.min(((Integer) this.maxOrderSlots.get()).intValue(), handler.slots.size());
        for (int i = 0; i < limit; i++) {
            Slot slot = handler.getSlot(i);
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                OrderInfo info = extractOrderInfo(stack);
                if (isRejectedOrder(info)) {
                    log("Bỏ qua order đã hết số lượng: " + info.customerName + " | $" + formatPrice(info.price));
                } else if (info.price > 0.0d && (best == null || info.price > best.price)) {
                    best = new OrderCandidate(slot.id, info.price, info.customerName, info.priceLine, info.actionLine);
                }
            }
        }
        return best;
    }

    private OrderInfo extractOrderInfo(ItemStack stack) {
        double price = 0.0d;
        String customerName = "unknown";
        String priceLine = "";
        String actionLine = "";
        List<String> lines = new ArrayList<>();
        lines.add(stack.getName().getString());
        try {
            for (Text text : stack.getTooltip(Item.TooltipContext.create(this.mc.world), this.mc.player, net.minecraft.item.tooltip.TooltipType.Default.BASIC)) {
                lines.add(text.getString());
            }
        } catch (Exception e) {
        }
        for (String line : lines) {
            String normalized = normalizeOrderLine(line);
            if (isDeliverActionLine(normalized)) {
                actionLine = line;
                customerName = extractCustomerName(line);
            }
            if (isTruePriceLine(normalized)) {
                Matcher matcher = TRUE_PRICE_PATTERN.matcher(line);
                if (matcher.find()) {
                    price = parsePrice(matcher.group(1));
                    priceLine = line;
                }
            }
        }
        return new OrderInfo(price, customerName, priceLine, actionLine);
    }

    private boolean isTruePriceLine(String normalized) {
        return normalized.contains("gia") && normalized.contains("moi") && normalized.contains("item") && normalized.contains("$");
    }

    private boolean isDeliverActionLine(String normalized) {
        return normalized.contains("giao") && normalized.contains("hang") && normalized.contains("cho");
    }

    private boolean isRejectedOrder(OrderInfo info) {
        if (info.price <= 0.0d) {
            return false;
        }
        for (OrderCandidate rejected : this.rejectedOrders) {
            if (Math.abs(rejected.price - info.price) < 1.0E-4d && rejected.customerName.equalsIgnoreCase(info.customerName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSellingStage() {
        return this.stage == Stage.DEPOSIT_ITEMS || this.stage == Stage.WAIT_CONFIRM || this.stage == Stage.CONFIRM_ORDER || this.stage == Stage.CLOSE_ORDER || this.stage == Stage.DEPOSIT_SELL || this.stage == Stage.CLOSE_SELL;
    }

    private boolean isOrderAmountExceededMessage(String message) {
        String normalized = normalizeOrderLine(message);
        return normalized.contains("so luong giao") && normalized.contains("vuot qua") && normalized.contains("so luong con lai");
    }

    private String extractCustomerName(String line) {
        String normalized = normalizeOrderLine(line);
        int index = normalized.lastIndexOf(" cho ");
        if (index < 0) {
            return "unknown";
        }
        String[] rawParts = line.trim().split("\\s+");
        return rawParts.length == 0 ? "unknown" : rawParts[rawParts.length - 1];
    }

    private String normalizeOrderLine(String line) {
        if (line == null) {
            return "";
        }
        String value = line.replace((char) 7424, 'a').replace((char) 665, 'b').replace((char) 7428, 'c').replace((char) 7429, 'd').replace((char) 7431, 'e').replace((char) 610, 'g').replace((char) 668, 'h').replace((char) 618, 'i').replace((char) 7435, 'k').replace((char) 671, 'l').replace((char) 7437, 'm').replace((char) 628, 'n').replace((char) 7439, 'o').replace((char) 7448, 'p').replace((char) 640, 'r').replace((char) 42801, 's').replace((char) 7451, 't').replace((char) 7452, 'u').replace((char) 7456, 'v').replace((char) 7457, 'w').replace((char) 655, 'y').toLowerCase(Locale.ROOT);
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private double parsePrice(String raw) {
        if (raw == null) {
            return 0.0d;
        }
        String value = raw.trim().replace(",", "").replace("_", "").replace(" ", "").toLowerCase(Locale.ROOT);
        double multiplier = 1.0d;
        if (value.endsWith("k")) {
            multiplier = 1000.0d;
            value = value.substring(0, value.length() - 1);
        } else if (value.endsWith("m")) {
            multiplier = 1000000.0d;
            value = value.substring(0, value.length() - 1);
        } else if (value.endsWith("b")) {
            multiplier = 1.0E9d;
            value = value.substring(0, value.length() - 1);
        }
        try {
            return Double.parseDouble(value) * multiplier;
        } catch (NumberFormatException e) {
            return 0.0d;
        }
    }

    private Slot findConfirmSlot(ScreenHandler handler) {
        for (Slot slot : handler.slots) {
            if (isConfirmButton(slot.getStack())) {
                return slot;
            }
        }
        return null;
    }

    private boolean isConfirmButton(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String name = stack.getName().getString().toLowerCase(Locale.ROOT);
        return stack.getItem() == Items.LIME_DYE || stack.getItem() == Items.GREEN_DYE || name.contains("confirm") || name.contains("accept");
    }

    private boolean isTargetItem(ItemStack stack) {
        return isItemMatchingTarget(stack, this.currentTarget);
    }

    private boolean isItemMatchingTarget(ItemStack stack, String targetName) {
        if (stack.isEmpty()) {
            return false;
        }
        Item exact = (Item) this.targetItem.get();
        if (!hasSpawnerItemTargets() && exact != Items.AIR) {
            return stack.getItem() == exact;
        }
        String name = stack.getName().getString();
        String itemName = stack.getItem().getName().getString();
        return matchesConfiguredItem(name, targetName) || matchesConfiguredItem(itemName, targetName);
    }

    private boolean matchesConfiguredItem(String itemName, String targetName) {
        String strNormalizeItemTarget = normalizeItemTarget(targetName);
        if (strNormalizeItemTarget.isEmpty()) {
            return false;
        }
        String strNormalizeItemTarget2 = normalizeItemTarget(itemName);
        CharSequence singularTarget = removeTrailingPluralS(strNormalizeItemTarget);
        String singularName = removeTrailingPluralS(strNormalizeItemTarget2);
        return strNormalizeItemTarget2.contains(strNormalizeItemTarget) || strNormalizeItemTarget.contains(strNormalizeItemTarget2) || strNormalizeItemTarget2.contains(singularTarget) || singularName.contains(singularTarget);
    }

    private void updateCurrentTargetFromInventory() {
        if (!hasSpawnerItemTargets()) {
            this.currentTarget = normalizeItemTarget((String) this.targetItemName.get());
            return;
        }
        String activeTarget = getConfiguredTargetForSpawner(this.activeSpawnerIndex);
        if (countTargetStacksFor(activeTarget) > 0) {
            this.currentTarget = activeTarget;
            return;
        }
        for (String target : getConfiguredSpawnerTargets()) {
            if (countTargetStacksFor(target) > 0) {
                this.currentTarget = target;
                return;
            }
        }
        this.currentTarget = activeTarget;
    }

    private boolean hasSpawnerItemTargets() {
        for (String item : (List<String>) this.spawnerItems.get()) {
            if (!normalizeItemTarget(item).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void updateCurrentTargetForActiveSpawner() {
        this.currentTarget = getConfiguredTargetForSpawner(this.activeSpawnerIndex);
    }

    private String getConfiguredTargetForSpawner(int index) {
        List<String> clean = getConfiguredSpawnerTargets();
        return !clean.isEmpty() ? clean.get(Math.min(Math.max(index, 0), clean.size() - 1)) : normalizeItemTarget((String) this.targetItemName.get());
    }

    private List<String> getConfiguredSpawnerTargets() {
        List<String> items = (List) this.spawnerItems.get();
        List<String> clean = new ArrayList<>();
        for (String item : items) {
            String value = normalizeItemTarget(item);
            if (!value.isEmpty()) {
                clean.add(value);
            }
        }
        return clean;
    }

    private String normalizeItemTarget(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private String removeTrailingPluralS(String value) {
        return (!value.endsWith("s") || value.length() <= 1) ? value : value.substring(0, value.length() - 1);
    }

    private boolean isArrowItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.ARROW;
    }

    private int countEmptyMainInventorySlots() {
        int empty = 0;
        for (int i = 0; i < 36; i++) {
            if (this.mc.player.getInventory().getStack(i).isEmpty()) {
                empty++;
            }
        }
        return empty;
    }

    private int countTargetStacks() {
        return countTargetStacksFor(this.currentTarget);
    }

    private int countTargetStacksFor(String targetName) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            if (isItemMatchingTarget(this.mc.player.getInventory().getStack(i), targetName)) {
                count++;
            }
        }
        return count;
    }

    private int countArrowStacks() {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            if (isArrowItem(this.mc.player.getInventory().getStack(i))) {
                count++;
            }
        }
        return count;
    }

    private int inventoryIndexToHandlerSlot(ScreenHandler handler, int inventoryIndex) {
        for (Slot slot : handler.slots) {
            if (slot.inventory == this.mc.player.getInventory() && slot.getIndex() == inventoryIndex) {
                return slot.id;
            }
        }
        return -1;
    }

    private void setStage(Stage next) {
        this.stage = next;
        this.ticks = 0;
        this.stageStartedAt = System.currentTimeMillis();
    }

    private String getTargetLabel() {
        if (!hasSpawnerItemTargets() && this.targetItem.get() != Items.AIR) {
            return ((Item) this.targetItem.get()).getName().getString();
        }
        return this.currentTarget;
    }

    private void log(String message) {
        if (((Boolean) this.debug.get()).booleanValue()) {
            info("[AutoOrder] " + message, new Object[0]);
        }
    }

    private void logImportant(String message) {
        info("[AutoOrder] " + message, new Object[0]);
    }

    private String formatPrice(double price) {
        return price == Math.rint(price) ? String.format(Locale.US, "%,.0f", Double.valueOf(price)) : String.format(Locale.US, "%,.4f", Double.valueOf(price)).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private long getRestockRemainingSeconds() {
        if (this.spawnerRestockUntil <= 0) {
            return 0L;
        }
        long remainingMs = this.spawnerRestockUntil - System.currentTimeMillis();
        if (remainingMs <= 0) {
            return 0L;
        }
        return (remainingMs + 999) / 1000;
    }

    private long getStageSeconds() {
        if (this.stageStartedAt <= 0) {
            return 0L;
        }
        return Math.max(0L, (System.currentTimeMillis() - this.stageStartedAt) / 1000);
    }

    private String formatDuration(long seconds) {
        long minutes = seconds / 60;
        long rest = seconds % 60;
        String.format(Locale.US, "%02d", Long.valueOf(rest));
        return minutes + ":" + minutes;
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    private String getStageName(Stage value) throws MatchException {
        switch (value) {
            case IDLE:
                return "Đang nghỉ";
            case WAIT_FOR_AUTOSIGN:
                return "Chờ AutoSign";
            case FIND_SPAWNER:
                return "Tìm spawner";
            case OPEN_SPAWNER:
                return "Mở spawner";
            case COLLECT_SPAWNER:
                return "Lấy item spawner";
            case WAIT_AFTER_COLLECT:
                return "Chờ sau khi lấy item";
            case WAIT_SPAWNER_RESTOCK:
                return "Chờ spawner có item";
            case CHECK_INVENTORY:
                return "Kiểm tra túi đồ";
            case OPEN_SELL:
                return "Mở sell arrow";
            case DEPOSIT_SELL:
                return "Bán arrow";
            case CLOSE_SELL:
                return "Đóng sell";
            case OPEN_ORDER:
                return "Mở order";
            case SELECT_BEST_ORDER:
                return "Chọn order giá cao nhất";
            case DEPOSIT_ITEMS:
                return "Giao item";
            case WAIT_CONFIRM:
                return "Chờ GUI xác nhận";
            case CONFIRM_ORDER:
                return "Xác nhận order";
            case CLOSE_ORDER:
                return "Đóng order";
            case WAIT_NEXT_COLLECT:
                return "Chờ lượt lấy tiếp theo";
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    /* JADX INFO: Thrown type has an unknown type hierarchy: java.lang.MatchException */
    public String getInfoString() throws MatchException {
        if (!isActive()) {
            return null;
        }
        if (this.stage == Stage.WAIT_SPAWNER_RESTOCK) {
            return "Đợi spawner " + formatDuration(getRestockRemainingSeconds()) + " | " + getTargetLabel() + " | cycles " + this.cycles;
        }
        String stageName = getStageName(this.stage);
        long stageSeconds = getStageSeconds();
        String targetLabel = getTargetLabel();
        int i = this.cycles;
        return stageName + " " + stageSeconds + "s | " + stageName + " | cycles " + targetLabel;
    }

    /* JADX INFO: loaded from: grunt-1.21.4-n-16.1.jar:com/nnpg/glazed/modules/main/AutoOrder$OrderInfo.class */
    private static final class OrderInfo {
        private final double price;
        private final String customerName;
        private final String priceLine;
        private final String actionLine;

        private OrderInfo(double price, String customerName, String priceLine, String actionLine) {
            this.price = price;
            this.customerName = customerName;
            this.priceLine = priceLine;
            this.actionLine = actionLine;
        }

        @Override
        public final String toString() {
            return "OrderInfo{price=" + price + ", customerName=" + customerName + ", priceLine=" + priceLine + ", actionLine=" + actionLine + "}";
        }

        @Override
        public final int hashCode() {
            return Objects.hash(price, customerName, priceLine, actionLine);
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OrderInfo)) return false;
            OrderInfo other = (OrderInfo) o;
            return Double.compare(price, other.price) == 0
                && Objects.equals(customerName, other.customerName)
                && Objects.equals(priceLine, other.priceLine)
                && Objects.equals(actionLine, other.actionLine);
        }

        public double price() {
            return this.price;
        }

        public String customerName() {
            return this.customerName;
        }

        public String priceLine() {
            return this.priceLine;
        }

        public String actionLine() {
            return this.actionLine;
        }
    }

    /* JADX INFO: loaded from: grunt-1.21.4-n-16.1.jar:com/nnpg/glazed/modules/main/AutoOrder$OrderCandidate.class */
    private static final class OrderCandidate {
        private final int slotId;
        private final double price;
        private final String customerName;
        private final String priceLine;
        private final String actionLine;

        private OrderCandidate(int slotId, double price, String customerName, String priceLine, String actionLine) {
            this.slotId = slotId;
            this.price = price;
            this.customerName = customerName;
            this.priceLine = priceLine;
            this.actionLine = actionLine;
        }

        @Override
        public final String toString() {
            return "OrderCandidate{slotId=" + slotId + ", price=" + price + ", customerName=" + customerName + ", priceLine=" + priceLine + ", actionLine=" + actionLine + "}";
        }

        @Override
        public final int hashCode() {
            return Objects.hash(slotId, price, customerName, priceLine, actionLine);
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OrderCandidate)) return false;
            OrderCandidate other = (OrderCandidate) o;
            return slotId == other.slotId
                && Double.compare(price, other.price) == 0
                && Objects.equals(customerName, other.customerName)
                && Objects.equals(priceLine, other.priceLine)
                && Objects.equals(actionLine, other.actionLine);
        }

        public int slotId() {
            return this.slotId;
        }

        public double price() {
            return this.price;
        }

        public String customerName() {
            return this.customerName;
        }

        public String priceLine() {
            return this.priceLine;
        }

        public String actionLine() {
            return this.actionLine;
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!((Boolean) this.showSpawnerEsp.get()).booleanValue() || this.targetSpawner == null) {
            return;
        }
        Color color = new Color((Color) this.spawnerEspColor.get());
        event.renderer.box(this.targetSpawner, color, color, ShapeMode.Both, 0);
    }
}
