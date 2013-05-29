package com.idega.graphics.image.business.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;

import com.idega.core.business.DefaultSpringBean;
import com.idega.graphics.image.business.ImageResizer;
import com.idega.util.IOUtil;
import com.idega.util.StringUtil;

public class ImageResizerImpl extends DefaultSpringBean implements ImageResizer {

	@Override
	public OutputStream getScaledImage(int newWidth, int newHeight, InputStream streamToImage, String imageType) throws IOException {
		return getScaledImage(newWidth, newHeight, streamToImage, imageType, new ByteArrayOutputStream());
	}

	@Override
	public OutputStream getScaledImage(int newWidth, int newHeight, InputStream streamToImage, String imageType, OutputStream outputStream)
			throws IOException {
		if (newWidth < 0 || newHeight < 0 || streamToImage == null || StringUtil.isEmpty(imageType)) {
			getLogger().warning("Invalid parameters!");
			return null;
		}

		return getScaledImage(newWidth, newHeight, -1, streamToImage, imageType, outputStream);
	}

	private OutputStream getScaledImage(int newWidth, int newHeight, int minSize,
			InputStream streamToImage, String imageType, OutputStream outputStream) throws IOException {

		if (streamToImage == null) {
			getLogger().warning("Stream to image is not provided!");
			return null;
		}

		int originalWidth = -1, originalHeight = -1;
		long start = System.currentTimeMillis();
		try {
			BufferedImage image = ImageIO.read(streamToImage);

			if (newWidth < 0 && newHeight < 0 && minSize > 0) {
				originalWidth = image.getWidth();
				originalHeight = image.getHeight();

				double ratio = minSize * 1.0 / originalWidth;
				newHeight = Double.valueOf(originalHeight * ratio).intValue();
				if (newHeight < minSize) {
					ratio = minSize * 1.0 / originalHeight;
				}

				newHeight = Double.valueOf(originalHeight * ratio).intValue();
				newWidth = Double.valueOf(originalWidth * ratio).intValue();
			}

			BufferedImage scaled = Scalr.resize(image, Method.SPEED, newWidth, newHeight, Scalr.OP_ANTIALIAS, Scalr.OP_BRIGHTER);

			if (outputStream == null) {
				outputStream = new ByteArrayOutputStream();
			}

			ImageIO.write(scaled, imageType, outputStream);
			return outputStream;
		} finally {
			IOUtil.close(streamToImage);

			long duration = System.currentTimeMillis() - start;
			if (duration > 300) {
			getLogger().info("It took " + duration + " ms to scale image from " + originalWidth + "x" + originalHeight + " to " +
					newWidth + "x" + newHeight);
			}
		}
	}

	@Override
	public OutputStream getScaledImage(int minSize, InputStream streamToImage, String imageType) throws IOException {
		return getScaledImage(-1, -1, minSize, streamToImage, imageType, new ByteArrayOutputStream());
	}

}