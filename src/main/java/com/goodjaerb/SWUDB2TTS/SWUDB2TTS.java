package com.goodjaerb.SWUDB2TTS;

import com.goodjaerb.SWUDB2TTS.json.Card;
import com.goodjaerb.SWUDB2TTS.json.DeckListJson;
import com.goodjaerb.SWUDB2TTS.util.JsonStringConverter;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class SWUDB2TTS extends Application {
    private static final String SWUDB_IMAGE_URL = "https://www.swudb.com/cdn-cgi/image/quality=35/images/cards/%SET%/%ID%.png";
    private static final StringConverter<DeckListJson> DECKLIST_JSON_CONVERTER = new JsonStringConverter<>(DeckListJson.class);

    private static final int CARD_PNG_WIDTH = 716;
    private static final int CARD_PNG_HEIGHT = 1000;

    private final BorderPane pane = new BorderPane();
    private final ProgressBar progressBar = new ProgressBar();
    private final Text progressText = new Text();

    private GridTask task;

    @Override
    public void start(Stage stage) {
        Text text = new Text("Go to SWUDB.com and find or create a deck. Select 'Export' and download the JSON file, then drag-and-drop the JSON onto this window to create your Tabletop Simulator deck grid!");
        text.setMouseTransparent(true);
        text.setTextAlignment(TextAlignment.CENTER);
        text.setLineSpacing(8);
        text.setWrappingWidth(275);

        progressText.setMouseTransparent(true);
        progressBar.setPrefWidth(450);
        progressBar.setProgress(0);

        StackPane progressPane = new StackPane(progressBar, progressText);

        pane.setPrefSize(450, 450);
        pane.setCenter(text);
        pane.setBottom(progressPane);

        addListeners(stage);

        Scene scene = new Scene(pane);
        stage.setScene(scene);
        stage.setTitle("SWUDB2TTS");
        stage.setResizable(false);
        stage.show();
    }

    private void addListeners(Stage stage) {
        pane.setOnDragOver(event -> {
            if((task == null || !task.isRunning()) && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        pane.setOnDragDropped(event -> {
            if(task == null || !task.isRunning()) {
                Dragboard board = event.getDragboard();
                boolean success = false;
                if(board.hasFiles() && board.getFiles().size() == 1) {
                    File droppedFile = board.getFiles().getFirst();
                    if(droppedFile.isFile()) {
                        String filename = droppedFile.getName();
                        if(filename.substring(filename.length() - 5).equalsIgnoreCase(".json")) {
                            task = new GridTask(droppedFile);
                            progressText.textProperty().bind(task.messageProperty());
                            progressBar.progressProperty().bind(task.progressProperty());

                            Thread th = new Thread(task);
                            th.setDaemon(true);
                            th.start();

                            success = true;
                        }
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            }
        });
    }


    static void main(String[] args) {
        launch();
    }

    static class GridTask extends Task<Void> {
        private final File file;

        public GridTask(File file) {
            this.file = file;
        }

        @Override
        protected Void call() throws Exception {
            createDeckGrid();
            return null;
        }

        private void createDeckGrid() throws URISyntaxException, IOException {
            DeckListJson deckList = DECKLIST_JSON_CONVERTER.fromString(Files.readString(file.toPath()));

            List<Card> cards = new ArrayList<>(deckList.deck);
            cards.add(deckList.base);
            cards.add(deckList.leader);
            cards.add(new Card(deckList.leader.id + "-back", 1));
            if(deckList.secondleader != null) {
                cards.add(deckList.secondleader);
                cards.add(new Card(deckList.secondleader.id + "-back", 1));
            }
            if(deckList.sideboard != null && !deckList.sideboard.isEmpty()) {
                cards.addAll(deckList.sideboard);
            }

            int work = 0;
            for(Card card : cards) {
                updateMessage("Step 1: Downloading image " + ++work + "/" + cards.size());
                updateProgress(work, cards.size());

                String[] split = card.id.split("_");
                BufferedImage image = ImageIO.read(new URI(SWUDB_IMAGE_URL.replace("%SET%", split[0]).replace("%ID%", split[1])).toURL());

                if(image.getWidth() > image.getHeight()) {
                    // sideways card.
                    image = rotateImage(image, 270);
                }
                if(image.getWidth() != CARD_PNG_WIDTH || image.getHeight() != CARD_PNG_HEIGHT) {
                    image = resizeImage(image, CARD_PNG_WIDTH, CARD_PNG_HEIGHT);
                }
                card.image = image;
            }

            List<Card> cardList = deckList.getExpandedCardList();
            int currentCardCount = 0;
            int numGrids = 0;
            final int maxPerSheet = 69;
            final int maxCols = 10;
            final int maxRows = 7;

            work = 0;
            while(currentCardCount < cardList.size()) {
                BufferedImage deckGrid = new BufferedImage(CARD_PNG_WIDTH * maxCols, CARD_PNG_HEIGHT * maxRows, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = deckGrid.createGraphics();
                outerloop:
                for(int r = 0; r < maxRows; r++) {
                    for(int c = 0; c < maxCols; c++) {
                        if(currentCardCount <= maxPerSheet) {
                            updateMessage("Step 2: Drawing Grid " + ++work + "/" + cardList.size());
                            updateProgress(work, cardList.size());

                            BufferedImage cardImage = cardList.get(currentCardCount).image;
                            graphics.drawImage(cardImage, null, c * CARD_PNG_WIDTH, r * CARD_PNG_HEIGHT);
                            currentCardCount++;

                            if(currentCardCount >= cardList.size()) {
                                break outerloop;
                            }
                        }
                    }
                }

                String gridFilename = file.getName().substring(0, file.getName().length() - 5);
                gridFilename += (cardList.size() > 69 ? "_" + ++numGrids : "" ) + ".png";

                updateMessage("Step 3: Outputting Deck Grid");
                updateProgress(-1, 1);

                ImageIO.write(deckGrid, "PNG", file.toPath().resolveSibling(gridFilename).toFile());
                graphics.dispose();
            }
            updateMessage("Operation Complete!");
            updateProgress(0, 1);
        }

        public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
            // Create a new BufferedImage with the desired dimensions and type (TYPE_INT_ARGB for transparency support)
            BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);

            // Get the Graphics2D object from the new image to draw onto it
            Graphics2D g2d = resizedImage.createGraphics();

            // Optional: Set rendering hints for better image quality (e.g., bicubic interpolation)
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Draw the original image onto the new image, scaling it to the new width and height
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);

            // Dispose of the Graphics2D context to free up resources
            g2d.dispose();

            return resizedImage;
        }

        public static BufferedImage rotateImage(BufferedImage image, double angle) {
            double radians = Math.toRadians(angle);
            double sin = Math.abs(Math.sin(radians));
            double cos = Math.abs(Math.cos(radians));

            int w = image.getWidth();
            int h = image.getHeight();
            int newW = (int) Math.floor(w * cos + h * sin);
            int newH = (int) Math.floor(h * cos + w * sin);

            // Create a new image with the calculated dimensions
            BufferedImage result = new BufferedImage(newW, newH, image.getType());
            Graphics2D g2d = result.createGraphics();

            // Configure the transform
            // 1. Translate to the center of the new image
            g2d.translate((newW - w) / 2, (newH - h) / 2);
            // 2. Rotate around the center of the original image
            g2d.rotate(radians, w / 2, h / 2);

            // Draw the original image onto the new image context
            g2d.drawRenderedImage(image, null);
            g2d.dispose();

            return result;
        }
    }
}