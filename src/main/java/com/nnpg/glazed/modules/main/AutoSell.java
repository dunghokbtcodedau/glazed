package com.nnpg.glazed.modules.main;

import com.nnpg.glazed.GlazedAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

public class AutoSell extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SellMode> mode = sgGeneral.add(new EnumSetting.Builder<SellMode>()
        .name("mode")
        .description("Whether to whitelist or blacklist the selected items.")
        .defaultValue(SellMode.Whitelist)
        .build()
    );

    private final Setting<List<Item>> itemList = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Items to sell.")
        .defaultValue(List.of(Items.SEA_PICKLE))
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks between actions.")
        .defaultValue(1)
        .min(0)
        .max(20)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> notifications = sgGeneral.add(new BoolSetting.Builder()
        .name("notifications")
        .description("Show chat feedback.")
        .defaultValue(true)
        .build()
    );

    private int delayCounter;
    private boolean shouldReopen = false;
    private int pendingSlot45Clicks = 0;

    public AutoSell() {
        super(GlazedAddon.CATEGORY, "auto-sell", "Automatically sells items.");
    }

    @Override
    public void onActivate() {
        delayCounter = 20;
        shouldReopen = false;
        pendingSlot45Clicks = 0;
    }

    @Override
    public void onDeactivate() {
        shouldReopen = false;
        pendingSlot45Clicks = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (delayCounter > 0) {
            delayCounter--;
            return;
        }

        if (shouldReopen) {
            mc.getNetworkHandler().sendChatCommand("sell");
            shouldReopen = false;
            delayCounter = delay.get();
            return;
        }

        handleSellMode();
    }

    private void handleSellMode() {
        ScreenHandler currentScreenHandler = mc.player.currentScreenHandler;

        if (!(currentScreenHandler instanceof GenericContainerScreenHandler)) {
            mc.getNetworkHandler().sendChatCommand("sell");
            delayCounter = 20;
            return;
        }

        // Xử lý click slot 45 nếu đang pending
        if (pendingSlot45Clicks > 0) {
            mc.interactionManager.clickSlot(currentScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, mc.player);
            pendingSlot45Clicks--;
            delayCounter = delay.get();
            return;
        }

        if (areAllSellSlotsOccupied(currentScreenHandler)) {
            if (notifications.get()) info("All sell menu slots (0-35) are occupied. Clicking slot 45 twice.");
            pendingSlot45Clicks = 2;
            delayCounter = delay.get();
            return;
        }

        int totalSlots = currentScreenHandler.slots.size();
        boolean foundItemToSell = false;

        for (int slot = 36; slot < totalSlots; slot++) {
            ItemStack stack = currentScreenHandler.getSlot(slot).getStack();

            if (stack.isEmpty()) continue;

            Item itemInSlot = stack.getItem();
            if (!shouldSellItem(itemInSlot)) continue;

            foundItemToSell = true;
            mc.interactionManager.clickSlot(currentScreenHandler.syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
            delayCounter = delay.get();
            return;
        }

        if (!foundItemToSell) {
            if (notifications.get()) info("All items sold. Closing GUI.");
            mc.player.closeHandledScreen();
            toggle();
            delayCounter = 40;
        }
    }

    private boolean areAllSellSlotsOccupied(ScreenHandler screenHandler) {
        for (int slot = 0; slot <= 35; slot++) {
            if (slot >= screenHandler.slots.size()) return false;
            if (screenHandler.getSlot(slot).getStack().isEmpty()) return false;
        }
        return true;
    }

    private boolean shouldSellItem(Item item) {
        List<Item> selectedItems = itemList.get();
        if (mode.get() == SellMode.Whitelist) {
            return selectedItems.contains(item);
        } else {
            return !selectedItems.contains(item);
        }
    }

    public enum SellMode {
        Whitelist,
        Blacklist
    }
}