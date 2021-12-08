
package demo;

import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.VirtualStackAdapter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import sc.fiji.labkit.ui.InitialLabeling;
import sc.fiji.labkit.ui.SegmentationComponent;
import sc.fiji.labkit.ui.inputimage.DatasetInputImage;
import sc.fiji.labkit.ui.models.DefaultSegmentationModel;
import sc.fiji.labkit.ui.models.ImageLabelingModel;
import sc.fiji.labkit.ui.models.SegmentationModel;
import sc.fiji.labkit.pixel_classification.utils.SingletonContext;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;
import org.scijava.Context;
import org.scijava.ui.behaviour.util.RunnableAction;

import javax.swing.*;
import java.awt.*;

public class ChangingImageSegmentationComponentDemo {

	static {
		LegacyInjector.preinit();
	}

	private final SegmentationComponent segmenter;

	private final DefaultSegmentationModel segmentationModel;

	public static void main(String... args) {
		new ChangingImageSegmentationComponentDemo();
	}

	private ChangingImageSegmentationComponentDemo() {
		JFrame frame = setupFrame();
		ImgPlus<?> image = VirtualStackAdapter.wrap(new ImagePlus(
			"https://imagej.nih.gov/ij/images/FluorescentCells.jpg"));
		Context context = new Context();
		segmentationModel = new DefaultSegmentationModel(context, new DatasetInputImage(image));
		segmenter = new SegmentationComponent(frame, segmentationModel, false);
		frame.add(segmenter);
		frame.add(initChangeImageButton(segmentationModel), BorderLayout.PAGE_START);
		frame.add(getBottomPanel(), BorderLayout.PAGE_END);
		frame.setVisible(true);
	}

	private JButton initChangeImageButton(SegmentationModel segmentationModel) {
		JButton button = new JButton("change image");
		button.addActionListener(ignore -> ChangingImageSegmentationComponentDemo
			.onChangeImageButtonClicked(segmentationModel));
		return button;
	}

	private static void onChangeImageButtonClicked(SegmentationModel segmentationModel) {
		final ImagePlus imp = new ImagePlus(
			"https://imagej.nih.gov/ij/images/apple.tif");
		ImageLabelingModel model = segmentationModel.imageLabelingModel();
		ImgPlus<?> image = VirtualStackAdapter.wrap(imp);
		DatasetInputImage datasetInputImage = new DatasetInputImage(image);
		model.showable().set(datasetInputImage.showable());
		model.imageForSegmentation().set(datasetInputImage.imageForSegmentation());
		model.labeling().set(InitialLabeling.initialLabeling(SingletonContext.getInstance(),
			datasetInputImage));
	}

	private JPanel getBottomPanel() {
		JButton segmentation = new JButton(new RunnableAction("Show Segmentation",
			this::showSegmentation));
		JButton prediction = new JButton(new RunnableAction("Show Prediction",
			this::showPrediction));
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout());
		panel.add(segmentation);
		panel.add(prediction);
		return panel;
	}

	private void showSegmentation() {
		if (!segmentationModel.isTrained()) System.out.println("not trained yet");
		else {
			for (RandomAccessibleInterval<UnsignedByteType> segmentation : segmentationModel
				.getSegmentations(new UnsignedByteType()))
			{
				Views.iterable(segmentation).forEach(x -> x.mul(128));
				ImageJFunctions.show(segmentation);
			}
		}
	}

	private void showPrediction() {
		if (!segmentationModel.isTrained()) System.out.println("not trained yet");
		else {
			segmentationModel.getPredictions().forEach(ImageJFunctions::show);
		}
	}

	private static JFrame setupFrame() {
		JFrame frame = new JFrame();
		frame.setSize(500, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		return frame;
	}
}
