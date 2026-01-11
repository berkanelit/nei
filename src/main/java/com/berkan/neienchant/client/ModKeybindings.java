package com.berkan.neienchant.client;

import com.berkan.neienchant.NEIEnchantMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class ModKeybindings {
    public static final KeyBinding OPEN_ENCHANT_KEY = new KeyBinding(
            "key.neienchant.open_enchantment_menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            KeyBinding.Category.MISC
    );

    public static void register() {
        KeyBindingHelper.registerKeyBinding(OPEN_ENCHANT_KEY);
    }
}
