package dev.charles.multigolem.ability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZombieGolemConversionTest {

    @Test
    void conversionRollNeverDealsNormalDamage() {
        assertEquals(ZombieGolemConversion.Outcome.CONVERT, ZombieGolemConversion.roll(1.0, 0.99));
        assertEquals(ZombieGolemConversion.Outcome.FAILED_ROLL_NO_DAMAGE, ZombieGolemConversion.roll(0.0, 0.0));
        assertEquals(ZombieGolemConversion.Outcome.FAILED_ROLL_NO_DAMAGE, ZombieGolemConversion.roll(0.25, 0.25));
    }

    @Test
    void disabledConversionStillSuppressesNormalDamage() {
        assertEquals(ZombieGolemConversion.Outcome.FAILED_ROLL_NO_DAMAGE,
            ZombieGolemConversion.roll(false, 1.0, 0.0));
    }
}
