package com.idega.graphics.image.business.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import com.idega.core.business.DefaultSpringBean;
import com.idega.graphics.image.business.ImageResizer;
import com.idega.util.IOUtil;
import com.idega.util.StringUtil;
import com.mortennobel.imagescaling.ResampleOp;

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

		try {
			BufferedImage sourceImage = ImageIO.read(streamToImage);
			return getScaledImage(newWidth, newHeight, sourceImage, imageType, outputStream);
		} finally {
			IOUtil.close(streamToImage);
		}
	}

	private OutputStream getScaledImage(int newWidth, int newHeight, BufferedImage sourceImage, String imageType, OutputStream output) throws IOException {
		ResampleOp resizeOp = new ResampleOp(newWidth, newHeight);
		BufferedImage resizedImage = resizeOp.filter(sourceImage, null);
		ImageIO.write(resizedImage, imageType, output);
		return output;
	}

	@Override
	public OutputStream getScaledImage(int minSize, InputStream streamToImage, String imageType) throws IOException {
		BufferedImage sourceImage = ImageIO.read(streamToImage);
		int width = sourceImage.getWidth();
		int height = sourceImage.getHeight();

		double ratio = minSize * 1.0 / width;
		double newHeight = height * ratio;
		if (newHeight < minSize) {
			ratio = minSize * 1.0 / height;
		}

		newHeight = height * ratio;
		double newWidth = width * ratio;
		try {
			return getScaledImage(Double.valueOf(newWidth).intValue(), Double.valueOf(newHeight).intValue(), sourceImage, imageType,
					new ByteArrayOutputStream());
		} finally {
			IOUtil.close(streamToImage);
		}
	}

}