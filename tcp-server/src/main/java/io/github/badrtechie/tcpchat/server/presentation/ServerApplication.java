package io.github.badrtechie.tcpchat.server.presentation;

import io.github.badrtechie.tcpchat.server.business.ChatServer;
import io.github.badrtechie.tcpchat.server.business.ServerEventListener;
import io.github.badrtechie.tcpchat.server.business.ServerSettings;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class ServerApplication extends Application implements ServerEventListener {
    private final ObservableList<ConnectedUser> users = FXCollections.observableArrayList();
    private final Map<String, Color> userColors = new ConcurrentHashMap<>();
    private final ChatServer server = new ChatServer(this);
    private TextField hostField;
    private TextField portField;
    private TextArea activityLog;
    private Label status;
    private Button startButton;
    private Button stopButton;

    public static void launchApplication() {
        launch(ServerApplication.class);
    }

    @Override
    public void start(Stage stage) {
        hostField = new TextField();
        portField = new TextField();
        activityLog = new TextArea();
        activityLog.setEditable(false);
        activityLog.setWrapText(true);
        ListView<ConnectedUser> userList = new ListView<>(users);
        userList.setCellFactory(ignored -> coloredCell());
        status = new Label("Stopped");
        status.getStyleClass().add("status-offline");
        startButton = new Button("START");
        stopButton = new Button("STOP");
        stopButton.setDisable(true);

        loadDefaults();
        startButton.setOnAction(event -> startServer());
        stopButton.setOnAction(event -> Thread.startVirtualThread(server::stop));

        GridPane root = new GridPane();
        root.getStyleClass().add("root-grid");
        root.setPadding(new Insets(18));
        root.setHgap(10);
        root.setVgap(10);
        root.add(new Label("Bind address"), 0, 0);
        root.add(hostField, 1, 0);
        root.add(new Label("Port"), 2, 0);
        root.add(portField, 3, 0);
        root.add(startButton, 4, 0);
        root.add(stopButton, 5, 0);
        root.add(status, 0, 1, 6, 1);
        root.add(new Label("Activity log"), 0, 2, 4, 1);
        root.add(new Label("Connected users"), 4, 2, 2, 1);
        root.add(activityLog, 0, 3, 4, 1);
        root.add(userList, 4, 3, 2, 1);
        GridPane.setHgrow(activityLog, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setVgrow(activityLog, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(userList, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setVgrow(userList, javafx.scene.layout.Priority.ALWAYS);

        Scene scene = new Scene(root, 900, 560);
        scene.getStylesheets().add(getClass().getResource("/server.css").toExternalForm());
        stage.setTitle("TCP Chat Server");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(460);
        stage.setOnCloseRequest(event -> server.stop());
        stage.show();
    }

    private void loadDefaults() {
        try {
            ServerSettings settings = ServerSettings.load(ServerSettings.DEFAULT_PATH);
            hostField.setText(settings.host());
            portField.setText(Integer.toString(settings.port()));
        } catch (Exception ex) {
            hostField.setText("");
            portField.setText("");
            append("Configuration error in " + ServerSettings.DEFAULT_PATH + ": " + ex.getMessage());
        }
    }

    private void startServer() {
        String host = hostField.getText();
        String portText = portField.getText();
        startButton.setDisable(true);
        Thread.startVirtualThread(() -> {
            try {
                server.start(new ServerSettings(host, Integer.parseInt(portText.trim())));
            } catch (NumberFormatException ex) {
                append("Start failed: port must be a number from 1 to 65535.");
                Platform.runLater(() -> startButton.setDisable(false));
            } catch (IllegalArgumentException | IOException ex) {
                append("Start failed: " + ex.getMessage());
                Platform.runLater(() -> startButton.setDisable(false));
            }
        });
    }

    private ListCell<ConnectedUser> coloredCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ConnectedUser user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(user.displayName());
                Color color = userColors.computeIfAbsent(user.displayName(), ignored ->
                        Color.hsb(ThreadLocalRandom.current().nextDouble(360), 0.42, 0.92));
                setStyle("-fx-background-color: " + toHex(color) + "; -fx-text-fill: #17202a;");
            }
        };
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255));
    }

    private void append(String text) {
        Platform.runLater(() -> activityLog.appendText(text + System.lineSeparator()));
    }

    @Override
    public void onActivity(String text) {
        append(text);
    }

    @Override
    public void onUserConnected(ConnectedUser user) {
        Platform.runLater(() -> users.add(user));
    }

    @Override
    public void onUserDisconnected(ConnectedUser user) {
        Platform.runLater(() -> {
            users.remove(user);
            userColors.remove(user.displayName());
        });
    }

    @Override
    public void onRunningChanged(boolean running, String endpoint) {
        Platform.runLater(() -> {
            status.setText(running ? "Running on " + endpoint : "Stopped");
            status.getStyleClass().setAll(running ? "status-online" : "status-offline");
            startButton.setDisable(running);
            stopButton.setDisable(!running);
            hostField.setDisable(running);
            portField.setDisable(running);
        });
    }

    @Override
    public void stop() {
        server.stop();
    }
}
