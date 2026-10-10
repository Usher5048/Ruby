package ruby.helpers.inventory;

import ruby.RubyClient;
import ruby.helpers.InventoryHelper;
import ruby.systems.events.Events;
import ruby.systems.events.packet.PacketEvent;
import ruby.systems.events.packet.PacketEvents;
import ruby.systems.events.TickEvents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

public final class InventoryManager {
    private static final ArrayList<InventoryActionChain> pendingChains = new ArrayList<>();
    private static InventoryActionChain activeChain;
    private static int actionIndex;
    private static int waitTicks;
    private static boolean openedThisSession;
    private static boolean serverInventoryOpen;
    private static boolean pendingClose;

    private InventoryManager() {}

    static {
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
        if(!(event.packet() instanceof ServerboundContainerClickPacket packet)) return;
        if(packet.containerId() == 0) serverInventoryOpen = true;
    }

    private static void onReceive(PacketEvent event) {
        switch(event.packet()) {
            case ClientboundContainerClosePacket packet -> {
                if(packet.getContainerId() == 0) serverInventoryOpen = false;
            }
            case ClientboundOpenScreenPacket ignored -> serverInventoryOpen = true;
            default -> {}
        }
    }

    private static void tick() {
        Minecraft mc = RubyClient.client;
        LocalPlayer player = mc.player;
        if(player == null || mc.gameMode == null) {
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

    private static void startNextChain(Minecraft mc, LocalPlayer player) {
        if(InventoryManager.pendingChains.isEmpty()) return;

        InventoryManager.activeChain = InventoryManager.pendingChains.removeFirst();
        InventoryManager.actionIndex = 0;
        InventoryManager.openedThisSession = false;
        InventoryManager.beginChain(mc, player);
    }

    private static void beginChain(Minecraft mc, LocalPlayer player) {
        InventoryActionChain chain = InventoryManager.activeChain;
        if(chain == null) return;

        if(!InventoryManager.passesMovement(chain.constraints(), player)) {
            InventoryManager.activeChain = null;
            return;
        }

        if(chain.requiresInventoryOpen() && chain.constraints().requireInventoryOpen()) {
            if(mc.gui.screen() instanceof AbstractContainerScreen<?> screen && screen.getMenu().containerId != 0) {
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

    private static void advance(Minecraft mc, LocalPlayer player) {
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

    private static void closeInventory(Minecraft mc) {
        if(InventoryHelper.isOwned()) {
            InventoryHelper.closeOwned(mc);
        } else if(mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundContainerClosePacket(0));
        }
        serverInventoryOpen = false;
    }

    private static boolean passesMovement(InventoryConstraints constraints, LocalPlayer player) {
        if(!constraints.requireNoMovement()) return true;
        return player.zza == 0 && player.xxa == 0 && player.onGround();
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
