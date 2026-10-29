package com.c3s.ai.browse.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlUtilTest {

    @Test
    void compactMoneyMatchesThePrototypesStyle() {
        assertEquals("842K", HtmlUtil.compactMoney(842_000));
        assertEquals("12.4K", HtmlUtil.compactMoney(12_400));
        assertEquals("21.8K", HtmlUtil.compactMoney(21_830));
        assertEquals("218K", HtmlUtil.compactMoney(218_000));
    }

    @Test
    void compactMoneyKeepsSmallAmountsAsTheyAre() {
        assertEquals("0", HtmlUtil.compactMoney(0));
        assertEquals("138", HtmlUtil.compactMoney(138));
        assertEquals("999", HtmlUtil.compactMoney(999));
        assertEquals("1K", HtmlUtil.compactMoney(1_000));
    }

    @Test
    void compactMoneySwitchesToMillions() {
        assertEquals("1.3M", HtmlUtil.compactMoney(1_250_000));
        // 999,999 would round to "1000K"
        assertEquals("1M", HtmlUtil.compactMoney(999_999));
    }

    @Test
    void compactMoneyIsNullSafe() {
        assertEquals("", HtmlUtil.compactMoney(null));
    }

    @Test
    void escEscapesHtml() {
        assertEquals("L. &amp; T. Okafor &lt;b&gt; &#34;x&#34; &#39;y&#39;", HtmlUtil.esc("L. & T. Okafor <b> \"x\" 'y'"));
        assertEquals("", HtmlUtil.esc(null));
    }
}
