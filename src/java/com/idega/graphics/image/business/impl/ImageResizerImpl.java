package com.idega.graphics.image.business.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;

import com.idega.core.business.DefaultSpringBean;
import com.idega.graphics.image.business.ImageResizer;
import com.idega.util.IOUtil;
import com.idega.util.StringUtil;

public class ImageResizerImpl extends DefaultSpringBean implements ImageResizer {

	@Override
	public InputStream getScaledImage(int newWidth, int newHeight, InputStream streamToImage, String imageType) throws IOException {
		if (newWidth < 0 || newHeight < 0 || streamToImage == null || StringUtil.isEmpty(imageType)) {
			getLogger().warning("Invalid parameters!");
			return null;
		}

		return getScaledImage(newWidth, newHeight, -1, streamToImage, imageType, null);
	}

	@Override
	public InputStream getScaledImage(int minSize, InputStream streamToImage, String imageType) throws IOException {
		return getScaledImage(-1, -1, minSize, streamToImage, imageType, null);
	}

	@Override
	public InputStream getScaledImageIfBigger(int newSize, InputStream streamToImage, String imageType) throws IOException {
		return getScaledImage(-1, -1, newSize, streamToImage, imageType, newSize);
	}

	private InputStream getScaledImage(
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

				scaled = Scalr.resize(image, Method.SPEED, newWidth, newHeight, Scalr.OP_ANTIALIAS, Scalr.OP_BRIGHTER);
			} else {
				scaled = image;
			}

			output = new ByteArrayOutputStream();
			ImageIO.write(scaled, imageType, output);
			return new ByteArrayInputStream(output.toByteArray());
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