package com.goodjaerb.SWUDB2TTS.util;

import com.google.gson.GsonBuilder;
import javafx.util.StringConverter;

import java.lang.reflect.Type;
import java.security.InvalidParameterException;

public class JsonStringConverter<T> extends StringConverter<T> {

    private final Class<T> clazz;
    private final Object[] customAdapters;

    public JsonStringConverter(Class<T> clazz, Object... customAdapters) {
        super();

        if(customAdapters.length % 2 != 0) {
            throw new InvalidParameterException("customAdapters length must be even.");
        }
        this.clazz = clazz;
        this.customAdapters = customAdapters;
    }

    public JsonStringConverter(Class<T> clazz) {
        this(clazz, new Object[] { });
    }

    @Override
    public String toString(T object) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(object, clazz);
    }

    @Override
    public T fromString(String string) {
        GsonBuilder builder = new GsonBuilder();
        for(int i = 0; i < customAdapters.length; i += 2) {
            builder.registerTypeAdapter((Type)customAdapters[i], customAdapters[i + 1]);
        }

        return builder.create().fromJson(string, clazz);
    }
}
