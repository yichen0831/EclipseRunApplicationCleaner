package com.ychstudio;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

public class Main extends Application {

	final String META_PATH = ".metadata";
	final String DEBUG_CORE_PATH = ".plugins" + File.separator + "org.eclipse.debug.core";
	final String LAUNCHES_PATH = ".launches";
	
	String workSpaceFolder = ".";
	
	ListView<String> listView;
	
	ObservableList<String> fileList;
	Map<String, File> fileMap;
	
	Button deleteAllButton;
	
	class FileListCell extends ListCell<String> {

		@Override
		protected void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			if (item != null) {
				String projectAttr = "";
				String mainType = "";
				
				try {
					File launchFile = fileMap.get(item);
					if (launchFile == null) {
						return;
					}
					DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
					Document document = documentBuilder.parse(launchFile);
					
					document.getDocumentElement().normalize();
					NodeList nodeList = document.getDocumentElement().getChildNodes();
					for (int i = 0; i < nodeList.getLength(); i++) {
						if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
							Element element = (Element) nodeList.item(i);
							if (element.getAttribute("key").equals("org.eclipse.jdt.launching.MAIN_TYPE")) {
								mainType = element.getAttribute("value");
							}
							if (element.getAttribute("key").equals("org.eclipse.jdt.launching.PROJECT_ATTR")) {
								projectAttr = element.getAttribute("value");
							}
						}
					}
					
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				setText(item + "\n\t" + projectAttr + " - " + mainType);
			}
			else {
				setText("");
			}
		}
		
	}

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
				int index = listView.getSelectionModel().getSelectedIndex();
				
				Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure to delete \"" + fileName + "\"?", ButtonType.YES, ButtonType.NO);
				alert.setHeaderText("");
				Optional<ButtonType> answer = alert.showAndWait();
				
				if (answer.isPresent() && answer.get().equals(ButtonType.YES)) {
					File fileToDelete = fileMap.get(fileName);
					if (fileToDelete.delete()) {
						fileList.remove(index);
					}
					else {
						alert = new Alert(AlertType.ERROR, "Cannot delete " + fileName);
						alert.setHeaderText("");
						alert.showAndWait();
					}
				}

			}
		});
		deleteButton.setDisable(true);
		
		deleteAllButton = new Button("Delete All");
		deleteAllButton.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<Event>() {

			@Override
			public void handle(Event event) {
				Alert alert = new Alert(AlertType.CONFIRMATION, "Are you sure to delete all?", ButtonType.YES, ButtonType.NO);
				alert.setHeaderText("");
				Optional<ButtonType> answer = alert.showAndWait();
				
				if (answer.isPresent() && answer.get().equals(ButtonType.YES)) {
					for (int i = fileList.size() - 1; i >= 0; i--) {
						File fileToDelete = fileMap.get(fileList.get(i));
						if (!fileToDelete.delete()) {
							alert = new Alert(AlertType.ERROR, "Cannot delete " + fileList.get(i));
							alert.setHeaderText("");
							alert.showAndWait();
						}
						else {
							fileList.remove(i);
						}
					}
				}
			}
			
		});
		deleteAllButton.setDisable(true);
		
		HBox hBox = new HBox();
		HBox.setMargin(openButton, new Insets(6));
		HBox.setMargin(deleteButton, new Insets(6));
		HBox.setMargin(deleteAllButton, new Insets(6));
		hBox.getChildren().add(openButton);
		hBox.getChildren().add(deleteButton);
		hBox.getChildren().add(deleteAllButton);
		
		fileMap = new HashMap<>();
		fileList = FXCollections.observableArrayList();
		listView = new ListView<>();
		listView.setItems(fileList);
		
		listView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {

			@Override
			public ListCell<String> call(ListView<String> param) {
				return new FileListCell();
			}
			
		});
		
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
		
		// initialize the list if the program is in the WorkPlace folder 
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
		// initialize the list if the program is in the WorkPlace folder 
		
		File launchesDir = new File(joinFolder(workSpaceFolder, META_PATH, DEBUG_CORE_PATH), LAUNCHES_PATH);
		
		if (!launchesDir.isDirectory()) {
			// no launches folder
			return;
		}
		
		for (File file : launchesDir.listFiles()) {
			fileList.add(file.getName());
			fileMap.put(file.getName(), file);
		}
		
		if (!fileList.isEmpty()) {
			deleteAllButton.setDisable(false);
		}
		else {
			deleteAllButton.setDisable(true);
		}
	}
	
	private void getFileList() {
		if (!checkIsWorkSpaceFolder()) {
			return;
		}
		
		fileList.clear();
		fileMap.clear();
		
		File launchesDir = new File(joinFolder(workSpaceFolder, META_PATH, DEBUG_CORE_PATH), LAUNCHES_PATH);
		
		if (!launchesDir.isDirectory()) {
			// no launches folder
			return;
		}
		
		for (File file : launchesDir.listFiles()) {
			fileList.add(file.getName());
			fileMap.put(file.getName(), file);
		}
		
		if (!fileList.isEmpty()) {
			deleteAllButton.setDisable(false);
		}
		else {
			deleteAllButton.setDisable(true);
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
