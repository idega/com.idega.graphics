package com.idega.graphics.image.business;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.idega.business.SpringBeanName;

@SpringBeanName("imageResizer")
public interface ImageResizer {

	public OutputStream getScaledImage(int newWidth, int newHeight, InputStream streamToImage, String imageType) throws IOException;
	
	public OutputStream getScaledImage(int newWidth, int newHeight, InputStream streamToImage, String imageType,OutputStream outputStream) throws IOException;

}