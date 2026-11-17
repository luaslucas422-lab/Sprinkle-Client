package fr.darkerdawn.sprinkle;

import fr.darkerdawn.sprinkle.feature.Coords;
import fr.darkerdawn.sprinkle.feature.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PureTest {
    @Test
    void parsesMoney() {
        assertEquals(1_250_000, Money.parse("$ Money: $1.25M"), 1);
        assertEquals(12_500, Money.parse("Money: 12,500"), 0.01);
        assertEquals(1.2e9, Money.parse("Balance $1.2b"), 1);
        assertEquals(500, Money.parse("$500"), 0.01);
        assertEquals(-1, Money.parse("Shards"), 0.01);
        assertEquals(3_000, Money.parse("§a$§f3K"), 0.01);
        assertTrue(Money.isMoneyLine("$ Money 1.2M"));
        assertFalse(Money.isMoneyLine("Kills 4"));
    }

    @Test
    void formatsMoney() {
        assertEquals("$999", Money.format(999));
        assertEquals("$1.25M", Money.format(1_250_000));
        assertEquals("$12.5K", Money.format(12_500));
        assertEquals("$100M", Money.format(100_000_000));
        assertEquals("-$2.4M", Money.format(-2_400_000));
        assertEquals(1e6, Money.milestone(3.2e6));
        assertEquals(0, Money.milestone(500));
    }

    @Test
    void censorsCoords() {
        assertEquals("XYZ: ??? / ??? / ???", Coords.censor("XYZ: 12.500 / 64.000 / -30.250"));
        assertEquals("meet me at ???, ???, ???", Coords.censor("meet me at 120, 64, -3000"));
        assertEquals("x: ??? z: ???", Coords.censor("x: 120 z: -3000"));
        assertEquals("I have 5 diamonds", Coords.censor("I have 5 diamonds"));
        assertTrue(Coords.hasCoords("base at 100 70 200"));
    }
}
