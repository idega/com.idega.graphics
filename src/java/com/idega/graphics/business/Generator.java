package com.idega.graphics.business;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public interface Generator {

	/**
	 * @see ImageGenerator#generatePreview(String, String, String, int, int)
	 */
	public boolean generatePreview(String urlToFile, String fileName, String uploadDirectory, int width, int height, boolean encode, boolean makeJpg, float quality);
	
	/**
	 * @see ImageGenerator#generatePreview(List, List, String, int, int)
	 */
	public boolean generatePreview(List <String> urls, List <String> names, String uploadDirectory, int width, int height, boolean encode, boolean makeJpg, float quality);
	
	/**
	 * @see ImageGenerator#generateImage(String, int, int)
	 */
	public BufferedImage generateImage(String urlToFile, int width, int height);
	
	/**
	 * @see ImageGenerator#generateImage(String, int, int, boolean)
	 */
	public BufferedImage generateImage(String urlToFile, int width, int height, boolean isJpg);
	
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
	
	/**
	 * @see ImageGenerator#getConvertedImageFromPNGToJPG(BufferedImage)
	 */
	public BufferedImage getConvertedImageFromPNGToJPG(BufferedImage originalImage);
	
	/**
	 * @see ImageGenerator#getImageWithNewQuality(BufferedImage, float, boolean)
	 */
	public BufferedImage getImageWithNewQuality(BufferedImage originalImage, float quality, boolean isJpgImage);
	
	/**
	 * @see ImageGenerator#getScaledImage(InputStream, int, int)
	 */
	public Image getScaledImage(InputStream imageStream, int width, int height, boolean isJpg);
	
	/**
	 * @see ImageGenerator#getScaledImage(BufferedImage, int, int)
	 */
	public Image getScaledImage(BufferedImage originalImage, int width, int height, boolean isJpg);
	
	/**
	 * @see ImageGenerator#getImageInputStream(BufferedImage, String)
	 */
	public InputStream getImageInputStream(BufferedImage image, String extension);
	
	/**
	 * @see ImageGenerator#getImageInputStream(Image, String)
	 */
	public InputStream getImageInputStream(Image image, String extension, boolean isJpg);
	
	/**
	 * @see ImageGenerator#generatePreviews(String, List, boolean, float)
	 */
	public List<BufferedImage> generatePreviews(String url, List<Dimension> dimensions, boolean isJpg, float quality);
}
