package com.goodjaerb.SWUDB2TTS;

import com.goodjaerb.SWUDB2TTS.json.Card;
import com.goodjaerb.SWUDB2TTS.json.DeckListJson;
import com.goodjaerb.SWUDB2TTS.util.JsonStringConverter;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class SWUDB2TTS extends Application {
    private static final String USER_AGENT_PROPERTY = "User-Agent";
    private static final String USER_AGENT_MOZILLA  = "Mozilla/5.0";
    private static final String SWUDB_IMAGE_URL = "https://www.swudb.com/cdn-cgi/image/quality=95/images/cards/%SET%/%ID%.png";
    private static final StringConverter<DeckListJson> DECKLIST_JSON_CONVERTER = new JsonStringConverter<>(DeckListJson.class);

    private final Label jsonBrowseLabel = new Label("Deck JSON:");
    private final TextField jsonBrowseField = new TextField();
    private final Button jsonBrowseButton = new Button("Browse...");

    private final Button createDeckGridButton = new Button("Create Deck Grid");

    @Override
    public void start(Stage stage) {
        jsonBrowseField.setPrefWidth(400);
        jsonBrowseField.setEditable(false);
        jsonBrowseField.setDisable(true);

        addListeners(stage);

        FlowPane browsePane = new FlowPane(Orientation.HORIZONTAL);
        browsePane.setHgap(3);
        browsePane.getChildren().addAll(jsonBrowseLabel, jsonBrowseField, jsonBrowseButton);

        VBox vertPane = new VBox(3);
        vertPane.setPadding(new Insets(4, 4, 4, 4));
        vertPane.getChildren().addAll(browsePane, createDeckGridButton);

        Scene scene = new Scene(vertPane);
        stage.setScene(scene);
        stage.setTitle("SWUDB2TTS");
        stage.show();
    }

    private void addListeners(Stage stage) {
        jsonBrowseButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
//            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "json"));
            chooser.setTitle("Select Deck JSON");
            chooser.setInitialDirectory(new File(System.getProperty("user.home")));

            File selectedFile = chooser.showOpenDialog(stage);
            if(selectedFile != null) {
                jsonBrowseField.setText(selectedFile.getAbsolutePath());
            }
        });

        createDeckGridButton.setOnAction(event -> {
            if(!jsonBrowseField.getText().isBlank() && Files.exists(Path.of(jsonBrowseField.getText()))) {
                try {
                    Path selectedFilePath = Path.of(jsonBrowseField.getText());
                    DeckListJson deckList = DECKLIST_JSON_CONVERTER.fromString(Files.readString(selectedFilePath));
                    System.out.println(deckList);

                    try(HttpClient client = HttpClient.newBuilder().build()) {
                        List<String> headers = new ArrayList<>();
                        headers.add(USER_AGENT_PROPERTY);
                        headers.add(USER_AGENT_MOZILLA);

                        List<Card> cards = new ArrayList<>(deckList.deck);
                        cards.add(deckList.base);
                        cards.add(deckList.leader);
                        cards.add(new Card(deckList.leader.id + "-back", 1));

                        for(Card card : cards) {
                            String[] split = card.id.split("_");

                            HttpRequest request =
                                    HttpRequest.newBuilder()
                                            .uri(URI.create(SWUDB_IMAGE_URL.replace("%SET%", split[0]).replace("%ID%", split[1])))
                                            .headers(headers.toArray(new String[]{}))
                                            .build();

                            client.send(request, HttpResponse.BodyHandlers.ofFile(selectedFilePath.resolveSibling(split[0] + "_" + split[1] + ".png"), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
                        }
                    }
                    catch(InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                catch(IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    static void main(String[] args) {
        launch();
    }
}