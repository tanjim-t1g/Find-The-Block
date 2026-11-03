package com.findtheblock.ui;

import com.findtheblock.util.Chat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Iterator;
import java.util.List;

/**
 * Visual recipe & acquisition guide GUI for the active match target.
 */
public class RecipeMenu implements InventoryHolder {

    private final Inventory inventory;

    public RecipeMenu(Material target) {
        this.inventory = Bukkit.createInventory(this, 45, Chat.toComponent("<gold><bold>Recipe Guide: </bold><yellow>" + target.name() + "</yellow></gold>"));
        buildGuide(target);
    }

    private void buildGuide(Material target) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        if (fMeta != null) {
            fMeta.displayName(Chat.toComponent(" "));
            filler.setItemMeta(fMeta);
        }
        for (int i = 0; i < 45; i++) inventory.setItem(i, filler);

        ItemStack result = new ItemStack(target);
        inventory.setItem(24, result);

        List<Recipe> recipes = Bukkit.getRecipesFor(result);
        if (!recipes.isEmpty()) {
            Recipe r = recipes.get(0);
            if (r instanceof ShapedRecipe shaped) {
                int[] gridSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
                String[] shape = shaped.getShape();
                int slotIdx = 0;
                for (String row : shape) {
                    for (char c : row.toCharArray()) {
                        RecipeChoice choice = shaped.getChoiceMap().get(c);
                        if (choice != null) {
                            inventory.setItem(gridSlots[slotIdx], choice.getItemStack());
                        } else {
                            inventory.setItem(gridSlots[slotIdx], new ItemStack(Material.AIR));
                        }
                        slotIdx++;
                    }
                }
                return;
            } else if (r instanceof ShapelessRecipe shapeless) {
                int[] gridSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
                int slotIdx = 0;
                for (RecipeChoice choice : shapeless.getChoiceList()) {
                    if (slotIdx < gridSlots.length) {
                        inventory.setItem(gridSlots[slotIdx++], choice.getItemStack());
                    }
                }
                return;
            } else if (r instanceof CookingRecipe<?> cooking) {
                inventory.setItem(11, cooking.getInputChoice().getItemStack());
                inventory.setItem(20, new ItemStack(Material.CAMPFIRE));
                inventory.setItem(29, new ItemStack(Material.COAL));
                return;
            }
        }

        // If no direct crafting recipe, display an acquisition hint book
        ItemStack hintBook = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta bMeta = hintBook.getItemMeta();
        if (bMeta != null) {
            bMeta.displayName(Chat.toComponent("<gold><bold>Exploration Item</bold></gold>"));
            bMeta.lore(List.of(
                    Chat.toComponent("<gray>This item cannot be crafted directly.</gray>"),
                    Chat.toComponent("<yellow>Obtain via mining, chest looting, bartering,</yellow>"),
                    Chat.toComponent("<yellow>or Nether exploration!</yellow>")
            ));
            hintBook.setItemMeta(bMeta);
        }
        inventory.setItem(20, hintBook);
    }

    public void open(Player player) {
        if (player != null && player.isOnline()) {
            player.openInventory(inventory);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
