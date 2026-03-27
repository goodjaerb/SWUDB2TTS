package com.goodjaerb.SWUDB2TTS.json;

public class BaseCard extends Card {
    public BaseCard(String id, int count) {
        super(id, count);
    }

    @Override
    public boolean isBase() {
        return true;
    }
}
