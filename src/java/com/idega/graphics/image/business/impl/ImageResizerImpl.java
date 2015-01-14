package com.idega.graphics.image.business.impl;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;

import com.idega.core.business.DefaultSpringBean;
import com.idega.graphics.image.business.ImageResizer;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.util.FileUtil;
import com.idega.util.IOUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

public class ImageResizerImpl extends DefaultSpringBean implements ImageResizer {

	@Override
	public InputStream getScaledImage(int newWidth, int newHeight, InputStream streamToImage, String imageType) throws IOException {
		if (newWidth < 0 || newHeight < 0 || streamToImage == null || StringUtil.isEmpty(imageType)) {
			getLogger().warning("Invalid parameters!");
			return null;
		}

		return getStream(getScaledImage(newWidth, newHeight, -1, streamToImage, imageType, null));
	}

	@Override
	public ByteArrayOutputStream getScaledImage(int newWidth, int newHeight, InputStream streamToImage, String imageType, ByteArrayOutputStream output) throws IOException {
		byte[] bytes = getScaledImage(newWidth, newHeight, -1, streamToImage, imageType, null);
		if (bytes == null || bytes.length <= 0) {
			return null;
		}

		InputStream input = null;
		try {
			if (output == null) {
				output = new ByteArrayOutputStream();
			}
			input = new ByteArrayInputStream(bytes);
			FileUtil.streamToOutputStream(input, output);
			return output;
		} finally {
			IOUtil.close(input);
		}
	}

	@Override
	public InputStream getScaledImage(int minSize, InputStream streamToImage, String imageType) throws IOException {
		return getStream(getScaledImage(-1, -1, minSize, streamToImage, imageType, null));
	}

	@Override
	public InputStream getScaledImageIfBigger(int newSize, InputStream streamToImage, String imageType) throws IOException {
		return getStream(getScaledImage(-1, -1, newSize, streamToImage, imageType, newSize));
	}

	private InputStream getStream(byte[] bytes) {
		if (bytes == null || bytes.length <= 0) {
			return null;
		}

		return new ByteArrayInputStream(bytes);
	}

	private byte[] getScaledImage(
			int newWidth,
			int newHeight,
			int minSize,
			InputStream streamToImage,
			String imageType,
			Integer scaleIfBiggerThan
	) throws IOException {

		if (streamToImage == null) {
			getLogger().warning("Stream to image is not provided!");
			return null;
		}

		int originalWidth = -1, originalHeight = -1;
		long start = System.currentTimeMillis();
		ByteArrayOutputStream output = null;
		try {
			BufferedImage image = ImageIO.read(streamToImage);

			originalHeight = image.getHeight();
			boolean scale = true;
			if (scaleIfBiggerThan != null && originalHeight <= scaleIfBiggerThan) {
				scale = false;
			}

			BufferedImage scaled = null;
			if (scale) {
				if (newWidth < 0 && newHeight < 0 && minSize > 0) {
					originalWidth = image.getWidth();

					double ratio = minSize * 1.0 / originalWidth;
					newHeight = Double.valueOf(originalHeight * ratio).intValue();
					if (newHeight < minSize) {
						ratio = minSize * 1.0 / originalHeight;
					}

					newHeight = Double.valueOf(originalHeight * ratio).intValue();
					newWidth = Double.valueOf(originalWidth * ratio).intValue();
				}

				IWMainApplicationSettings settings = IWMainApplication.getDefaultIWMainApplication().getSettings();
				List<BufferedImageOp> options = new ArrayList<BufferedImageOp>();
				if (settings.getBoolean("graphics.resizer_antialias", Boolean.TRUE)) {
					options.add(Scalr.OP_ANTIALIAS);
				}
				if (settings.getBoolean("graphics.resizer_brighter", Boolean.TRUE)) {
					options.add(Scalr.OP_BRIGHTER);
				}
				Method scalingMethod = Method.valueOf(settings.getProperty("graphics.resizer_method", Method.SPEED.name()));
				Mode resizeMode = Mode.valueOf(settings.getProperty("graphics.resizer_mode", Mode.AUTOMATIC.name()));
				if (ListUtil.isEmpty(options)) {
					scaled = Scalr.resize(image, scalingMethod, resizeMode, newWidth, newHeight);
				} else {
					BufferedImageOp[] opts = new BufferedImageOp[options.size()];
					{
					int i = 0;
					for(BufferedImageOp opt : options){
						opts[i++] = opt;
					}
					}
					scaled = Scalr.resize(image, scalingMethod, resizeMode, newWidth, newHeight, opts);
				}
			} else {
				scaled = image;
			}

			output = new ByteArrayOutputStream();
			ImageIO.write(scaled, imageType, output);
			return output.toByteArray();
		} finally {
			IOUtil.close(streamToImage);
			IOUtil.close(output);

			long duration = System.currentTimeMillis() - start;
			if (duration > 300) {
				getLogger().info("It took " + duration + " ms to scale image from " + originalWidth + "x" + originalHeight + " to " +
					newWidth + "x" + newHeight);
			}
		}
	}

}