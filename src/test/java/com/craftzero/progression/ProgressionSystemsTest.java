package com.craftzero.progression;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.ExperienceOrbEntity;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ProgressionSystemsTest {

    @Test
    @DisplayName("Experience orbs should use Release-style split values")
    void experienceOrbSplitValues() {
        assertEquals(2477, ExperienceOrbEntity.getOrbValue(5000));
        assertEquals(617, ExperienceOrbEntity.getOrbValue(800));
        assertEquals(17, ExperienceOrbEntity.getOrbValue(20));
        assertEquals(1, ExperienceOrbEntity.getOrbValue(1));
    }

    @Test
    @DisplayName("Armor calculator should apply armor points and protection enchantments")
    void armorReductionIncludesProtection() {
        ItemStack chestplate = new ItemStack(ItemType.IRON_CHESTPLATE, 1);
        chestplate.addEnchantment(new EnchantmentInstance(EnchantmentType.PROTECTION, 4));
        ItemStack[] armor = new ItemStack[4];
        armor[ArmorSlot.CHESTPLATE.getIndex()] = chestplate;

        float reduced = ArmorCalculator.reduceDamage(10.0f, armor, DamageSource.generic());

        assertTrue(reduced < 7.6f, "Protection should reduce damage beyond the iron chestplate armor points");
        assertEquals(6, ArmorCalculator.armorPoints(armor));
    }

    @Test
    @DisplayName("Potion resolver should expose Release 1.0 effect durations")
    void potionEffectsResolve() {
        PotionData speed = new PotionData(PotionType.SWIFTNESS, false, true, false);
        List<StatusEffectInstance> effects = PotionEffectResolver.effects(speed);

        assertEquals(1, effects.size());
        assertSame(StatusEffectType.SPEED, effects.get(0).type());
        assertEquals(9600, effects.get(0).durationTicks());
        assertEquals("Potion of Swiftness Extended", PotionEffectResolver.displayName(speed));
    }

    @Test
    @DisplayName("Enchanting offers should cap at old level 50 and ignore enchanted items")
    void enchantingOfferCapAndExistingEnchantments() {
        ItemStack sword = new ItemStack(ItemType.DIAMOND_SWORD, 1);
        int cost = EnchantmentResolver.offerCost(new Random(1L), 2, 30, sword);
        assertTrue(cost <= 50);

        sword.addEnchantment(new EnchantmentInstance(EnchantmentType.SHARPNESS, 1));
        assertFalse(EnchantmentResolver.isEnchantable(sword));
        assertEquals(0, EnchantmentResolver.offerCost(new Random(1L), 2, 30, sword));
    }
}
