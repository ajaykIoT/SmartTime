package userInterface;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import core.LogObject;
import core.SQL;
import core.Settings;
import core.Utils;
import fontAwesome.FontIcon;
import fontAwesome.FontIconType;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import logger.Logger;
import tools.AlertGenerator;
import tools.ConvertTo;

public class InsertTimeController 
{
	@FXML private DatePicker datePicker1;
	@FXML private DatePicker datePicker2;
	@FXML private Parent timePicker1;
	@FXML private TimePickerController timePicker1Controller;	
	@FXML private Parent timePicker2;
	@FXML private TimePickerController timePicker2Controller;	
	@FXML private Button buttonUseCurrentTime;
	@FXML private Button buttonAdd;
	@FXML private Button buttonCancel;
	@FXML private Label labelDuration;	
	@FXML private ComboBox<String> comboBoxProject;
	@FXML private ComboBox<String> comboBoxTask;
	
	private Stage stage;
	private UserInterfaceController controller;
	private String savePath;
	private Image icon;
	
	public void init(Stage stage, UserInterfaceController controller, Settings settings, Image icon)
	{	
		this.savePath = settings.getSavePath() + "/" + Utils.DATABASE_NAME;
		this.stage = stage;
		this.controller = controller;		
		this.icon = icon;
		
		buttonUseCurrentTime.setGraphic(new FontIcon(FontIconType.CLOCK_ALT, 14, Color.BLACK));
		
		ArrayList<String> objects = new ArrayList<String>();
		
		SQL sql = new SQL(savePath);
		try
		{					
			objects = sql.getProjectNames();	
			Collections.sort(objects);
		}
		catch(Exception e)
		{								
		}	
		
		comboBoxProject.getItems().addAll(objects);			
		comboBoxProject.setStyle("-fx-font-family: \"Arial\";-fx-font-size: 15px;");		
		comboBoxTask.setStyle("-fx-font-family: \"Arial\";-fx-font-size: 15px;");		
		
		comboBoxProject.valueProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
			{
				comboBoxTask.getItems().clear();
				
				if(newValue != null && !newValue.equals(""))
				{
					try
					{
						ArrayList<String> tasks = sql.getTaskNamesByProject(newValue);
						Collections.sort(tasks);
						comboBoxTask.getItems().addAll(tasks);						
					}
					catch(Exception e)
					{							
					}
				}
			}
		});	
		
		timePicker1Controller.setController(this);
		timePicker2Controller.setController(this);		
		
		datePicker1.setValue(LocalDate.now());
		datePicker2.setValue(LocalDate.now());		
		
		final Callback<DatePicker, DateCell> dayCellFactory = new Callback<DatePicker, DateCell>()
		{
			@Override
			public DateCell call(final DatePicker datePicker)
			{
				return new DateCell()
				{
					@Override
					public void updateItem(LocalDate item, boolean empty)
					{
						super.updateItem(item, empty);

						if(item.isBefore(datePicker1.getValue()))
						{
							setDisable(true);
							setStyle("-fx-background-color: #ffc0cb;");
						}
					}
				};
			}
		};
		datePicker2.setDayCellFactory(dayCellFactory);
		
		datePicker1.valueProperty().addListener(new ChangeListener<LocalDate>()
		{
			@Override
			public void changed(ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue)
			{
				if(isEndDateAfterStartDate())
				{
					setLabelDuration();
				}
				else
				{
					labelDuration.setText("Endzeit liegt vor Startzeit");
				}			
			}
		});
		
		datePicker2.valueProperty().addListener(new ChangeListener<LocalDate>()
		{
			@Override
			public void changed(ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue)
			{
				if(isEndDateAfterStartDate())
				{
					setLabelDuration();
				}
				else
				{
					labelDuration.setText("Endzeit liegt vor Startzeit");
				}			
			}
		});
		
		comboBoxProject.requestFocus();		
	}
	
	@FXML
	public void buttonAdd()
	{
		String project = comboBoxProject.getValue();		
		String task = comboBoxTask.getValue(); 	
		
		if(project != null && !project.equals("") && task != null && !task.equals(""))
		{	
			if(isEndDateAfterStartDate())
			{
				int hours1 = timePicker1Controller.getHours();
				int minutes1 = timePicker1Controller.getMinutes();
				int seconds1 = timePicker1Controller.getSeconds();
				
				int hours2 = timePicker2Controller.getHours();
				int minutes2 = timePicker2Controller.getMinutes();
				int seconds2 = timePicker2Controller.getSeconds();
				
				LocalDate dateOne = datePicker1.getValue();
				LocalDate dateTwo = datePicker2.getValue();				
				
				Timestamp timestampStart = Timestamp.valueOf(dateOne.toString() + " " + hours1 + ":" + minutes1 +":" + seconds1 + ".000");
				Timestamp timestampEnd = Timestamp.valueOf(dateTwo.toString() + " " + hours2 + ":" + minutes2 +":" + seconds2 + ".000");				
				
				LogObject log = new LogObject(
						dateOne.getYear(),
						dateOne.getMonthValue(),
						dateOne.getDayOfMonth(),						 
						getCorrectedString(hours1) + ":" + getCorrectedString(minutes1) +":" + getCorrectedString(seconds1), 						
						getCorrectedString(hours2) + ":" + getCorrectedString(minutes2) +":" + getCorrectedString(seconds2),
						timestampEnd.getTime()-timestampStart.getTime(),
						project, 
						task
						);		
				
				SQL sql = new SQL(savePath);							
				try
				{					
					sql.insert(log);					
				}
				catch(Exception e)
				{	
					Logger.error(e);
					AlertGenerator.showAlert(AlertType.ERROR, "Fehler", "", "Fehler beim Speichern des Eintrags.", icon, stage, null, false);									
				}				
				
				AlertGenerator.showAlert(AlertType.INFORMATION, "Gespeichert", "", "Der Eintrag wurde erfolgreich gespeichert.", icon, stage, null, false);				
				stage.close();
				controller.loadAll();				
			}
			else
			{
				AlertGenerator.showAlert(AlertType.WARNING, "Warnung", "", "Endzeit muss vor Startzeit liegen.", icon, stage, null, false);
			}
		}
		else
		{
			AlertGenerator.showAlert(AlertType.WARNING, "Warnung", "", "Die Felder für Projekt und Task dürfen nicht leer sein.", icon, stage, null, false);
		}
	}
	
	@FXML
	public void buttonCancel()
	{
		stage.close();
	}
	
	@FXML
	public void useCurrentTime()
	{
		LocalDateTime now = LocalDateTime.now();
		timePicker2Controller.setTime(now.getHour(), now.getMinute(), now.getSecond());	
		timePicker2Controller.init();
		setLabelDuration();
	}
	
	public void refresh(TimePickerController controller, int hours, int minutes, int seconds, String item, String direction)
	{
		if(controller == timePicker1Controller)
		{
			if(item.equals("hours"))
			{
				if(direction.equals("up"))
				{
					hours++;	
					if(hours == 24)
					{
						hours = 0;
					}
				}
				else
				{						
					hours--;
					if(hours == -1 )
					{
						hours = 23;
					}
				}
			}
			else if(item.equals("minutes"))
			{
				if(direction.equals("up"))
				{
					minutes++;	
					if(minutes == 60)
					{
						minutes = 0;
					}
				}
				else
				{						
					minutes--;	
					if(minutes == -1)
					{
						minutes = 59;
					}
				}
			}
			else
			{
				if(direction.equals("up"))
				{
					seconds++;
					if(seconds == 60)
					{
						seconds = 0;
					}
				}
				else
				{						
					seconds--;
					if(seconds == -1)
					{
						seconds = 59;
					}
				}
			}			
			timePicker1Controller.setTime(hours, minutes, seconds);
			timePicker1Controller.init();	
		}
		else
		{
			if(item.equals("hours"))
			{
				if(direction.equals("up"))
				{
					hours++;	
					if(hours == 24)
					{
						hours = 0;
					}
				}
				else
				{						
					hours--;
					if(hours == -1 )
					{
						hours = 23;
					}
				}
			}
			else if(item.equals("minutes"))
			{
				if(direction.equals("up"))
				{
					minutes++;	
					if(minutes == 60)
					{
						minutes = 0;
					}
				}
				else
				{						
					minutes--;	
					if(minutes == -1)
					{
						minutes = 59;
					}
				}
			}
			else
			{
				if(direction.equals("up"))
				{
					seconds++;
					if(seconds == 60)
					{
						seconds = 0;
					}
				}
				else
				{						
					seconds--;
					if(seconds == -1)
					{
						seconds = 59;
					}
				}
			}			
			timePicker2Controller.setTime(hours, minutes, seconds);
			timePicker2Controller.init();		
		}
		
		if(isEndDateAfterStartDate())
		{
			setLabelDuration();
		}
		else
		{
			labelDuration.setText("Endzeit liegt vor Startzeit");
		}
	}
	
	public void setLabelDuration()
	{
		int hours1 = timePicker1Controller.getHours();
		int minutes1 = timePicker1Controller.getMinutes();
		int seconds1 = timePicker1Controller.getSeconds();
		
		int hours2 = timePicker2Controller.getHours();
		int minutes2 = timePicker2Controller.getMinutes();
		int seconds2 = timePicker2Controller.getSeconds();		
		
		String dateString = datePicker1.getValue().getYear() + "-" + getCorrectedString(datePicker1.getValue().getMonthValue()) + "-" + getCorrectedString(datePicker1.getValue().getDayOfMonth()) + "-" + getCorrectedString(hours1) + ":" + getCorrectedString(minutes1) + ":" + getCorrectedString(seconds1);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss");
		LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
		
		dateString = datePicker2.getValue().getYear() + "-" + getCorrectedString(datePicker2.getValue().getMonthValue()) + "-" + getCorrectedString(datePicker2.getValue().getDayOfMonth()) + "-" + getCorrectedString(hours2) + ":" + getCorrectedString(minutes2) + ":" + getCorrectedString(seconds2);
		LocalDateTime dateTime2 = LocalDateTime.parse(dateString, formatter);
		
		Duration d= Duration.between(dateTime, dateTime2);
		
		labelDuration.setText(ConvertTo.ConvertSecondsToTime(d.getSeconds()));		
	}
	
	public boolean isEndDateAfterStartDate()
	{
		int hours1 = timePicker1Controller.getHours();
		int minutes1 = timePicker1Controller.getMinutes();
		int seconds1 = timePicker1Controller.getSeconds();
		
		int hours2 = timePicker2Controller.getHours();
		int minutes2 = timePicker2Controller.getMinutes();
		int seconds2 = timePicker2Controller.getSeconds();
		
		String dateOne = java.sql.Date.valueOf(datePicker1.getValue()).toString();
		String dateTwo = java.sql.Date.valueOf(datePicker2.getValue()).toString();	
		
		DateFormat format = new SimpleDateFormat("yy-MM-dd - HH:mm:ss");
		
		dateOne = dateOne + " - " + getCorrectedString(hours1) + ":"+  getCorrectedString(minutes1) + ":" + getCorrectedString(seconds1); 
		dateTwo = dateTwo + " - " + getCorrectedString(hours2) + ":"+  getCorrectedString(minutes2) + ":" + getCorrectedString(seconds2); 
		
		try
		{
			Date startDate = format.parse(dateOne);
			Date endDate = format.parse(dateTwo);	
			
			if(startDate.before(endDate))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		catch(ParseException e)
		{		
			Logger.error(e);
		}		
		return false;				
	}
	
	private String getCorrectedString(int number)
	{
		if(number < 10)
		{
			return "0" + number;
		}
		else
		{
			return "" + number;
		}	
	}	
}