package com.example.dripfall.client;

import com.example.dripfall.SettingsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class DripFallClient implements ClientModInitializer {
    public static SettingsPayload current = SettingsPayload.defaults();
    private static KeyBinding openMenu;

    @Override
    public void onInitializeClient() {
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.dripfall.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.dripfall"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.wasPressed()) {
                client.setScreen(new ConfigScreen(client.currentScreen));
            }
        });

        // Po wejściu na świat wyślij zapamiętane ustawienia.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> send());
    }

    public static void send() {
        if (ClientPlayNetworking.canSend(SettingsPayload.ID)) {
            ClientPlayNetworking.send(current);
        }
    }
}
