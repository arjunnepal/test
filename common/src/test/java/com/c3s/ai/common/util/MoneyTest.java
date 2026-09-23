package com.c3s.ai.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyTest {

    @Test
    void compactMoneyMatchesThePrototypesStyle() {
        assertEquals("842K", Money.compactMoney(842_000));
        assertEquals("12.4K", Money.compactMoney(12_400));
        assertEquals("21.8K", Money.compactMoney(21_830));
        assertEquals("218K", Money.compactMoney(218_000));
    }

    @Test
    void compactMoneyKeepsSmallAmountsAsTheyAre() {
        assertEquals("0", Money.compactMoney(0));
        assertEquals("138", Money.compactMoney(138));
        assertEquals("999", Money.compactMoney(999));
        assertEquals("1K", Money.compactMoney(1_000));
    }

    @Test
    void compactMoneySwitchesToMillions() {
        assertEquals("1.3M", Money.compactMoney(1_250_000));
        // 999,999 would round to "1000K"
        assertEquals("1M", Money.compactMoney(999_999));
    }

    @Test
    void formatsMoneyNumbersAndPercentages() {
        assertEquals("$1,234.56", Money.money(1234.56));
        assertEquals("$0.30", Money.money(0.3));
        assertEquals("12,345", Money.num(12_345));
        assertEquals("13.3", Money.pct1(13.25));
    }

    @Test
    void everythingIsNullSafe() {
        assertEquals("", Money.compactMoney(null));
        assertEquals("", Money.money(null));
        assertEquals("", Money.num(null));
        assertEquals("", Money.pct1(null));
    }
}
