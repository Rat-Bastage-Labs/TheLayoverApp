/*
 * © 2026 Javier Yzaguirre
 * All Rights Reserved.
 * Unauthorized copying or distribution is prohibited.
 */

import javafx.application.Application;
import javafx.application.Platform;

import javafx.stage.Stage;

import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import javafx.geometry.Pos;
import javafx.geometry.Insets;

import javafx.animation.*;
import javafx.util.Duration;

import javafx.concurrent.Task;

import javafx.beans.property.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.binding.Bindings;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder; 
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.http.*;

import java.util.*;
import javafx.util.Duration;
import java.util.function.Function;
import java.util.function.Consumer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Main extends Application { 
    private static final String AIRPORTS_CSV_URL =
        Optional.ofNullable(System.getenv("AIRPORTS_CSV_URL"))
                .orElse(System.getenv("AIRPORTS_CSV_DEF"));
    private static final String AVIATIONSTACK_KEY = System.getenv("AVIATIONSTACK_KEY");
    private static final String AVIATIONSTACK_URL = System.getenv("AVIATIONSTACK_URL");
    private static final Set<String> ALLOWED_AIRLINES = Set.of(
        "AA","DL","UA","WN","NK","F9","AS","B6",
        "AC","AM","WS","AF","BA","LH","EK","QR","CX","IB","KL"
    );
    private HttpServer server;
    
    private static volatile double lastLat = 0;
    private static volatile double lastLon = 0;

    private static final List<String> lobbyTitles = List.of(
        "Quick Activities",
        "Chill & Chat",
        "Food & Drink",
        "Move Around",
        "Soft Social",
        "Time-Based Meetups",
        "Spontaneous & Fun",
        "Live Lobby Features"
    );    
    
    private static final Map<Integer, List<String[]>> LOBBY_DETAIL_ITEMS = Map.of(
        1, List.of( 
            new String[]{"Card & Board Games", "Uno, Chess, Checkers, mini trivia"},
            new String[]{"Mobile Game Hangouts", "Among Us, Mario Kart, etc."},
            new String[]{"Puzzle Corners", "Sudoku, riddles, escape-room challenges"},
            new String[]{"Icebreaker Prompts", "Fun questions to spark conversation"}
        ),
        2, List.of( 
            new String[]{"Coffee & Chat", "Meet up for a drink"},
            new String[]{"Quiet Conversation", "Introvert-friendly meetups"},
            new String[]{"Story Swap", "“Weirdest travel story?” sessions"},
            new String[]{"Language Exchange", "Practice a language"}
        ),
        3, List.of( 
            new String[]{"Food Court Meetup", "“Grabbing food at Gate B”"},
            new String[]{"Bar & Lounge Hangouts", "Drinks and easy conversation"},
            new String[]{"Snack Crawl", "Try multiple spots together"},
            new String[]{"Diet-Friendly Meetups", "Vegan, gluten-free, etc."}
        ),
        4, List.of( 
            new String[]{"Terminal Walks", "10-minute walk before boarding"},
            new String[]{"Stretch & Mobility", "Light stretching / yoga"},
            new String[]{"Step Challenges", "Who hits 3k steps?"},
            new String[]{"Walk & Talk", "Move while chatting"}
        ),
        5, List.of( 
            new String[]{"Watch Party", "Sync a show or movie"},
            new String[]{"Music Share", "Swap playlists"},
            new String[]{"Read Together", "Quiet co-presence"},
            new String[]{"Work / Study Room", "Digital nomads unite"}
        ),
        6, List.of( 
            new String[]{"15-Minute Kill Time", ""},
            new String[]{"1-Hour Layover Crew", ""},
            new String[]{"Long Layover (3+ hrs)", ""},
            new String[]{"Last Call Before Boarding", ""}
        ),
        7, List.of( 
            new String[]{"Random Gate Meetup", "Show up and see who’s there"},
            new String[]{"Coincidence Club", "Same destination or airline"},
            new String[]{"Birthday Board", "Celebrate together"},
            new String[]{"Nervous Flyers", "Support & chat"}
        ),
        8, List.of( 
            new String[]{"Popular Right Now", ""},
            new String[]{"Ending Soon", ""},
            new String[]{"Boarding Near You", ""},
            new String[]{"Looking for 1–2 More", ""}
        )
    );

    class FlightRow {
        String flight, airline, fromTo, status, scheduled, estimated, terminal, gate;
        ZonedDateTime scheduledTime; 

        public FlightRow(String flight, String airline, String fromTo, String status,
                         String scheduled, String estimated,
                         String terminal, String gate,
                         ZonedDateTime scheduledTime) {

            this.flight = flight;
            this.airline = airline;
            this.fromTo = fromTo;
            this.status = status;
            this.scheduled = scheduled;
            this.estimated = estimated;
            this.terminal = terminal;
            this.gate = gate;
            this.scheduledTime = scheduledTime;
        }
    }

    private TableColumn<FlightRow, String> col(
        String title,
        Function<FlightRow, String> mapper
    ) {
    TableColumn<FlightRow, String> column = new TableColumn<>(title);
    column.setCellValueFactory(data ->
            new SimpleStringProperty(mapper.apply(data.getValue()))
    );

        column.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }

                setAlignment(Pos.CENTER); 
            }
        });
        
    return column;
    }

    private BooleanProperty hasAgreedToTerms = new SimpleBooleanProperty(false);
    private BooleanProperty isOver18 = new SimpleBooleanProperty(false);
    
    private static final String SAVE_FILE = "layover_user_data.json";
    private String userProfileImagePath = "/resources/images/image_placeholder.jpg";
    private Set<Integer> usedProfileNumbers = new HashSet<>();
    private static final int MAX_PROFILES = 100; 

    private final ObjectProperty<Image> profileAvatar = new SimpleObjectProperty<>();
    private StringProperty displayName = new SimpleStringProperty("Larry L. Layover");
    private StringProperty username = new SimpleStringProperty("@layoverlarry");
    private StringProperty bio = new SimpleStringProperty("Traveler • Aviation fan • Always chasing the next layover ✈️");
    private StringProperty ageRange = new SimpleStringProperty("");
    private StringProperty gender = new SimpleStringProperty("");
    private StringProperty accountEmail = new SimpleStringProperty("larry@layover.com");
    private BooleanProperty emailVerified = new SimpleBooleanProperty(true);
    private IntegerProperty meetupsCompleted = new SimpleIntegerProperty(3);
    private final List<Profile> matchHistory = new ArrayList<>();
    Map<String, StackPane> userCardMap = new HashMap<>();
    private Set<String> reportedUsers = new HashSet<>();
    
    private BorderPane root;
    private VBox centerBox;

    @Override
    public void start(Stage stage) {
        loadUserData();
        
        Label appTitle = new Label("The Layover App");
        appTitle.setFont(Font.font(14));
        appTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Hyperlink lobbyLink = navLink("Lobby");
        Hyperlink meetLink = navLink("Meet");
        Hyperlink flightLink = navLink("Flights");
        Hyperlink airportLink = navLink("Airports");
        Hyperlink profileLink = navLink("Profile");

        HBox navBar = new HBox(20, appTitle, new Region(),
                lobbyLink, meetLink, flightLink, airportLink, profileLink);
        HBox.setHgrow(navBar.getChildren().get(1), Priority.ALWAYS);
        navBar.setPadding(new Insets(10));
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setStyle("-fx-background-color: #2c3e50;");

        List<String> options = List.of("People", "Flights", "Airports");
        IntegerProperty optionIndex = new SimpleIntegerProperty(1);

        Label findLabel = new Label("Find");
        findLabel.setFont(Font.font(18));
        findLabel.setStyle("-fx-text-fill: white;");

        Label optionLabel = new Label(options.get(optionIndex.get()));
        optionLabel.setFont(Font.font(18));
        optionLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        VBox textBox = new VBox(5, findLabel, optionLabel);
        textBox.setAlignment(Pos.CENTER);

        StackPane findCircle = new StackPane(textBox);
        findCircle.setPrefSize(200, 200);
        findCircle.setStyle(
                "-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-background-radius: 100px;" +
                        "-fx-min-width: 200px; -fx-min-height: 200px; -fx-max-width: 200px; -fx-max-height: 200px;"
        );

        findCircle.setOnScroll(e -> {
            int dir = e.getDeltaY() > 0 ? -1 : 1;
            optionIndex.set((optionIndex.get() + dir + options.size()) % options.size());
            optionLabel.setText(options.get(optionIndex.get()));
        });

        findCircle.setOnMouseClicked(e -> {
            switch (options.get(optionIndex.get())) {
                case "People" -> root.setCenter(createMeetPage()); 
                case "Flights" -> root.setCenter(createFlightsPage()); 
                case "Airports" -> root.setCenter(createAirportsPage());
            }
        });

        Label topLabel = new Label("The Layover App - Fly Friendly");
        topLabel.setFont(Font.font(15));
        topLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");

        Label bottomLabel = new Label("Explore connections between flights");
        bottomLabel.setFont(Font.font(14));
        bottomLabel.setStyle("-fx-text-fill: black; -fx-font-weight: bold;");

        Timeline autoRotate = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> {
                    optionIndex.set((optionIndex.get() + 1) % options.size());
                    optionLabel.setText(options.get(optionIndex.get()));
                })
        );
        autoRotate.setCycleCount(Animation.INDEFINITE);
        autoRotate.play();

        centerBox = new VBox(55, topLabel, findCircle, bottomLabel);
        centerBox.setAlignment(Pos.CENTER);

        Label footerText = new Label("© 2026 Layover App | All Rights Reserved");
        footerText.setStyle("-fx-text-fill: white;");

        Hyperlink legalLink = new Hyperlink("Legal");
        legalLink.setStyle(
                "-fx-text-fill: white;" +
                "-fx-underline: false;" +
                "-fx-border-color: transparent;" +
                "-fx-focus-color: transparent;"
        );

        HBox rightBox = new HBox(legalLink);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.setPadding(new Insets(0, 10, 0, 0));

        StackPane footer = new StackPane(footerText, rightBox);
        StackPane.setAlignment(footerText, Pos.CENTER);
        StackPane.setAlignment(rightBox, Pos.CENTER_RIGHT);

        footer.setPadding(new Insets(10));
        footer.setStyle("-fx-background-color: #2c3e50;");

        legalLink.setOnAction(e -> root.setCenter(createLegalPage()));

        root = new BorderPane();
        root.setTop(navBar);
        root.setCenter(centerBox);
        root.setBottom(footer);

        appTitle.setOnMouseClicked(e -> root.setCenter(centerBox));

        lobbyLink.setOnAction(e -> root.setCenter(createLobbyPage())); 
        meetLink.setOnAction(e -> {
            if (hasAgreedToTerms.get() && isOver18.get()) {
                root.setCenter(createMeetPage());
            } else {
                root.setCenter(createMeetGatePage(() ->
                    root.setCenter(createMeetPage())
                ));
            }
        });
        airportLink.setOnAction(e -> root.setCenter(createAirportsPage()));  
        flightLink.setOnAction(e -> root.setCenter(createFlightsPage()));
        profileLink.setOnAction(e -> root.setCenter(createProfilePage()));

        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("The Layover App");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
        stage.setOnCloseRequest(e -> saveUserData());

        new Thread(this::startServer).start(); 
    }

    private Hyperlink navLink(String text) {
        Hyperlink link = new Hyperlink(text);
        link.setStyle(
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;" +
                    "-fx-focus-color: transparent;" +
                    "-fx-faint-focus-color: transparent;" +
                    "-fx-underline: false;"
        );    
        link.setFocusTraversable(false);
        return link;
    }

    private Node createLegalPage() {
        Label title = new Label("Legal Information");
        title.setFont(Font.font(null, FontWeight.BOLD, 14));

        Label legalText = new Label(
        "LAYOVER APP – TERMS OF USE, LIABILITY WAIVER, AND LEGAL NOTICE\n\n" +

        "Effective Date: 2026\n\n" +

        "1. ACCEPTANCE OF TERMS\n" +
        "By accessing or using The Layover App (“App”), you agree to be bound by these Terms. If you do not agree, you must not use the App.\n\n" +

        "2. GENERAL DISCLAIMER\n" +
        "The App is a social coordination platform only. We do not organize, supervise, verify, or guarantee any meetups, individuals, or activities. All interactions are at your own risk.\n\n" +

        "3. USER RESPONSIBILITY\n" +
        "You are solely responsible for your conduct, safety, and decisions while using the App, including any in-person meetings at airports or elsewhere.\n\n" +

        "4. NO BACKGROUND CHECKS\n" +
        "We do not perform background checks or identity verification. Users may misrepresent themselves.\n\n" +

        "5. PERSONAL MEETUPS\n" +
        "Meeting other users is voluntary and at your own risk. You agree to take all necessary precautions when interacting with others.\n\n" +

        "6. AIRPORT AND SECURITY COMPLIANCE\n" +
        "You agree to comply with all airport rules, TSA regulations, airline policies, and local laws. The App is not affiliated with any airport authority, airline, or government agency.\n\n" +

        "7. PROHIBITED ACTIVITIES\n" +
        "You agree not to use the App for:\n" +
        "- Illegal activities of any kind\n" +
        "- Harassment, threats, or violence\n" +
        "- Soliciting or engaging in illegal drug use\n" +
        "- Sharing or promoting explicit sexual content\n" +
        "- Carrying or promoting illegal weapons\n" +
        "- Any activity that violates airport or aviation regulations\n\n" +

        "8. DRUGS AND ALCOHOL\n" +
        "The App does not permit or encourage illegal drug use. Users must comply with all laws regarding alcohol consumption and controlled substances.\n\n" +

        "9. WEAPONS\n" +
        "Users must comply with all laws regarding weapons. The App strictly prohibits use of the platform to coordinate or promote unlawful possession or use of weapons.\n\n" +

        "10. SEXUAL CONTENT AND CONDUCT\n" +
        "The App is not a dating or adult platform. Any explicit, exploitative, or inappropriate sexual behavior is prohibited.\n\n" +

        "11. DATA ACCURACY (FLIGHTS & AIRPORTS)\n" +
        "Flight and airport information may be sourced from third parties. We do not guarantee accuracy, completeness, or timeliness of this data.\n\n" +

        "12. INTERNATIONAL USE\n" +
        "Users are responsible for complying with all local, national, and international laws. Laws vary by country, and you assume full responsibility for compliance.\n\n" +

        "13. LIMITATION OF LIABILITY\n" +
        "To the fullest extent permitted by law, The Layover App and its owner shall not be liable for any direct, indirect, incidental, or consequential damages, including but not limited to personal injury, loss, or damages arising from user interactions.\n\n" +

        "14. ASSUMPTION OF RISK\n" +
        "You acknowledge that meeting strangers in public or private spaces carries inherent risks, including but not limited to injury, theft, or harm.\n\n" +

        "15. INDEMNIFICATION\n" +
        "You agree to indemnify and hold harmless the App and its owner from any claims, damages, or liabilities resulting from your use of the App.\n\n" +

        "16. NO WARRANTIES\n" +
        "The App is provided “as is” without warranties of any kind, express or implied.\n\n" +

        "17. TERMINATION\n" +
        "We reserve the right to suspend or terminate access for violations of these Terms.\n\n" +

        "18. PRIVACY\n" +
        "We may collect and store user data as necessary to operate the App. Use of the App constitutes consent to such collection.\n\n" +

        "19. GOVERNING LAW\n" +
        "These Terms shall be governed by the laws of the United States and applicable state laws.\n\n" +

        "20. CHANGES TO TERMS\n" +
        "We reserve the right to modify these Terms at any time. Continued use constitutes acceptance of updated Terms.\n\n" +

        "By using this App, you acknowledge that you have read, understood, and agree to these Terms.\n\n" +

        "© 2026 Javier Yzaguirre. All Rights Reserved."
        );
        legalText.setStyle(
            "-fx-text-fill: black;" +
            "-fx-font-size: 14px;"
        );    
        legalText.setFocusTraversable(false);
        legalText.setWrapText(true);
        legalText.setTextAlignment(TextAlignment.CENTER);

        ScrollPane scrollPane = new ScrollPane(legalText);
        scrollPane.setFitToWidth(true); 
        scrollPane.setPrefHeight(400);  
        scrollPane.setMaxWidth(400); 
        scrollPane.setStyle(
                "-fx-background: transparent;" +
                "-fx-background-color: transparent;" +
                "-fx-border-color: black;" +
                "-fx-border-width: 1px;"
        );

        Button backButton = new Button("← Back");
        backButton.setFocusTraversable(false);
        backButton.setStyle(
            "-fx-background-color: #ecf0f1;" +
            "-fx-font-weight: bold;" +
            "-fx-border-color: transparent;"
        );
        backButton.setOnAction(e -> root.setCenter(centerBox));

        VBox layout = new VBox(20, title, scrollPane, legalText, backButton);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));

        return layout;
    }

    private Node createMeetGatePage(Runnable onSuccess) {

        Label title = new Label("Before You Continue");
        title.setFont(Font.font(null, FontWeight.BOLD, 16));

        Label warningText = new Label(
                "This feature allows you to meet other travelers in person.\n\n" +
                "By proceeding, you acknowledge that:\n" +
                "- You are at least 18 years old\n" +
                "- You accept all risks of meeting others\n" +
                "- You agree to follow all airport laws and regulations\n" +
                "- You have read and agree to the Legal Terms"
        );
        warningText.setWrapText(true);
        warningText.setTextAlignment(TextAlignment.CENTER);

        CheckBox ageCheck = new CheckBox("I confirm that I am 18 years or older");
        CheckBox agreeCheck = new CheckBox("I agree to the Terms and Legal Conditions");

        Button continueBtn = new Button("Continue");
        continueBtn.setDisable(true);  
        continueBtn.disableProperty().bind(
                ageCheck.selectedProperty().not()
                        .or(agreeCheck.selectedProperty().not())
        );

        continueBtn.setOnAction(e -> {
            hasAgreedToTerms.set(true);
            isOver18.set(true);
            onSuccess.run(); 
        });

        Button backButton = new Button("← Back");
        backButton.setStyle(
            "-fx-background-color: #ecf0f1;" +
            "-fx-font-weight: bold;" +
            "-fx-border-color: transparent;"
        );
        backButton.setOnAction(e -> root.setCenter(centerBox));

        VBox layout = new VBox(20,
                title,
                warningText,
                ageCheck,
                agreeCheck,
                continueBtn,
                backButton
        );

        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));
        layout.setMaxWidth(500);

        return new StackPane(layout); 
    }

    private Node createPage(String titleText) {
        Label title = new Label(titleText);
        title.setFont(Font.font(18));
        title.setStyle("-fx-font-weight: bold;");
        VBox box = new VBox(title);
        box.setAlignment(Pos.CENTER); 
        return box;
    } 

    private Node createLobbyPage() {
        VBox pageBox = new VBox(30);
        pageBox.setAlignment(Pos.CENTER);
        pageBox.setPadding(new Insets(30));

        Image placeholderImage = new Image(
            getClass().getResourceAsStream("/resources/images/airport.png")
        );

        for (int row = 0; row < 2; row++) {

            HBox rowBox = new HBox(30);
            rowBox.setAlignment(Pos.CENTER);

            for (int col = 0; col < 4; col++) {

                int lobbyIndex = row * 4 + col;
                int lobbyNumber = lobbyIndex + 1;

                VBox card = new VBox(12);
                card.setAlignment(Pos.TOP_CENTER);
                card.setPrefSize(180, 220);
                card.setPadding(new Insets(0, 0, 15, 0));

                card.setStyle(
                    "-fx-background-color: white;" +
                    "-fx-background-radius: 18;" +
                    "-fx-border-radius: 18;" +
                    "-fx-border-color: #e0e0e0;" +
                    "-fx-border-width: 1;"
                );

                DropShadow shadow = new DropShadow();
                shadow.setRadius(18);
                shadow.setOffsetY(6);
                shadow.setColor(Color.rgb(0, 0, 0, 0.18));
                card.setEffect(shadow);

                card.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.15)));

                ImageView imageView = new ImageView(placeholderImage);
                imageView.setFitWidth(180);
                imageView.setFitHeight(110);

                Rectangle imageClip = new Rectangle(180, 110);
                imageClip.setArcWidth(30);
                imageClip.setArcHeight(30);
                imageView.setClip(imageClip);

                VBox contentBox = new VBox();
                contentBox.setAlignment(Pos.CENTER);
                contentBox.setPadding(new Insets(5, 10, 10, 10));

                Label nameLabel = new Label(lobbyTitles.get(lobbyIndex));
                nameLabel.setWrapText(true);
                nameLabel.setTextAlignment(TextAlignment.CENTER);
                nameLabel.setAlignment(Pos.CENTER);
                nameLabel.setMaxWidth(160);
                nameLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 14));
                nameLabel.setStyle("-fx-text-fill: #2c3e50;");

                contentBox.getChildren().add(nameLabel);

                card.setOnMouseClicked(e ->
                    root.setCenter(createLobbyDetailPage(lobbyNumber))
                );

                card.setOnMouseEntered(e -> {
                    card.setTranslateY(-6);
                    shadow.setRadius(25);
                    shadow.setOffsetY(10);
                });

                card.setOnMouseExited(e -> {
                    card.setTranslateY(0);
                    shadow.setRadius(18);
                    shadow.setOffsetY(6);
                });

                card.setOnMousePressed(e -> card.setScaleX(0.97));
                card.setOnMousePressed(e -> card.setScaleY(0.97));

                card.setOnMouseReleased(e -> {
                    card.setScaleX(1);
                    card.setScaleY(1);
                });

                card.getChildren().addAll(imageView, contentBox);
                rowBox.getChildren().add(card);
            }

            pageBox.getChildren().add(rowBox);
        }

        return pageBox;
    }

    private Node createMeetPage() {
        StackPane rootStack = new StackPane();
        VBox pageBox = new VBox(20);
        pageBox.setAlignment(Pos.CENTER);
        pageBox.setPadding(new Insets(20));
        rootStack.getChildren().add(pageBox);

        for (int row = 0; row < 3; row++) {
            HBox rowBox = new HBox(20);
            rowBox.setAlignment(Pos.CENTER);

            for (int col = 0; col < 5; col++) {
                VBox card = new VBox(10);
                card.setAlignment(Pos.CENTER);
                card.setPadding(new Insets(10));
                card.setPrefWidth(120);
                card.setPrefHeight(160);
                card.setStyle(
                    "-fx-background-color: white;" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-radius: 12;" +
                    "-fx-border-color: #e0e0e0;" +
                    "-fx-border-width: 1;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0.3, 0, 3);"
                );

                int profileNum = row * 5 + col + 1;
                usedProfileNumbers.add(profileNum);
                card.setUserData(profileNum);
    
                Profile prof = ProfileDatabase.getProfile(profileNum);

                ImageView imgView = new ImageView(getProfilePlaceholderImage(prof, 100));
                imgView.setFitWidth(100);
                imgView.setPreserveRatio(true);
                imgView.setSmooth(true);
                
                Rectangle clip = new Rectangle(100, 100);
                clip.setArcWidth(15);
                clip.setArcHeight(15);
                imgView.setClip(clip);

                Label nameLabel = new Label(prof.getDisplayName());
                nameLabel.setStyle(
                    "-fx-font-size: 13px;" +
                    "-fx-font-weight: 600;" +
                    "-fx-text-fill: #2c3e50;"
                );

                card.getChildren().addAll(imgView, nameLabel);

                card.setOnMouseEntered(e -> {
                    card.setStyle(
                        "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-color: #d0d0d0;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 18, 0.4, 0, 5);"
                    );
                });

                card.setOnMouseExited(e -> {
                    card.setStyle(
                        "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-color: #e0e0e0;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0.3, 0, 3);"
                    );
                });

                card.setOnMouseClicked(e -> showProfileOverlay(rootStack, pageBox, card));
                rowBox.getChildren().add(card);
            }

            pageBox.getChildren().add(rowBox);
        }

        return rootStack;
    } 

    private Node createFlightsPage() {
        VBox pageBox = new VBox(10);
        pageBox.setAlignment(Pos.TOP_CENTER);
        pageBox.setPadding(new Insets(20));

        StackPane headerBox = new StackPane();
        headerBox.setPadding(new Insets(0, 20, 0, 20));
        headerBox.setPrefHeight(40);

        Label topLabel = new Label("Fetching nearest airport...");
        topLabel.setFont(Font.font(14));
        topLabel.setStyle("-fx-font-weight: bold;");
        StackPane.setAlignment(topLabel, Pos.CENTER);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle("""
            -fx-background-radius: 20;
            -fx-padding: 6 14;
            -fx-background-color: #8EC5FC;
            -fx-text-fill: white;
            -fx-font-weight: bold;
        """);

        refreshBtn.setOnAction(e -> {
            topLabel.setText("Refreshing location...");
            root.setCenter(createFlightsPage());
        });

        StackPane.setAlignment(refreshBtn, Pos.CENTER_LEFT);

        ToggleButton toggle = new ToggleButton("Arrivals");
        toggle.setSelected(true);
        toggle.setStyle("""
            -fx-background-radius: 20;
            -fx-padding: 6 14;
            -fx-background-color: #B8B8FF;
            -fx-text-fill: white;
            -fx-font-weight: bold;
        """);

        StackPane.setAlignment(toggle, Pos.CENTER_RIGHT);
        headerBox.getChildren().addAll(topLabel, toggle, refreshBtn);

        TableView<FlightRow> localTable = new TableView<>();
        ObservableList<FlightRow> arrivalsData = FXCollections.observableArrayList();
        ObservableList<FlightRow> departuresData = FXCollections.observableArrayList();

        localTable.setItems(arrivalsData);
        localTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        localTable.setPrefHeight(500);
        localTable.setStyle(
            "-fx-focus-color: transparent; " +
            "-fx-faint-focus-color: transparent;"
        );

        localTable.getColumns().addAll(
            col("Flight", r -> r.flight),
            col("Airline", r -> r.airline),
            col("From / To", r -> r.fromTo),
            col("Status", r -> r.status),
            col("Scheduled", r -> r.scheduled),
            col("Estimated", r -> r.estimated),
            col("Terminal", r -> r.terminal),
            col("Gate", r -> r.gate)
        );

        toggle.selectedProperty().addListener((obs, wasArrivals, isArrivals) -> {
            toggle.setText(isArrivals ? "Arrivals" : "Departures");
            localTable.setItems(isArrivals ? arrivalsData : departuresData);
        });

        pageBox.getChildren().addAll(headerBox, localTable);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<Map<String, String>> airports = fetchAirportsCSV();
                double[] location = fetchUserLocation();
                double lat = location[0];
                double lon = location[1];

                Map<String, String> nearest = airports.stream()
                        .min(Comparator.comparingDouble(a ->
                                haversine(lat, lon,
                                        Double.parseDouble(a.get("latitude_deg")),
                                        Double.parseDouble(a.get("longitude_deg")))))
                        .orElseThrow();

                String airportName = nearest.get("name");
                String iata = nearest.get("iata_code");

                Platform.runLater(() -> topLabel.setText(airportName + " (" + iata + ")"));

                Map<String, List<Map<String, String>>> flights = fetchFlights(iata);

                Platform.runLater(() -> {
                    arrivalsData.clear();
                    departuresData.clear();

                    flights.getOrDefault("arrivals", Collections.emptyList())
                           .forEach(f -> {
                               ZonedDateTime scheduledTime = ZonedDateTime.parse(f.get("scheduledTime"));

                               arrivalsData.add(new FlightRow(
                                   f.getOrDefault("flight", "—"),
                                   f.getOrDefault("airline", "—"),
                                   f.getOrDefault("from", "—") + " → " + f.getOrDefault("to", "—"),
                                   f.getOrDefault("status", "Unknown"),
                                   f.getOrDefault("scheduled", "—"),
                                   f.getOrDefault("estimated", "—"),
                                   f.getOrDefault("terminal", "—"),
                                   f.getOrDefault("gate", "—"),
                                   scheduledTime
                               ));
                           });

                    flights.getOrDefault("departures", Collections.emptyList())
                           .forEach(f -> {
                               ZonedDateTime scheduledTime = ZonedDateTime.parse(f.get("scheduledTime"));

                               departuresData.add(new FlightRow(
                                   f.getOrDefault("flight", "—"),
                                   f.getOrDefault("airline", "—"),
                                   f.getOrDefault("from", "—") + " → " + f.getOrDefault("to", "—"),
                                   f.getOrDefault("status", "Unknown"),
                                   f.getOrDefault("scheduled", "—"),
                                   f.getOrDefault("estimated", "—"),
                                   f.getOrDefault("terminal", "—"),
                                   f.getOrDefault("gate", "—"),
                                   scheduledTime
                               ));
                           });

                    arrivalsData.sort(Comparator.comparing(r -> r.scheduledTime));
                    departuresData.sort(Comparator.comparing(r -> r.scheduledTime));
                });

                return null;
            }
        };

        task.setOnFailed(e -> Platform.runLater(() -> topLabel.setText("Failed to load flights..")));

        new Thread(task).start();
        return pageBox;
    }

    public static Map<String, List<Map<String, String>>> fetchFlights(String iata) throws IOException {
        List<Map<String, String>> arrivals = new ArrayList<>();
        List<Map<String, String>> departures = new ArrayList<>(); 

        fetchAndAdd(AVIATIONSTACK_URL + "?dep_iata=" + iata + "&access_key=" + AVIATIONSTACK_KEY, departures, true);
        fetchAndAdd(AVIATIONSTACK_URL + "?arr_iata=" + iata + "&access_key=" + AVIATIONSTACK_KEY, arrivals, true);
 
        return Map.of("arrivals", arrivals, "departures", departures);
    }
 

    private static void fetchAndAdd(String url, List<Map<String, String>> target, boolean arrival) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");

        int code = conn.getResponseCode();
        InputStream stream = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
        String response = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        if (code != 200) {
            System.out.println("ERROR RESPONSE:\n" + response);
            throw new IOException("API Error: HTTP " + code);
        }

        Gson gson = new Gson();
        JsonObject root = gson.fromJson(response, JsonObject.class);

        if (root.has("error")) {
            System.out.println("API ERROR:\n" + root.get("error"));
            throw new IOException("API returned error");
        }

        JsonArray data = root.getAsJsonArray("data");
        if (data == null || data.size() == 0) {
            System.out.println("No flight data returned.");
            return;
        }

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        ZonedDateTime now = ZonedDateTime.now(zone);

        for (JsonElement elem : data) {
            if (!elem.isJsonObject()) continue;
            JsonObject f = elem.getAsJsonObject();

            JsonObject flightObj = getObj(f, "flight");
            JsonObject airlineObj = getObj(f, "airline");
            JsonObject depObj = getObj(f, "departure");
            JsonObject arrObj = getObj(f, "arrival");

            JsonObject timeObj = arrival ? arrObj : depObj;
            if (timeObj == null) continue;

            String flightNum = getString(flightObj, "iata");
            String airline = getString(airlineObj, "name");
            String airlineCode = getString(airlineObj, "iata");
            String from = getString(depObj, "iata");
            String to = getString(arrObj, "iata");
            
            if (flightNum.isBlank() || airline.isBlank()) continue;
            
            String scheduledRaw = getString(timeObj, "scheduled");
            String estimatedRaw = getString(timeObj, "estimated");
            String terminal = getString(timeObj, "terminal");
            String gate = getString(timeObj, "gate");

            if (scheduledRaw.isEmpty()) continue;

            ZonedDateTime flightTime;
            try {
                flightTime = OffsetDateTime.parse(scheduledRaw).atZoneSameInstant(zone);
            } catch (Exception e) {
                continue;
            }
            
            if (flightTime.isBefore(now.minusHours(24)) || flightTime.isAfter(now.plusHours(24))) {
                continue;
            } 

            String statusRaw = getString(f, "flight_status").toLowerCase();
            if (statusRaw.equals("cancelled")) continue;

            if (!ALLOWED_AIRLINES.isEmpty() && !airlineCode.isEmpty()
                    && !ALLOWED_AIRLINES.contains(airlineCode)) {
                continue;
            }

            String scheduled = formatTime(scheduledRaw);
            String estimated = formatTime(estimatedRaw);

            Map<String, String> flight = new HashMap<>();
            flight.put("flight", emptyDash(flightNum));
            flight.put("airline", emptyDash(airline));
            flight.put("from", emptyDash(from));
            flight.put("to", emptyDash(to));
            flight.put("status", formatStatus(statusRaw));
            flight.put("scheduled", scheduled);
            flight.put("estimated", estimated);
            flight.put("terminal", emptyDash(terminal));
            flight.put("gate", emptyDash(gate));
            flight.put("scheduledTime", flightTime.toString());

            target.add(flight);
        }

    }

    private static JsonObject getObj(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && obj.get(key).isJsonObject())
                ? obj.getAsJsonObject(key)
                : null;
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return "";
        }
        return obj.get(key).getAsString();
    }

    private static String emptyDash(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private static String formatTime(String iso) {
        if (iso == null || iso.isBlank()) return "—";
        try {
            ZonedDateTime zdt = OffsetDateTime.parse(iso)
                    .atZoneSameInstant(ZoneId.systemDefault());
            return zdt.format(DateTimeFormatter.ofPattern("hh:mm a"));
        } catch (Exception e) {
            return "—";
        }
    }

    private static String formatStatus(String status) {
        switch (status) {
            case "scheduled": return "On Time";
            case "active": return "En Route";
            case "landed": return "Arrived";
            case "delayed": return "Delayed";
            case "cancelled": return "Cancelled";
            case "diverted": return "Diverted";
            default: return status.isEmpty() ? "Unknown" : status.substring(0,1).toUpperCase() + status.substring(1);
        }
    }

    private String currentSortKey = null;
    private boolean sortAscending = true; 

    private Node createAirportsPage() {
        VBox pageBox = new VBox(20);
        pageBox.setAlignment(Pos.CENTER);
        pageBox.setPadding(new Insets(20));

        Label title = new Label("Global Airport Explorer");
        title.setFont(Font.font(15));
        title.setStyle("-fx-font-weight: bold;");
        pageBox.getChildren().add(title);

        VBox tableBox = new VBox();
        tableBox.setAlignment(Pos.CENTER);
        tableBox.setSpacing(10);
        VBox.setVgrow(tableBox, Priority.ALWAYS);

        Label loading = new Label("Loading airports...");
        tableBox.getChildren().add(loading);
        pageBox.getChildren().add(tableBox);

        Task<List<Map<String, String>>> task = new Task<>() {
            @Override
            protected List<Map<String, String>> call() throws Exception {
                return fetchAirportsCSV();
            }
        }; 

        task.setOnSucceeded(e -> {
            tableBox.getChildren().clear();
            List<Map<String, String>> airports = task.getValue(); 
            if (airports.isEmpty()) {
                tableBox.getChildren().add(new Label("No airports found."));
                return;
            }

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(5);
            grid.setAlignment(Pos.CENTER);

            ColumnConstraints nameCol = new ColumnConstraints();
            nameCol.setMinWidth(260);  
            nameCol.setPrefWidth(300);

            ColumnConstraints iataCol = new ColumnConstraints();
            iataCol.setMinWidth(60);
            iataCol.setPrefWidth(80);

            ColumnConstraints cityCol = new ColumnConstraints();
            cityCol.setMinWidth(180);
            cityCol.setPrefWidth(220);

            ColumnConstraints countryCol = new ColumnConstraints();
            countryCol.setMinWidth(100);
            countryCol.setPrefWidth(120);

            ColumnConstraints continentCol = new ColumnConstraints();
            continentCol.setMinWidth(100);
            continentCol.setPrefWidth(120);

            grid.getColumnConstraints().addAll(
                    nameCol,
                    iataCol,
                    cityCol,
                    countryCol,
                    continentCol
            ); 

            String[] headers = {"Name", "IATA", "City", "Country", "Continent"};
            String[] keys    = {"name", "iata_code", "municipality", "iso_country", "continent"};

            for (int c = 0; c < headers.length; c++) {
                final String sortKey = keys[c];

                Hyperlink h = new Hyperlink(headers[c]);
                h.setUnderline(false);
                h.setFocusTraversable(false);
                h.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-padding: 4;");

                h.setOnAction(ev -> {
                    if (sortKey.equals(currentSortKey)) {
                        sortAscending = !sortAscending;
                    } else {
                        currentSortKey = sortKey;
                        sortAscending = true;
                    }

                    airports.sort((a, b) -> {
                        String v1 = a.getOrDefault(sortKey, "");
                        String v2 = b.getOrDefault(sortKey, "");
                        int cmp = v1.compareToIgnoreCase(v2);
                        return sortAscending ? cmp : -cmp;
                    });

                    rebuildTable(grid, airports, keys);
                });

                grid.add(h, c, 0);
            } 

            rebuildTable(grid, airports, keys); 

            ScrollPane scrollPane = new ScrollPane(grid);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setPannable(true);
            scrollPane.setFocusTraversable(false);
            
            scrollPane.setStyle(
                "-fx-focus-color: transparent;" +
                "-fx-faint-focus-color: transparent;"
            );

            tableBox.getChildren().add(scrollPane); 
        });

        task.setOnFailed(e -> {
            tableBox.getChildren().clear();
            Label error = new Label("Failed to load airports.");
            error.setStyle("-fx-text-fill: black;");
            tableBox.getChildren().add(error);
        });

        new Thread(task).start();
        
        StackPane centeredPane = new StackPane(pageBox);
        centeredPane.setAlignment(Pos.CENTER);
        
        return centeredPane;
    } 

    private void rebuildTable(
        GridPane grid,
        List<Map<String, String>> airports,
        String[] keys
    ) {
    
    grid.getChildren().removeIf(node -> {
        Integer row = GridPane.getRowIndex(node);
        return row != null && row > 0;
    });

    for (int r = 0; r < airports.size(); r++) {
        Map<String, String> a = airports.get(r);

        for (int c = 0; c < keys.length; c++) {
            String key = keys[c];
            String text = a.getOrDefault(key, "");

            if ("name".equals(key)
                    && a.get("home_link") != null
                    && !a.get("home_link").isBlank()
                    && !text.isBlank()) {

                String url = a.get("home_link");

                Hyperlink link = new Hyperlink(text);
                link.setUnderline(false);
                link.setFocusTraversable(false);
                link.setStyle("-fx-padding: 2; -fx-text-fill: #0077cc;");

                link.setOnMouseEntered(ev -> link.setUnderline(true));
                link.setOnMouseExited(ev -> link.setUnderline(false));

                link.setOnAction(ev -> {
                    link.setVisited(false);
                    System.out.println("\nAirport clicked: \n" + text + " -> " + url);
                });

                grid.add(link, c, r + 1);

            } else if ("municipality".equals(key)
                    && a.get("latitude_deg") != null
                    && a.get("longitude_deg") != null
                    && !text.isBlank()) {

                double lat = Double.parseDouble(a.get("latitude_deg"));
                double lon = Double.parseDouble(a.get("longitude_deg"));

                Hyperlink mapLink = new Hyperlink(text);
                mapLink.setUnderline(false);
                mapLink.setFocusTraversable(false);
                mapLink.setStyle("-fx-padding: 2; -fx-text-fill: #0077cc;");

                mapLink.setOnMouseEntered(ev -> mapLink.setUnderline(true));
                mapLink.setOnMouseExited(ev -> mapLink.setUnderline(false));

                mapLink.setOnAction(ev -> {
                    mapLink.setVisited(false); 
                    String mapsUrl = "https://www.google.com/maps/@" + lat + "," + lon + ",14z";
                    System.out.println("\nOpening map link in browser:");
                    System.out.println(text + " -> " + mapsUrl);
                });

                grid.add(mapLink, c, r + 1);

            } else {
                Label label = new Label(text);
                label.setStyle("-fx-padding: 2;");

                if ("iso_country".equals(key) || "continent".equals(key)) {
                    StackPane cell = new StackPane(label);
                    cell.setMaxWidth(Double.MAX_VALUE);
                    cell.setPadding(new Insets(0, 70, 0, 0));
                    StackPane.setAlignment(label, Pos.CENTER_RIGHT);
                    grid.add(cell, c, r + 1);
                } else {
                    grid.add(label, c, r + 1);
                }
            }
        }
    }
  }

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }

        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    } 

    private Node createProfilePage() {
        final StringProperty originalDisplayName = new SimpleStringProperty();
        final StringProperty originalUsername = new SimpleStringProperty();
        final StringProperty originalAgeRange = new SimpleStringProperty();
        final StringProperty originalGender = new SimpleStringProperty();
        final StringProperty originalBio = new SimpleStringProperty();
        final StringProperty originalEmail = new SimpleStringProperty();
        final ObjectProperty<Image> originalAvatar = new SimpleObjectProperty<>();
        ObjectProperty<Image> draftAvatar = new SimpleObjectProperty<>(); 

        VBox pageBox = new VBox(25);
        pageBox.setAlignment(Pos.TOP_CENTER);
        pageBox.setPadding(new Insets(30));

        Label title = new Label("Your Profile");
        title.setFont(Font.font(15));
        title.setStyle("-fx-font-weight: bold;");

        ImageView avatar = new ImageView();
        avatar.setFitWidth(140);
        avatar.setFitHeight(140);
        avatar.setPreserveRatio(true);
        avatar.setSmooth(true);
        avatar.setStyle("-fx-background-radius: 70;"); 
        avatar.imageProperty().bind(profileAvatar);

        Label nameLabel = new Label();
        nameLabel.textProperty().bind(displayName);
        nameLabel.setStyle("-fx-font-size: 15; -fx-font-weight: bold;");

        Label usernameLabel = new Label();
        usernameLabel.textProperty().bind(username);
        usernameLabel.setStyle("-fx-text-fill: #555;");

        Label ageGenderLabel = new Label();
        ageGenderLabel.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    if (!ageRange.get().isEmpty() && !gender.get().isEmpty())
                        return ageRange.get() + " • " + gender.get();
                    if (!ageRange.get().isEmpty())
                        return ageRange.get();
                    if (!gender.get().isEmpty())
                        return gender.get();
                    return "";
                }, ageRange, gender)
        );
        ageGenderLabel.setStyle("-fx-text-fill: #666;");
        ageGenderLabel.setAlignment(Pos.CENTER);
        ageGenderLabel.setTextAlignment(TextAlignment.CENTER);

        ageGenderLabel.visibleProperty().bind(
                ageRange.isNotEmpty().or(gender.isNotEmpty())
        );
        ageGenderLabel.managedProperty().bind(
                ageRange.isNotEmpty().or(gender.isNotEmpty())
        );

        Label bioLabel = new Label();
        bioLabel.textProperty().bind(bio);
        bioLabel.setWrapText(true);
        bioLabel.setMaxWidth(420);
        bioLabel.setAlignment(Pos.CENTER);
        bioLabel.setTextAlignment(TextAlignment.CENTER);

        Label verifiedLabel = new Label();
        verifiedLabel.textProperty().bind(
                Bindings.when(emailVerified)
                        .then("✔ Email Verified")
                        .otherwise("")
        );
        verifiedLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

        Label meetupCountLabel = new Label();
        meetupCountLabel.textProperty().bind(
                meetupsCompleted.asString("Meetups Completed: %d")
        );
        meetupCountLabel.setStyle("-fx-text-fill: #555;");

        VBox publicViewBox = new VBox(8,
                nameLabel,
                usernameLabel,
                ageGenderLabel,
                bioLabel,
                verifiedLabel,
                meetupCountLabel
        );
        publicViewBox.setAlignment(Pos.CENTER);

        VBox cardContainer = new VBox(publicViewBox);
        cardContainer.setAlignment(Pos.CENTER);
        cardContainer.setPadding(new Insets(20));
        cardContainer.setSpacing(10);
        cardContainer.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 15;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);" +
            "-fx-border-radius: 15;" +
            "-fx-border-color: #ddd;" +
            "-fx-border-width: 1;"
        );
        cardContainer.setMaxWidth(460);
        cardContainer.setMinWidth(460); 

        TextField nameField = new TextField();
        nameField.textProperty().bindBidirectional(displayName);

        ComboBox<String> ageRangeBox = new ComboBox<>();
        ageRangeBox.getItems().addAll(
                "",
                "18–24",
                "25–34",
                "35–44",
                "45–54",
                "55+"
        );
        ageRangeBox.valueProperty().bindBidirectional(ageRange);
        ageRangeBox.setPromptText("Prefer not to say");

        ComboBox<String> genderBox = new ComboBox<>();
        genderBox.getItems().addAll(
                "",
                "Female",
                "Male",
                "Non-binary",
                "Another Gender",
                "Prefer Not To Say"
        );
        genderBox.valueProperty().bindBidirectional(gender);
        genderBox.setPromptText("Optional");
        genderBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Profile tempProfile = new Profile(
                    0, "", "", "", ageRange.get(), newVal, "", true, 0
                );
                Image newAvatar = getProfilePlaceholderImage(tempProfile, 140);
                profileAvatar.set(newAvatar);

                if (newVal.equalsIgnoreCase("Male")) {
                    userProfileImagePath = "/resources/images/male_place.jpg";
                } else if (newVal.equalsIgnoreCase("Female")) {
                    userProfileImagePath = "/resources/images/female_place.jpg";
                } else {
                    userProfileImagePath = "/resources/images/image_placeholder.jpg";
                }
            }
        });
        
        HBox ageGenderRow = new HBox(12,
            labeledField("Age Range", ageRangeBox),
            labeledField("Gender", genderBox)
        );
        ageGenderRow.setAlignment(Pos.CENTER);
        ageRangeBox.setPrefWidth(140);
        genderBox.setPrefWidth(160); 

        TextField usernameField = new TextField();
        usernameField.textProperty().bindBidirectional(username);

        TextArea bioField = new TextArea();
        bioField.textProperty().bindBidirectional(bio);
        bioField.setWrapText(true);
        bioField.setPrefRowCount(2);

        Label accountTitle = new Label("Account & Safety");
        accountTitle.setStyle("-fx-font-weight: bold;");

        TextField emailField = new TextField();
        emailField.textProperty().bindBidirectional(accountEmail);

        Label emailHint = new Label("Email is never shown publicly.");
        emailHint.setStyle("-fx-text-fill: #777; -fx-font-size: 11;");

        VBox accountBox = new VBox(6,
                labeledField("Email", emailField),
                emailHint
        );
        accountBox.setMaxWidth(400);
        accountBox.setVisible(false);
        accountBox.setManaged(false);

        VBox editProfileBox = new VBox(10,
                labeledField("Display Name", nameField),
                labeledField("Username", usernameField),
                accountBox,
                ageGenderRow,
                labeledField("Bio (keep it general)", bioField)
        );
        editProfileBox.setMaxWidth(400);
        editProfileBox.setVisible(false);
        editProfileBox.setManaged(false);

        VBox editContent = new VBox(20,
            editProfileBox
        );
        editContent.setAlignment(Pos.TOP_CENTER);
        editContent.setPadding(new Insets(10, 0, 10, 0));

        ScrollPane editScrollPane = new ScrollPane(editContent);
        editScrollPane.setFitToWidth(true);
        editScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        editScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        editScrollPane.setVisible(false);
        editScrollPane.setManaged(false);
        editScrollPane.setPrefViewportHeight(420);
        editScrollPane.setStyle("-fx-background-color: transparent;"); 

        Button editBtn = new Button("Edit Profile");
        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");

        editBtn.setFocusTraversable(false);
        editBtn.setStyle(
            "-fx-focus-color: transparent;" +
            "-fx-faint-focus-color: transparent;"
        );
        saveBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
        cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

        Button historyBtn = new Button("View Match History");
        historyBtn.setStyle(
            "-fx-background-color: #3498db;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;"
        );

        historyBtn.setOnAction(e -> root.setCenter(createMatchHistoryPage()));

        HBox buttonBox = new HBox(10, editBtn, historyBtn);
        buttonBox.setAlignment(Pos.CENTER);

        editBtn.setOnAction(e -> {
            originalDisplayName.set(displayName.get());
            originalUsername.set(username.get());
            originalAgeRange.set(ageRange.get());
            originalGender.set(gender.get());
            originalBio.set(bio.get());
            originalEmail.set(accountEmail.get());
            originalAvatar.set(avatar.getImage());

            publicViewBox.setVisible(false);
            publicViewBox.setManaged(false);

            editScrollPane.setVisible(true);
            editScrollPane.setManaged(true);
            editContent.heightProperty().addListener((obs, oldH, newH) -> {
                editScrollPane.setVvalue(0);
            });

            editProfileBox.setVisible(true);
            editProfileBox.setManaged(true);

            accountBox.setVisible(true);
            accountBox.setManaged(true);

            buttonBox.getChildren().setAll(saveBtn, cancelBtn);
        });

        saveBtn.setOnAction(e -> {
            Profile tempProfile = new Profile(
                0,
                displayName.get(),
                username.get(),
                bio.get(),
                ageRange.get(),
                gender.get(),
                accountEmail.get(),
                true,
                meetupsCompleted.get()
            );
            profileAvatar.set(getProfilePlaceholderImage(tempProfile, 140));

            publicViewBox.setVisible(true);
            publicViewBox.setManaged(true);

            editScrollPane.setVisible(false);
            editScrollPane.setManaged(false);
            editProfileBox.setVisible(false);
            editProfileBox.setManaged(false);
            accountBox.setVisible(false);
            accountBox.setManaged(false);

            buttonBox.getChildren().setAll(editBtn, historyBtn);
            saveUserData();
        });

        cancelBtn.setOnAction(e -> {
            displayName.set(originalDisplayName.get());
            username.set(originalUsername.get());
            ageRange.set(originalAgeRange.get());
            gender.set(originalGender.get());
            bio.set(originalBio.get());
            accountEmail.set(originalEmail.get());
           
            profileAvatar.set(originalAvatar.get());

            publicViewBox.setVisible(true);
            publicViewBox.setManaged(true);

            editScrollPane.setVisible(false);
            editScrollPane.setManaged(false);

            editProfileBox.setVisible(false);
            editProfileBox.setManaged(false);

            accountBox.setVisible(false);
            accountBox.setManaged(false);

            buttonBox.getChildren().setAll(editBtn, historyBtn);
        });

        pageBox.getChildren().addAll(
                title,
                avatar,
                cardContainer,
                editScrollPane,
                buttonBox
        ); 

        return pageBox;
    } 

    private VBox labeledField(String labelText, Control field) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold;");
        VBox box = new VBox(4, label, field);
        return box;
    }      

    private double[] fetchUserLocation() { 
        return new double[]{lastLat, lastLon};
    }

    private List<Map<String, String>> fetchAirportsCSV() throws Exception {
        URL url = new URL(AIRPORTS_CSV_URL); 
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        List<Map<String, String>> airports = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                return airports;
            }

            String[] headers = parseCsvLine(headerLine);

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = parseCsvLine(line);

                if (values.length != headers.length) {
                    continue; 
                }

                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i], values[i]);
                }

                
                if ("large_airport".equals(row.get("type"))) {
                    airports.add(row);
                }
            }
        }

        return airports;
    } 

    private void printReadableJson(List<Map<String, String>> airports) {
        System.out.println("[");
        for (int i = 0; i < airports.size(); i++) {
            Map<String, String> row = airports.get(i);
            System.out.println("  {");
            int j = 0;
            for (Map.Entry<String, String> entry : row.entrySet()) {
                System.out.print("    \"" + entry.getKey() + "\": \"" + entry.getValue() + "\"");
                if (j < row.size() - 1) System.out.println(",");
                else System.out.println();
                j++;
            }
            System.out.print("  }");
            if (i < airports.size() - 1) System.out.println(",");
            else System.out.println();
        }
        System.out.println("]");
    } 

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private Node createLobbyDetailPage(int lobbyNumber) {
        VBox pageBox = new VBox(30);
        pageBox.setAlignment(Pos.TOP_CENTER);
        pageBox.setPadding(new Insets(60, 30, 10, 30));

        Image placeholderImage = new Image(
            getClass().getResourceAsStream("/resources/images/airport.png")
        );

        String lobbyTitle = lobbyTitles.get(lobbyNumber - 1);
        Label title = new Label(lobbyTitle);
        title.setFont(Font.font(15));
        title.setStyle("-fx-font-weight: bold;");

        Button backButton = new Button("← Back to Lobby");
        backButton.setFocusTraversable(false);
        backButton.setStyle(
            "-fx-background-color: #ecf0f1;" +
            "-fx-font-weight: bold;" +
            "-fx-border-color: transparent;"
        );
        backButton.setOnAction(e -> root.setCenter(createLobbyPage()));

        HBox cardRow = new HBox(30);
        cardRow.setAlignment(Pos.CENTER);

        List<String[]> items = LOBBY_DETAIL_ITEMS.getOrDefault(lobbyNumber, List.of());

        for (String[] item : items) {
            VBox card = new VBox(14);
            card.setAlignment(Pos.TOP_CENTER);
            card.setPadding(new Insets(0, 0, 18, 0));
            card.setPrefWidth(220);
            card.setMinHeight(220);

            card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 18;" +
                "-fx-border-radius: 18;" +
                "-fx-border-color: #e0e0e0;" +
                "-fx-border-width: 1;"
            );

            DropShadow shadow = new DropShadow();
            shadow.setRadius(18);
            shadow.setOffsetY(6);
            shadow.setColor(Color.rgb(0, 0, 0, 0.18));
            card.setEffect(shadow);

            card.setOnMouseEntered(e -> {
                card.setTranslateY(-6);
                shadow.setRadius(25);
                shadow.setOffsetY(10);
            });

            card.setOnMouseExited(e -> {
                card.setTranslateY(0);
                shadow.setRadius(18);
                shadow.setOffsetY(6);
            });

            card.setOnMousePressed(e -> {
                card.setScaleX(0.97);
                card.setScaleY(0.97);
            });

            card.setOnMouseReleased(e -> {
                card.setScaleX(1);
                card.setScaleY(1);
            });

            card.setOnMouseClicked(e -> {
                if (hasAgreedToTerms.get() && isOver18.get()) {
                    root.setCenter(createQuickMatchPage(lobbyNumber, item[0]));
                } else {
                    root.setCenter(createMeetGatePage(() ->
                        root.setCenter(createQuickMatchPage(lobbyNumber, item[0]))
                    ));
                }
            });

            ImageView imageView = new ImageView(placeholderImage);
            imageView.setFitWidth(220);
            imageView.setFitHeight(120);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);

            Rectangle clip = new Rectangle(220, 120);
            clip.setArcWidth(30);
            clip.setArcHeight(30);
            imageView.setClip(clip);

            Label itemTitle = new Label(item[0]);
            itemTitle.setWrapText(true);
            itemTitle.setTextAlignment(TextAlignment.CENTER);
            itemTitle.setAlignment(Pos.CENTER);
            itemTitle.setMaxWidth(180);
            itemTitle.setFont(Font.font("System", FontWeight.SEMI_BOLD, 15));
            itemTitle.setStyle("-fx-text-fill: #2c3e50;");

            card.getChildren().addAll(imageView, itemTitle);

            if (item.length > 1 && !item[1].isEmpty()) {
                Label subtitle = new Label(item[1]);
                subtitle.setWrapText(true);
                subtitle.setTextAlignment(TextAlignment.CENTER);
                subtitle.setAlignment(Pos.CENTER);
                subtitle.setMaxWidth(180);
                subtitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12;");
                card.getChildren().add(subtitle);
            }

            cardRow.getChildren().add(card);
        }

        pageBox.getChildren().addAll(title, cardRow, backButton);
        return pageBox;
    }

    private Node createQuickMatchPage(int lobbyNumber, String cardTitle) {
        StackPane rootStack = new StackPane();
        VBox pageBox = new VBox(15);
        pageBox.setAlignment(Pos.CENTER);
        pageBox.setPadding(new Insets(-60, 0, 0, 0));

        Label title = new Label("Quick Match");
        title.setFont(Font.font(16));
        title.setStyle("-fx-font-weight: bold;");
        pageBox.getChildren().add(title);

        Label subtitle = new Label(cardTitle);
        subtitle.setFont(Font.font(13));
        subtitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");
        pageBox.getChildren().add(subtitle);

        HBox cardRow = new HBox(30);
        cardRow.setAlignment(Pos.CENTER);

        Random rand = new Random();
        Set<Integer> selectedProfiles = new HashSet<>();
        while (selectedProfiles.size() < 5) {
            selectedProfiles.add(rand.nextInt(MAX_PROFILES) + 1);
        }

        for (int profileNum : selectedProfiles) {
            VBox card = new VBox(12);
            card.setPadding(new Insets(14));
            card.setAlignment(Pos.CENTER);
            card.setPrefWidth(140);
            card.setPrefHeight(190);
            card.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #ffffff, #f8fafc);" +
                "-fx-background-radius: 16;" +
                "-fx-border-radius: 16;" +
                "-fx-border-color: #e2e8f0;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0.35, 0, 6);"
            );

            Profile prof = ProfileDatabase.getProfile(profileNum);

            ImageView imgView = new ImageView(getProfilePlaceholderImage(prof, 100));
            imgView.setFitWidth(115);
            imgView.setPreserveRatio(true);
            imgView.setSmooth(true);

            Rectangle clip = new Rectangle(115, 115);
            clip.setArcWidth(18);
            clip.setArcHeight(18);
            imgView.setClip(clip);

            Label nameLabel = new Label(prof.getDisplayName());
            nameLabel.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-font-weight: 600;" +
                "-fx-text-fill: #1f2937;"
            );

            card.setOnMouseEntered(e -> {
                card.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #ffffff, #f1f5f9);" +
                    "-fx-background-radius: 14;" +
                    "-fx-border-radius: 14;" +
                    "-fx-border-color: #cbd5e1;" +
                    "-fx-border-width: 1;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 25, 0.4, 0, 8);"
                );
            });

            card.setOnMouseExited(e -> {
                card.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #ffffff, #f8fafc);" +
                    "-fx-background-radius: 14;" +
                    "-fx-border-radius: 14;" +
                    "-fx-border-color: #e2e8f0;" +
                    "-fx-border-width: 1;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 15, 0.3, 0, 4);"
                );
            });

            card.setUserData(profileNum);
            card.getChildren().addAll(imgView, nameLabel);
            card.setOnMouseClicked(e -> showProfileOverlay(rootStack, pageBox, card));
            cardRow.getChildren().add(card);
        }

        pageBox.getChildren().add(cardRow);

        Button backButton = new Button("← Back"); 
        backButton.setStyle("-fx-background-color: #ecf0f1; -fx-font-weight: bold; -fx-border-color: transparent;");
        backButton.setOnAction(e -> root.setCenter(createLobbyDetailPage(lobbyNumber)));
        pageBox.getChildren().add(backButton);

        rootStack.getChildren().add(pageBox);
        return rootStack;
    } 

    private Node createMatchHistoryPage() {
        VBox pageBox = new VBox(15);
        pageBox.setAlignment(Pos.TOP_CENTER);
        pageBox.setPadding(new Insets(15, 15, 15, 15));

        Label title = new Label("Match History");
        title.setFont(Font.font(15));
        title.setStyle("-fx-font-weight: bold;");

        FlowPane matchesBox = new FlowPane();
        matchesBox.setPadding(new Insets(0, 0, 0, 0));
        matchesBox.setHgap(1);
        matchesBox.setVgap(0);
        matchesBox.setAlignment(Pos.CENTER);

        if (matchHistory.isEmpty()) {
            Label empty = new Label("No connections yet.");
            empty.setStyle("-fx-text-fill: #777;");
            matchesBox.getChildren().add(empty);
        } else {
            for (Profile profile : new ArrayList<>(matchHistory)) {

                VBox miniCard = buildMiniProfileCard(profile);
                miniCard.setScaleX(0.65);
                miniCard.setScaleY(0.65);

                Button unmatchBtn = new Button("✕");
                unmatchBtn.setStyle(
                    "-fx-background-color: #e74c3c;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 9;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 10;" +
                    "-fx-min-width: 16;" +
                    "-fx-min-height: 16;" +
                    "-fx-cursor: hand;"
                );

                StackPane wrapper = new StackPane(miniCard);
                StackPane.setAlignment(unmatchBtn, Pos.TOP_RIGHT);
                StackPane.setMargin(unmatchBtn, new Insets(10));
                wrapper.getChildren().add(unmatchBtn);

                userCardMap.put(profile.getUsername(), wrapper);

                if (reportedUsers.contains(profile.getUsername())) {

                    Label flag = new Label("⚑");
                    flag.setStyle(
                        "-fx-text-fill: #f39c12;" +
                        "-fx-font-size: 18;" +
                        "-fx-font-weight: bold;"
                    );

                    StackPane.setAlignment(flag, Pos.TOP_RIGHT);
                    StackPane.setMargin(flag, new Insets(10, 30, 0, 0));

                    Tooltip tooltip = new Tooltip("Reported user");
                    Tooltip.install(flag, tooltip);

                    wrapper.getChildren().add(flag);

                    wrapper.setStyle(
                        "-fx-opacity: 0.7;" +
                        "-fx-background-color: rgba(255,0,0,0.1);"
                    );

                    flag.setOnMouseClicked(event -> {
                        wrapper.getChildren().remove(flag);
                        Tooltip.uninstall(flag, tooltip);
                        wrapper.setStyle("");

                        reportedUsers.remove(profile.getUsername()); 
                        saveUserData();
                    });
                }

                unmatchBtn.setOnAction(e -> {
                    FadeTransition fade = new FadeTransition(Duration.millis(250), wrapper);
                    fade.setToValue(0);

                    ScaleTransition shrink = new ScaleTransition(Duration.millis(250), wrapper);
                    shrink.setToX(0.8);
                    shrink.setToY(0.8);

                    ParallelTransition animation = new ParallelTransition(fade, shrink);

                    animation.setOnFinished(event -> {
                        matchHistory.remove(profile);
                        saveUserData();
                        matchesBox.getChildren().remove(wrapper);

                        if (matchHistory.isEmpty()) {
                            matchesBox.getChildren().clear();
                            Label empty = new Label("No connections yet.");
                            empty.setStyle("-fx-text-fill: #777;");
                            matchesBox.getChildren().add(empty);
                        }
                    });

                    animation.play();
                });

                matchesBox.getChildren().add(wrapper);
            }
        }

        ScrollPane scrollPane = new ScrollPane(matchesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(400);
        scrollPane.setFocusTraversable(false);
        scrollPane.setStyle(
            "-fx-focus-color: transparent;" +
            "-fx-faint-focus-color: transparent;"
        );

        Button backBtn = new Button("← Back to Profile");
        backBtn.setStyle(
            "-fx-background-color: #ecf0f1;" +
            "-fx-font-weight: bold;"
        );
        backBtn.setOnAction(e -> root.setCenter(createProfilePage()));

        pageBox.getChildren().addAll(title, scrollPane, backBtn);

        Button reportBtn = new Button("Report/Block");
        reportBtn.setStyle(
            "-fx-background-color: #e67e22;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 10 20 10 20;" +
            "-fx-cursor: hand;"
        );

        StackPane.setAlignment(reportBtn, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(reportBtn, new Insets(0, 20, 12, 0));

        reportBtn.setOnAction(e -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Report/Block User");
            dialog.getDialogPane().setStyle(
                "-fx-font-size: 12px;"
            );

            Stage primaryStage = (Stage) pageBox.getScene().getWindow();
            dialog.initOwner(primaryStage);

            ComboBox<String> userCombo = new ComboBox<>();
            for (Profile p : matchHistory) {
                userCombo.getItems().add(p.getUsername()); 
            }
            userCombo.setPromptText("Select user");

            ComboBox<String> reasonCombo = new ComboBox<>();
            reasonCombo.getItems().addAll("Spam", "Harassment", "Inappropriate content", "Other");
            reasonCombo.setPromptText("Select reason");

            CheckBox blockCheck = new CheckBox("Block this user from future interactions");
            CheckBox verifyCheck = new CheckBox("I confirm this report to be fully accurate");

            VBox dialogBox = new VBox(15, userCombo, reasonCombo, blockCheck, verifyCheck);
            dialogBox.setPadding(new Insets(20));
            dialogBox.setAlignment(Pos.CENTER_LEFT);

            dialogBox.setPrefWidth(350);  
            dialogBox.setPrefHeight(180);

            dialog.getDialogPane().setContent(dialogBox);
            dialog.getDialogPane().setMinWidth(350);  
            dialog.getDialogPane().setMinHeight(200); 
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.getDialogPane().setStyle(
                "-fx-background-color: white;" +     
                "-fx-border-color: #bdc3c7;" +        
                "-fx-border-width: 1;"      
            );

            Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            okButton.setDisable(true);
            okButton.setStyle(
                "-fx-background-color: #27ae60;" + 
                "-fx-text-fill: white;" +           
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: hand;" 
            );

            Runnable validateForm = () -> {
                boolean valid =
                    userCombo.getValue() != null &&
                    reasonCombo.getValue() != null &&
                    verifyCheck.isSelected();

                okButton.setDisable(!valid);
            };

            userCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
            reasonCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateForm.run());
            verifyCheck.selectedProperty().addListener((obs, oldVal, newVal) -> validateForm.run());

            Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            cancelButton.setStyle(
                "-fx-background-color: #FF0000;" + 
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: hand;"
            );

            Optional<ButtonType> result = dialog.showAndWait();
            result.ifPresent(response -> {
                if (response == ButtonType.OK &&
                    userCombo.getValue() != null &&
                    reasonCombo.getValue() != null &&
                    verifyCheck.isSelected()) {

                    String selectedUser = userCombo.getValue();
                    StackPane userCard = userCardMap.get(selectedUser);

                    if (userCard != null) {
                        if (blockCheck.isSelected()) {

                            FadeTransition fade = new FadeTransition(Duration.millis(250), userCard);
                            fade.setToValue(0);

                            ScaleTransition shrink = new ScaleTransition(Duration.millis(250), userCard);
                            shrink.setToX(0.8);
                            shrink.setToY(0.8);

                            ParallelTransition animation = new ParallelTransition(fade, shrink);

                            animation.setOnFinished(event -> {
                                matchHistory.removeIf(p -> p.getUsername().equals(selectedUser));
                                reportedUsers.remove(selectedUser); 
                                matchesBox.getChildren().remove(userCard);
                                saveUserData();
                            });

                            animation.play();

                        } else {
                            boolean alreadyFlagged = userCard.getChildren().stream()
                                .anyMatch(node -> node instanceof Label && "⚑".equals(((Label) node).getText()));

                            if (!alreadyFlagged) {
                                
                                Label flag = new Label("⚑");
                                flag.setStyle(
                                    "-fx-text-fill: #f39c12;" +
                                    "-fx-font-size: 18;" +
                                    "-fx-font-weight: bold;"
                                );

                                StackPane.setAlignment(flag, Pos.TOP_RIGHT);
                                StackPane.setMargin(flag, new Insets(10, 30, 0, 0));

                                Tooltip tooltip = new Tooltip("Reported user");
                                Tooltip.install(flag, tooltip);
                                tooltip.setStyle(
                                    "-fx-background-color: #FF0000;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-font-size: 10;" +
                                    "-fx-font-weight: bold;"
                                );

                                userCard.getChildren().add(flag);
                                userCard.setStyle(
                                    "-fx-opacity: 0.7;" +                  
                                    "-fx-background-color: rgba(255,0,0,0.1);" 
                                );

                                reportedUsers.add(selectedUser); 
                                saveUserData();
                        
                                flag.setOnMouseClicked(event -> {
                                    userCard.getChildren().remove(flag);          
                                    Tooltip.uninstall(flag, tooltip);             
                                    userCard.setStyle("");

                                    reportedUsers.remove(selectedUser); 
                                    saveUserData();

                                    System.out.println("Removed flag from user: " + selectedUser);
                                });
                                
                            }
                        }
                    }
                    
                    System.out.println("Reported user: " + selectedUser + " Reason: " + reasonCombo.getValue());
                    
                }
            });
            
        });

        StackPane mainPane = new StackPane(pageBox, reportBtn);
        return mainPane;
    }

    private void showProfileOverlay(StackPane rootStack, VBox pageBox, VBox sourceCard) {
        int profileNumber = (int) sourceCard.getUserData();

        Pane backdrop = new Pane();
        backdrop.setStyle(
            "-fx-background-color: linear-gradient(to bottom, rgba(0,0,0,0.0), rgba(0,0,0,0.65));"
        );
        backdrop.prefWidthProperty().bind(rootStack.widthProperty());
        backdrop.prefHeightProperty().bind(rootStack.heightProperty());

        VBox bigCard = createProfileCard(profileNumber, () -> {
            rootStack.getChildren().removeIf(n -> n instanceof StackPane);
        });

        pageBox.applyCss();
        pageBox.layout();

        double smallCardWidth = 120;
        double hGap = 20;
        double popupWidth = (smallCardWidth * 3) + (hGap * 2);
        double popupHeight = pageBox.getBoundsInParent().getHeight();

        bigCard.setPrefWidth(popupWidth);
        bigCard.setPrefHeight(popupHeight);
        bigCard.setMaxWidth(popupWidth);
        bigCard.setMaxHeight(popupHeight);

        StackPane overlay = new StackPane(backdrop, bigCard);
        overlay.setAlignment(Pos.CENTER);
        rootStack.getChildren().add(overlay);

        bigCard.setScaleX(0.8);
        bigCard.setScaleY(0.8);
        bigCard.setOpacity(0);
        backdrop.setOpacity(0);

        FadeTransition backdropFade = new FadeTransition(Duration.millis(250), backdrop);
        backdropFade.setFromValue(0);
        backdropFade.setToValue(1);
        backdropFade.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(280), bigCard);
        scaleUp.setFromX(0.8);
        scaleUp.setFromY(0.8);
        scaleUp.setToX(1.05); 
        scaleUp.setToY(1.05);
        scaleUp.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(120), bigCard);
        scaleDown.setFromX(1.05);
        scaleDown.setFromY(1.05);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        scaleDown.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(280), bigCard);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setInterpolator(Interpolator.EASE_BOTH);

        SequentialTransition scaleSequence = new SequentialTransition(scaleUp, scaleDown);
        ParallelTransition showAnim = new ParallelTransition(fadeIn, scaleSequence, backdropFade);
        showAnim.play();

        enableSwipe(bigCard, direction -> {
            if (direction == 1) {
                handleRightSwipe(rootStack, profileNumber, root.getCenter());
            }
            replaceCardWithRandom(sourceCard);
            rootStack.getChildren().removeIf(n -> n instanceof StackPane);
        });
    }

    private VBox createProfileCard(int profileNumber, Runnable onClose) {
        Profile profile = ProfileDatabase.getProfile(profileNumber);

        Image profileImage = getProfilePlaceholderImage(profile, 220);
        ImageView imageView = new ImageView(profileImage);
        imageView.setFitWidth(220);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        Label name = new Label(profile.getDisplayName());
        name.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        Label username = new Label(profile.getUsername());
        username.setStyle("-fx-text-fill: #666;");

        Label ageRange = new Label(profile.getAgeRange());
        ageRange.setStyle("-fx-text-fill: #666; -fx-font-size: 12; -fx-font-weight: bold;");

        Label gender = new Label(profile.getGender());
        gender.setStyle("-fx-text-fill: #666; -fx-font-size: 12; -fx-font-weight: bold;");

        Label bio = new Label(profile.getBio());
        bio.setWrapText(true);
        bio.setTextAlignment(TextAlignment.CENTER);

        Label footer = new Label("Swipe Left or Right");
        footer.setStyle("-fx-text-fill: #555; -fx-font-size: 12; -fx-font-weight: bolder;");

        VBox infoBox = new VBox(6, name, username, ageRange, gender, bio, footer);
        infoBox.setAlignment(Pos.CENTER);

        VBox contentBox = new VBox(15, imageView, infoBox);
        contentBox.setAlignment(Pos.CENTER);

        Button closeBtn = new Button("✕");
        closeBtn.setOnAction(e -> onClose.run());
        closeBtn.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-font-size: 16;" +
            "-fx-text-fill: #555;" +
            "-fx-cursor: hand;"
        );

        StackPane card = new StackPane(contentBox, closeBtn);
        card.setPadding(new Insets(20));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 10);"
        );

        StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(closeBtn, new Insets(5));

        VBox wrapper = new VBox(card);
        wrapper.setAlignment(Pos.CENTER);

        return wrapper;
    }

    private void enableSwipe(Node card, Consumer<Integer> onSwipe) {
        final DoubleProperty startX = new SimpleDoubleProperty();

        card.setOnMousePressed(e -> startX.set(e.getSceneX()));

        card.setOnMouseDragged(e -> {
            double offset = e.getSceneX() - startX.get();
            card.setTranslateX(offset);
            card.setRotate(offset / 10); 
        });

        card.setOnMouseReleased(e -> {
            double offset = card.getTranslateX();

            if (Math.abs(offset) > 150) {
                int direction = offset > 0 ? 1 : -1;
                swipeAway(card, direction, () -> onSwipe.accept(direction));
            } else {
                resetCard(card);
            }
        });
    }

    private void swipeAway(Node card, int direction, Runnable onDismiss) {
        TranslateTransition move = new TranslateTransition(Duration.millis(300), card);
        move.setToX(direction * 800);

        RotateTransition rotate = new RotateTransition(Duration.millis(300), card);
        rotate.setToAngle(direction * 30);

        ParallelTransition swipe = new ParallelTransition(move, rotate);
        swipe.setOnFinished(e -> onDismiss.run());
        swipe.play();
    }

    private void resetCard(Node card) {
        TranslateTransition move = new TranslateTransition(Duration.millis(200), card);
        move.setToX(0);

        RotateTransition rotate = new RotateTransition(Duration.millis(200), card);
        rotate.setToAngle(0);

        new ParallelTransition(move, rotate).play();
    }

    private void replaceCardWithRandom(VBox card) { 
        int oldNumber = (int) card.getUserData();
        usedProfileNumbers.remove(oldNumber);

        int newNumber;
        Random rand = new Random();
        do {
            newNumber = rand.nextInt(MAX_PROFILES) + 1;
        } while (usedProfileNumbers.contains(newNumber));

        card.setUserData(newNumber);
        usedProfileNumbers.add(newNumber);
        
        Profile prof = ProfileDatabase.getProfile(newNumber);

        ImageView imgView = (ImageView) card.getChildren().get(0);
        imgView.setImage(getProfilePlaceholderImage(prof, 100));

        Label nameLabel = (Label) card.getChildren().get(1);
        nameLabel.setText(prof.getDisplayName());
    } 

    private void handleRightSwipe(StackPane rootStack, int matchedProfileId, Node previousPage) {
        Random r = new Random();

        double matchChance = 0.10; 
        if (r.nextDouble() < matchChance) {
            meetupsCompleted.set(meetupsCompleted.get() + 1);
            Profile matchedProfile = ProfileDatabase.getProfile(matchedProfileId);
            matchHistory.add(matchedProfile);
            saveUserData();
            showFloatingReward(rootStack, "+1 Meetup!");

            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> showMatchPage(matchedProfileId, previousPage));
            pause.play();
            return;
        }

        int roll = r.nextInt(100); 
        if (roll < 1) {
            meetupsCompleted.set(meetupsCompleted.get() + 2);
            showFloatingReward(rootStack, "+2 Meetups!");
        }
    }
    
    private void showFloatingReward(StackPane rootStack, String text) {
        Label reward = new Label(text);
        reward.setStyle(
            "-fx-font-size: 22;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #2ecc71;"
        );

        reward.setOpacity(0);

        rootStack.getChildren().add(reward);
        StackPane.setAlignment(reward, Pos.TOP_CENTER);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), reward);
        fadeIn.setToValue(1);

        TranslateTransition floatUp = new TranslateTransition(Duration.millis(1200), reward);
        floatUp.setByY(-80);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(1200), reward);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition seq = new SequentialTransition(
            fadeIn,
            new ParallelTransition(floatUp, fadeOut)
        );

        seq.setOnFinished(e -> rootStack.getChildren().remove(reward));
        seq.play();
    }

    private void showMatchPage(int matchedProfileId, Node previousPage) {
        VBox pageBox = new VBox(30);
        pageBox.setAlignment(Pos.CENTER);
        pageBox.setPadding(new Insets(-60, 15, 15, 15)); 

        Label title = new Label("Connection Initiated");
        title.setFont(Font.font(15));
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;"); 

        Profile matched = ProfileDatabase.getProfile(matchedProfileId);    

        VBox matchedCard = buildMiniProfileCard(matched);

        Profile currentUser = new Profile(
            0,
            displayName.get(),
            username.get(),
            bio.get(),
            ageRange.get(),
            gender.get(),
            accountEmail.get(),
            true,
            meetupsCompleted.get()
        );

        VBox userCard = buildMiniProfileCard(currentUser);
        matchedCard.setScaleX(1.1);
        matchedCard.setScaleY(1.1);

        userCard.setScaleX(1.1);
        userCard.setScaleY(1.1);

        HBox cardsRow = new HBox(60, userCard, matchedCard);
        cardsRow.setAlignment(Pos.CENTER);

        cardsRow.setOpacity(0);
        cardsRow.setTranslateY(20);

        FadeTransition fade = new FadeTransition(Duration.millis(350), cardsRow);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(350), cardsRow);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, slide).play();

        Button backBtn = new Button("← Back");
        backBtn.setOnAction(e -> root.setCenter(previousPage));
        backBtn.setStyle("-fx-background-color: #ecf0f1; -fx-font-weight: bold;");

        pageBox.getChildren().addAll(title, cardsRow, backBtn);

        StackPane stack = new StackPane(pageBox);
        root.setCenter(stack);
    }

    private VBox buildMiniProfileCard(Profile profile) {
        ImageView imageView = new ImageView(
            getProfilePlaceholderImage(profile, 120)
        );
        imageView.setFitWidth(120);
        imageView.setPreserveRatio(true);

        Label name = new Label(profile.getDisplayName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");

        Label usernameLabel = new Label(profile.getUsername());
        usernameLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");

        VBox card = new VBox(10, imageView, name, usernameLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 15;" +
            "-fx-border-radius: 15;" +
            "-fx-border-color: #ddd;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);"
        );

        card.setOnMouseClicked(e -> showExpandedProfile(profile));

        card.setOnMouseEntered(e ->
            card.setStyle(
                "-fx-background-color: #f9f9f9;" +
                "-fx-background-radius: 15;" +
                "-fx-border-radius: 15;" +
                "-fx-border-color: #ddd;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 6);"
            )
        );

        card.setOnMouseExited(e ->
            card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 15;" +
                "-fx-border-radius: 15;" +
                "-fx-border-color: #ddd;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 5);"
            )
        );

        return card;
    }

    private void showExpandedProfile(Profile profile) {

        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.6);");

        VBox expandedCard = new VBox(15);
        expandedCard.setAlignment(Pos.CENTER);
        expandedCard.setPadding(new Insets(30));
        expandedCard.setMaxWidth(400);

        expandedCard.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 20;" +
            "-fx-border-radius: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 25, 0, 0, 10);"
        );

        ImageView imageView = new ImageView(
            getProfilePlaceholderImage(profile, 220)
        );
        imageView.setFitWidth(220);
        imageView.setPreserveRatio(true);

        Label name = new Label(profile.getDisplayName());
        name.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");

        Label usernameLabel = new Label(profile.getUsername());
        usernameLabel.setStyle("-fx-text-fill: #666;");

        Label ageRange = new Label(profile.getAgeRange());
        ageRange.setStyle("-fx-text-fill: #555;");

        Label gender = new Label(profile.getGender());
        gender.setStyle("-fx-text-fill: #555;");

        Label bio = new Label(profile.getBio());
        bio.setWrapText(true);
        bio.setTextAlignment(TextAlignment.CENTER);

        Label emailVerifiedLabel = new Label(
            profile.isEmailVerified() ? "✔ Email Verified" : ""
        );
        emailVerifiedLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

        Label meetups = new Label(
            "Meetups Completed: " + profile.getMeetupsCompleted()
        );
        meetups.setStyle("-fx-text-fill: #555;");

        Button closeBtn = new Button("Close");
        closeBtn.setStyle(
            "-fx-background-color: #ecf0f1;" +
            "-fx-font-weight: bold;"
        );

        closeBtn.setOnAction(e ->
            ((StackPane) root.getCenter()).getChildren().remove(overlay)
        );

        expandedCard.getChildren().addAll(
            imageView,
            name,
            usernameLabel,
            ageRange,
            gender,
            bio,
            emailVerifiedLabel,
            meetups,
            closeBtn
        );

        overlay.getChildren().add(expandedCard);
        StackPane.setAlignment(expandedCard, Pos.CENTER);

        ((StackPane) root.getCenter()).getChildren().add(overlay);
    }

    private Image getProfilePlaceholderImage(Profile profile, int size) {
        if (profile.imagePath != null && !profile.imagePath.isEmpty()) {
            InputStream is = getClass().getResourceAsStream(profile.imagePath);
            if (is != null) {
                return new Image(is, size, size, true, true);
            }
        }
        
        String path;
        if (profile.getGender().equalsIgnoreCase("Male")) {
            path = "resources/images/male_place.jpg";  
        } else if (profile.getGender().equalsIgnoreCase("Female")) {
            path = "resources/images/female_place.jpg";
        } else {
            path = "resources/images/image_placeholder.jpg";
        }

        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            System.err.println("Placeholder image not found at: " + path);
            return new Image("https://via.placeholder.com/" + size); 
        }
        return new Image(is, size, size, true, true);
    }

    private void startServer() {
        try {
            int port = 8080;
            server = HttpServer.create(new InetSocketAddress(port), 0);

            server.createContext("/", new StaticFileHandler());

            server.createContext("/location", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if ("POST".equals(exchange.getRequestMethod())) {
                        InputStream is = exchange.getRequestBody();
                        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                        Gson gson = new Gson();
                        Map<?, ?> json = gson.fromJson(body, Map.class);
                        lastLat = ((Number) json.get("lat")).doubleValue();
                        lastLon = ((Number) json.get("lon")).doubleValue();

                        String response = "Location received";
                        exchange.sendResponseHeaders(200, response.length());
                        exchange.getResponseBody().write(response.getBytes());
                        exchange.close();
                    } else {
                        exchange.sendResponseHeaders(405, -1); 
                    }
                }
            });

            server.setExecutor(null); 
            server.start();
            System.out.println("Server running at http://localhost:" + port);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File file = new File("index.html"); 

            if (!file.exists()) {
                String response = "404 Not Found";
                exchange.sendResponseHeaders(404, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
                return;
            }

            byte[] bytes = Files.readAllBytes(file.toPath());

            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, bytes.length);

            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Server stopped.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    static class Profile {

        final int id;
        final String displayName;
        final String username;
        final String bio;
        final String ageRange;
        final String gender;
        final String email;
        final boolean emailVerified;
        final int meetupsCompleted;
        public String imagePath;

        String getDisplayName() {
            return displayName;
        }

        String getUsername() {
            return username;
        }

        String getBio() {
            return bio;
        }

        String getAgeRange() {
            return ageRange;
        }

        String getGender() {
            return gender;
        }

        String getEmail() {
            return email;
        }

        boolean isEmailVerified() {
            return emailVerified;
        }

        int getMeetupsCompleted() {
            return meetupsCompleted;
        }

        Profile(
                int id,
                String displayName,
                String username,
                String bio,
                String ageRange,
                String gender,
                String email,
                boolean emailVerified,
                int meetupsCompleted
        ) {
            this.id = id;
            this.displayName = displayName;
            this.username = username;
            this.bio = bio;
            this.ageRange = ageRange;
            this.gender = gender;
            this.email = email;
            this.emailVerified = emailVerified;
            this.meetupsCompleted = meetupsCompleted;
             this.imagePath = "";
        }
    }

    static class ProfileDatabase {

        static final int MAX_PROFILES = 100;
        private static final Map<Integer, Profile> PROFILES = new HashMap<>();

        private static final String[] FIRST_NAMES = {
            "Alex", "Jordan", "Sam", "Taylor", "Chris",
            "Morgan", "Jamie", "Riley", "Casey", "Drew"
        };

        private static final String[] LAST_NAMES = {
            "Miles", "Turner", "Reed", "Brooks", "Carter",
            "Lopez", "Nguyen", "Patel", "Kim", "Garcia"
        };

        private static final String[] BIOS = {
            "Frequent traveler ✈️",
            "Layovers are my second home.",
            "Here for chill conversation.",
            "Aviation nerd • Window seat only",
            "Killing time between flights"
        };

        private static final String[] AGE_RANGES = {
            "", "18–24", "25–34", "35–44", "45–54", "55+"
        };

        private static final String[] GENDERS = {
            "", "Female", "Male", "Non-binary", "Prefer Not To Say"
        };

        static {
            generate();
        }

        private static void generate() {
            Random r = new Random();

            for (int i = 1; i <= MAX_PROFILES; i++) {
                String first = FIRST_NAMES[r.nextInt(FIRST_NAMES.length)];
                String last = LAST_NAMES[r.nextInt(LAST_NAMES.length)];
                String gender = GENDERS[r.nextInt(GENDERS.length)];

                Profile profile = new Profile(
                    i,
                    first + " " + last,
                    "@" + first.toLowerCase() + i,
                    BIOS[r.nextInt(BIOS.length)],
                    AGE_RANGES[r.nextInt(AGE_RANGES.length)],
                    gender,
                    first.toLowerCase() + i + "@example.com",
                    r.nextBoolean(),
                    r.nextInt(20)
                );

                if ("Male".equals(gender)) {
                    profile.imagePath = "/resources/images/male_place.jpg";
                } else if ("Female".equals(gender)) {
                    profile.imagePath = "/resources/images/female_place.jpg";
                } else {
                    profile.imagePath = "/resources/images/image_placeholder.jpg";
                }

                PROFILES.put(i, profile);
            }
        }

        static Profile getProfile(int id) {
            return PROFILES.get(id);
        }
    }

    static class UserData {
        String displayName;
        String username;
        String bio;
        String ageRange;
        String gender;
        String email;
        boolean emailVerified;
        int meetupsCompleted;
        String imagePath;
        List<Profile> matchHistoryProfiles = new ArrayList<>(); 
    }

        private void saveUserData() {
            try {
                UserData data = new UserData();

                data.displayName = displayName.get();
                data.username = username.get();
                data.bio = bio.get();
                data.ageRange = ageRange.get();
                data.gender = gender.get();
                data.email = accountEmail.get();
                data.emailVerified = emailVerified.get();
                data.meetupsCompleted = meetupsCompleted.get();
                data.imagePath = userProfileImagePath; 
                data.matchHistoryProfiles.addAll(matchHistory); 

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Writer writer = new FileWriter(SAVE_FILE);
                gson.toJson(data, writer);
                writer.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void loadUserData() {
            try {
                File file = new File(SAVE_FILE);
                if (!file.exists()) return;

                Gson gson = new Gson();
                Reader reader = new FileReader(file);
                UserData data = gson.fromJson(reader, UserData.class);
                reader.close();

                if (data == null) return;

                displayName.set(data.displayName);
                username.set(data.username);
                bio.set(data.bio);
                ageRange.set(data.ageRange);
                gender.set(data.gender);
                accountEmail.set(data.email);
                emailVerified.set(data.emailVerified);
                meetupsCompleted.set(data.meetupsCompleted);
                userProfileImagePath = data.imagePath != null ? data.imagePath : "/resources/images/image_placeholder.jpg";

                if (userProfileImagePath != null && !userProfileImagePath.isEmpty()) {
                    InputStream is = getClass().getResourceAsStream(userProfileImagePath);
                    if (is != null) {
                        profileAvatar.set(new Image(is, 140, 140, true, true));
                    } else {
                        profileAvatar.set(new Image("/resources/images/image_placeholder.jpg", 140, 140, true, true));
                    }
                } else {
                    profileAvatar.set(new Image("/resources/images/image_placeholder.jpg", 140, 140, true, true));
                } 

                matchHistory.clear();
                if (data.matchHistoryProfiles != null) {
                    matchHistory.addAll(data.matchHistoryProfiles);
                }

                for (Profile p : matchHistory) {
                    if (p.imagePath == null || p.imagePath.isEmpty()) {
                        p.imagePath = "/resources/images/image_placeholder.jpg";
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
}
