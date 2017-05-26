package userInterface;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import charts.ChartGUIController;
import core.ConvertToTime;
import core.Exporter;
import core.Importer;
import core.LogObject;
import core.SQL;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import tools.PathUtils;


public class UserInterfaceController
{
	@FXML private Label aktuellesProjektAusgabe;
	@FXML private Label aktuellerTaskAusgabe;
	@FXML public Label labelTime;
	@FXML private Accordion accordion;
	@FXML private TitledPane Projekte;
	@FXML private TitledPane gesamtesLog;
	@FXML private AnchorPane MainFrame;
	@FXML private ToggleButton startButton;
	@FXML private TableView<LogObject> table;
	@FXML private ScrollPane scrollPane;

	private Stage stage;
	private core.Counter stoppUhr;
	public boolean stoppUhrLäuftFlag;
	public boolean projektExistiertFlag;
	private ArrayList<TreeItem<HBox>> aktuelleTasks;
	private TreeItem<HBox> item;
	private LogObject log;
	private long startTimestamp;
	private long endTimestamp;
	private int longestProject;
	private ArrayList<LogObject> logObjects = new ArrayList<LogObject>();	
	private final String savePath = PathUtils.getOSindependentPath() + "/Deadlocker/SmartTime/save.db";
	private SQL sql;
	private Stage waitingStage = new Stage();
	private Image icon;
	private final ResourceBundle bundle = ResourceBundle.getBundle("userInterface/", Locale.GERMANY);

	public void init(Stage stage)
	{		
	    this.stage = stage;
	    
		PathUtils.checkFolder(new File(new File(savePath).getParent()));
		icon = new Image("/userInterface/icon.png");

		accordion.setExpandedPane(gesamtesLog);

		projektExistiertFlag = false;
		stoppUhrLäuftFlag = false;

		labelTime.setText("0 h  0  min  0 sek");		

		loadAll();

		// verwaltet den Start/Stopp-Button
		startButton.setOnAction(event -> {

			if(projektExistiertFlag == true)

				if(startButton.isSelected())
				{
					stoppUhrLäuftFlag = true;

					labelTime.setText("");
					core.Counter.ausgabe = 0;

					startButton.setText("Stopp");

					stoppUhr = new core.Counter();
					core.Counter.running = true;
					core.Counter.uic = this;
					startClock();

					stoppUhr.start();
				}
				else
				{
					stoppUhrLäuftFlag = false;
					startButton.setText("Start");

					stoppUhr.interrupt();

					endClock();

					loadAll();
				}
			else
			{
				Alert alert = new Alert(AlertType.WARNING);
				alert.setTitle("Warnung");
				alert.setHeaderText("");
				alert.setContentText("Kein Projekt ausgewählt!");
				alert.initOwner(stage);
				Stage dialogStage = (Stage)alert.getDialogPane().getScene().getWindow();
				dialogStage.getIcons().add(icon);
				alert.showAndWait();

				startButton.setSelected(false);
			}
		});
	}

	/**
	 * Fängt die Aufforderung das Fenster zu schließen ab, um vorher noch eine
	 * Prüfung duchzuführen
	 */
	public void closeRequest()
	{
		// Prüft, ob die Stoppuhr noch läuft
		if(stoppUhrLäuftFlag == true)
		{
			Alert alert = new Alert(AlertType.WARNING);
			alert.setTitle("Warnung");
			alert.setHeaderText("");
			alert.setContentText("Stoppuhr läuft noch!");
			alert.initOwner(stage);
			Stage dialogStage = (Stage)alert.getDialogPane().getScene().getWindow();
			dialogStage.getIcons().add(icon);
			alert.showAndWait();
		}
		else
		{
			stage.close();
		}
	}

	public void setLabels(String project, String task)
	{
		aktuellesProjektAusgabe.setText(project);
		aktuellerTaskAusgabe.setText(task);
	}

	public void openProjectGUI(ActionEvent e)
	{
		if( ! stoppUhrLäuftFlag)
		{
			try
			{
				FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("projektFenster.fxml"));
				Parent root = (Parent)fxmlLoader.load();
				Stage newStage = new Stage();
				newStage.setScene(new Scene(root, 455, 300));
				newStage.setTitle("Neues Projekt");
				newStage.initOwner(stage);

				newStage.getIcons().add(icon);

				ProjektFensterController pfc = (ProjektFensterController)fxmlLoader.getController();
				pfc.init(this, stage, savePath, icon);

				newStage.setResizable(false);
				newStage.initModality(Modality.APPLICATION_MODAL);
				newStage.showAndWait();
			}
			catch(IOException d)
			{
				d.printStackTrace();
			}
		}
		else
		{
			Alert alert = new Alert(AlertType.WARNING);
			alert.setTitle("Warnung");
			alert.setHeaderText("Stoppuhr läuft noch!");
			alert.setContentText("Projekt und Task können nur geändert werden,\nwenn die Stoppuhr nicht läuft.");
			alert.initOwner(stage);
			Stage dialogStage = (Stage)alert.getDialogPane().getScene().getWindow();
			dialogStage.getIcons().add(icon);
			alert.showAndWait();
		}
	}

	private void createTreeView()
	{
		// generiert den root-Knoten
		Label labelRoot = new Label("Gesamt");
		labelRoot.setPrefWidth((longestProject * 6) + 150);
		Label labelRootTime = new Label(completeTime(logObjects));
		labelRootTime.setPrefWidth(150);

		HBox boxRoot = new HBox();
		boxRoot.getChildren().add(labelRoot);
		boxRoot.getChildren().add(labelRootTime);

		TreeItem<HBox> gesamt = new TreeItem<HBox>(boxRoot);
		gesamt.setExpanded(true);

		try
		{
			ArrayList<String> projectNames = sql.getProjectNames();
			Collections.sort(projectNames);

			ArrayList<TreeItem<HBox>> alleTasks = new ArrayList<>();

			for(String projectName : projectNames)
			{
				Label labelProjekt = new Label(projectName);
				labelProjekt.setPrefWidth((longestProject * 6) + 150);
				Label labelProjektTime = new Label(completeTime(sql.getByProject(projectName)));
				labelProjektTime.setPrefWidth(200);

				HBox box = new HBox();
				box.getChildren().add(labelProjekt);
				box.getChildren().add(labelProjektTime);

				item = new TreeItem<HBox>(box);

				aktuelleTasks = new ArrayList<TreeItem<HBox>>();

				ArrayList<String> taskNames = sql.getTaskNamesByProject(projectName);
				Collections.sort(taskNames);
				for(String taskName : taskNames)
				{
					Label labelTask = new Label(taskName);
					labelTask.setPrefWidth((longestProject * 6) + 150);
					Label labelTaskTime = new Label(completeTime(sql.getByProjectAndTask(projectName, taskName)));
					labelTaskTime.setPrefWidth(200);

					HBox box2 = new HBox();
					box2.getChildren().add(labelTask);
					box2.getChildren().add(labelTaskTime);

					// und ein neus TreeItem erzeugt, was später Kind des
					// übergeordneten Knoten mit dem Projektnamen sein wird
					aktuelleTasks.add(new TreeItem<HBox>(box2));
				}

				// fügt alle TreeItems der Ansicht hinzu
				item.getChildren().setAll(aktuelleTasks);
				alleTasks.add(item);
			}

			gesamt.getChildren().setAll(alleTasks);
			TreeView<HBox> tree = new TreeView<HBox>(gesamt);
			Projekte.setContent(tree);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Fehler");
			alert.setHeaderText("");
			alert.setContentText("Beim Laden der Daten ist ein Fehler aufgetreten.");
			alert.initOwner(stage);
			Stage dialogStage = (Stage)alert.getDialogPane().getScene().getWindow();
			dialogStage.getIcons().add(icon);
			alert.showAndWait();
		}
	}

	private void loadFromDB()
	{
		sql = new SQL(savePath);
		try
		{
			logObjects = sql.getLogObjects();

			longestProject = 0;

			for(LogObject current : logObjects)
			{
				int length = current.getProject().length();
				if(length > longestProject)
				{
					longestProject = length;
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			try
			{
				sql.createDB();
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Fehler");
				alert.setHeaderText("");
				alert.setContentText("Fehler beim Erstellen der Datenbank.");
				alert.initOwner(stage);
				Stage dialogStage = (Stage)alert.getDialogPane().getScene().getWindow();
				dialogStage.getIcons().add(icon);
				alert.showAndWait();
			}
		}
	}

	private void createLogView()
	{		
		table.getItems().clear();
		table.getColumns().clear();	

		TableColumn<LogObject, String> dates = new TableColumn<>("Datum");
		dates.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LogObject, String>, ObservableValue<String>>()
		{
			@Override
			public ObservableValue<String> call(CellDataFeatures<LogObject, String> param)
			{
				StringProperty value = new SimpleStringProperty();
				value.set(param.getValue().getDate());
				return value;
			}
		});
		dates.setStyle("-fx-alignment: CENTER;");
		dates.setComparator(new Comparator<String>()
		{
			@Override
			public int compare(String input1, String input2)
			{
				// 0 --> input1 == input2
				// 1 --> input1 > input2
				// -1 --> input1 < input2

				if(input1.equals(input2))
				{
					return 0;
				}
				else
				{
					String[] date1 = input1.split(Pattern.quote("."));
					String[] date2 = input2.split(Pattern.quote("."));

					String newDate1 = date1[2] + "." + date1[1] + "." + date1[0];
					String newDate2 = date2[2] + "." + date2[1] + "." + date2[0];

					return newDate1.compareTo(newDate2);
				}
			}
		});

		TableColumn<LogObject, String> startTimes = new TableColumn<>("Startzeit");
		startTimes.setCellValueFactory(new PropertyValueFactory<LogObject, String>("startTime"));
		startTimes.setStyle("-fx-alignment: CENTER;");

		TableColumn<LogObject, String> endTimes = new TableColumn<>("Endzeit");
		endTimes.setCellValueFactory(new PropertyValueFactory<LogObject, String>("endTime"));
		endTimes.setStyle("-fx-alignment: CENTER;");

		TableColumn<LogObject, String> projects = new TableColumn<>("Projekt");
		projects.setCellValueFactory(new PropertyValueFactory<LogObject, String>("project"));
		projects.setStyle("-fx-alignment: CENTER;");

		TableColumn<LogObject, String> tasks = new TableColumn<>("Task");
		tasks.setCellValueFactory(new PropertyValueFactory<LogObject, String>("task"));
		tasks.setStyle("-fx-alignment: CENTER;");

		TableColumn<LogObject, String> durations = new TableColumn<>("Dauer");
		durations.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LogObject, String>, ObservableValue<String>>()
		{
			@Override
			public ObservableValue<String> call(CellDataFeatures<LogObject, String> param)
			{
				StringProperty value = new SimpleStringProperty();
				value.set(ConvertToTime.ConvertMillisToTime(param.getValue().getDuration()));
				return value;
			}
		});
		durations.setStyle("-fx-alignment: CENTER;");

		table.getColumns().add(dates);
		table.getColumns().add(startTimes);
		table.getColumns().add(endTimes);
		table.getColumns().add(projects);
		table.getColumns().add(tasks);
		table.getColumns().add(durations);

		ObservableList<LogObject> objectsForTable = FXCollections.observableArrayList(logObjects);
		table.setItems(objectsForTable);
		table.setFixedCellSize(26);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

		table.setOnMousePressed(new EventHandler<MouseEvent>()
		{
			@SuppressWarnings("unchecked")
			@Override
			public void handle(MouseEvent event)
			{
				if(event.isPrimaryButtonDown() && event.getClickCount() == 2)
				{
					Node node = ((Node)event.getTarget()).getParent();
					TableRow<LogObject> row;
					if(node instanceof TableRow)
					{
						row = (TableRow<LogObject>)node;
					}
					else
					{
						// clicking on text part
						row = (TableRow<LogObject>)node.getParent();
					}				
					
					editEntry(row.getItem());
				}
			}
		});
		
		table.prefWidthProperty().bind(scrollPane.widthProperty().subtract(2));
		table.prefHeightProperty().bind(scrollPane.heightProperty().subtract(2));
	}

	@FXML
	private void charts()
	{
		try
		{
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/charts/chartGUI.fxml"));
			Parent root = (Parent)fxmlLoader.load();
			Scene scene = new Scene(root, 800, 600);
			scene.getStylesheets().add("charts/Chart.css");
			Stage newStage = new Stage();
			newStage.setScene(scene);
			newStage.setTitle("Diagramme");
			ChartGUIController controller = (ChartGUIController)fxmlLoader.getController();
			controller.init(savePath, stage, icon);
			newStage.getIcons().add(icon);
			newStage.initOwner(stage);

			newStage.setResizable(true);
			newStage.setMinWidth(800);
			newStage.setMinHeight(600);
			newStage.initModality(Modality.APPLICATION_MODAL);
			newStage.showAndWait();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public void loadAll()
	{
		loadFromDB();
		createLogView();
		createTreeView();	
	}

	public void newProject(String project, String task)
	{
		log = new LogObject();
		log.setProject(project);
		log.setTask(task);
	}

	private void startClock()
	{
		stoppUhrLäuftFlag = true;
		log.createStartTime();
		startTimestamp = System.currentTimeMillis();
	}

	private void endClock()
	{
		stoppUhrLäuftFlag = false;
		log.createEndTime();
		endTimestamp = System.currentTimeMillis();
		log.setDuration(endTimestamp - startTimestamp);

		SQL sql = new SQL(savePath);
		try
		{
			sql.insert(log);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Fehler");
			alert.setHeaderText("");
			alert.setContentText("Fehler beim Speichern des Eintrags.");
			alert.initOwner(stage);
			Stage dialogStage = (Stage)alert.getDialogPane().getScene().getWindow();
			dialogStage.getIcons().add(icon);
			alert.showAndWait();
		}

		loadAll();
	}

	private String completeTime(ArrayList<LogObject> list)
	{
		long total = 0;
		for(LogObject current : list)
		{
			total += current.getDuration();
		}

		return ConvertToTime.ConvertMillisToTime(total);
	}

	public void insertTime()
	{
		try
		{
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/userInterface/InsertTimeGUI.fxml"));
			Parent root = (Parent)fxmlLoader.load();
			Scene scene = new Scene(root, 540, 400);
			Stage newStage = new Stage();
			newStage.setScene(scene);
			newStage.setTitle("Zeit nachträglich einfügen");

			InsertTimeController controller = (InsertTimeController)fxmlLoader.getController();
			controller.init(stage, this, savePath, icon);
			newStage.getIcons().add(icon);
			newStage.initOwner(stage);

			newStage.setResizable(false);
			newStage.initModality(Modality.APPLICATION_MODAL);
			newStage.showAndWait();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public void importFromSmartTime()
	{
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Import von SmartTime bis v4.5.0");
		fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("SmartTime-Logdateien", "*.log"));
		File file = fileChooser.showOpenDialog(stage);
		if(file != null)
		{
			Thread importThread = new Thread()
			{
				public void run()
				{
					Platform.runLater(() -> {
						showWaitingDialog("Importiere...", "Bitte warten...");
					});
					Importer importer = new Importer(savePath, stage, icon);
					importer.importFromSmartTime(file);
					Platform.runLater(() -> {
						closeWaitingDialog();
						loadAll();
					});
				}
			};
			importThread.start();
		}
	}

	public void importFromDB()
	{
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Import von SmartTime Datenbank");
		fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("SmartTime Datenbank", "*.db"));
		File file = fileChooser.showOpenDialog(stage);
		if(file != null)
		{
			Thread importThread = new Thread()
			{
				public void run()
				{
					Platform.runLater(() -> {
						showWaitingDialog("Importiere...", "Bitte warten...");
					});
					Importer importer = new Importer(savePath, stage, icon);
					importer.importFromDB(file);
					Platform.runLater(() -> {
						closeWaitingDialog();
						loadAll();
					});
				}
			};
			importThread.start();
		}
	}

	public void importFromJSON()
	{
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Import von JSON");
		fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("JSON", "*.json"));
		File file = fileChooser.showOpenDialog(stage);
		if(file != null)
		{
			Thread importThread = new Thread()
			{
				public void run()
				{
					Platform.runLater(() -> {
						showWaitingDialog("Importiere...", "Bitte warten...");
					});
					Importer importer = new Importer(savePath, stage, icon);
					importer.importFromJSON(file);
					Platform.runLater(() -> {
						closeWaitingDialog();
						loadAll();
					});
				}
			};
			importThread.start();
		}
	}

	public void exportAsDB()
	{
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Export als SmartTime Datenbank");
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("SmartTime-Datenbank", "*.db");
		fileChooser.getExtensionFilters().add(extFilter);
		File file = fileChooser.showSaveDialog(stage);
		if(file != null)
		{
			try
			{
				Files.copy(new File(savePath).toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			catch(IOException e)
			{
				e.printStackTrace();
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Fehler");
				alert.setHeaderText("");
				alert.setContentText("Beim Exportieren der Daten ist ein Fehler aufgetreten.");
				alert.initOwner(stage);
				Stage dialogStage = (Stage)alert.getDialogPane().getScene().getWindow();
				dialogStage.getIcons().add(icon);
				alert.showAndWait();
			}
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Erfolgreich exportiert");
			alert.setHeaderText("");
			alert.setContentText("Export erfolgreich abgeschlossen.");
			alert.initOwner(stage);
			Stage dialogStage = (Stage)alert.getDialogPane().getScene().getWindow();
			dialogStage.getIcons().add(icon);
			alert.showAndWait();
		}
	}

	public void exportAsJSON()
	{
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Export als JSON");
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JSON", "*.json");
		fileChooser.getExtensionFilters().add(extFilter);
		File file = fileChooser.showSaveDialog(stage);
		if(file != null)
		{
			Thread exportThread = new Thread()
			{
				public void run()
				{
					Platform.runLater(() -> {
						showWaitingDialog("Exportiere...", "Bitte warten...");
					});
					Exporter exporter = new Exporter(savePath, stage, icon);
					exporter.exportAsJSON(file);
					Platform.runLater(() -> {
						closeWaitingDialog();
					});
				}
			};
			exportThread.start();
		}
	}

	public void showWaitingDialog(String title, String text)
	{
		HBox hboxWaiting = new HBox();
		ProgressIndicator indicator = new ProgressIndicator();
		indicator.setPrefWidth(40.0);
		indicator.setPrefHeight(40.0);
		Label labelWait = new Label(text);
		labelWait.setStyle("-fx-font-size: 20;");
		hboxWaiting.getChildren().add(indicator);
		hboxWaiting.getChildren().add(labelWait);
		hboxWaiting.setAlignment(Pos.CENTER);
		HBox.setMargin(labelWait, new Insets(0.0, 0.0, 0.0, 30.0));
		hboxWaiting.setPadding(new Insets(20.0));
		waitingStage = new Stage();
		waitingStage.setTitle(title);
		waitingStage.setScene(new Scene(hboxWaiting, 250, 75));
		waitingStage.getIcons().add(icon);
		waitingStage.initOwner(stage);
		waitingStage.setResizable(false);
		waitingStage.initModality(Modality.APPLICATION_MODAL);
		waitingStage.setOnCloseRequest(new EventHandler<WindowEvent>()
		{
			@Override
			public void handle(WindowEvent event)
			{
				event.consume();
			}
		});
		waitingStage.show();
	}

	public void closeWaitingDialog()
	{
		if(waitingStage.isShowing())
		{
			waitingStage.close();
		}
	}
	
	private void editEntry(LogObject object)
	{
		try
		{
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("editGUI.fxml"));
			Parent root = (Parent)fxmlLoader.load();
			Stage newStage = new Stage();
			newStage.setScene(new Scene(root, 455, 280));
			newStage.setTitle("Eintrag bearbeiten");
			newStage.getIcons().add(icon);
			newStage.initOwner(stage);

			EditController pfc = (EditController)fxmlLoader.getController();			
			pfc.init(this, stage, savePath, icon, object);

			newStage.setResizable(false);
			newStage.initModality(Modality.APPLICATION_MODAL);
			newStage.showAndWait();			
		}
		catch(IOException d)
		{
			d.printStackTrace();
		}
	}
	
	public void updateEntry(LogObject oldLog, LogObject newLog)
	{
		try
		{
			sql.update(oldLog, newLog);
			loadAll();
		}
		catch(Exception e)
		{
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Fehler");
			alert.setHeaderText("");
			alert.setContentText("Beim Aktualisieren des Eintrags ist ein Fehler aufgetreten.");
			alert.initOwner(stage);
			Stage dialogStage = (Stage)alert.getDialogPane().getScene().getWindow();
			dialogStage.getIcons().add(icon);
			alert.showAndWait();
			e.printStackTrace();
		}
	}

	public void deleteEntry(LogObject object)
	{
		try
		{
			sql.delete(object);
			loadAll();
		}
		catch(Exception e)
		{
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Fehler");
			alert.setHeaderText("");
			alert.setContentText("Beim Löschen des Eintrags ist ein Fehler aufgetreten.");
			alert.initOwner(stage);
			Stage dialogStage = (Stage)alert.getDialogPane().getScene().getWindow();
			dialogStage.getIcons().add(icon);
			alert.showAndWait();
			e.printStackTrace();
		}
	}
	
	public void deleteDB()
	{
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Löschen");
		alert.setHeaderText("");
		alert.setContentText("Möchten Sie die gesamte Datenbank wirklich unwiederruflich löschen?");
		Stage dialogStage = (Stage)alert.getDialogPane().getScene().getWindow();
		dialogStage.getIcons().add(icon);
		
		Optional<ButtonType> result = alert.showAndWait();
		if(result.get() == ButtonType.OK)
		{
			try
			{
				sql.deleteDB();
				sql.createDB();
				loadAll();
			}
			catch(Exception e)
			{
				Alert alert2 = new Alert(AlertType.ERROR);
				alert2.setTitle("Fehler");
				alert2.setHeaderText("");
				alert2.setContentText("Beim Löschen der Datenbank ist ein Fehler aufgetreten.");
				alert2.initOwner(stage);
				Stage dialogStage2 = (Stage)alert.getDialogPane().getScene().getWindow();
				dialogStage2.getIcons().add(icon);
				alert2.showAndWait();
				e.printStackTrace();
			}			
		}		
	}

	public void about()
	{
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Über " + bundle.getString("app.name"));
		alert.setHeaderText(bundle.getString("app.name"));
		alert.setContentText("Version:     " + bundle.getString("version.name") + "\r\nDatum:      " + bundle.getString("version.date") + "\r\nAutor:        Robert Goldmann\r\n");
		Stage dialogStage = (Stage)alert.getDialogPane().getScene().getWindow();
		dialogStage.getIcons().add(icon);
		alert.showAndWait();
	}	
}