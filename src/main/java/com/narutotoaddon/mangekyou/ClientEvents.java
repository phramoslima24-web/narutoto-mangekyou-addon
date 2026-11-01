package com.narutotoaddon.mangekyou;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Parte do cliente: botoes extras no menu de dojutsu e deteccao da tecla de soltar jutsu. */
public final class ClientEvents {
    private ClientEvents() {}

    private static final int BTN_W = 150, BTN_H = 20, GAP = 22;
    private static KeyMapping releaseKey;
    private static boolean keyTried;
    private static boolean wasDown;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(ClientEvents.class);
    }

    private static boolean isDojutsuScreen(Screen s) {
        return s != null && s.getClass().getName().endsWith("DojutsuBoardScreen");
    }

    private static int originX(Screen s) {
        return s instanceof AbstractContainerScreen<?> cs ? cs.getGuiLeft() + cs.getXSize() + 6 : s.width / 2 + 100;
    }

    private static int originY(Screen s) {
        return s instanceof AbstractContainerScreen<?> cs ? cs.getGuiTop() + 6 : s.height / 2 - 60;
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post e) {
        Screen s = e.getScreen();
        if (!isDojutsuScreen(s)) return;
        Implant imp = Implant.byId(ClientState.implant);
        if (imp == null) return;
        GuiGraphics g = e.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        int x = originX(s), y = originY(s);
        g.drawString(font, Component.translatable("gui.narutoto_mangekyou.title", Component.translatable(imp.nameKey())),
                x, y, 0xFFFFFF, true);
        y += 14;
        for (Ability a : imp.abilities) {
            boolean hover = e.getMouseX() >= x && e.getMouseX() <= x + BTN_W && e.getMouseY() >= y && e.getMouseY() <= y + BTN_H;
            g.fill(x, y, x + BTN_W, y + BTN_H, hover ? 0xFF7A1010 : 0xFF3A0808);
            g.renderOutline(x, y, BTN_W, BTN_H, 0xFFFF3030);
            g.drawCenteredString(font, Component.translatable("gui.narutoto_mangekyou.learn", Component.translatable(a.jutsuKey())),
                    x + BTN_W / 2, y + 6, 0xFFFFFF);
            y += GAP;
        }
    }

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre e) {
        Screen s = e.getScreen();
        if (!isDojutsuScreen(s) || e.getButton() != 0) return;
        Implant imp = Implant.byId(ClientState.implant);
        if (imp == null) return;
        int x = originX(s), y = originY(s) + 14;
        for (Ability a : imp.abilities) {
            if (e.getMouseX() >= x && e.getMouseX() <= x + BTN_W && e.getMouseY() >= y && e.getMouseY() <= y + BTN_H) {
                Net.CH.sendToServer(new LearnJutsuPacket(a.id));
                e.setCanceled(true);
                return;
            }
            y += GAP;
        }
    }

    private static KeyMapping findKey() {
        if (!keyTried) {
            keyTried = true;
            try {
                releaseKey = (KeyMapping) Class.forName("net.mcreator.narutoto.init.NarutotoModKeyMappings")
                        .getField("RELEASE_JUSTU").get(null);
            } catch (Throwable t) {
                System.err.println("[narutoto_mangekyou] Tecla RELEASE_JUSTU nao encontrada: " + t);
            }
        }
        return releaseKey;
    }

    /** Ao soltar a tecla de jutsu do Narutoto com um jutsu do addon selecionado, avisa o servidor. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        KeyMapping km = findKey();
        if (km == null) return;
        boolean down = km.isDown();
        if (wasDown && !down && mc.screen == null) {
            String name = Bridge.getString(mc.player, "jutsu_choosed_name", "");
            if (Ability.byJutsuKey(name) != null) {
                Net.CH.sendToServer(new UseJutsuPacket());
            }
        }
        wasDown = down;
    }
}
