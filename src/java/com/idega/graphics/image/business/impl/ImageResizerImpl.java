package com.idega.graphics.image.business.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import com.idega.core.business.DefaultSpringBean;
import com.idega.graphics.image.business.ImageResizer;
import com.mortennobel.imagescaling.ResampleOp;

public class ImageResizerImpl extends DefaultSpringBean implements ImageResizer {

	@Override
	public OutputStream getScaledImage(int newWidth, int newHeight, InputStream streamToImage, String imageType) throws IOException {
		if (newWidth < 0 || newHeight < 0 || streamToImage == null) {
			getLogger().warning("Invalid parameters!");
			return null;
		}

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		BufferedImage sourceImage = ImageIO.read(streamToImage);
		ResampleOp resizeOp = new ResampleOp(newWidth, newHeight);
		BufferedImage resizedImage = resizeOp.filter(sourceImage, null);
		ImageIO.write(resizedImage, imageType, output);
		return output;
	}

}