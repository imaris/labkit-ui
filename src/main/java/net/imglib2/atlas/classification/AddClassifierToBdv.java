package net.imglib2.atlas.classification;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import bdv.img.cache.CreateInvalidVolatileCell;
import bdv.img.cache.VolatileCachedCellImg;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.VisibilityAndGrouping;
import net.imglib2.RealRandomAccessible;
import net.imglib2.atlas.LabelBrushController;
import net.imglib2.atlas.color.IntegerColorProvider;
import net.imglib2.cache.Cache;
import net.imglib2.cache.IoSync;
import net.imglib2.cache.img.AccessFlags;
import net.imglib2.cache.img.AccessIo;
import net.imglib2.cache.img.DiskCellCache;
import net.imglib2.cache.img.PrimitiveType;
import net.imglib2.cache.queue.BlockingFetchQueues;
import net.imglib2.cache.ref.GuardedStrongRefLoaderRemoverCache;
import net.imglib2.cache.ref.WeakRefVolatileCache;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.CreateInvalid;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.cache.volatiles.VolatileCache;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.LazyCellImg;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.type.volatiles.VolatileShortType;
import net.imglib2.view.Views;
import net.imglib2.view.composite.Composite;

public class AddClassifierToBdv< T extends RealType< T > > implements TrainClassifier.Listener< T >
{

	public static class CacheOptions
	{
		private final String cacheTempName;

		private final CellGrid grid;

		private final int entries;

		private final BlockingFetchQueues< Callable< ? > > queue;

		public CacheOptions( final String cacheTempName, final CellGrid grid, final int entries, final BlockingFetchQueues< Callable< ? > > queue )
		{
			super();
			this.cacheTempName = cacheTempName;
			this.grid = grid;
			this.entries = entries;
			this.queue = queue;
		}
	}

	private final BdvStackSource< ? > source;

	private final ClassifyingCacheLoader< T, VolatileShortArray > loader;

	private final IntegerColorProvider colorProvider;

	private final CacheOptions cacheOptions;

	private final int numChannels;

	public AddClassifierToBdv(
			final BdvStackSource< ? > source,
			final ClassifyingCacheLoader< T, VolatileShortArray > loader,
			final IntegerColorProvider colorProvider,
			final CacheOptions cacheOptions,
			final int numChannels )
	{
		super();
		this.source = source;
		this.loader = loader;
		this.colorProvider = colorProvider;
		this.cacheOptions = cacheOptions;
		this.numChannels = numChannels;
	}

	private boolean wasTrainedAtLeastOnce = false;

	private Cache< Long, Cell< VolatileShortArray > > cache = null;

	private VolatileCache< Long, Cell< VolatileShortArray > > volatileCache = null;

	private VolatileCachedCellImg< VolatileShortType, ? > vimg;

	private BdvStackSource< VolatileARGBType > stackSource = null;

	public Img< ShortType > getLazyImg()
	{
		if ( wasTrainedAtLeastOnce )
			return new LazyCellImg<>( cacheOptions.grid, new ShortType(), cache.unchecked()::get );
		else
			return null;
	}

	public Img< VolatileShortType > getVolatileImg()
	{
		return vimg;
	}

	@Override
	public void notify( final Classifier< Composite< T >, ?, ? > classifier, final boolean trainingSuccess ) throws IOException
	{
		if ( trainingSuccess )
			synchronized( source.getBdvHandle().getViewerPanel() ) {
//				loader.setClassifier( classifier );
				final ShortType type = new ShortType();
				final VolatileShortType vtype = new VolatileShortType();
				final Path blockcache = DiskCellCache.createTempDirectory( cacheOptions.cacheTempName, true );
				final DiskCellCache< VolatileShortArray > diskcache = new DiskCellCache<>(
						blockcache,
						cacheOptions.grid,
						loader,
						AccessIo.get( PrimitiveType.SHORT, AccessFlags.VOLATILE ),
						type.getEntitiesPerPixel() );
				final IoSync< Long, Cell< VolatileShortArray > > iosync = new IoSync<>( diskcache );
				final Cache< Long, Cell< VolatileShortArray > > cache = new GuardedStrongRefLoaderRemoverCache< Long, Cell< VolatileShortArray > >( 1000 )
						.withRemover( iosync )
						.withLoader( iosync );

				final CreateInvalid< Long, Cell< VolatileShortArray > > createInvalid = CreateInvalidVolatileCell.get( cacheOptions.grid, type );
				final WeakRefVolatileCache< Long, Cell< VolatileShortArray > > volatileCache = new WeakRefVolatileCache<>( cache, cacheOptions.queue, createInvalid );

				final CacheHints hints = new CacheHints( LoadingStrategy.VOLATILE, 0, false );
				final VolatileCachedCellImg< VolatileShortType, VolatileShortArray > vimg = new VolatileCachedCellImg<>( cacheOptions.grid, vtype, hints, volatileCache.unchecked()::get );


				final Converter< VolatileShortType, VolatileARGBType > conv = ( input, output ) -> {
					final boolean isValid = input.isValid();
					output.setValid( isValid );
					if ( isValid )
						output.set( colorProvider.getColor( input.get().get() ) );
				};

				final RealRandomAccessible< VolatileShortType > real = Views.interpolate(
						Views.extendValue( vimg, new VolatileShortType( ( short ) LabelBrushController.BACKGROUND ) ),
						new NearestNeighborInterpolatorFactory<>() );
				final RealRandomAccessible< VolatileARGBType > convertedReal = Converters.convert( real, conv, new VolatileARGBType() );

				if ( wasTrainedAtLeastOnce )
					stackSource.removeFromBdv();

				stackSource = BdvFunctions.show( convertedReal, vimg, "prediction", BdvOptions.options().addTo( source ) );

				final VisibilityAndGrouping vag = stackSource.getBdvHandle().getViewerPanel().getVisibilityAndGrouping();
				final int predictionSource = vag.numSources() - 1;
				for ( int i = 0; i < numChannels; ++i )
					vag.addSourceToGroup( predictionSource, i );

				stackSource.getBdvHandle().getViewerPanel().requestRepaint();
				this.cache = cache;
				this.volatileCache = volatileCache;
				this.vimg = vimg;
				wasTrainedAtLeastOnce = true;
			}

	}
}
