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
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

	float RPM;
	int MS = 0;

	DatagramSocket input;
	Thread T;

	public static void main(String[] args) {

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
						
						RPM = getBytes(currentData,16,20).getFloat();
						MS += 16;

						lCounter++;
						updateMessage(Integer.toString(lCounter));
						Thread.sleep(16);

					}	

					return null;

				}

			};

			T = new Thread(bgLoop);
			T.start();

			Label rpmLabel = new Label(Float.toString(RPM));

			NumberAxis xAxis = new NumberAxis();
			NumberAxis yAxis = new NumberAxis();
			XYChart.Series<Number, Number> series = new Series<>();
			LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);

			chart.getData().add(series);
			chart.setPrefWidth(500);
			chart.setPrefHeight(500);
			chart.setAnimated(false);			

			int maxTime = 60*1000;
			int timeDivision = 128;
			int maxPoints = maxTime / 64;

			xAxis.setAutoRanging(false);
			xAxis.setMinorTickCount(0);
			xAxis.setUpperBound((timeDivision * maxPoints) + (timeDivision * 10));
			xAxis.setTickUnit(5000);
			xAxis.setLowerBound(timeDivision * 4);

			chart.setCreateSymbols(false);

			yAxis.setAutoRanging(false);
			yAxis.setTickUnit(1000);
			yAxis.setUpperBound(8000);
			yAxis.setLowerBound(0);


			bgLoop.messageProperty().addListener((observable, old, updated) -> {

				rpmLabel.setText(Float.toString(RPM) + " RPM");	

				if(MS % timeDivision == 0) {
					series.getData().add(new XYChart.Data<Number, Number>(MS, RPM));
					if(series.getData().size() > maxPoints) {
						series.getData().remove(0, series.getData().size() - maxPoints);
						xAxis.setLowerBound(xAxis.getLowerBound() + timeDivision);
						xAxis.setUpperBound(xAxis.getUpperBound() + timeDivision);

					}
				}
			});

			VBox mainBox = new VBox();	
			mainBox.setPadding(new Insets(10));
			mainBox.setMinWidth(200d);

			mainBox.getChildren().addAll(rpmLabel, chart);

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
