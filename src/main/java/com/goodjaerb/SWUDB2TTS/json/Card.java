package com.goodjaerb.SWUDB2TTS.json;

public class Card {
    public String id;
    public final int count;

    public Card(String id, int count) {
        this.id = id;
        this.count = count;
    }

    public boolean isLeader() {
        return false;
    }

    public boolean isBase() {
        return false;
    }

    public boolean isToken() {
        return false;
    }

    @Override
    public String toString() {
        return "Card{" +
                "id='" + id + '\'' +
                ", count=" + count +
                '}';
    }
}
