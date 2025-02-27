/*-
 * #%L
 * The Labkit image segmentation tool for Fiji.
 * %%
 * Copyright (C) 2017 - 2023 Matthias Arzt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.labkit.ui.segmentation.weka;

import sc.fiji.labkit.ui.segmentation.SegmentationPlugin;
import sc.fiji.labkit.ui.segmentation.Segmenter;
import sc.fiji.labkit.pixel_classification.gson.GsonUtils;
import sc.fiji.labkit.pixel_classification.pixel_feature.settings.FeatureSettings;
import sc.fiji.labkit.pixel_classification.utils.SingletonContext;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Plugin provides the "Labkit Pixel Classification".
 */
@Plugin(type = SegmentationPlugin.class)
public class PixelClassificationPlugin implements SegmentationPlugin {

	@Parameter
	Context context;

	@Parameter(required = false)
	Boolean useGpu = false;

	@Parameter(required = false)
	String featureSettingsFilename;

	@Override
	public String getTitle() {
		return "Labkit Pixel Classification";
	}

	@Override
	public Segmenter createSegmenter() {
		TrainableSegmentationSegmenter segmenter = new TrainableSegmentationSegmenter(context);

		if ( featureSettingsFilename != null ) {
			FeatureSettings featureSettings = FeatureSettings.fromJson( GsonUtils.read( featureSettingsFilename ) );
			segmenter.setFeatureSettings( featureSettings );
		}

		segmenter.setUseGpu( useGpu );
		return segmenter;
	}

	@Override
	public boolean canOpenFile(String filename) {
		try {
			new TrainableSegmentationSegmenter(context).openModel(filename);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	public static SegmentationPlugin create() {
		return create( false, null );
	}

	public static SegmentationPlugin create( boolean useGpu, String featureSettingsFilename ) {
		Context context = SingletonContext.getInstance();
		PixelClassificationPlugin plugin = new PixelClassificationPlugin();
		plugin.useGpu = useGpu;
		plugin.featureSettingsFilename = featureSettingsFilename;
		context.inject(plugin);
		return plugin;
	}
}
