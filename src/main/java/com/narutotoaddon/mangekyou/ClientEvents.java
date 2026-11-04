package com.narutotoaddon.mangekyou;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Parte do cliente: botoes extras no menu de dojutsu e deteccao da tecla de soltar jutsu. */
public final class ClientEvents {
    private ClientEvents() {}

    private static final int BTN_W = 150, BTN_H = 20, GAP = 22;
    private static KeyMapping releaseKey;
    private static boolean keyTried;
    private static boolean wasDown;

    private static final KeyMapping USE_KEY = new KeyMapping("key." + MangekyouAddon.MODID + ".use",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories." + MangekyouAddon.MODID);
    private static final KeyMapping CYCLE_KEY = new KeyMapping("key." + MangekyouAddon.MODID + ".cycle",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories." + MangekyouAddon.MODID);

    public static void register() {
        MinecraftForge.EVENT_BUS.register(ClientEvents.class);
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(ClientEvents::onRegisterKeys);
        modBus.addListener(ClientEvents::onAddLayers);
        modBus.addListener(ClientEvents::onRegisterRenderers);
    }

    public static void onRegisterKeys(RegisterKeyMappingsEvent e) {
        e.register(USE_KEY);
        e.register(CYCLE_KEY);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.IZANAGI_BODY.get(), IzanagiBodyRenderer::new);
    }

    /**
     * Um olho cego: a metade da tela do lado desse olho fica preta, com uma faixa branca leitosa na borda.
     * Os dois cegos: a tela inteira fica preta (cegueira escondida aplicada pelo servidor) e as criaturas
     * em volta brilham em branco (efeito Glowing aplicado pelo servidor).
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre e) {
        int mask = ClientState.myBlind();
        if (mask == 0 || mask == 3) return;
        int w = e.getWindow().getGuiScaledWidth(), h = e.getWindow().getGuiScaledHeight();
        GuiGraphics g = e.getGuiGraphics();
        int mid = w / 2, band = 28;
        if ((mask & 1) != 0) {
            g.fill(0, 0, mid - band, h, 0xFF000000);
            for (int i = 0; i < band; i++) {
                int a = 255 - (int) (150 * (i / (float) band)); // borda: branco leitoso -> preto
                g.fill(mid - band + i, 0, mid - band + i + 1, h, (a << 24) | 0xFFFFFF);
            }
        }
        if ((mask & 2) != 0) {
            g.fill(mid + band, 0, w, h, 0xFF000000);
            for (int i = 0; i < band; i++) {
                int a = 105 + (int) (150 * (i / (float) band));
                g.fill(mid + i, 0, mid + i + 1, h, (a << 24) | 0xFFFFFF);
            }
        }
    }

    /** Coloca a camada dos olhos do implante nos dois modelos de jogador (normal e slim). */
    public static void onAddLayers(EntityRenderersEvent.AddLayers e) {
        for (String skin : new String[] {"default", "slim"}) {
            Object r = e.getSkin(skin);
            if (r instanceof PlayerRenderer pr) {
                pr.addLayer(new EyeLayer(pr));
            }
        }
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
        Implant imp = ClientState.mine();
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
        Implant imp = ClientState.mine();
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
        ClientState.self = mc.player.getUUID();
        while (USE_KEY.consumeClick()) Net.CH.sendToServer(new UseJutsuPacket(UseJutsuPacket.OWN_USE));
        while (CYCLE_KEY.consumeClick()) Net.CH.sendToServer(new UseJutsuPacket(UseJutsuPacket.OWN_CYCLE));
        KeyMapping km = findKey();
        if (km == null) return;
        boolean down = km.isDown();
        if (wasDown && !down && mc.screen == null) {
            String name = Bridge.getString(mc.player, "jutsu_choosed_name", "");
            if (Ability.byJutsuKey(name) != null) {
                Net.CH.sendToServer(new UseJutsuPacket(UseJutsuPacket.MOD_KEY));
            }
        }
        wasDown = down;
    }
}
