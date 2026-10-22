package io.github.badrtechie.tcpchat.client.presentation;

import io.github.badrtechie.tcpchat.client.business.ChatClient;
import io.github.badrtechie.tcpchat.client.business.ClientEventListener;
import io.github.badrtechie.tcpchat.client.business.ClientSettings;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public final class ClientApplication extends Application implements ClientEventListener {
    private static volatile String[] startupArguments = new String[0];
    private final ChatClient client = new ChatClient(this);
    private TextField hostField;
    private TextField portField;
    private TextField usernameField;
    private TextArea history;
    private TextArea message;
    private Button connectButton;
    private Button sendButton;
    private Button leaveButton;
    private Button usersButton;
    private Label status;
    private Label mode;
    private Circle indicator;

    public static void setArguments(String[] args) {
        startupArguments = args.clone();
    }

    public static void launchApplication() {
        launch(ClientApplication.class);
    }

    @Override
    public void start(Stage stage) {
        buildControls();
        loadDefaults();
        GridPane root = buildLayout();
        Scene scene = new Scene(root, 820, 600);
        scene.getStylesheets().add(getClass().getResource("/client.css").toExternalForm());
        stage.setTitle("TCP Chat Client");
        stage.setScene(scene);
        stage.setMinWidth(700);
        stage.setMinHeight(500);
        stage.setOnCloseRequest(event -> client.disconnect());
        stage.show();
    }

    private void buildControls() {
        hostField = new TextField();
        portField = new TextField();
        usernameField = new TextField();
        usernameField.setPromptText("Optional — blank enables READ-ONLY MODE");
        history = new TextArea();
        history.setEditable(false);
        history.setWrapText(true);
        message = new TextArea();
        message.setPromptText("Type a message, allUsers, end, or bye");
        message.setWrapText(true);
        message.setPrefRowCount(3);
        connectButton = new Button("CONNECT");
        sendButton = new Button("SEND");
        leaveButton = new Button("LEAVE");
        usersButton = new Button("ACTIVE USERS");
        status = new Label("Offline");
        mode = new Label("");
        indicator = new Circle(7, Color.web("#b42335"));
        setConnectedControls(false, false);

        connectButton.setOnAction(event -> connect());
        sendButton.setOnAction(event -> send());
        leaveButton.setOnAction(event -> Thread.startVirtualThread(client::disconnect));
        usersButton.setOnAction(event -> runNetwork(client::requestActiveUsers));
        message.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                send();
            }
        });
    }

    private GridPane buildLayout() {
        GridPane root = new GridPane();
        root.getStyleClass().add("root-grid");
        root.setPadding(new Insets(18));
        root.setHgap(10);
        root.setVgap(10);
        root.add(new Label("Server"), 0, 0);
        root.add(hostField, 1, 0);
        root.add(new Label("Port"), 2, 0);
        root.add(portField, 3, 0);
        root.add(new Label("Username"), 0, 1);
        root.add(usernameField, 1, 1, 2, 1);
        root.add(connectButton, 3, 1);
        root.add(indicator, 0, 2);
        root.add(status, 1, 2);
        root.add(mode, 2, 2, 2, 1);
        root.add(history, 0, 3, 4, 1);
        root.add(message, 0, 4, 4, 1);
        root.add(sendButton, 0, 5);
        root.add(usersButton, 1, 5);
        root.add(leaveButton, 3, 5);
        GridPane.setHgrow(hostField, Priority.ALWAYS);
        GridPane.setHgrow(history, Priority.ALWAYS);
        GridPane.setVgrow(history, Priority.ALWAYS);
        return root;
    }

    private void loadDefaults() {
        try {
            ClientSettings settings = ClientSettings.resolve(ClientSettings.DEFAULT_PATH, startupArguments);
            hostField.setText(settings.host());
            portField.setText(Integer.toString(settings.port()));
        } catch (Exception ex) {
            append("Configuration error: " + ex.getMessage());
        }
    }

    private void connect() {
        String host = hostField.getText();
        String portText = portField.getText();
        String username = usernameField.getText();
        connectButton.setDisable(true);
        append("Connecting...");
        Thread.startVirtualThread(() -> {
            try {
                int port = Integer.parseInt(portText.trim());
                client.connect(new ClientSettings(host, port), username);
            } catch (NumberFormatException ex) {
                connectionFailed("Port must be a number from 1 to 65535.");
            } catch (IllegalArgumentException | IOException ex) {
                connectionFailed(ex.getMessage());
            }
        });
    }

    private void connectionFailed(String text) {
        Platform.runLater(() -> {
            appendNow("Connection failed: " + text);
            connectButton.setDisable(false);
            setConnectedControls(false, false);
        });
    }

    private void send() {
        String text = message.getText();
        runNetwork(() -> {
            client.sendMessage(text);
            Platform.runLater(message::clear);
        });
    }

    private void runNetwork(IoAction action) {
        Thread.startVirtualThread(() -> {
            try {
                action.run();
            } catch (IllegalArgumentException | IllegalStateException | IOException ex) {
                onError(ex.getMessage());
            }
        });
    }

    private void setConnectedControls(boolean connected, boolean readOnly) {
        hostField.setDisable(connected);
        portField.setDisable(connected);
        usernameField.setDisable(connected);
        connectButton.setDisable(connected);
        leaveButton.setDisable(!connected);
        usersButton.setDisable(!connected);
        sendButton.setDisable(!connected || readOnly);
        message.setDisable(!connected || readOnly);
        message.setEditable(connected && !readOnly);
    }

    private void append(String text) {
        Platform.runLater(() -> appendNow(text));
    }

    private void appendNow(String text) {
        history.appendText(text + System.lineSeparator());
    }

    @Override
    public void onConnected(String displayName, boolean readOnly) {
        Platform.runLater(() -> {
            status.setText("Online — " + displayName);
            status.getStyleClass().setAll("status-online");
            indicator.setFill(Color.web("#16863a"));
            mode.setText(readOnly ? "READ-ONLY MODE" : "Chat enabled");
            mode.getStyleClass().setAll(readOnly ? "read-only" : "chat-enabled");
            setConnectedControls(true, readOnly);
            appendNow("Connected as " + displayName + (readOnly ? " in READ-ONLY MODE." : "."));
        });
    }

    @Override
    public void onMessage(String formattedMessage) {
        append(formattedMessage);
    }

    @Override
    public void onActiveUsers(List<String> users) {
        append("Active users (" + users.size() + "): " + String.join(", ", users));
    }

    @Override
    public void onError(String error) {
        append("Error: " + error);
    }

    @Override
    public void onDisconnected(String reason) {
        Platform.runLater(() -> {
            status.setText("Offline");
            status.getStyleClass().setAll("status-offline");
            indicator.setFill(Color.web("#b42335"));
            mode.setText("");
            setConnectedControls(false, false);
            appendNow(reason);
        });
    }

    @Override
    public void stop() {
        client.disconnect();
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }
}
