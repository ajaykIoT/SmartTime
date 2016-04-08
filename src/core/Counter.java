package core;

import javafx.application.Platform;
import userInterface.UserInterfaceController;

/**
 * Thread f�r die zeitgleiche Ausf�hrung der Zeitmessung ohne Beeintr�chtigung der Hauptanwendung
 * @author Robert
 *
 */
public class Counter extends Thread
{
	public static boolean running;
	public static long ausgabe;
	public static UserInterfaceController uic;	
			
	@Override
	public void run() 
	{		
			//l�scht zu Beginn den Text des Labels
			uic.labelTime.setText("");
			//initialisiert die Z�hlvariable
			ausgabe = 0;
				
			 while (running) 
			 {
				 try 
				 {			
					 //konvertiert die bereits verstrichenen Millisekunden in Stunden, Minuten und Sekunden
					 //und gibt diese auf dem Label aus
					 Platform.runLater(()->{
						uic.labelTime.setText(ConvertToTime.ConvertMillisToTime(ausgabe));
					 });
					 
					 //schl�ft 1000 Millisekunden
					 Thread.sleep(1000);
					 //erh�ht die Z�hlvariable um 1000 Millisekunden
					 ausgabe = ausgabe + 1000;
				 }
				 //reagiert auf eine InterruptedException, die ausgel�st wird, wenn der Stopp-Button gedr�ckt wird
				 catch (InterruptedException e) 
				 {
					 running = false;
				 }			
			}	
	}
}