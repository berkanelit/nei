package com.berkan.neienchant.client.screen;

import com.berkan.neienchant.network.ApplyEnchantmentPayload;
import com.berkan.neienchant.network.RemoveEnchantmentPayload;
import com.berkan.neienchant.screen.EnchantmentScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class EnchantmentMenuScreen extends HandledScreen<EnchantmentScreenHandler> {
    private List<EnchantEntry> enchantments = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int selectedLevel = 1;
    private ItemStack lastStack = ItemStack.EMPTY;
    
    // Liste alanı koordinatları
    private int listX;
    private int listY;
    private int listWidth = 160;
    private int listHeight = 110;
    private int itemHeight = 14;
    private boolean wasMousePressed = false;

    public EnchantmentMenuScreen(EnchantmentScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 222;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        
        // Level + button (üst sağda)
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), btn -> {
            if (selectedLevel < 10) selectedLevel++;
        }).dimensions(this.x + 78, this.y + 5, 12, 12).build());

        // Level - button
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), btn -> {
            if (selectedLevel > 1) selectedLevel--;
        }).dimensions(this.x + 92, this.y + 5, 12, 12).build());

        // Apply button
        addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), btn -> {
            if (selectedIndex >= 0 && selectedIndex < enchantments.size()) {
                EnchantEntry entry = enchantments.get(selectedIndex);
                ClientPlayNetworking.send(new ApplyEnchantmentPayload(entry.id, selectedLevel));
            }
        }).dimensions(this.x + 30, this.y + 20, 35, 16).build());

        // Remove button
        addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), btn -> {
            if (selectedIndex >= 0 && selectedIndex < enchantments.size()) {
                EnchantEntry entry = enchantments.get(selectedIndex);
                ClientPlayNetworking.send(new RemoveEnchantmentPayload(entry.id));
            }
        }).dimensions(this.x + 68, this.y + 20, 40, 16).build());
        
        refreshEnchantments();
    }

    private void refreshEnchantments() {
        ItemStack stack = handler.getEnchantingStack();
        enchantments.clear();
        selectedIndex = -1;
        
        if (stack.isEmpty() || this.client == null || this.client.world == null) {
            return;
        }

        var registry = this.client.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        ItemEnchantmentsComponent currentEnchants = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        
        // Her büyüyü kontrol et
        for (var entry : registry.getIndexedEntries()) {
            try {
                Enchantment enchantment = entry.value();
                
                // İtem bu büyüyü kabul ediyor mu kontrol et
                if (!enchantment.isAcceptableItem(stack)) {
                    continue;
                }
                
                // Büyü ID ve ismini al
                Identifier id = entry.getKey().get().getValue();
                String name = Enchantment.getName(entry, 1).getString();
                if (name.endsWith(" I")) {
                    name = name.substring(0, name.length() - 2);
                }
                
                // Şu an bu büyü var mı kontrol et
                boolean isEnchanted = false;
                for (var currentEntry : currentEnchants.getEnchantmentEntries()) {
                    if (currentEntry.getKey().equals(entry)) {
                        isEnchanted = true;
                        name += " (" + currentEntry.getIntValue() + ")";
                        break;
                    }
                }
                
                enchantments.add(new EnchantEntry(id.toString(), name, isEnchanted));
            } catch (Exception e) {
                // Bu büyüyü atla
            }
        }
        
        // İsme göre sırala
        enchantments.sort((a, b) -> a.name.compareTo(b.name));
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        ItemStack stack = handler.getEnchantingStack();
        if (!ItemStack.areEqual(stack, lastStack)) {
            lastStack = stack.copy();
            refreshEnchantments();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
        
        // Mouse tıklama kontrolü - liste seçimi için
        if (this.client != null) {
            boolean isMousePressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                this.client.getWindow().getHandle(), 
                org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT
            ) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            
            if (isMousePressed && !wasMousePressed) {
                // Convert to relative coordinates
                handleListClick(mouseX - this.x, mouseY - this.y);
            }
            wasMousePressed = isMousePressed;
        }
    }
    
    private void handleListClick(double mouseX, double mouseY) {
        // Liste içinde tıklama kontrolü (relative coordinates)
        int relativeListX = 8;
        int relativeListY = 40;
        
        if (mouseX >= relativeListX && mouseX < relativeListX + listWidth && 
            mouseY >= relativeListY && mouseY < relativeListY + listHeight) {
            
            int clickedIndex = (int)((mouseY - relativeListY) / itemHeight) + scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < enchantments.size()) {
                selectedIndex = clickedIndex;
            }
        }
    }

    
    private void renderEnchantmentList(DrawContext context, int mouseX, int mouseY) {
        // Use relative coordinates
        int relativeListX = 8;
        int relativeListY = 40;
        listWidth = 160;
        listHeight = 88;
        
        int maxVisible = listHeight / itemHeight;
        
        // List background
        context.fill(relativeListX - 2, relativeListY - 2, relativeListX + listWidth + 2, relativeListY + listHeight + 2, 0xFF555555);
        context.fill(relativeListX - 1, relativeListY - 1, relativeListX + listWidth + 1, relativeListY + listHeight + 1, 0xFF333333);
        context.fill(relativeListX, relativeListY, relativeListX + listWidth, relativeListY + listHeight, 0xEE000000);
        
        for (int i = 0; i < Math.min(enchantments.size(), maxVisible); i++) {
            int index = i + scrollOffset;
            if (index >= enchantments.size()) break;
            
            EnchantEntry entry = enchantments.get(index);
            int y = relativeListY + i * itemHeight;
            
            boolean hovered = mouseX >= relativeListX && mouseX < relativeListX + listWidth && 
                            mouseY >= y && mouseY < y + itemHeight;
            
            // Renk ve arkaplan belirleme
            int textColor;
            int bgColor = 0x00000000;
            
            if (entry.isEnchanted) {
                textColor = 0xFF55FF55; // Parlak yeşil
                bgColor = 0x5500AA00; // Koyu yeşil arkaplan
            } else if (index == selectedIndex) {
                textColor = 0xFFFFAA00; // Turuncu
                bgColor = 0x55FF8800; // Turuncu arkaplan
            } else if (hovered) {
                textColor = 0xFFFFFFFF; // Beyaz
                bgColor = 0x55666666; // Gri arkaplan
            } else {
                textColor = 0xFFCCCCCC; // Açık gri
            }
            
            // Hover/selected background
            if (bgColor != 0x00000000) {
                context.fill(relativeListX + 1, y, relativeListX + listWidth - 1, y + itemHeight, bgColor);
            }
            
            // Left colored bar
            if (entry.isEnchanted) {
                context.fill(relativeListX + 1, y + 2, relativeListX + 3, y + itemHeight - 2, 0xFF00FF00);
            } else if (index == selectedIndex) {
                context.fill(relativeListX + 1, y + 2, relativeListX + 3, y + itemHeight - 2, 0xFFFFAA00);
            }
            
            // Draw name
            context.drawText(textRenderer, entry.name, relativeListX + 6, y + 3, textColor, true);
            
            // Checkmark if enchanted
            if (entry.isEnchanted) {
                context.drawText(textRenderer, "✔", relativeListX + listWidth - 15, y + 3, 0xFF00FF00, true);
            }
            
            // Divider line
            if (i < maxVisible - 1 && index < enchantments.size() - 1) {
                context.fill(relativeListX + 4, y + itemHeight - 1, relativeListX + listWidth - 4, y + itemHeight, 0x33FFFFFF);
            }
        }
        
        // Scrollbar
        if (enchantments.size() > maxVisible) {
            int scrollBarX = relativeListX + listWidth + 1;
            int scrollBarHeight = listHeight - 4;
            int scrollThumbHeight = Math.max(20, scrollBarHeight * maxVisible / enchantments.size());
            int scrollThumbY = relativeListY + 2 + (scrollOffset * (scrollBarHeight - scrollThumbHeight) / (enchantments.size() - maxVisible));
            
            context.fill(scrollBarX, relativeListY + 2, scrollBarX + 4, relativeListY + listHeight - 2, 0x88333333);
            context.fill(scrollBarX + 1, scrollThumbY, scrollBarX + 3, scrollThumbY + scrollThumbHeight, 0xFFAAAAAA);
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int i = this.x;
        int j = this.y;
        
        // Main semi-transparent background
        context.fill(i, j, i + this.backgroundWidth, j + this.backgroundHeight, 0xDD000000);
        
        // Border
        context.fill(i, j, i + this.backgroundWidth, j + 1, 0xFFAAAAAA);
        context.fill(i, j + this.backgroundHeight - 1, i + this.backgroundWidth, j + this.backgroundHeight, 0xFFAAAAAA);
        context.fill(i, j, i + 1, j + this.backgroundHeight, 0xFFAAAAAA);
        context.fill(i + this.backgroundWidth - 1, j, i + this.backgroundWidth, j + this.backgroundHeight, 0xFFAAAAAA);
        
        // Item enchanting slot
        int slotX = i + 7;
        int slotY = j + 19;
        context.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF8B8B8B);
        context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF373737);
        
        // Player inventory slots (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = i + 8 + col * 18;
                int sy = j + 140 + row * 18;
                context.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
                context.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF373737);
            }
        }
        
        // Player hotbar slots
        for (int col = 0; col < 9; col++) {
            int sx = i + 8 + col * 18;
            int sy = j + 198;
            context.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
            context.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF373737);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Title
        context.drawText(textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);
        
        // Player inventory title
        context.drawText(textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);
        
        // Level display - daha görünür yap
        String levelText = "Lvl: " + selectedLevel;
        int levelX = 32;
        int levelY = 7;
        // Arka plan kutusu
        context.fill(levelX - 2, levelY - 2, levelX + textRenderer.getWidth(levelText) + 2, levelY + 10, 0xAA000000);
        // Parlak sarı text
        context.drawText(textRenderer, levelText, levelX, levelY, 0xFFFFFF00, true);
        
        // Enchantments list
        renderEnchantmentList(context, mouseX - this.x, mouseY - this.y);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxVisible = listHeight / itemHeight;
        if (enchantments.size() > maxVisible) {
            int maxScroll = enchantments.size() - maxVisible;
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)verticalAmount));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    // Entry sınıfı - büyü bilgilerini tutar
    private record EnchantEntry(String id, String name, boolean isEnchanted) {}
}
