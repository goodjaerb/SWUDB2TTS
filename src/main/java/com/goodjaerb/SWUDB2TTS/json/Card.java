package com.goodjaerb.SWUDB2TTS.json;

public class Card {
    public final String id;
    public final int count;

    public Card(String id, int count) {
        this.id = id;
        this.count = count;
    }

    @Override
    public String toString() {
        return "Card{" +
                "id='" + id + '\'' +
                ", count=" + count +
                '}';
    }
}
