package com.berkan.neienchant.client.screen;

import com.berkan.neienchant.network.ApplyEnchantmentPayload;
import com.berkan.neienchant.network.RemoveEnchantmentPayload;
import com.berkan.neienchant.screen.EnchantmentScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public class EnchantmentMenuScreen extends HandledScreen<EnchantmentScreenHandler> {
    private List<EnchantEntry> enchantments = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int selectedLevel = 1;
    private ItemStack lastStack = ItemStack.EMPTY;

    // Button references (for active/inactive control)
    private ButtonWidget applyButton;
    private ButtonWidget removeButton;

    // List area coordinates (fixed) — right panel
    private static final int LIST_X = 90;
    private static final int LIST_Y = 12;
    private int listWidth = 148;
    private int listHeight = 112;
    private int itemHeight = 14;

    public EnchantmentMenuScreen(EnchantmentScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 248;
        this.backgroundHeight = 222;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // Left panel layout:
        // y+19 → item slot
        // y+42 → Level [-] [ 1 ] [+]
        // y+57 → Apply
        // y+73 → Remove

        // Level - button
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), btn -> {
            if (selectedLevel > 1) selectedLevel--;
        }).dimensions(this.x + 8, this.y + 42, 16, 12).build());

        // Level + button
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), btn -> {
            if (selectedLevel < 10) selectedLevel++;
        }).dimensions(this.x + 56, this.y + 42, 16, 12).build());

        // Apply button
        applyButton = ButtonWidget.builder(Text.literal("Apply"), btn -> {
            if (selectedIndex >= 0 && selectedIndex < enchantments.size()) {
                EnchantEntry entry = enchantments.get(selectedIndex);
                ClientPlayNetworking.send(new ApplyEnchantmentPayload(entry.id, selectedLevel));
            }
        }).dimensions(this.x + 8, this.y + 57, 72, 14).build();
        addDrawableChild(applyButton);

        // Remove button
        removeButton = ButtonWidget.builder(Text.literal("Remove"), btn -> {
            if (selectedIndex >= 0 && selectedIndex < enchantments.size()) {
                EnchantEntry entry = enchantments.get(selectedIndex);
                ClientPlayNetworking.send(new RemoveEnchantmentPayload(entry.id));
            }
        }).dimensions(this.x + 8, this.y + 73, 72, 14).build();
        addDrawableChild(removeButton);

        // Buttons inactive initially
        applyButton.active = false;
        removeButton.active = false;

        refreshEnchantments();
    }

    private void refreshEnchantments() {
        // Save current selection by ID
        String previouslySelectedId = (selectedIndex >= 0 && selectedIndex < enchantments.size())
                ? enchantments.get(selectedIndex).id : null;

        ItemStack stack = handler.getEnchantingStack();
        enchantments.clear();
        selectedIndex = -1;

        if (stack.isEmpty() || this.client == null || this.client.world == null) {
            clampScrollOffset();
            return;
        }

        var registry = this.client.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        ItemEnchantmentsComponent currentEnchants = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

        // Iterate all enchantments
        for (var entry : registry.getIndexedEntries()) {
            try {
                Enchantment enchantment = entry.value();

                // Check if the item accepts this enchantment
                if (!enchantment.isAcceptableItem(stack)) {
                    continue;
                }

                // Get enchantment ID and display name
                Identifier id = entry.getKey().get().getValue();
                String name = Enchantment.getName(entry, 1).getString();
                if (name.endsWith(" I")) {
                    name = name.substring(0, name.length() - 2);
                }

                // Check if this enchantment is already applied
                boolean isEnchanted = false;
                for (var currentEntry : currentEnchants.getEnchantmentEntries()) {
                    if (currentEntry.getKey().equals(entry)) {
                        isEnchanted = true;
                        name += " (" + currentEntry.getIntValue() + ")";
                        break;
                    }
                }

                // Check for conflicts with existing enchantments
                String conflictsWith = null;
                if (!isEnchanted) { // skip conflict check if already applied
                    for (var currentEntry : currentEnchants.getEnchantmentEntries()) {
                        if (!Enchantment.canBeCombined(entry, currentEntry.getKey())) {
                            // Get display name of conflicting enchantment
                            String cName = Enchantment.getName(currentEntry.getKey(), 1).getString();
                            if (cName.endsWith(" I")) cName = cName.substring(0, cName.length() - 2);
                            conflictsWith = cName;
                            break;
                        }
                    }
                }

                enchantments.add(new EnchantEntry(id.toString(), name, isEnchanted, enchantment.getMaxLevel(), conflictsWith));
            } catch (Exception e) {
                // Skip this enchantment
            }
        }

        // Sort alphabetically using locale-aware Collator
        Collator collator = Collator.getInstance(Locale.getDefault());
        enchantments.sort((a, b) -> collator.compare(a.name, b.name));

        // Restore previous selection
        if (previouslySelectedId != null) {
            for (int i = 0; i < enchantments.size(); i++) {
                if (enchantments.get(i).id.equals(previouslySelectedId)) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        clampScrollOffset();
    }

    /** Clamp scrollOffset to valid range based on current list size */
    private void clampScrollOffset() {
        int maxVisible = listHeight / itemHeight;
        int maxScroll = Math.max(0, enchantments.size() - maxVisible);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        ItemStack stack = handler.getEnchantingStack();
        if (!ItemStack.areEqual(stack, lastStack)) {
            lastStack = stack.copy();
            refreshEnchantments();
        }

        // Update button states
        boolean hasSelection = selectedIndex >= 0 && selectedIndex < enchantments.size();
        boolean hasItem = !handler.getEnchantingStack().isEmpty();
        if (applyButton != null) applyButton.active = hasSelection && hasItem
                && (enchantments.get(selectedIndex).conflictsWith() == null);
        if (removeButton != null) {
            removeButton.active = hasSelection && hasItem
                    && enchantments.get(selectedIndex).isEnchanted();
        }

        // Prevent selected level from exceeding the enchantment's max level
        if (hasSelection) {
            int maxLvl = enchantments.get(selectedIndex).maxLevel();
            if (selectedLevel > maxLvl) selectedLevel = maxLvl;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean over) {
        // Handle list click on left mouse button (button 0)
        if (click.button() == 0) {
            handleListClick(click.x() - this.x, click.y() - this.y);
        }
        return super.mouseClicked(click, over);
    }
    
    private void handleListClick(double mouseX, double mouseY) {
        // Use consistent coordinates with fixed LIST_X / LIST_Y constants
        if (mouseX >= LIST_X && mouseX < LIST_X + listWidth &&
            mouseY >= LIST_Y && mouseY < LIST_Y + listHeight) {

            int clickedIndex = (int)((mouseY - LIST_Y) / itemHeight) + scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < enchantments.size()) {
                selectedIndex = clickedIndex;
            }
        }
    }

    
    private void renderEnchantmentList(DrawContext context, int mouseX, int mouseY) {
        // Use fixed coordinates (dimensions come from fields)
        int relativeListX = LIST_X;
        int relativeListY = LIST_Y;
        
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
            
            // Determine text color and background
            int textColor;
            int bgColor = 0x00000000;
            boolean hasConflict = entry.conflictsWith() != null;

            if (entry.isEnchanted()) {
                textColor = 0xFF55FF55; // bright green
                bgColor = 0x5500AA00;  // dark green background
            } else if (hasConflict) {
                textColor = 0xFFFF5555; // red
                bgColor = (index == selectedIndex) ? 0x55AA0000 : 0x00000000; // dark red if selected
            } else if (index == selectedIndex) {
                textColor = 0xFFFFAA00; // orange
                bgColor = 0x55FF8800;
            } else if (hovered) {
                textColor = 0xFFFFFFFF; // white
                bgColor = 0x55666666;
            } else {
                textColor = 0xFFCCCCCC; // light grey
            }
            
            // Hover/selected background
            if (bgColor != 0x00000000) {
                context.fill(relativeListX + 1, y, relativeListX + listWidth - 1, y + itemHeight, bgColor);
            }
            
            // Left colored bar
            if (entry.isEnchanted()) {
                context.fill(relativeListX + 1, y + 2, relativeListX + 3, y + itemHeight - 2, 0xFF00FF00);
            } else if (hasConflict) {
                context.fill(relativeListX + 1, y + 2, relativeListX + 3, y + itemHeight - 2, 0xFFFF3333);
            } else if (index == selectedIndex) {
                context.fill(relativeListX + 1, y + 2, relativeListX + 3, y + itemHeight - 2, 0xFFFFAA00);
            }

            // Draw name
            context.drawText(textRenderer, entry.name(), relativeListX + 6, y + 3, textColor, true);

            // Right icon: green ✔ (applied), red ✖ (conflict)
            if (entry.isEnchanted()) {
                context.drawText(textRenderer, "✔", relativeListX + listWidth - 15, y + 3, 0xFF00FF00, true);
            } else if (hasConflict) {
                context.drawText(textRenderer, "✖", relativeListX + listWidth - 15, y + 3, 0xFFFF4444, true);
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

        // ── Main background ──
        context.fill(i, j, i + backgroundWidth, j + backgroundHeight, 0xE8101010);

        // Left panel slightly lighter
        context.fill(i + 1, j + 1, i + 83, j + 129, 0x18FFFFFF);

        // Outer border (1px)
        context.fill(i,                         j,                          i + backgroundWidth,     j + 1,               0xFF666666); // top
        context.fill(i,                         j + backgroundHeight - 1,   i + backgroundWidth,     j + backgroundHeight, 0xFF444444); // bottom
        context.fill(i,                         j,                          i + 1,                   j + backgroundHeight, 0xFF666666); // left
        context.fill(i + backgroundWidth - 1,   j,                          i + backgroundWidth,     j + backgroundHeight, 0xFF444444); // right

        // Vertical divider (left panel | right list)
        context.fill(i + 83, j + 1,   i + 84, j + 128, 0xFF444444);
        context.fill(i + 84, j + 1,   i + 85, j + 128, 0xFF222222);

        // Horizontal divider (top controls | inventory)
        context.fill(i + 1,  j + 128, i + backgroundWidth - 1, j + 129, 0xFF444444);
        context.fill(i + 1,  j + 129, i + backgroundWidth - 1, j + 130, 0xFF222222);

        // Item slot frame — handler slot x=8,y=20 → 1px border outside
        int slotX = i + 7;
        int slotY = j + 19;
        context.fill(slotX, slotY, slotX + 20, slotY + 20, 0xFF555555); // outer
        context.fill(slotX + 1, slotY + 1, slotX + 19, slotY + 19, 0xFF111111); // inner

        // Level indicator frame (26..53, y+43..y+53)
        context.fill(i + 25, j + 43, i + 55, j + 54, 0xFF333333);
        context.fill(i + 26, j + 44, i + 54, j + 53, 0xFF1A1A1A);

        // Player inventory slots (3 rows, centered at x=43)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = i + 43 + col * 18;
                int sy = j + 140 + row * 18;
                context.fill(sx, sy, sx + 18, sy + 18, 0xFF555555);
                context.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF1A1A1A);
            }
        }

        // Hotbar slots
        for (int col = 0; col < 9; col++) {
            int sx = i + 43 + col * 18;
            int sy = j + 198;
            context.fill(sx, sy, sx + 18, sy + 18, 0xFF666666);
            context.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF1A1A1A);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // ── Title (top left) ──
        context.drawText(textRenderer, "NEI Enchant", 5, 5, 0xFFCCCCCC, false);

        // ── Left panel labels ──
        // "Item:" label
        context.drawText(textRenderer, "Item:", 8, 10, 0xFF888888, false);

        // Hint when item slot is empty
        if (handler.getEnchantingStack().isEmpty()) {
            context.drawText(textRenderer, "Place", 10, 22, 0xFF444444, false);
            context.drawText(textRenderer, "item",  12, 30, 0xFF444444, false);
        }

        // "Level" label (above button row)
        context.drawText(textRenderer, "Level", 8, 35, 0xFF888888, false);

        // Level number (centered between the two buttons)
        String lvlNum = String.valueOf(selectedLevel);
        int lvlNumX = 40 - textRenderer.getWidth(lvlNum) / 2; // center at x=40
        context.drawText(textRenderer, lvlNum, lvlNumX, 45, 0xFFFFDD44, true);

        // ── Selected enchantment info ──
        if (selectedIndex >= 0 && selectedIndex < enchantments.size()) {
            EnchantEntry sel = enchantments.get(selectedIndex);

            if (sel.conflictsWith() != null) {
                // Conflict warning — red box in left panel
                context.fill(7, 88, 77, 118, 0xAA330000); // warning box background
                context.fill(7, 88, 77, 89, 0xFFAA0000);  // top border line
                context.drawText(textRenderer, "\u00a7cConflicts with:", 9, 91, 0xFFFF5555, false);
                // Truncate if too long
                String cName = sel.conflictsWith();
                if (textRenderer.getWidth(cName) > 64) {
                    cName = cName.substring(0, 9) + "…";
                }
                context.drawText(textRenderer, "\u00a7e" + cName, 9, 102, 0xFFFFCC44, false);
                context.drawText(textRenderer, "\u00a77Can't apply!", 9, 112, 0xFF888888, false);
            } else {
                // Normal info
                context.drawText(textRenderer, "Max Lvl: " + sel.maxLevel(), 8, 90, 0xFF666666, false);
                if (selectedLevel > sel.maxLevel()) {
                    context.drawText(textRenderer, "\u00a7c> max!", 8, 100, 0xFFFF4444, false);
                }
                String shortId = sel.id().contains(":") ? sel.id().split(":")[1] : sel.id();
                String idText = shortId.length() > 13 ? shortId.substring(0, 12) + "…" : shortId;
                context.drawText(textRenderer, idText, 8, 110, 0xFF444444, false);
            }
        }

        // ── Right panel header ──
        int enchCount = enchantments.size();
        int appliedCount = (int) enchantments.stream().filter(EnchantEntry::isEnchanted).count();
        String listHeader = "Enchantments  " + appliedCount + "/" + enchCount;
        context.drawText(textRenderer, listHeader, LIST_X, 3, 0xFF999999, false);

        // ── Player inventory title (centered) ──
        int invTitleWidth = textRenderer.getWidth(this.playerInventoryTitle);
        int invTitleX = (backgroundWidth - invTitleWidth) / 2;
        context.drawText(textRenderer, this.playerInventoryTitle, invTitleX, this.playerInventoryTitleY, 0xFF777777, false);

        // ── Enchantment list ──
        renderEnchantmentList(context, mouseX - this.x, mouseY - this.y);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxVisible = listHeight / itemHeight;
        if (enchantments.size() > maxVisible) {
            int maxScroll = enchantments.size() - maxVisible;
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(verticalAmount)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    // Entry class — holds enchantment data
    // conflictsWith: null = no conflict, non-null = name of the conflicting enchantment
    private record EnchantEntry(String id, String name, boolean isEnchanted, int maxLevel, String conflictsWith) {}
}
