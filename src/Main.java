import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
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
	
	int MS = 0;

	float maxRecordedSpeed = 10f;
	
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

					int lCounter = 0;

					while(loop.get()) {

						byte buf[] = new byte[500];	

						DatagramPacket packet = new DatagramPacket(buf, 500);
						input.receive(packet);
						byte[] currentData = packet.getData();

						UNPAUSE = getBytes(currentData,0,4).getInt();
						RPM = getBytes(currentData,16,20).getFloat();
						SPEED = getBytes(currentData,244,248).getFloat() * 2.237f;
						XPOS = getBytes(currentData,232,236).getFloat();
						YPOS = getBytes(currentData,236,240).getFloat();
						ZPOS = getBytes(currentData,240,244).getFloat();

						if(UNPAUSE == 1) { 
							MS += 16;
							lCounter++;
							updateMessage(Integer.toString(lCounter));
						}
						
						Thread.sleep(16);

					}	

					return null;

				}

			};

			T = new Thread(bgLoop);
			T.start();

			Label rpmLabel = new Label(Float.toString(RPM));

			NumberAxis xSpeedAxis = new NumberAxis();
			NumberAxis ySpeedAxis = new NumberAxis();
			XYChart.Series<Number, Number> speedSeries = new Series<>();
			LineChart<Number, Number> speedChart = new LineChart<>(xSpeedAxis, ySpeedAxis);

			speedChart.getData().add(speedSeries);
			speedChart.setPrefWidth(500);
			speedChart.setPrefHeight(500);
			speedChart.setAnimated(false);			

			int maxTime = 60*1000;
			int timeDivision = 128;
			int maxPoints = maxTime / 64;
			
			xSpeedAxis.setAutoRanging(false);
			xSpeedAxis.setMinorTickCount(0);
			xSpeedAxis.setLowerBound(0);
			xSpeedAxis.setTickUnit(5000);

			speedChart.setCreateSymbols(false);

			ySpeedAxis.setAutoRanging(false);
			ySpeedAxis.setTickUnit(25);
			ySpeedAxis.setUpperBound(maxRecordedSpeed);
			ySpeedAxis.setLowerBound(0);
			
			NumberAxis xPosAxis = new NumberAxis();
			NumberAxis yPosAxis = new NumberAxis();
			XYChart.Series<Number, Number> posSeries = new Series<>();
			ScatterChart<Number, Number> posChart = new ScatterChart<>(xPosAxis, yPosAxis);

			posChart.getData().add(posSeries);
			posChart.setPrefWidth(500);
			posChart.setPrefHeight(500);
			posChart.setAnimated(false);			
			
			
			xSpeedAxis.setAutoRanging(true);

			speedChart.setCreateSymbols(false);

			ySpeedAxis.setAutoRanging(true);


			bgLoop.messageProperty().addListener((observable, old, updated) -> {

				rpmLabel.setText(Float.toString(RPM) + " RPM");	

				if(maxRecordedSpeed < SPEED) {
					maxRecordedSpeed = SPEED;
					ySpeedAxis.setUpperBound(maxRecordedSpeed);
				}
				
				if(MS % timeDivision == 0) {
					
					speedSeries.getData().add(new XYChart.Data<Number, Number>(MS, SPEED));
					if(speedSeries.getData().size() > maxPoints) {
						speedSeries.getData().remove(0, speedSeries.getData().size() - maxPoints);
						xSpeedAxis.setLowerBound(xSpeedAxis.getLowerBound() + timeDivision);
						xSpeedAxis.setUpperBound(xSpeedAxis.getUpperBound() + timeDivision);

					}
					xSpeedAxis.setUpperBound(xSpeedAxis.getUpperBound() + timeDivision);
					
					posSeries.getData().add(new XYChart.Data<Number, Number>(XPOS, ZPOS));
					
				}
				
			});

			VBox mainBox = new VBox();	
			mainBox.setPadding(new Insets(10));
			mainBox.setMinWidth(200d);

			mainBox.getChildren().addAll(rpmLabel, speedChart, posChart);

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

}
