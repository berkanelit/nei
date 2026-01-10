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
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Environment(EnvType.CLIENT)
public class EnchantmentMenuScreen extends HandledScreen<EnchantmentScreenHandler> {
    private EnchantmentListWidget list;
    private int selectedLevel = 1;
    private ItemStack lastStack = ItemStack.EMPTY;

    public EnchantmentMenuScreen(EnchantmentScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 260;
        this.backgroundHeight = 220;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        
        // List of enchantments - Wider to fit long names
        this.list = new EnchantmentListWidget(this.client, 160, 110, this.y + 20, 14);
        this.list.setX(this.x + 40);
        this.addDrawableChild(this.list); // This handles both clicking and rendering
        
        // Level buttons - Moved further right
        int btnX = this.x + 205;
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), btn -> {
            if (selectedLevel < 10) selectedLevel++;
        }).dimensions(btnX, this.y + 20, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("-"), btn -> {
            if (selectedLevel > 1) selectedLevel--;
        }).dimensions(btnX + 25, this.y + 20, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), btn -> {
            EnchantmentListWidget.Entry entry = this.list.getSelectedOrNull();
            if (entry != null) {
                ClientPlayNetworking.send(new ApplyEnchantmentPayload(entry.id, selectedLevel));
            }
        }).dimensions(btnX, this.y + 45, 50, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), btn -> {
            EnchantmentListWidget.Entry entry = this.list.getSelectedOrNull();
            if (entry != null) {
                ClientPlayNetworking.send(new RemoveEnchantmentPayload(entry.id));
            }
        }).dimensions(btnX, this.y + 70, 50, 20).build());

        refreshEnchantments();
    }

    private void refreshEnchantments() {
        ItemStack stack = handler.getEnchantingStack();
        if (stack.isEmpty()) {
            this.list.replaceEntries(List.of());
            return;
        }

        var registry = this.client.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        ItemEnchantmentsComponent currentEnchants = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        
        List<EnchantmentListWidget.Entry> entries = StreamSupport.stream(registry.getIndexedEntries().spliterator(), false)
                .filter(entry -> entry.value().isSupportedItem(stack)) // Possible method name in 1.21.1
                .map(entry -> {
                    Identifier id = entry.getKey().get().getValue();
                    String name = Enchantment.getName(entry, 1).getString();
                    if (name.endsWith(" I")) name = name.substring(0, name.length() - 2);
                    
                    boolean isEnchanted = false;
                    for (var currentEntry : currentEnchants.getEnchantmentEntries()) {
                        if (currentEntry.getKey().equals(entry)) {
                            isEnchanted = true;
                            name += " (" + currentEntry.getIntValue() + ")";
                            break;
                        }
                    }
                    
                    return this.list.new Entry(id.toString(), name, isEnchanted);
                })
                .sorted(Comparator.comparing(e -> e.name))
                .collect(Collectors.toList());

        this.list.replaceEntries(entries);
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
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
        
        context.drawText(textRenderer, "Level: " + selectedLevel, this.x + 205, this.y + 110, 0xFFFFFF, true);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int i = this.x;
        int j = this.y;
        
        // Main semi-transparent background
        context.fill(i, j, i + this.backgroundWidth, j + this.backgroundHeight, 0xDD000000); 
        context.drawBorder(i, j, this.backgroundWidth, this.backgroundHeight, 0xFFAAAAAA);
        
        // Slot background drawing (Standard 18x18 slot look)
        int slotX = i + 7;
        int slotY = j + 19;
        context.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFFFFFFFF); // White border
        context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF373737); // Dark background
        
        // List area background
        context.fill(this.list.getX() - 1, this.list.getY() - 1, this.list.getX() + this.list.getWidth() + 1, this.list.getY() + this.list.getHeight() + 1, 0xFFAAAAAA);
        context.fill(this.list.getX(), this.list.getY(), this.list.getX() + this.list.getWidth(), this.list.getY() + this.list.getHeight(), 0xFF000000);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, this.title, this.titleX, this.titleY, 0xFFFFFF, false);
        context.drawText(textRenderer, "Enchantments", 40, 6, 0xAAAAAA, false);
    }

    @Environment(EnvType.CLIENT)
    class EnchantmentListWidget extends AlwaysSelectedEntryListWidget<EnchantmentListWidget.Entry> {
        public EnchantmentListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }

        public void replaceEntries(List<Entry> newEntries) {
            this.clearEntries();
            newEntries.forEach(this::addEntry);
        }

        @Override
        public int getRowWidth() { return 150; }

        @Override
        protected int getScrollbarX() { return this.getX() + this.width - 6; }

        @Environment(EnvType.CLIENT)
        public class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
            public final String id;
            public final String name;
            public final boolean isEnchanted;

            public Entry(String id, String name, boolean isEnchanted) {
                this.id = id;
                this.name = name;
                this.isEnchanted = isEnchanted;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int color = isEnchanted ? 0x55FF55 : (this == EnchantmentListWidget.this.getSelectedOrNull() ? 0xFFFF00 : (hovered ? 0xAAAAAA : 0xFFFFFF));
                context.drawText(textRenderer, this.name, x + 2, y + 2, color, false);
                
                if (isEnchanted) {
                    context.drawText(textRenderer, "✔", x + entryWidth - 12, y + 2, 0x55FF55, false);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                EnchantmentListWidget.this.setSelected(this);
                return true;
            }

            @Override
            public Text getNarration() { return Text.literal(name); }
        }
    }
}
