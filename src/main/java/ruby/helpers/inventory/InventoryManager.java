package ruby.helpers.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import ruby.RubyClient;
import ruby.helpers.InventoryHelper;
import ruby.systems.events.Events;
import ruby.systems.events.packet.PacketEvent;
import ruby.systems.events.packet.PacketEvents;
import ruby.systems.events.tick.TickEvents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class InventoryManager {
    private static final ArrayList<InventoryActionChain> pendingChains = new ArrayList<>();
    private static InventoryActionChain activeChain;
    private static int actionIndex;
    private static int waitTicks;
    private static boolean openedThisSession;
    private static boolean serverInventoryOpen;
    private static boolean pendingClose;

    private InventoryManager() {}

    public static void init() {
        Events.TICK.register(TickEvents.END, event -> InventoryManager.tick());
        Events.PACKET.register(PacketEvents.SEND, InventoryManager::onSend);
        Events.PACKET.register(PacketEvents.RECEIVE, InventoryManager::onReceive);
    }

    public static boolean isOperating() {
        return activeChain != null || !pendingChains.isEmpty() || waitTicks > 0 || pendingClose;
    }

    public static boolean isServerInventoryOpen() {
        return serverInventoryOpen || InventoryHelper.isOpen(RubyClient.client);
    }

    private static void onSend(PacketEvent event) {
        if(!(event.packet() instanceof ClickSlotC2SPacket packet)) return;
        if(packet.syncId() == 0) serverInventoryOpen = true;
    }

    private static void onReceive(PacketEvent event) {
        switch(event.packet()) {
            case CloseScreenS2CPacket packet -> {
                if(packet.getSyncId() == 0) serverInventoryOpen = false;
            }
            case OpenScreenS2CPacket ignored -> serverInventoryOpen = true;
            default -> {}
        }
    }

    private static void tick() {
        MinecraftClient mc = RubyClient.client;
        ClientPlayerEntity player = mc.player;
        if(player == null || mc.interactionManager == null) {
            InventoryManager.reset();
            return;
        }

        if(InventoryManager.waitTicks > 0) {
            InventoryManager.waitTicks--;
            InventoryHelper.lock();
            return;
        }

        if(InventoryManager.pendingClose) {
            InventoryManager.closeInventory(mc);
            InventoryManager.pendingClose = false;
            InventoryManager.activeChain = null;
            InventoryManager.actionIndex = 0;
            InventoryManager.openedThisSession = false;
            if(!InventoryManager.pendingChains.isEmpty())
                InventoryManager.startNextChain(mc, player);
            else
                InventoryHelper.unlock();
            return;
        }

        if(InventoryManager.activeChain != null) {
            InventoryManager.advance(mc, player);
            return;
        }

        ScheduleInventoryActionEvent scheduleEvent = new ScheduleInventoryActionEvent();
        if(Events.INVENTORY_SCHEDULE.fire(scheduleEvent)) return;

        List<InventoryActionChain> chains = scheduleEvent.chains();
        if(chains.isEmpty()) return;

        chains.sort(Comparator
                .comparingInt((InventoryActionChain chain) -> chain.requiresInventoryOpen() ? 1 : 0)
                .thenComparingInt(chain -> -chain.priority().weight()));

        InventoryManager.pendingChains.addAll(chains);
        InventoryManager.startNextChain(mc, player);
    }

    private static void startNextChain(MinecraftClient mc, ClientPlayerEntity player) {
        if(InventoryManager.pendingChains.isEmpty()) return;

        InventoryManager.activeChain = InventoryManager.pendingChains.removeFirst();
        InventoryManager.actionIndex = 0;
        InventoryManager.openedThisSession = false;
        InventoryManager.beginChain(mc, player);
    }

    private static void beginChain(MinecraftClient mc, ClientPlayerEntity player) {
        InventoryActionChain chain = InventoryManager.activeChain;
        if(chain == null) return;

        if(!InventoryManager.passesMovement(chain.constraints(), player)) {
            InventoryManager.activeChain = null;
            return;
        }

        if(chain.requiresInventoryOpen() && chain.constraints().requireInventoryOpen()) {
            if(mc.currentScreen instanceof HandledScreen<?> screen && screen.getScreenHandler().syncId != 0) {
                InventoryManager.activeChain = null;
                return;
            }
            if(!InventoryManager.isServerInventoryOpen()) {
                InventoryHelper.openOwned(mc);
                InventoryManager.openedThisSession = true;
                InventoryManager.waitTicks = chain.constraints().startDelay();
                InventoryHelper.lock();
                return;
            }
        }

        InventoryManager.waitTicks = chain.constraints().startDelay();
        if(InventoryManager.waitTicks > 0) InventoryHelper.lock();
    }

    private static void advance(MinecraftClient mc, ClientPlayerEntity player) {
        InventoryActionChain chain = InventoryManager.activeChain;
        if(chain == null) return;

        if(!InventoryManager.passesMovement(chain.constraints(), player)) {
            InventoryManager.scheduleClose(chain);
            return;
        }

        if(InventoryManager.actionIndex >= chain.actions().size()) {
            InventoryManager.scheduleClose(chain);
            return;
        }

        if(chain.requiresInventoryOpen() && chain.constraints().requireInventoryOpen()
                && !InventoryManager.isServerInventoryOpen()) {
            InventoryHelper.openOwned(mc);
            InventoryManager.openedThisSession = true;
            InventoryManager.waitTicks = chain.constraints().startDelay();
            InventoryHelper.lock();
            return;
        }

        InventoryAction action = chain.actions().get(InventoryManager.actionIndex);
        InventoryHelper.lock();
        action.perform();
        InventoryManager.actionIndex++;

        if(InventoryManager.actionIndex >= chain.actions().size())
            InventoryManager.scheduleClose(chain);
        else
            InventoryManager.waitTicks = chain.constraints().clickDelay();
    }

    private static void scheduleClose(InventoryActionChain chain) {
        if(InventoryManager.openedThisSession && chain.constraints().requireInventoryOpen()) {
            InventoryManager.pendingClose = true;
            InventoryManager.waitTicks = chain.constraints().closeDelay();
            InventoryHelper.lock();
            return;
        }

        InventoryManager.activeChain = null;
        InventoryManager.actionIndex = 0;
        InventoryManager.openedThisSession = false;
        if(!InventoryManager.pendingChains.isEmpty())
            InventoryManager.startNextChain(RubyClient.client, RubyClient.client.player);
        else
            InventoryHelper.unlock();
    }

    private static void closeInventory(MinecraftClient mc) {
        if(InventoryHelper.isOwned()) {
            InventoryHelper.closeOwned(mc);
        } else if(mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
        }
        serverInventoryOpen = false;
    }

    private static boolean passesMovement(InventoryConstraints constraints, ClientPlayerEntity player) {
        if(!constraints.requireNoMovement()) return true;
        return player.forwardSpeed == 0 && player.sidewaysSpeed == 0 && player.isOnGround();
    }

    private static void reset() {
        pendingChains.clear();
        activeChain = null;
        actionIndex = 0;
        waitTicks = 0;
        openedThisSession = false;
        pendingClose = false;
        InventoryHelper.releaseOwnership();
    }
}
