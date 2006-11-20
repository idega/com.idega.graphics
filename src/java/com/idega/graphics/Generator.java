package com.idega.graphics;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public interface Generator {

	/**
	 * @see ImageGenerator#generatePreview(String, String, String, int, int)
	 */
	public boolean generatePreview(String urlToFile, String fileName, String uploadDirectory, int width, int height, boolean encode);
	
	/**
	 * @see ImageGenerator#generatePreview(List, List, String, int, int)
	 */
	public boolean generatePreview(List <String> urls, List <String> names, String uploadDirectory, int width, int height, boolean encode);
	
	/**
	 * @see ImageGenerator#generateImage(String, int, int)
	 */
	public BufferedImage generateImage(String urlToFile, int width, int height);
	
	/**
	 * @see ImageGenerator#generateImageURLWithExternalService(String, int, int)
	 */
	public URL generateImageURLWithExternalService(String urlToFile, int width, int height);
	
	/**
	 * @see ImageGenerator#getFileExtension()
	 */
	public String getFileExtension();
	
	/**
	 * @see ImageGenerator#encodeAndUploadImage(String, String, String, InputStream, int, int)
	 */
	public boolean encodeAndUploadImage(String uploadDirectory, String fileName, String mimeType, InputStream stream, int width, int height);
}
