package com.goodjaerb.SWUDB2TTS.json;

import java.util.List;

public class DeckListJson {
    public final Card base;
    public final Card leader;
    public final List<Card> deck;

    private DeckListJson(Card base, Card leader, List<Card> deck) {
        this.base = base;
        this.leader = leader;
        this.deck = deck;
    }

    @Override
    public String toString() {
        return "DeckListJson{" +
                "base=" + base +
                ", leader=" + leader +
                ", deck=" + deck +
                '}';
    }
}
