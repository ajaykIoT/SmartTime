package charts;

import java.util.ArrayList;
import java.util.Calendar;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.event.*;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import core.LogObject;
import core.SQL;

public class BarChartGenerator
{
	private SQL sql;

	public BarChartGenerator(String path)
	{
		sql = new SQL(path);
	}

	private long getMinute(long millis)
	{
		return (millis / (1000 * 60));
	}

	private String getHours(int minutes)
	{
		int hours = (int)minutes / 60;
		int min = (int)minutes % 60;
		return "" + hours + " h " + min + " min";
	}

	private int getMax(int year, int month)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month);
		calendar.set(Calendar.DATE, - 1);

		int numDays = calendar.getActualMaximum(Calendar.DATE);

		return numDays;
	}

	private String getMonthName(int month)
	{
		switch(month)
		{
			case 1:
				return "Januar";
			case 2:
				return "Februar";
			case 3:
				return "M�rz";
			case 4:
				return "April";
			case 5:
				return "Mai";
			case 6:
				return "Juni";
			case 7:
				return "Juli";
			case 8:
				return "August";
			case 9:
				return "September";
			case 10:
				return "Oktober";
			case 11:
				return "November";
			case 12:
				return "Dezember";
			default:
				return null;
		}
	}

	@SuppressWarnings("unchecked")
	public BarChart<String, Number> getBarChart(String project, String task, int year, int month) throws Exception
	{
		final CategoryAxis xAxis = new CategoryAxis();
		final NumberAxis yAxis = new NumberAxis();
		final BarChart<String, Number> chart = new BarChart<String, Number>(xAxis, yAxis);
		chart.setTitle(project + " - " + task + " - " + getMonthName(month) + " " + year);
		xAxis.setLabel("Tag");
		yAxis.setLabel("Zeit in Minuten");
		chart.setCategoryGap(2);

		XYChart.Series<String, Number> series = new XYChart.Series<>();
		series.setName("Arbeitszeit in Minuten");

		ArrayList<LogObject> objects = sql.getByProjectAndTaskAndYearAndMonth(project, task, year, month);

		Long[] times = new Long[getMax(year, month)];

		for(int i = 0; i < times.length; i++)
		{
			times[i] = 0L;
			for(LogObject current : objects)
			{
				if(current.getDay() == i+1)
				{
					times[i] += getMinute(current.getDuration());
				}
			}
		}

		for(int l = 0; l < getMax(year, month); l++)
		{
			series.getData().add(new XYChart.Data<String, Number>(String.valueOf(l + 1), times[l]));
		}
		chart.getData().addAll(series);

		for(final Series<String, Number> serie : chart.getData())
		{
			for(final Data<String, Number> data : serie.getData())
			{
				Tooltip tooltip = new Tooltip();
				String stunden = getHours(data.getYValue().intValue());
				tooltip.setText(stunden);
				Node node = data.getNode();
				node.setOnMouseEntered(new EventHandler<MouseEvent>()
				{
					@Override
					public void handle(MouseEvent event)
					{
						Point2D p = node.localToScreen(event.getX() + 5, event.getY() + 7);
						tooltip.show(node, p.getX(), p.getY());
					}
				});
				node.setOnMouseExited(new EventHandler<MouseEvent>()
				{
					@Override
					public void handle(MouseEvent event)
					{
						tooltip.hide();
					}
				});
			}
		}
		return chart;
	}
}