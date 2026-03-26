package com.goodjaerb.SWUDB2TTS.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeckListJson {
    public final Metadata metadata;
    public final Card base;
    public final Card leader;
    public final Card secondleader;
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

    public List<Card> getExpandedCardList() {
        List<Card> cardList = new ArrayList<>();
        cardList.add(base);
        cardList.add(leader);
        if(secondleader != null) {
            cardList.add(secondleader);
        }
        if(sideboard != null && !sideboard.isEmpty()) {
            for(Card c : sideboard) {
                for(int i = 0; i < c.count; i++) {
                    cardList.add(c);
                }
            }
        }

        for(Card c : deck) {
            for(int i = 0; i < c.count; i++) {
                cardList.add(c);
            }
        }
        return Collections.unmodifiableList(cardList);
    }

//    public int numCards() {
//        int count = 2; //start with 2 because base and leader should be automatic.
//        if(secondleader != null) {
//            count++;
//        }
//
//        for(Card c : deck) {
//            count += c.count;
//        }
//
//        if(sideboard != null && !sideboard.isEmpty()) {
//            for(Card c : sideboard) {
//                count += c.count;
//            }
//        }
//        return count;
//    }

    private DeckListJson(Metadata metadata, Card base, Card leader, Card secondleader, List<Card> deck, List<Card> sideboard) {
        this.metadata = metadata;
        this.base = base;
        this.leader = leader;
        this.secondleader = secondleader;
        this.deck = deck;
        this.sideboard = sideboard;
    }

}
