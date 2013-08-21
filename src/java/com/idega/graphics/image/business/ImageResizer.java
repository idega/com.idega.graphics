package com.idega.graphics.image.business;

import java.io.IOException;
import java.io.InputStream;

import com.idega.business.SpringBeanName;

@SpringBeanName("imageResizer")
public interface ImageResizer {

	public InputStream getScaledImage(int newWidth, int newHeight, InputStream streamToImage, String imageType) throws IOException;

	public InputStream getScaledImage(int minSize, InputStream streamToImage, String imageType) throws IOException;

	public InputStream getScaledImageIfBigger(int newSize, InputStream streamToImage, String imageType) throws IOException;

}