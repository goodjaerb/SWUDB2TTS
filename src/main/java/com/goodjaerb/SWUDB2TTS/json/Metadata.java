package com.goodjaerb.SWUDB2TTS.json;

public class Metadata {
    public final String name;

    @Override
    public String toString() {
        return "Metadata{" +
                "name='" + name + '\'' +
                '}';
    }

    public Metadata(String name) {
        this.name = name;
    }
}
