package com.yourserver.autofish;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class AutoFishMod implements ClientModInitializer {

    private final AutoFishConfig config = new AutoFishConfig();
    private KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("autofish", "category"));

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autofish.openmenu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));

        new AutoFishLogic(config).register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                openMenu(client);
            }
        });
    }

    private void openMenu(MinecraftClient client) {
        if (client.currentScreen == null) {
            client.setScreen(new AutoFishScreen(config));
        }
    }
}
