package com.goodjaerb.SWUDB2TTS.json;

public class TokenCard extends Card {
    public final String backId;

    public TokenCard(String id) {
        super(id, 1);
        this.backId = null;
    }

    public TokenCard(String id, String backId) {
        super(id, 1);
        this.backId = backId;
    }

    @Override
    public boolean isToken() {
        return true;
    }
}
