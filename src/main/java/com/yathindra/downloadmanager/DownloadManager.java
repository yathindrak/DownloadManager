package com.yathindra.downloadmanager;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DownloadManager extends Application {

    DownloadPool downloadPool = new DownloadPool();
    Stage window;
    TableView<DownloadThread> table;

    Label urlLabel = new Label("URL");
    TextField urlInput = new TextField();

    Button newDownload = new Button("Download");
    Button pauseButton = new Button("Pause");
    Button resumeButton = new Button("Resume");
    Button stopButton = new Button("Stop");
    Button removeButton = new Button("Remove");

    Alert alert = new Alert(Alert.AlertType.INFORMATION);

    public static void main(String args[]) {
        launch(args);
    }

    @Override
    public void stop() {
        downloadPool.stopAll();
        downloadPool.joinThreads();
        downloadPool.save();
    }

    public void setTable() {
        TableColumn<DownloadThread, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setMinWidth(50);
        idColumn.setCellValueFactory((TableColumn.CellDataFeatures<DownloadThread, Integer> download) -> download.getValue().getDownloadMetadata().getDownloadIDProperty());

        TableColumn<DownloadThread, String> urlColumn = new TableColumn<>("URL");
        urlColumn.setMinWidth(200);
        urlColumn.setCellValueFactory((TableColumn.CellDataFeatures<DownloadThread, String> download) -> download.getValue().getDownloadMetadata().getUrlProperty());

        TableColumn<DownloadThread, String> filenameColumn = new TableColumn<>("Filename");
        filenameColumn.setMinWidth(150);
        filenameColumn.setCellValueFactory((TableColumn.CellDataFeatures<DownloadThread, String> download) -> download.getValue().getDownloadMetadata().getFilenameProperty());

        TableColumn<DownloadThread, String> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setMinWidth(100);
        sizeColumn.setCellValueFactory((TableColumn.CellDataFeatures<DownloadThread, String> download) -> download.getValue().getDownloadMetadata().getSizeProperty());

        TableColumn<DownloadThread, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setMinWidth(200);
        statusColumn.setCellValueFactory((TableColumn.CellDataFeatures<DownloadThread, String> download) -> download.getValue().getDownloadMetadata().getStatusProperty());

        TableColumn<DownloadThread, String> acceleratedColumn = new TableColumn<>("Accelerated");
        acceleratedColumn.setMinWidth(50);
        acceleratedColumn.setCellValueFactory((TableColumn.CellDataFeatures<DownloadThread, String> download) -> download.getValue().getDownloadMetadata().getAcceleratedProperty());
        table = new TableView();

        table.setItems(downloadPool.getDownloadThreads());
        table.getColumns().addAll(idColumn, urlColumn, filenameColumn, sizeColumn, statusColumn, acceleratedColumn);

    }

    public void setButtons() {
        newDownload.setOnAction(eh -> {
            downloadPool.newDownload(urlInput.getText());
            urlInput.clear();
        });

        pauseButton.setOnAction(eh -> downloadPool.pauseDownload(table.getSelectionModel().getSelectedItem()));

        resumeButton.setOnAction(eh -> downloadPool.resumeDownload(table.getSelectionModel().getSelectedItem()));

        stopButton.setOnAction(eh -> downloadPool.stopDownload(table.getSelectionModel().getSelectedItem()));

        removeButton.setOnAction(eh -> {
            Object obj = table.getSelectionModel().getSelectedItem();
            if(obj == null) {
                alert.setContentText("Please select an item before selecting the action");
                alert.show();
            } else {
                downloadPool.removeDownload(table.getSelectionModel().getSelectedItem());
            }
        });

    }

    @Override
    public void start(Stage stage) {
        window = stage;
        window.setTitle("Download Manager");
        setTable();
        setButtons();
        urlInput.setMinWidth(400);

        HBox hBox = new HBox();
        hBox.setPadding(new Insets(25));
        hBox.getChildren().addAll(urlLabel, urlInput, newDownload);
        hBox.setSpacing(10);

        HBox buttonList = new HBox();
        buttonList.setPadding(new Insets(0, 25, 25, 25));
        buttonList.getChildren().addAll(pauseButton, stopButton, resumeButton, removeButton);
        buttonList.setSpacing(10);

        VBox vBox = new VBox();
        vBox.getChildren().addAll(hBox, buttonList, table);
        Scene scene = new Scene(vBox);
        scene.getStylesheets().add("styles.css");
        window.setScene(scene);
        window.show();
    }

}
