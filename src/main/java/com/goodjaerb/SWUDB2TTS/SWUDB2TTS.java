package com.goodjaerb.SWUDB2TTS;

import com.goodjaerb.SWUDB2TTS.json.Card;
import com.goodjaerb.SWUDB2TTS.json.DeckListJson;
import com.goodjaerb.SWUDB2TTS.json.LeaderCard;
import com.goodjaerb.SWUDB2TTS.json.TokenCard;
import com.goodjaerb.SWUDB2TTS.util.JsonStringConverter;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

public class SWUDB2TTS extends Application {
    private static final String SWUDB_IMAGE_URL = "https://www.swudb.com/cdn-cgi/image/quality=35/images/cards/%SET%/%ID%.png";
    private static final StringConverter<DeckListJson> DECKLIST_JSON_CONVERTER = new JsonStringConverter<>(DeckListJson.class);

    private static final int CARD_PNG_WIDTH = 409;
    private static final int CARD_PNG_HEIGHT = 572;

    private final BorderPane pane = new BorderPane();
    private final ProgressBar progressBar = new ProgressBar();
    private final Text progressText = new Text();
    private final CheckBox addTokensCheckBox = new CheckBox("Add Token Cards");
    private final CheckBox includeSideboardCheckBox = new CheckBox("Include Sideboard Cards");

    private GridTask task;

    @Override
    public void start(Stage stage) {
        Text text = new Text("Go to SWUDB.com and find or create a deck. Select 'Export' and download the JSON file, then drag-and-drop the JSON onto this window to create your Tabletop Simulator deck grid!");
        text.setMouseTransparent(true);
        text.setTextAlignment(TextAlignment.CENTER);
        text.setLineSpacing(8);
        text.setWrappingWidth(275);

        progressText.setMouseTransparent(true);
        progressBar.setPrefWidth(442);
        progressBar.setProgress(0);

        StackPane progressPane = new StackPane(progressBar, progressText);

        VBox vbox = new VBox(4);
        vbox.getChildren().add(addTokensCheckBox);
        vbox.getChildren().add(includeSideboardCheckBox);
        vbox.getChildren().add(progressPane);

        pane.setPrefSize(450, 450);
        pane.setPadding(new Insets(4,4,4,4));
        pane.setCenter(text);
        pane.setBottom(vbox);

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

    class GridTask extends Task<Void> {
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

            List<Card> cardsToDownload = new ArrayList<>(deckList.deck);
            cardsToDownload.add(deckList.base);
            cardsToDownload.add(deckList.leader);
            cardsToDownload.add(new LeaderCard(deckList.leader.id + "-back", 1));
            if(deckList.secondleader != null) {
                cardsToDownload.add(deckList.secondleader);
                cardsToDownload.add(new LeaderCard(deckList.secondleader.id + "-back", 1));
            }
            if(includeSideboardCheckBox.isSelected()) {
                if(deckList.sideboard != null && !deckList.sideboard.isEmpty()) {
                    cardsToDownload.addAll(deckList.sideboard);
                }
            }

            Map<String, BufferedImage> imageMap = new HashMap<>();

            int work = 0;
            for(Card card : cardsToDownload) {
                updateMessage("Step 1: Downloading image " + ++work + "/" + cardsToDownload.size());
                updateProgress(work, cardsToDownload.size());

                String[] split = card.id.split("_");

                URL imageUrl        = new URI(SWUDB_IMAGE_URL.replace("%SET%", split[0]).replace("%ID%", split[1])).toURL();
                BufferedImage image = null;
                try {
                    image = ImageIO.read(imageUrl);
                }
                catch(IOException ex) {
                    // this can happen because sometimes the back image on swudb isn't named "-back", but instead "-portrait".
                    if(imageUrl.toString().contains("-back")) {
                        imageUrl = new URI(imageUrl.toString().replace("-back", "-portrait")).toURL();
                        image = ImageIO.read(imageUrl);

//                        card.id = card.id.replace("-back", "-portrait");
                    }

                    // if it still isn't there, then get outta here i guess.
                    if(image == null) {
                        throw ex;
                    }
                }

                if(image.getWidth() > image.getHeight()) {
                    // sideways card.
                    image = rotateImage(image, 270);
                }
                image = resizeImage(image, CARD_PNG_WIDTH, CARD_PNG_HEIGHT);
                imageMap.put(card.id, image);
            }

            imageMap.put("cardback_swu", resizeImage(ImageIO.read(Objects.requireNonNull(getClass().getResource("/images/cardback/cardback_swu.png"))), CARD_PNG_WIDTH, CARD_PNG_HEIGHT));
            imageMap.put("hidden_resistancelogo", resizeImage(ImageIO.read(Objects.requireNonNull(getClass().getResource("/images/hidden/hidden_resistancelogo.png"))), CARD_PNG_WIDTH, CARD_PNG_HEIGHT));
            if(addTokensCheckBox.isSelected()) {
                imageMap.put("token_battledroid", resizeImage(ImageIO.read(Objects.requireNonNull(getClass().getResource("/images/token/token_battledroid.webp"))), CARD_PNG_WIDTH, CARD_PNG_HEIGHT));
                imageMap.put("token_clonetrooper", resizeImage(ImageIO.read(Objects.requireNonNull(getClass().getResource("/images/token/token_clonetrooper.webp"))), CARD_PNG_WIDTH, CARD_PNG_HEIGHT));
                imageMap.put("token_credit", resizeImage(ImageIO.read(Objects.requireNonNull(getClass().getResource("/images/token/token_credit.webp"))), CARD_PNG_WIDTH, CARD_PNG_HEIGHT));
                imageMap.put("token_experience", resizeImage(ImageIO.read(Objects.requireNonNull(getClass().getResource("/images/token/token_experience.webp"))), CARD_PNG_WIDTH, CARD_PNG_HEIGHT));
                imageMap.put("token_force", resizeImage(rotateImage(ImageIO.read(Objects.requireNonNull(getClass().getResource("/images/token/token_force.webp"))), 270), CARD_PNG_WIDTH, CARD_PNG_HEIGHT));
                imageMap.put("token_shield", resizeImage(ImageIO.read(Objects.requireNonNull(getClass().getResource("/images/token/token_shield.webp"))), CARD_PNG_WIDTH, CARD_PNG_HEIGHT));
                imageMap.put("token_spy", resizeImage(ImageIO.read(Objects.requireNonNull(getClass().getResource("/images/token/token_spy.webp"))), CARD_PNG_WIDTH, CARD_PNG_HEIGHT));
                imageMap.put("token_tiefighter", resizeImage(ImageIO.read(Objects.requireNonNull(getClass().getResource("/images/token/token_tiefighter.webp"))), CARD_PNG_WIDTH, CARD_PNG_HEIGHT));
                imageMap.put("token_xwing", resizeImage(ImageIO.read(Objects.requireNonNull(getClass().getResource("/images/token/token_xwing.webp"))), CARD_PNG_WIDTH, CARD_PNG_HEIGHT));
            }

            List<Card> cardList = deckList.getExpandedCardList(includeSideboardCheckBox.isSelected(), addTokensCheckBox.isSelected());
            int totalCardCount = 0;
            int numGrids = 0;
            final int maxPerSheet = 69;
            final int maxCols = 10;
            final int maxRows = 7;

            work = 0;
            while(totalCardCount < cardList.size()) {
                BufferedImage deckFrontGrid = new BufferedImage(CARD_PNG_WIDTH * maxCols, CARD_PNG_HEIGHT * maxRows, BufferedImage.TYPE_INT_ARGB);
                BufferedImage deckBackGrid = new BufferedImage(CARD_PNG_WIDTH * maxCols, CARD_PNG_HEIGHT * maxRows, BufferedImage.TYPE_INT_ARGB);

                Graphics2D frontGraphics = deckFrontGrid.createGraphics();
                Graphics2D backGraphics = deckBackGrid.createGraphics();

                int currentCardCount = 0;
                outerloop:
                for(int r = 0; r < maxRows; r++) {
                    for(int c = 0; c < maxCols; c++) {
                        if(currentCardCount < maxPerSheet) {
                            updateMessage("Step 2: Drawing Card Grids " + ++work + "/" + cardList.size());
                            updateProgress(work, cardList.size());

                            BufferedImage frontImage = imageMap.get(cardList.get(totalCardCount).id);//cardList.get(currentCardCount).image;
                            frontGraphics.drawImage(frontImage, null, c * CARD_PNG_WIDTH, r * CARD_PNG_HEIGHT);

                            if(cardList.get(totalCardCount).isLeader()) {
                                BufferedImage leaderImage = imageMap.get(cardList.get(totalCardCount).id + "-back");
//                                if(leaderImage == null) {
//                                    leaderImage = imageMap.get(cardList.get(totalCardCount).id + "-portrait");
//                                }
                                backGraphics.drawImage(leaderImage, null, c * CARD_PNG_WIDTH, r * CARD_PNG_HEIGHT);
                            }
                            else if(cardList.get(totalCardCount).isToken() && ((TokenCard)cardList.get(totalCardCount)).backId != null) {
                                BufferedImage tokenImage = imageMap.get(((TokenCard)cardList.get(totalCardCount)).backId);
                                backGraphics.drawImage(tokenImage, null, c * CARD_PNG_WIDTH, r * CARD_PNG_HEIGHT);
                            }
                            else {
                                backGraphics.drawImage(imageMap.get("cardback_swu"), null, c * CARD_PNG_WIDTH, r * CARD_PNG_HEIGHT);
                            }
                            currentCardCount++;
                            totalCardCount++;

                            if(totalCardCount >= cardList.size()) {
                                break outerloop;
                            }
                        }
                    }
                }
                // these fill in the 'hidden card' slots of the deck grids to hide card faces (and show card backs!) when cards are in other people's hands.
                frontGraphics.drawImage(imageMap.get("hidden_resistancelogo"), null, (maxCols - 1) * CARD_PNG_WIDTH, (maxRows - 1) * CARD_PNG_HEIGHT);
                backGraphics.drawImage(imageMap.get("cardback_swu"), null, (maxCols - 1) * CARD_PNG_WIDTH, (maxRows - 1) * CARD_PNG_HEIGHT);

                String frontGridFilename = file.getName().substring(0, file.getName().length() - 5);
                frontGridFilename += (cardList.size() > maxPerSheet ? "_" + ++numGrids : "" ) + "_fronts.png";

                String backGridFilename = frontGridFilename.replace("_fronts.png", "_backs.png");

                updateMessage("Step 3: Outputting Deck Grids");
                updateProgress(-1, 1);

                ImageIO.write(deckFrontGrid, "PNG", file.toPath().resolveSibling(frontGridFilename).toFile());
                ImageIO.write(deckBackGrid, "PNG", file.toPath().resolveSibling(backGridFilename).toFile());

                frontGraphics.dispose();
                backGraphics.dispose();
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