
package net.imglib2.labkit;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.labkit.actions.AddLabelingIoAction;
import net.imglib2.labkit.actions.BatchSegmentAction;
import net.imglib2.labkit.actions.BitmapImportExportAction;
import net.imglib2.labkit.actions.ClassifierIoAction;
import net.imglib2.labkit.actions.LabelEditAction;
import net.imglib2.labkit.actions.LabelingIoAction;
import net.imglib2.labkit.actions.ResetViewAction;
import net.imglib2.labkit.actions.SegmentationAsLabelAction;
import net.imglib2.labkit.actions.SegmentationSave;
import net.imglib2.labkit.actions.ClassifierSettingsAction;
import net.imglib2.labkit.menu.MenuKey;
import net.imglib2.labkit.models.ColoredLabelsModel;
import net.imglib2.labkit.models.DefaultSegmentationModel;
import net.imglib2.labkit.panel.ImageInfoPanel;
import net.imglib2.labkit.panel.LabelPanel;
import net.imglib2.labkit.panel.SegmenterPanel;
import net.imglib2.labkit.plugin.MeasureConnectedComponents;
import net.imglib2.labkit.segmentation.PredictionLayer;
import net.imglib2.labkit.segmentation.TrainClassifier;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.real.FloatType;
import net.miginfocom.swing.MigLayout;
import org.scijava.Context;

import javax.swing.*;
import java.util.List;

public class SegmentationComponent implements AutoCloseable {

	private final JComponent panel;

	private final boolean fixedLabels;

	private final DefaultExtensible extensible;

	private BasicLabelingComponent labelingComponent;

	private DefaultSegmentationModel segmentationModel;

	public SegmentationComponent(Context context, JFrame dialogBoxOwner,
		DefaultSegmentationModel segmentationModel, boolean fixedLabels)
	{
		this.extensible = new DefaultExtensible(context, dialogBoxOwner);
		this.fixedLabels = fixedLabels;
		this.segmentationModel = segmentationModel;
		labelingComponent = new BasicLabelingComponent(dialogBoxOwner,
			segmentationModel.imageLabelingModel());
		labelingComponent.addBdvLayer(new PredictionLayer(segmentationModel
			.selectedSegmenter(), segmentationModel.segmentationVisibility()));
		initActions();
		this.panel = initPanel();
	}

	private void initActions() {
		new TrainClassifier(extensible, segmentationModel);
		new ClassifierSettingsAction(extensible, segmentationModel
				.selectedSegmenter());
		new ClassifierIoAction(extensible, segmentationModel.selectedSegmenter());
		new LabelingIoAction(extensible, segmentationModel.imageLabelingModel());
		new AddLabelingIoAction(extensible, segmentationModel.imageLabelingModel()
			.labeling());
		new SegmentationSave(extensible, segmentationModel.selectedSegmenter());
		new ResetViewAction(extensible, segmentationModel.imageLabelingModel());
		new BatchSegmentAction(extensible, segmentationModel.selectedSegmenter());
		new SegmentationAsLabelAction(extensible, segmentationModel
			.selectedSegmenter(), segmentationModel.imageLabelingModel().labeling());
		new BitmapImportExportAction(extensible, segmentationModel
			.imageLabelingModel());
		new LabelEditAction(extensible, fixedLabels, new ColoredLabelsModel(segmentationModel.imageLabelingModel()));
		MeasureConnectedComponents.addAction(extensible, segmentationModel
			.imageLabelingModel());
		labelingComponent.addShortcuts(extensible.getShortCuts());
	}

	private JPanel initLeftPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("", "[grow]", "[][grow][grow]"));
		panel.add(ImageInfoPanel.newFramedImageInfoPanel(segmentationModel.imageLabelingModel()), "grow, wrap");
		panel.add(LabelPanel.newFramedLabelPanel(segmentationModel.imageLabelingModel(), extensible, fixedLabels), "grow, wrap");
		panel.add(SegmenterPanel.newFramedSegmeterPanel(segmentationModel, extensible), "grow");
		panel.invalidate();
		panel.repaint();
		return panel;
	}

	private JComponent initPanel() {
		JSplitPane panel = new JSplitPane();
		panel.setOneTouchExpandable(true);
		panel.setLeftComponent(initLeftPanel());
		panel.setRightComponent(labelingComponent.getComponent());
		panel.setBorder(BorderFactory.createEmptyBorder());
		return panel;
	}

	public JComponent getComponent() {
		return panel;
	}

	public JMenu getActions(MenuKey< Void > key) {
		return extensible.createMenu( key, () -> null );
	}

	public <T extends IntegerType<T> & NativeType<T>>
		List<RandomAccessibleInterval<T>> getSegmentations(T type)
	{
		return segmentationModel.getSegmentations(type);
	}

	public List<RandomAccessibleInterval<FloatType>> getPredictions() {
		return segmentationModel.getPredictions();
	}

	public boolean isTrained() {
		return segmentationModel.isTrained();
	}

	@Override
	public void close() {
		labelingComponent.close();
	}

}
