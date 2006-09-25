package com.idega.graphics;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;

public interface PreviewGenerator {

	/**
	 * @see WebPagePreviewGenerator#generatePreview(String, String, String, int, int)
	 */
	public boolean generatePreview(String urlToFile, String fileName, String uploadDirectory, int width, int height);
	
	/**
	 * @see WebPagePreviewGenerator#generatePreview(List, List, String, int, int)
	 */
	public boolean generatePreview(List <String> urls, List <String> names, String uploadDirectory, int width, int height);
	
	/**
	 * @see WebPagePreviewGenerator#generateImage(String, int, int)
	 */
	public BufferedImage generateImage(String urlToFile, int width, int height);
	
	/**
	 * @see WebPagePreviewGenerator#parseHTML(String)
	 */
	public boolean parseHTML(String urlToFile);
	
	/**
	 * @see WebPagePreviewGenerator#generateImageURLWithExternalService(String, int, int)
	 */
	public URL generateImageURLWithExternalService(String urlToFile, int width, int height);
}
