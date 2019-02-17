import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

	float RPM;
	float SPEED;
	int UNPAUSE;
	float XPOS;
	float YPOS;
	float ZPOS;
	IntegerProperty LAPNO = new SimpleIntegerProperty(-1);
	FloatProperty LAPTIME = new SimpleFloatProperty(0); 

	IntegerProperty MS = new SimpleIntegerProperty(0);

	int timeDivision = 128;
	float maxRecordedSpeed = 10f;
	float maxRecordedTime = 5f;
	int maxSeries = 3;
	ObservableList<Series<Number, Number>> chartSeries;
	NumberAxis xSpeedAxis = new NumberAxis();
	NumberAxis ySpeedAxis = new NumberAxis();

	DatagramSocket input;
	Thread T;

	public static void main(String[] args) {

		System.out.println("Starting...");
		launch(args);

	}

	@Override
	public void start(Stage mainStage) throws Exception {

		try {

			input = new DatagramSocket(1024);
			DatagramPacket packet = new DatagramPacket(new byte[274], 274);

			input.receive(packet);
			System.out.println("launch");

			AtomicBoolean loop = new AtomicBoolean(true);
			Task<Void> bgLoop = new Task<Void>() {

				@Override
				protected Void call() throws Exception {

					while(loop.get()) {

						byte buf[] = new byte[500];	

						DatagramPacket packet = new DatagramPacket(buf, 500);
						input.receive(packet);
						byte[] currentData = packet.getData();

						Platform.runLater(() -> {

							UNPAUSE = getBytes(currentData,0,4).getInt();

							if(UNPAUSE == 1) { 

								MS.set(MS.get() + 16);

								RPM = getBytes(currentData,16,20).getFloat();

								SPEED = getBytes(currentData,244,248).getFloat() * 2.237f;

								XPOS = getBytes(currentData,232,236).getFloat();
								YPOS = getBytes(currentData,236,240).getFloat();
								ZPOS = getBytes(currentData,240,244).getFloat();

								LAPNO.set((int) getBytes(currentData, 300, 302).get());

								LAPTIME.set(getBytes(currentData, 292, 296).getFloat());

							}

						});

						Thread.sleep(16);

					}	

					return null;

				}

			};

			T = new Thread(bgLoop);
			T.start();

			Label rpmLabel = new Label(Float.toString(RPM));
			Label lapLabel = new Label(Integer.toString(LAPNO.get()));

			LineChart<Number, Number> speedChart = new LineChart<>(xSpeedAxis, ySpeedAxis);

			chartSeries = speedChart.getData();



			speedChart.setPrefWidth(500);
			speedChart.setPrefHeight(500);
			speedChart.setAnimated(false);			

			xSpeedAxis.setAutoRanging(false);
			xSpeedAxis.setMinorTickCount(0);
			xSpeedAxis.setLowerBound(0);
			xSpeedAxis.setTickUnit(5);

			speedChart.setCreateSymbols(false);

			ySpeedAxis.setAutoRanging(false);
			ySpeedAxis.setTickUnit(25);
			ySpeedAxis.setLowerBound(0);	

			resetChart();

			MS.addListener((observable, old, updated) -> {

				rpmLabel.setText(Float.toString(RPM) + " RPM");	

				if(maxRecordedSpeed < SPEED) {
					maxRecordedSpeed = SPEED;
					ySpeedAxis.setUpperBound(maxRecordedSpeed);
				}

				if(maxRecordedTime < LAPTIME.get()) { 
					maxRecordedTime = LAPTIME.get();
					xSpeedAxis.setUpperBound(maxRecordedTime);
				}

				if(MS.get() % timeDivision == 0) {

					chartSeries.get(maxSeries-1).getData().add(new XYChart.Data<Number, Number>(LAPTIME.get(), SPEED));

					xSpeedAxis.setUpperBound(xSpeedAxis.getUpperBound() + timeDivision/1000);

				}

			});

			LAPNO.addListener((observable, oldValue, newValue) -> {

				if((int) newValue == 0) { 
					resetChart();
				} else {
					lapLabel.setText("Lap " + newValue);
					chartSeries.remove(0);
					Series<Number, Number> newSeries = new Series<Number, Number>();
					newSeries.setName(Integer.toString(LAPNO.get()));
					chartSeries.add(newSeries);
				}
			});

			VBox mainBox = new VBox();	
			mainBox.setPadding(new Insets(10));
			mainBox.setMinWidth(200d);

			mainBox.getChildren().addAll(rpmLabel, lapLabel, speedChart);

			Scene mainScene = new Scene(mainBox);
			mainStage.setScene(mainScene);

			mainStage.setOnCloseRequest(e -> {				
				loop.set(false);		
			});

			mainStage.show();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private ByteBuffer getBytes(byte[] buffer, int offset, int length) {
		return ByteBuffer.wrap(Arrays.copyOfRange(buffer, offset, length)).order(ByteOrder.LITTLE_ENDIAN);
	}

	private void resetChart() {

		chartSeries.clear();

		for(int s = 0; s < maxSeries; s++) { 
			chartSeries.add(new Series<Number, Number>());
		}

		maxRecordedSpeed = 0;
		maxRecordedTime = 0;

		xSpeedAxis.setUpperBound(maxRecordedTime);
		ySpeedAxis.setUpperBound(maxRecordedSpeed);

	}

}
