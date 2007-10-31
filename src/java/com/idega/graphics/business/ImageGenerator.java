package com.idega.graphics.business;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xhtmlrenderer.simple.Graphics2DRenderer;
import org.xhtmlrenderer.swing.Java2DRenderer;
import org.xhtmlrenderer.util.DownscaleQuality;
import org.xhtmlrenderer.util.FSImageWriter;
import org.xhtmlrenderer.util.ImageUtil;
import org.xhtmlrenderer.util.ScalingOptions;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.graphics.image.business.ImageEncoder;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.io.MemoryFileBuffer;
import com.idega.io.MemoryInputStream;
import com.idega.io.MemoryOutputStream;
import com.idega.presentation.IWContext;
import com.idega.slide.business.IWSlideService;
import com.idega.util.CoreConstants;

public class ImageGenerator implements Generator {
	
	private static final Log log = LogFactory.getLog(ImageGenerator.class);
	
	private static final String EXTERNAL_SERVICE = "http://webdesignbook.net/snapper.php?url=";
	private static final String IMAGE_WIDTH_PARAM = "&w=";
	private static final String IMAGE_HEIGHT_PARAM = "&h=";
	private static final String MIME_TYPE = "image/";
	
	private String fileExtension = null;
	
	private boolean isExternalService = false;

	private IWSlideService service = null;
	private ImageEncoder encoder = null;
	
	public ImageGenerator() {
		fileExtension = GraphicsConstants.JPG_FILE_NAME_EXTENSION;
	}
	
	public ImageGenerator(IWContext iwc) {
		this();
		initializeSlideService(iwc);
		initializeImageEncoder(iwc);
	}
	
	/**
	 * Converts ARGB (png) to RGB (jpg, gif)
	 */
	public BufferedImage getConvertedImageFromPNGToJPG(BufferedImage originalImage) {
		if (originalImage == null) {
			return null;
		}
		int w = originalImage.getWidth();
        int h = originalImage.getHeight();
        BufferedImage convertedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = convertedImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.drawRenderedImage(originalImage, null);
        g.dispose();
        return convertedImage;
	}
	
	/**
	 * Sets new quality to image
	 */
	public BufferedImage getImageWithNewQuality(BufferedImage originalImage, float quality, boolean isJpgImage) {
		if (originalImage == null) {
			return null;
		}
		String tempFile = "temp_quality.";
		if (isJpgImage) {
			tempFile = new StringBuffer(tempFile).append(GraphicsConstants.JPG_FILE_NAME_EXTENSION).toString();
		}
		else {
			tempFile = new StringBuffer(tempFile).append(GraphicsConstants.PNG_FILE_NAME_EXTENSION).toString();
		}
		File file = new File(tempFile);
		if (file.exists()) {
			file.delete();
		} else {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		OutputStream fos = null;
		try {
			fos = new BufferedOutputStream(new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		
		FSImageWriter imageWriter = null;
		if (isJpgImage) {
			imageWriter = FSImageWriter.newJpegWriter(quality);
		}
		else {
			imageWriter = new FSImageWriter();
			imageWriter.setWriteCompressionQuality(quality);
		}
		
        try {
			imageWriter.write(originalImage, fos);
			return ImageIO.read(file);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			file.delete();
			closeOutputStream(fos);
		}
	}
	
	/**
	 * Encodes image (from InputStream) and uploads to Slide
	 */
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
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			closeInputStream(stream);
			closeOutputStream(output);
		}
		return result;
	}
	
	/**
	 * Generates preview of provided image (url), sets new quality and scales it to multiple images
	 */
	@SuppressWarnings("unchecked")
	public List<BufferedImage> generatePreviews(String url, List<Dimension> dimensions, boolean isJpg, float quality) {
		if (!isValidString(url) || dimensions == null) {
			return null;
		}
		BufferedImage image = getImage(url, 800, 600, isJpg);
		/*List<BufferedImage> temp = new ArrayList<BufferedImage>();
		temp.add(image);
		temp.add(image);
		return temp;*/
		if (image == null) {
			return null;
		}
		long start = System.currentTimeMillis();

        //	Setting new quality
		if (quality < 1) {
			image = getImageWithNewQuality(image, quality, isJpg);
	        if (image == null) {
	        	return null;
	        }
		}

        //	Scaling image, creating multiple images
        ScalingOptions options = getScalingOptions(isJpg);
        List images = ImageUtil.scaleMultiple(options, image, dimensions);
        if (images == null) {
        	return null;
        }
        List<BufferedImage> allImages = new ArrayList<BufferedImage>(images.size());
        Object o = null;
        for (int i = 0; i < images.size(); i++) {
        	o = images.get(i);
        	if (o instanceof BufferedImage) {
        		allImages.add((BufferedImage) o);
        	}
        }

		isExternalService = false;

		long end = System.currentTimeMillis();
		log.info(new StringBuffer("Got images in ").append((end - start)).append(" ms: ").append(url));

		return allImages;
	}
	
	/**
	 * Generates preview of provided web page
	 */
	public boolean generatePreview(String url, String fileName, String uploadDirectory, int width, int height, boolean encode, boolean makeJpg, float quality) {
		if (!isValidString(url) || !isValidString(fileName) || !isValidString(uploadDirectory) || !isValidInt(width) ||
				!isValidInt(height)) {
			return false;
		}
		
		boolean result = true;
		InputStream stream = getImageInputStream(url, width, height, makeJpg, quality);
		String fullName = new StringBuffer(fileName).append(".").append(getFileExtension()).toString();
		if (stream == null) {
			log.error("Error getting InputStream");
			return false;
		}
		
		if (isExternalService) {
			return uploadImage(uploadDirectory, fullName, stream);
		}
		
		if (encode) {
			result = encodeAndUploadImage(uploadDirectory, fullName, new StringBuffer(MIME_TYPE).append(getFileExtension()).toString(),
					stream, width, height);
		}
		else {
			return uploadImage(uploadDirectory, fullName, stream);
		}

		return result;
	}

	/**
	 * Generates preview of provided web pages
	 */
	public boolean generatePreview(List <String> urls, List <String> names, String uploadDirectory, int width, int height,
			boolean encode, boolean makeJpg, float quality) {
		if (!areValidParameters(urls, names, uploadDirectory, width, height)) {
			return false;
		}
		
		boolean result = true;
		
		for (int i = 0; i < urls.size(); i++) {
			result = generatePreview(urls.get(i), names.get(i), uploadDirectory, width, height, encode, makeJpg, quality);
		}
		return result;
	}
	
	/**
	 * Scales image to provided dimensions
	 */
	public Image getScaledImage(InputStream imageStream, int width, int height, boolean isJpg) {
		if (imageStream == null) {
			return null;
		}
		BufferedImage originalImage = null;
		try {
			originalImage = ImageIO.read(imageStream);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		closeInputStream(imageStream);
		return getScaledImage(originalImage, width, height, isJpg);
	}
	
	/**
	 * Scales image to provided dimensions
	 */
	public Image getScaledImage(BufferedImage originalImage, int width, int height, boolean isJpg) {
		if (originalImage == null) {
			return null;
		}
		ScalingOptions scalingOptions = getScalingOptions(isJpg);
				
		scalingOptions.setTargetDimensions(new Dimension(width, height));
		return ImageUtil.getScaledInstance(scalingOptions, originalImage);
	}
	
	/**
	 * Creates InputStream from BufferedImage
	 */
	public InputStream getImageInputStream(BufferedImage image, String extension) {
		if (image == null || extension == null) {
			return null;
		}
		
		//	Building InputStream
		String fileName = new StringBuffer("temp.").append(extension).toString();
		File imageFile = new File(fileName);
		if (imageFile.exists()) {
			imageFile.delete();
		} else {
			try {
				imageFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		try {
			ImageIO.write(image, extension, imageFile);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		InputStream stream = null;
		try {
			stream = new BufferedInputStream(new FileInputStream(imageFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		imageFile.delete();
		return stream;
	}
	
	/**
	 * Creates InputStream from BufferedImage
	 */
	public InputStream getImageInputStream(Image image, String extension, boolean isJpg) {
		if (image == null || extension == null) {
			return null;
		}
		int type = BufferedImage.TYPE_INT_ARGB;
		if (isJpg) {
			type = BufferedImage.TYPE_INT_RGB;
		}
		return getImageInputStream(ImageUtil.convertToBufferedImage(image, type), extension);
	}

	/**
	 * Generates image with Flying Saucer XHTMLRenderer
	 */
	public BufferedImage generateImage(String urlToFile, int width, int height) {
		return generateImage(urlToFile, width, height, false);
	}
	
	/**
	 * Generates image with Flying Saucer XHTMLRenderer
	 */
	public BufferedImage generateImage(String urlToFile, int width, int height, boolean isJpg) {
		if (urlToFile == null) {
			return null;
		}
		
		long start = System.currentTimeMillis();
		log.info(new StringBuffer("Trying with XHTMLRenderer: ").append(urlToFile));
		String errorMessage = "Unable to generate image with XHTMLRenderer: ";
		
		boolean useOldGenerator = true;
		IWMainApplication app = IWMainApplication.getDefaultIWMainApplication();
		if (app != null) {
			IWMainApplicationSettings settings = app.getSettings();
			if (settings != null) {
				String value = settings.getProperty(CoreConstants.APPLICATION_PROPERTY_TO_USE_OLD_THEME_PREVIEW_GENERATOR);
				if (value == null) {
					settings.setProperty(CoreConstants.APPLICATION_PROPERTY_TO_USE_OLD_THEME_PREVIEW_GENERATOR, Boolean.TRUE.toString());
				}
				else {
					useOldGenerator = Boolean.TRUE.toString().equalsIgnoreCase(value);
				}
			}
		}
		
		BufferedImage image = null;
		if (useOldGenerator) {
			try {
				image = Graphics2DRenderer.renderToImage(urlToFile, width, height);
			} catch (Exception e) {
				log.error(new StringBuffer(errorMessage).append(urlToFile));
				e.printStackTrace();
				return null;
			}
		}
		else {
			Java2DRenderer renderer = new Java2DRenderer(urlToFile, width, height);
			if (isJpg) {
				renderer.setBufferedImageType(BufferedImage.TYPE_INT_RGB);
			}
			else {
				renderer.setBufferedImageType(BufferedImage.TYPE_INT_ARGB);
			}
			try {
				image = renderer.getImage();
			} catch (Exception e) {
				log.error(new StringBuffer(errorMessage).append(urlToFile));
				e.printStackTrace();
				return null;
			}
		}
		
		if (useOldGenerator) {
			setFileExtension(GraphicsConstants.PNG_FILE_NAME_EXTENSION);
		}
		else {
			if (isJpg) {
				setFileExtension(GraphicsConstants.JPG_FILE_NAME_EXTENSION);
			}
			else {
				setFileExtension(GraphicsConstants.PNG_FILE_NAME_EXTENSION);
			}
		}
		
		long end = System.currentTimeMillis();
		log.info(new StringBuffer("XHTMLRenderer: success in ").append((end - start)).append(" ms: ").append(urlToFile));
		
		return image;
	}
	
	/**
	 * Returns URL: a link to service to read generated image
	 */
	public URL generateImageURLWithExternalService(String urlToFile, int width, int height) {
		log.info("Trying with external service: " + urlToFile);
		URL url = null;
		try {
			url = new URL(new StringBuffer(EXTERNAL_SERVICE).append(urlToFile).append(IMAGE_WIDTH_PARAM).append(width)
					.append(IMAGE_HEIGHT_PARAM).append(height).toString());
		}
		catch (MalformedURLException e) {
			log.error("Unable to generate image with external service: " + urlToFile);
			e.printStackTrace();
			return null;
		}
		log.info("External service: success: " + urlToFile);
		return url;
	}

	public String getFileExtension() {
		return fileExtension;
	}
	
	/**
	 * Gets InputStream of generated image
	 * @param urlToFile - where tu find a web file
	 * @param width - image width
	 * @param height - image height
	 * @return InputStream or null if error
	 */
	private InputStream getImageInputStream(String urlToFile, int width, int height, boolean makeJpg, float quality) {
		long start = System.currentTimeMillis();
		
		List<Dimension> dimensions = new ArrayList<Dimension>(1);
		dimensions.add(new Dimension(width, height));
		List<BufferedImage> images = generatePreviews(urlToFile, dimensions, makeJpg, quality);
		if (images == null) {
			return null;
		}
		if (images.size() == 0) {
			return null;
		}
		
		BufferedImage image = images.get(0);
		if (image == null) {
			return null;
		}
		
		InputStream stream = null;
		stream = getImageInputStream(image, getFileExtension());
		if (stream == null) {
			return null;
		}
			
		long end = System.currentTimeMillis();
		log.info(new StringBuffer("Got image InputStream in ").append((end - start)).append(" ms: ").append(urlToFile));
			
		return stream;
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
					e.printStackTrace();
				}
			}
		}
		return service;
	}
	
	private void initializeSlideService(IWContext iwc) {
		synchronized (ImageEncoder.class) {
			try {
				service = (IWSlideService) IBOLookup.getServiceInstance(iwc, IWSlideService.class);
			} catch (IBOLookupException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void initializeImageEncoder(IWContext iwc) {
		synchronized (ImageEncoder.class) {
			try {
				encoder = (ImageEncoder) IBOLookup.getServiceInstance(iwc, ImageEncoder.class);
			} catch (IBOLookupException e) {
				e.printStackTrace();
			}
		}
	}
	
	private ImageEncoder getImageEncoder() {
		if (encoder == null) {
			synchronized (ImageGenerator.class) {
				try {
					encoder = (ImageEncoder) IBOLookup.getServiceInstance(IWContext.getInstance(), ImageEncoder.class);
				} catch (IBOLookupException e) {
					e.printStackTrace();
				}
			}
		}
		return encoder;
	}
	
	private ScalingOptions getScalingOptions(boolean isJpg) {
		DownscaleQuality quality = DownscaleQuality.HIGH_QUALITY;
		if (isJpg) {
			quality = DownscaleQuality.LOW_QUALITY;
		}
		return new ScalingOptions(quality, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
	}

	private BufferedImage getImage(String urlToFile, int width, int height, boolean isJpg) {
		BufferedImage generatedImage = generateImage(urlToFile, width, height, isJpg);
		if (generatedImage == null) {
			//	Failed to generate image, trying with external service
			URL url = generateImageURLWithExternalService(urlToFile, width, height);
			try {
				generatedImage = ImageIO.read(url);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			setFileExtension(GraphicsConstants.JPG_FILE_NAME_EXTENSION);
			isExternalService = true;
		}
		
		return generatedImage;
	}
	
	private boolean uploadImage(String uploadDirectory, String fullName, InputStream stream) {
		boolean result = true;
		try {
			if (!getSlideService().uploadFileAndCreateFoldersFromStringAsRoot(uploadDirectory, fullName, stream,
					new StringBuffer(MIME_TYPE).append(getFileExtension()).toString(), true)) {
				log.error("Error uploading file: " + fullName);
				result = false;
			}
		} catch(RemoteException e) {
			e.printStackTrace();
			return false;
		} finally {
			closeInputStream(stream);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
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
	
	private boolean closeInputStream(InputStream is) {
		if (is == null) {
			log.error("InputStream is null");
			return false;
		}
		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
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
			e.printStackTrace();
			log.error("Unable to close OutputStream");
			return false;
		}
		return true;
	}
}