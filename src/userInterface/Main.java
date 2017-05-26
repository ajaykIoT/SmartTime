package userInterface;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import tools.AlertGenerator;


public class Main extends Application
{
	@Override
	public void start(Stage stage)
	{
		try
		{			
			FXMLLoader loader = new FXMLLoader(getClass().getResource("userInterface.fxml"));
			Parent root = (Parent)loader.load();

			Scene scene = new Scene(root, 800, 800);

			stage.setResizable(false);
			stage.setTitle("SmartTime");
			stage.setScene(scene);
			
			UserInterfaceController controller = (UserInterfaceController)loader.getController();			
			controller.init(stage);

			stage.getIcons().add(new Image("/userInterface/icon.png"));
			stage.show();

			// fängt die Aufforderung das Fenster zu schließen ab, um vorher
			// noch eine Prüfung duchzuführen
			stage.setOnCloseRequest(new EventHandler<WindowEvent>()
			{
				public void handle(WindowEvent we)
				{
					if(controller.stoppUhrLäuftFlag == true)
					{
						AlertGenerator.showAlert(AlertType.WARNING, "Warnung", "", "Die Stoppuhr läuft noch!", new Image("/userInterface/icon.png"), stage, null, false);
						
						// "schluckt" die Aufforderung das Fenster zu schließen
						// (Fenster wird dadurch nicht geschlossen)
						we.consume();
					}
					else
					{
						stage.close();
					}
				}
			});
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		launch(args);
	}
}