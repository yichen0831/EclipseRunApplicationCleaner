package com.ychstudio;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class Main extends Application {

	final String META_PATH = ".metadata";
	final String DEBUG_CORE_PATH = ".plugins" + File.separator + "org.eclipse.debug.core";
	final String LAUNCHES_PATH = ".launches";
	
	String workSpaceFolder = ".";
	
	ListView<String> listView;
	
	ObservableList<String> fileList;
	Map<String, File> fileMap;

	@Override
	public void start(Stage primaryStage) throws Exception {
		Button openButton = new Button("Open WorkSpace folder");
		openButton.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<Event>() {

			@Override
			public void handle(Event event) {
				DirectoryChooser directoryChooser = new DirectoryChooser();
				directoryChooser.setInitialDirectory(new File("."));
				File file = directoryChooser.showDialog(primaryStage);
				if (file != null) {
					workSpaceFolder = file.getAbsolutePath();
					getFileList();
				}
			}
		});
		
		Button deleteButton = new Button("Delete");
		deleteButton.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<Event>() {

			@Override
			public void handle(Event event) {
				String fileName = listView.getSelectionModel().getSelectedItem();
				
				Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure to delete \"" + fileName + "\"?", ButtonType.YES, ButtonType.NO);
				alert.setHeaderText("");
				Optional<ButtonType> answer = alert.showAndWait();
				
				if (answer.get().equals(ButtonType.YES)) {
					File fileToDelete = fileMap.get(fileName);
					if (fileToDelete.delete()) {
						fileList.remove(fileName);
					}
				}

			}
		});
		deleteButton.setDisable(true);
		
		HBox hBox = new HBox();
		HBox.setMargin(openButton, new Insets(6d));
		HBox.setMargin(deleteButton, new Insets(6d));
		hBox.getChildren().add(openButton);
		hBox.getChildren().add(deleteButton);
		
		fileMap = new HashMap<>();
		fileList = FXCollections.observableArrayList();
		listView = new ListView<>();
		listView.setItems(fileList);
		
		listView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (newValue != null && !newValue.isEmpty()) {
					deleteButton.setDisable(false);
				}
				else {
					deleteButton.setDisable(true);
				}
			}
		});
		
		initFileList();
		
		VBox vBox = new VBox(6d);
		vBox.setPadding(new Insets(6d));
		vBox.getChildren().add(hBox);
		vBox.getChildren().add(listView);
		
		Scene mainScene = new Scene(vBox);
		
		primaryStage.setScene(mainScene);
		primaryStage.setTitle("EclipseRunApplicationCleaner");
		primaryStage.show();
	}
	
	private String joinFolder(String... folders) {
		StringBuilder stringBuilder = new StringBuilder();
		
		for (String file : folders) {
			stringBuilder.append(file);
			stringBuilder.append(File.separator);
		}
		
		return stringBuilder.toString();
	}
	
	private void initFileList() {
		File launchesDir = new File(joinFolder(workSpaceFolder, META_PATH, DEBUG_CORE_PATH), LAUNCHES_PATH);
		
		if (!launchesDir.isDirectory()) {
			// no launches folder
			return;
		}
		
		for (File file : launchesDir.listFiles()) {
			fileList.add(file.getName());
			fileMap.put(file.getName(), file);
		}
	}
	
	private void getFileList() {
		if (!checkIsWorkSpaceFolder()) {
			return;
		}
		
		File launchesDir = new File(joinFolder(workSpaceFolder, META_PATH, DEBUG_CORE_PATH), LAUNCHES_PATH);
		
		if (!launchesDir.isDirectory()) {
			// no launches folder
			return;
		}
		
		for (File file : launchesDir.listFiles()) {
			fileList.add(file.getName());
			fileMap.put(file.getName(), file);
		}
		
	}
	
	private boolean checkIsWorkSpaceFolder() {
		File metaFile = new File(workSpaceFolder, META_PATH);
		if (!metaFile.isDirectory()) {
			Alert alert = new Alert(AlertType.ERROR, workSpaceFolder + " is not a WorkSpace folder", ButtonType.OK);
			alert.setHeaderText("");
			alert.showAndWait();
			return false;
		}
		
		return true;
	}

	public static void main(String[] args) {
		launch(args);
	}
}
