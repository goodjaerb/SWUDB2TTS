package com.goodjaerb.SWUDB2TTS.json;

import java.awt.image.BufferedImage;

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

    @Override
    public String toString() {
        return "Card{" +
                "id='" + id + '\'' +
                ", count=" + count +
                '}';
    }
}
