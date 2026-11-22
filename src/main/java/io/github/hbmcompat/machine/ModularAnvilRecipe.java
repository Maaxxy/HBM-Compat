package io.github.hbmcompat.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.item.ItemStack;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilOutput;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.util.Platform;

public final class ModularAnvilRecipe {

    static final int ANY_TIER = Integer.MIN_VALUE;

    private final List<ItemStack> inputs;
    private final List<AnvilOutput> outputs;

    private ModularAnvilRecipe(List<ItemStack> inputs, List<AnvilOutput> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public static ModularAnvilRecipe match(ICraftingPatternDetails details, int tier, boolean local) {
        if (details == null || details.isCraftable() || details.isInputOnly()) return null;
        PatternStacks inputs = PatternStacks.inputs(details);
        PatternStacks outputs = PatternStacks.outputs(details);
        if (!itemOnly(inputs) || !itemOnly(outputs)) return null;

        List<ItemStack> in = inputs.getItems();
        List<ItemStack> out = outputs.getItems();
        ModularAnvilRecipe found = null;
        for (AnvilConstructionRecipe recipe : AnvilRecipes.getConstruction()) {
            if ((tier != ANY_TIER && !recipe.isTierValid(tier)) || !matchesConstruction(recipe.input, in)
                    || !matchesOutputs(recipe.output, out, local)) continue;
            if (found != null) return null;
            found = new ModularAnvilRecipe(in, recipe.output);
            if (tier == ANY_TIER) return found;
        }

        if (in.size() != 2) return found;
        for (AnvilSmithingRecipe recipe : AnvilRecipes.getSmithing()) {
            // Specialized smithing uses player actions, heat/NBT or reusable molds.
            if (recipe.getClass() != AnvilSmithingRecipe.class || (tier != ANY_TIER && recipe.tier > tier)) continue;
            ItemStack product = smith(recipe, in.get(0), in.get(1));
            if (product == null) product = smith(recipe, in.get(1), in.get(0));
            if (product == null || !sameItems(Collections.singletonList(product), out)) continue;
            if (found != null) return null;
            found = new ModularAnvilRecipe(in, Collections.singletonList(new AnvilOutput(product)));
            if (tier == ANY_TIER) return found;
        }
        return found;
    }

    private static ItemStack smith(AnvilSmithingRecipe recipe, ItemStack left, ItemStack right) {
        int order = recipe.matchesInt(left, right);
        if (order < 0 || left.stackSize != recipe.amountConsumed(0, order == 1)
                || right.stackSize != recipe.amountConsumed(1, order == 1)) return null;
        return recipe.getOutput(left, right);
    }

    private static boolean itemOnly(PatternStacks stacks) {
        return stacks.isValid() && stacks.getFluids().isEmpty() && !stacks.getItems().isEmpty();
    }

    private static boolean matchesConstruction(List<AStack> ingredients, List<ItemStack> inputs) {
        ItemStack[] stock = copy(inputs.toArray(new ItemStack[0]));
        for (AStack ingredient : ingredients) {
            int needed = ingredient.stacksize;
            for (ItemStack stack : stock) {
                if (needed == 0) break;
                if (stack.stackSize <= 0 || !ingredient.matchesRecipe(stack, true)) continue;
                int taken = Math.min(needed, stack.stackSize);
                stack.stackSize -= taken;
                needed -= taken;
            }
            if (needed != 0) return false;
        }
        return empty(stock);
    }

    private static boolean matchesOutputs(List<AnvilOutput> outputs, List<ItemStack> expected, boolean local) {
        List<ItemStack> guaranteed = new ArrayList<>();
        List<ItemStack> possible = new ArrayList<>();
        for (AnvilOutput output : outputs) {
            if (output == null || output.stack == null || output.stack.getItem() == null
                    || output.stack.stackSize <= 0) return false;
            if (output.chance >= 1F) guaranteed.add(output.stack);
            if (output.chance > 0F) possible.add(output.stack);
        }
        return sameItems(guaranteed, expected) || (local && sameItems(possible, expected));
    }

    public boolean accepts(PatternStacks supplied) {
        return itemOnly(supplied) && sameItems(inputs, supplied.getItems());
    }

    public ItemStack[] remainingInputs(ItemStack[] stock) {
        return subtract(stock, inputs);
    }

    private static boolean sameItems(List<ItemStack> expected, List<ItemStack> actual) {
        ItemStack[] remaining = subtract(actual.toArray(new ItemStack[0]), expected);
        return remaining != null && empty(remaining);
    }

    private static ItemStack[] subtract(ItemStack[] stock, List<ItemStack> required) {
        ItemStack[] remaining = copy(stock);
        for (ItemStack input : required) {
            int needed = input.stackSize;
            for (ItemStack stack : remaining) {
                if (needed == 0) break;
                if (stack == null || stack.stackSize <= 0 || !Platform.isSameItemPrecise(input, stack)) continue;
                int taken = Math.min(needed, stack.stackSize);
                stack.stackSize -= taken;
                needed -= taken;
            }
            if (needed != 0) return null;
        }
        for (int slot = 0; slot < remaining.length; slot++) {
            if (remaining[slot] != null && remaining[slot].stackSize == 0) remaining[slot] = null;
        }
        return remaining;
    }

    private static ItemStack[] copy(ItemStack[] stacks) {
        ItemStack[] result = new ItemStack[stacks.length];
        for (int slot = 0; slot < stacks.length; slot++) {
            if (stacks[slot] != null) result[slot] = stacks[slot].copy();
        }
        return result;
    }

    private static boolean empty(ItemStack[] stacks) {
        for (ItemStack stack : stacks) {
            if (stack != null && stack.stackSize > 0) return false;
        }
        return true;
    }

    public List<ItemStack> rollOutputs(Random random) {
        List<ItemStack> products = new ArrayList<>();
        for (AnvilOutput output : outputs) {
            if (!(output.chance >= 1F) && !(random.nextFloat() < output.chance)) continue;
            ItemStack stack = output.stack.copy();
            int limit = Math.min(64, stack.getMaxStackSize());
            while (stack.stackSize > limit) products.add(stack.splitStack(limit));
            products.add(stack);
        }
        return products;
    }
}
