module com.goodjaerb.SWUDB2TTS {
    requires javafx.controls;

    requires com.google.gson;
    requires java.net.http;

    exports com.goodjaerb.SWUDB2TTS;

    opens com.goodjaerb.SWUDB2TTS.json to com.google.gson;
}