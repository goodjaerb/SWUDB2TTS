package com.goodjaerb.SWUDB2TTS.json;

public class LeaderCard extends Card {
    public LeaderCard(String id, int count) {
        super(id, count);
    }

    @Override
    public boolean isLeader() {
        return true;
    }
}
