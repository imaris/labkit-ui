package net.imglib2.atlas;

import bdv.util.*;
import bdv.viewer.DisplayMode;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.atlas.actions.ToggleVisibility;
import net.imglib2.atlas.color.ColorMapProvider;
import net.imglib2.atlas.color.UpdateColormap;
import net.imglib2.atlas.control.brush.*;
import net.imglib2.atlas.control.brush.neighborhood.NeighborhoodFactories;
import net.imglib2.atlas.control.brush.neighborhood.NeighborhoodPixelsGenerator;
import net.imglib2.atlas.control.brush.neighborhood.NeighborhoodPixelsGeneratorForTimeSeries;
import net.imglib2.atlas.control.brush.neighborhood.PaintPixelsGenerator;
import net.imglib2.atlas.labeling.Labeling;
import net.imglib2.atlas.labeling.LabelsLayer;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.trainable_segmention.RevampUtils;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.ui.OverlayRenderer;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.util.AbstractNamedAction;

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;
import java.util.List;

public class LabelingComponent {

	private BdvHandle bdvHandle;

	private JPanel panel = new JPanel();

	private final JFrame dialogBoxOwner;

	private ColorMapProvider colorProvider;

	private Holder<Labeling> labels;

	private ActionsAndBehaviours actionsAndBehaviours;

	private AffineTransform3D sourceTransformation = new AffineTransform3D();

	public JComponent getComponent() {
		return panel;
	}

	public ActionMap getActions() {
		return actionsAndBehaviours.getActions();
	}

	LabelingComponent(final JFrame dialogBoxOwner,
			final RandomAccessibleInterval<? extends NumericType<?>> rawData,
			final List<String> labels,
			final boolean isTimeSeries)
	{
		this.dialogBoxOwner = dialogBoxOwner;

		final int nDim = rawData.numDimensions();

		initBdv(isTimeSeries || nDim != 3);

		actionsAndBehaviours = new ActionsAndBehaviours(bdvHandle);

		initLabelsLayer(labels, rawData, isTimeSeries);

		Pair<Double, Double> p = AtlasUtils.estimateMinMax(rawData);
		BdvStackSource<?> source = addLayer(RevampUtils.uncheckedCast(rawData), "original");
		source.setDisplayRange(p.getA(), p.getB());
		addAction(new ToggleVisibility("Image", source));
	}

	private static int[] cellDimensions(CellGrid grid) {
		final int[] cellDimensions = new int[ grid.numDimensions() ];
		grid.cellDimensions( cellDimensions );
		return cellDimensions;
	}

	public void addAction(AbstractNamedAction action) {
		actionsAndBehaviours.addAction(action);
	}

	public void addBehaviour(Behaviour behaviour, String name, String defaultTriggers) {
		actionsAndBehaviours.addBehaviour(behaviour, name, defaultTriggers);
	}

	public Labeling getLabeling() {
		return labels.get();
	}

	private void initBdv(boolean is2D) {
		final BdvOptions options = BdvOptions.options();
		if (is2D)
			options.is2D();
		bdvHandle = new BdvHandlePanel(dialogBoxOwner, options);
		panel.setLayout(new BorderLayout());
		panel.add(bdvHandle.getViewerPanel());
		bdvHandle.getViewerPanel().setDisplayMode( DisplayMode.FUSED );
	}

	private PaintPixelsGenerator<BitType, ? extends Iterator<BitType>> initPixelGenerator(boolean isTimeSeries, int numDimensions) {
		if ( isTimeSeries )
			return new NeighborhoodPixelsGeneratorForTimeSeries<>(numDimensions - 1, new NeighborhoodPixelsGenerator<BitType>(NeighborhoodFactories.hyperSphere(), 1.0));
		else
			return new NeighborhoodPixelsGenerator<>(NeighborhoodFactories.<BitType>hyperSphere(), 1.0);
	}

	private void initLabelsLayer(List<String> labels, Interval interval, boolean isTimeSeries) {
		this.labels = new Holder<>(new Labeling(labels, interval));
		colorProvider = new ColorMapProvider(this.labels);

		BdvSource source = addLayer(new LabelsLayer(this.labels, colorProvider, this).view(), "labels");
		addAction(new ToggleVisibility( "Labeling", source ));
		final LabelBrushController brushController = new LabelBrushController(
				bdvHandle.getViewerPanel(),
				this.labels,
				initPixelGenerator(isTimeSeries, this.labels.get().numDimensions()),
				actionsAndBehaviours,
				colorProvider,
				sourceTransformation);
		initColorMapUpdaterAction(labels, colorProvider);
		bdvHandle.getViewerPanel().getDisplay().addOverlayRenderer( brushController.getBrushOverlay() );
	}

	private void initColorMapUpdaterAction(List<String> labels, ColorMapProvider colorProvider) {
		final UpdateColormap colormapUpdater = new UpdateColormap( colorProvider, labels, bdvHandle.getViewerPanel(), 1.0f );
		addAction(colormapUpdater);
	}

	public ColorMapProvider colorProvider() {
		return colorProvider;
	}

	public void setLabeling(Labeling labeling) {
		if(! Intervals.equals(labels.get(), labeling))
			throw new IllegalArgumentException();
		labels.set(labeling);
	}

	public void requestRepaint() {
		bdvHandle.getViewerPanel().requestRepaint();
	}

	public <T extends NumericType<T>> BdvStackSource<T> addLayer(RandomAccessibleInterval<T> interval, String prediction) {
		return BdvFunctions.show(interval, prediction, BdvOptions.options().addTo(bdvHandle).sourceTransform(sourceTransformation));
	}

	public AffineTransform3D sourceTransformation() {
		return sourceTransformation;
	}

	public void addOverlayRenderer(OverlayRenderer overlay) {
		bdvHandle.getViewerPanel().getDisplay().addOverlayRenderer(overlay);
	}

	public void displayRepaint() {
		bdvHandle.getViewerPanel().getDisplay().repaint();
	}

	public Object viewerSync() {
		return bdvHandle.getViewerPanel();
	}
}
