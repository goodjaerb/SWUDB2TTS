package com.goodjaerb.SWUDB2TTS.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeckListJson {
    public final Metadata metadata;
    public final BaseCard base;
    public final LeaderCard leader;
    public final LeaderCard secondleader;
    public final List<Card> deck;
    public final List<Card> sideboard;

    @Override
    public String toString() {
        return "DeckListJson{" +
                "metadata=" + metadata +
                ", base=" + base +
                ", leader=" + leader +
                ", secondleader=" + secondleader +
                ", deck=" + deck +
                ", sideboard=" + sideboard +
                '}';
    }

    public List<Card> getExpandedCardList(boolean includeSideboard, boolean addTokenCards) {
        List<Card> cardList = new ArrayList<>();
        cardList.add(leader);
        if(secondleader != null) {
            cardList.add(secondleader);
        }
        cardList.add(base);
        for(Card c : deck) {
            for(int i = 0; i < c.count; i++) {
                cardList.add(c);
            }
        }

        if(includeSideboard && sideboard != null && !sideboard.isEmpty()) {
            for(Card c : sideboard) {
                for(int i = 0; i < c.count; i++) {
                    cardList.add(c);
                }
            }
        }

        if(addTokenCards) {
            cardList.add(new TokenCard("token_battledroid", "token_clonetrooper"));
            cardList.add(new TokenCard("token_experience", "token_shield"));
            cardList.add(new TokenCard("token_tiefighter", "token_xwing"));
            cardList.add(new TokenCard("token_credit"));
            cardList.add(new TokenCard("token_force"));
            cardList.add(new TokenCard("token_spy"));
        }
        return Collections.unmodifiableList(cardList);
    }

    private DeckListJson(Metadata metadata, BaseCard base, LeaderCard leader, LeaderCard secondleader, List<Card> deck, List<Card> sideboard) {
        this.metadata = metadata;
        this.base = base;
        this.leader = leader;
        this.secondleader = secondleader;
        this.deck = deck;
        this.sideboard = sideboard;
    }

}
