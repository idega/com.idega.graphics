package com.idega.graphics;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xhtmlrenderer.simple.Graphics2DRenderer;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.graphics.image.business.ImageEncoder;
import com.idega.io.MemoryFileBuffer;
import com.idega.io.MemoryInputStream;
import com.idega.io.MemoryOutputStream;
import com.idega.presentation.IWContext;
import com.idega.slide.business.IWSlideService;

public class ImageGenerator implements Generator {
	
	private static final Log log = LogFactory.getLog(ImageGenerator.class);
	
	private static final String EXTERNAL_SERVICE = "http://webdesignbook.net/snapper.php?url=";
	private static final String IMAGE_WIDTH_PARAM = "&w=";
	private static final String IMAGE_HEIGHT_PARAM = "&h=";
	private static final String EXTERNAL_SERVICE_IMAGE_EXTENSION = "jpg";
	private static final String MIME_TYPE = "image/";
	
	private String fileExtension = null;
	
	private boolean isExternalService = false;

	private IWSlideService service = null;
	private ImageEncoder encoder = null;
	
	public ImageGenerator() {
		fileExtension = "";
	}
	
	public boolean encodeAndUploadImage(String uploadDirectory, String fileName, String mimeType, InputStream stream, int width,
			int height) {
		MemoryFileBuffer buff = new MemoryFileBuffer();
		OutputStream output = new MemoryOutputStream(buff);
		InputStream is = null;
		boolean result = true;
		try {
			getImageEncoder().encode(mimeType, stream, output, width, height);
			is = new MemoryInputStream(buff);
			result = uploadImage(uploadDirectory, fileName, is);
		} catch (RemoteException e) {
			log.error(e);
			return false;
		} catch (IOException e) {
			log.error(e);
			return false;
		} finally {
			closeInputStream(stream);
			closeOutputStream(output);
		}
		return result;
	}
	
	/**
	 * Generates preview of provided web page
	 */
	public boolean generatePreview(String url, String fileName, String uploadDirectory, int width, int height, boolean encode) {
		if (!isValidString(url) || !isValidString(fileName) || !isValidString(uploadDirectory) || !isValidInt(width) ||
				!isValidInt(height)) {
			return false;
		}
		
		boolean result = true;
		InputStream stream = getImageInputStream(url, width, height);
		String fullName = fileName + "." + getFileExtension();
		if (stream == null) {
			log.error("Error getting InputStream");
			return false;
		}
		
		if (isExternalService) {
			return uploadImage(uploadDirectory, fullName, stream);
		}
		
		if (encode) {
			result = encodeAndUploadImage(uploadDirectory, fullName, MIME_TYPE + getFileExtension(), stream, width, height);
		}
		else {
			return uploadImage(uploadDirectory, fullName, stream);
		}

		return result;
	}
	
	private boolean uploadImage(String uploadDirectory, String fullName, InputStream stream) {
		boolean result = true;
		try {
			if (!getSlideService().uploadFileAndCreateFoldersFromStringAsRoot(uploadDirectory, fullName, stream, MIME_TYPE + getFileExtension(), true)) {
				log.error("Error uploading file: " + fullName);
				result = false;
			}
		} catch(RemoteException e) {
			log.error(e);
			return false;
		} finally {
			closeInputStream(stream);
		}
		return result;
	}

	/**
	 * Generates preview of provided web pages
	 */
	public boolean generatePreview(List <String> urls, List <String> names, String uploadDirectory, int width, int height,
			boolean encode) {
		if (!areValidParameters(urls, names, uploadDirectory, width, height)) {
			return false;
		}
		
		boolean result = true;
		
		for (int i = 0; i < urls.size(); i++) {
			result = generatePreview(urls.get(i), names.get(i), uploadDirectory, width, height, encode);
		}
		return result;
	}
	
	private boolean areValidParameters(List urls, List names, String directory, int width, int height) {
		if (urls == null || names == null) {
			return false;
		}
		if (urls.size() != names.size()) {
			return false;
		}
		if (!isValidString(directory)) {
			return false;
		}
		if (!isValidInt(width) || !isValidInt(height)) {
			return false;
		}
		return true;
	}
	
	private boolean isValidInt(int number) {
		if (number > 0 && number <= Integer.MAX_VALUE) {
			return true;
		}
		return false;
	}
	
	private boolean isValidString(String value) {
		if (value == null) {
			return false;
		}
		if ("".equals(value)) {
			return false;
		}
		return true;
	}
	
	/**
	 * Gets InputStream to read image from: firstly tries with XHTMLRenderer if it fails - then external service
	 * @param urlToFile - where tu find a web file
	 * @param width - image width
	 * @param height - image height
	 * @return InputStream
	 */
	private InputStream getImageInputStream(String urlToFile, int width, int height) {
		InputStream stream = null;
		BufferedImage bi = generateImage(urlToFile, width, height);
		if (bi == null) { // Failed to generate image, trying with external service
			URL url = generateImageURLWithExternalService(urlToFile, width, height);
			try {
				stream = new BufferedInputStream(url.openStream());
			} catch (IOException e) {
				log.trace(e);
				log.error("Unable to get InputStream from URL: " + urlToFile);
				return null;
			}
			if (stream == null) {
				return null;
			}
			setFileExtension(EXTERNAL_SERVICE_IMAGE_EXTENSION);
			isExternalService = true;
			return stream;
		}
		else {
			isExternalService = false;
			ByteArrayOutputStream baos = new  ByteArrayOutputStream();
			try {
				ImageIO.write(bi, "png", baos); // FlyingSaucer returns image with type "png"
			} catch (IOException e) {
				log.trace(e);
				log.error("Unable to create InputStream from BufferedImage: " + urlToFile);
				return null;
			}
			stream = new ByteArrayInputStream(baos.toByteArray());
			closeOutputStream(baos);
			setFileExtension("png"); // FlyingSaucer returns image with type "png"
			return stream;
		}
	}
	
	private boolean closeInputStream(InputStream is) {
		if (is == null) {
			log.error("InputStream is null");
			return false;
		}
		try {
			is.close();
		} catch (IOException e) {
			log.trace(e);
			log.error("Unable to close InputStream");
			return false;
		}
		return true;
	}
	
	private boolean closeOutputStream(OutputStream os) {
		if (os == null) {
			log.error("OutputStream is null");
			return false;
		}
		try {
			os.close();
		} catch (IOException e) {
			log.trace(e);
			log.error("Unable to close OutputStream");
			return false;
		}
		return true;
	}
		
	public BufferedImage generateImage(String urlToFile, int width, int height) {
		log.info("Trying with XHTMLRenderer: " + urlToFile);
		BufferedImage bufImg = null;
		try {
			bufImg = Graphics2DRenderer.renderToImage(urlToFile, width, height);
		}
		catch (Exception e) {
			log.error("Unable to generate image with XHTMLRenderer: " + urlToFile);
			log.trace(e);
			return null;
		}
		log.info("XHTMLRenderer: success: " + urlToFile);
		return bufImg;
	}
	
	/**
	 * Returns URL: a link to service to read generated image
	 */
	public URL generateImageURLWithExternalService(String urlToFile, int width, int height) {
		log.info("Trying with external service: " + urlToFile);
		URL url = null;
		try {
			url = new URL(EXTERNAL_SERVICE + urlToFile + IMAGE_WIDTH_PARAM + width + IMAGE_HEIGHT_PARAM + height);
		}
		catch (MalformedURLException e) {
			log.error("Unable to generate image with external service: " + urlToFile);
			log.trace(e);
			return null;
		}
		log.info("External service: success: " + urlToFile);
		return url;
	}

	public String getFileExtension() {
		return fileExtension;
	}

	private void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension.toLowerCase();
	}
	
	private IWSlideService getSlideService() {
		if (service == null) {
			synchronized (ImageGenerator.class) {
				try {
					service = (IWSlideService) IBOLookup.getServiceInstance(IWContext.getInstance(), IWSlideService.class);
				} catch (IBOLookupException e) {
					log.error(e);
				}
			}
		}
		return service;
	}
	
	public ImageEncoder getImageEncoder() {
		if (encoder == null) {
			synchronized (ImageGenerator.class) {
				try {
					encoder = (ImageEncoder) IBOLookup.getServiceInstance(IWContext.getInstance(), ImageEncoder.class);
				} catch (IBOLookupException e) {
					log.error(e);
				}
			}
		}
		return encoder;
	}

}