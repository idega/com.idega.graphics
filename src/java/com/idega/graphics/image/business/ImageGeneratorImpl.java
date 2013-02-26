package com.idega.graphics.image.business;

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
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.jcr.RepositoryException;

import org.springframework.beans.factory.annotation.Autowired;
import org.xhtmlrenderer.simple.Graphics2DRenderer;
import org.xhtmlrenderer.swing.Java2DRenderer;
import org.xhtmlrenderer.util.DownscaleQuality;
import org.xhtmlrenderer.util.FSImageWriter;
import org.xhtmlrenderer.util.ImageUtil;
import org.xhtmlrenderer.util.ScalingOptions;
import org.xhtmlrenderer.util.XRLog;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.builder.data.ICDomain;
import com.idega.graphics.util.GraphicsConstants;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.io.MemoryFileBuffer;
import com.idega.io.MemoryInputStream;
import com.idega.io.MemoryOutputStream;
import com.idega.presentation.IWContext;
import com.idega.repository.RepositoryService;
import com.idega.util.CoreConstants;
import com.idega.util.IOUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

public class ImageGeneratorImpl implements ImageGenerator {

	private static final Logger LOGGER = Logger.getLogger(ImageGeneratorImpl.class.getName());

	private static final String MIME_TYPE = "image/";

	private String fileExtension = null;

	private ImageEncoder encoder = null;
	private Random generator = null;

	@Autowired
	private RepositoryService repository;

	public ImageGeneratorImpl() {
		generator = new Random();

		fileExtension = GraphicsConstants.JPG_FILE_NAME_EXTENSION;
	}

	public ImageGeneratorImpl(IWContext iwc) {
		this();
		initializeImageEncoder(iwc);
	}

	/**
	 * Converts ARGB (png) to RGB (jpg, gif)
	 */
	@Override
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
	@Override
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
			IOUtil.close(fos);
		}
	}

	/**
	 * Encodes image (from InputStream) and uploads to repository
	 */
	@Override
	public boolean encodeAndUploadImage(String uploadDirectory, String fileName, String mimeType, InputStream stream, int width, int height) {
		//TODO use new JAI methods
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
			IOUtil.close(stream);
			IOUtil.close(output);
		}
		return result;
	}

	/**
	 * Generates preview of provided image (url), sets new quality and scales it to multiple images
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<BufferedImage> generatePreviews(String url, List<Dimension> dimensions, boolean isJpg, float quality) {
		if (StringUtil.isEmpty(url) || dimensions == null) {
			return null;
		}

		BufferedImage image = getImage(url, GraphicsConstants.GENERATED_IMAGE_HEIGHT, GraphicsConstants.GENERATED_IMAGE_WIDTH, isJpg);	//	"View" to image
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
        List<Object> images = ImageUtil.scaleMultiple(options, image, dimensions);	//	Scaling generated image to other sizes
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

		long end = System.currentTimeMillis();
		LOGGER.info(new StringBuffer("Got images in ").append((end - start)).append(" ms: ").append(url).toString());

		return allImages;
	}

	/**
	 * Generates preview of provided web page
	 */
	@Override
	public boolean generatePreview(String url, String fileName, String uploadDirectory, int width, int height, boolean encode, boolean makeJpg, float quality) {
		if (StringUtil.isEmpty(url) || StringUtil.isEmpty(fileName) || StringUtil.isEmpty(uploadDirectory) || !isValidInt(width) ||
				!isValidInt(height)) {
			return false;
		}

		boolean result = true;
		InputStream stream = getImageInputStream(url, width, height, makeJpg, quality);
		String fullName = new StringBuffer(fileName).append(".").append(getFileExtension()).toString();
		if (stream == null) {
			LOGGER.warning("Error getting InputStream to " + url);
			return false;
		}

		if (encode) {
			result = encodeAndUploadImage(uploadDirectory, fullName, new StringBuffer(MIME_TYPE).append(getFileExtension()).toString(),
					stream, width, height);
		} else {
			return uploadImage(uploadDirectory, fullName, stream);
		}

		return result;
	}

	/**
	 * Generates preview of provided web pages
	 */
	@Override
	public boolean generatePreview(List<String> urls, List<String> names, String uploadDirectory, int width, int height,
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
	@Override
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
		IOUtil.close(imageStream);
		return getScaledImage(originalImage, width, height, isJpg);
	}

	/**
	 * Scales image to provided dimensions
	 */
	@Override
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
	@Override
	public InputStream getImageInputStream(BufferedImage image, String extension) {
		if (image == null || extension == null) {
			return null;
		}

		//	Building InputStream
		String fileName = new StringBuffer("temp_").append(generator.nextInt(Integer.MAX_VALUE)).append(CoreConstants.DOT).append(extension).toString();
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
	@Override
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
	@Override
	public BufferedImage generateImage(String urlToFile, int width, int height) {
		return generateImage(urlToFile, width, height, false);
	}

	/**
	 * Generates image with Flying Saucer XHTMLRenderer
	 */
	@Override
	public BufferedImage generateImage(String urlToFile, int width, int height, boolean isJpg) {
		if (urlToFile == null) {
			return null;
		}
		if (!urlToFile.startsWith("http")) {
			ICDomain domain = IWMainApplication.getDefaultIWApplicationContext().getDomain();
			String url = domain == null ? null : domain.getURL();
			if (!StringUtil.isEmpty(url)) {
				if (url.endsWith(CoreConstants.SLASH))
					url = url.substring(0, url.length() - 1);
				urlToFile = url + urlToFile;
			}
		}

		long start = System.currentTimeMillis();
		LOGGER.info(new StringBuffer("Trying with XHTMLRenderer: ").append(urlToFile).toString());
		String errorMessage = "Unable to generate image with XHTMLRenderer: ";

		IWMainApplicationSettings settings = IWMainApplication.getDefaultIWMainApplication().getSettings();
		boolean useOldGenerator = settings.getBoolean(CoreConstants.APPLICATION_PROPERTY_TO_USE_OLD_THEME_PREVIEW_GENERATOR, Boolean.FALSE);

		XRLog.setLoggingEnabled(true);
		XRLog.setLevel(XRLog.EXCEPTION, Level.WARNING);
		BufferedImage image = null;
		if (useOldGenerator) {
			try {
				image = Graphics2DRenderer.renderToImage(urlToFile, width, height);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, errorMessage.concat(urlToFile), e);
				return null;
			}
		} else {
			Java2DRenderer renderer = new Java2DRenderer(urlToFile, width, height);
			renderer.setBufferedImageType(isJpg ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
			try {
				image = renderer.getImage();
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, errorMessage.concat(urlToFile), e);

				try {
					// This is only for the cases when XHTML renderer cannot
					// render the template icon. Fallback to unknown picture
					// thus making the theme still usable.
					image = ImageIO.read(Thread.currentThread()
						.getContextClassLoader()
						.getResourceAsStream("resources/unknown.png"));
				} catch (Exception inner) {
					throw new RuntimeException(inner);
				}
			}
		}

		setFileExtension(useOldGenerator ? GraphicsConstants.PNG_FILE_NAME_EXTENSION :
								   isJpg ? GraphicsConstants.JPG_FILE_NAME_EXTENSION : GraphicsConstants.PNG_FILE_NAME_EXTENSION);

		long end = System.currentTimeMillis();
		LOGGER.info(new StringBuffer("XHTMLRenderer: success in ").append((end - start)).append(" ms: ").append(urlToFile).toString());

		return image;
	}

	@Override
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
		LOGGER.info(new StringBuffer("Got image InputStream in ").append((end - start)).append(" ms: ").append(urlToFile).toString());

		return stream;
	}

	private void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension.toLowerCase();
	}

	private synchronized void initializeImageEncoder(IWApplicationContext iwac) {
		if (encoder == null) {
			try {
				encoder = IBOLookup.getServiceInstance(iwac, ImageEncoder.class);
			} catch (IBOLookupException e) {
				e.printStackTrace();
			}
		}
	}

	private ImageEncoder getImageEncoder() {
		initializeImageEncoder(IWMainApplication.getDefaultIWApplicationContext());
		return encoder;
	}

	private ScalingOptions getScalingOptions(boolean isJpg) {
		DownscaleQuality quality = DownscaleQuality.LOW_QUALITY;
		if (isJpg) {
			quality = DownscaleQuality.HIGH_QUALITY;
		}
		return new ScalingOptions(quality, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
	}

	private BufferedImage getImage(String urlToFile, int width, int height, boolean isJpg) {
		BufferedImage generatedImage = generateImage(urlToFile, width, height, isJpg);
		if (generatedImage == null)
			LOGGER.warning("Failed to generate image for " + urlToFile);

		return generatedImage;
	}

	private RepositoryService getRepositoryService() {
		if (repository == null) {
			ELUtil.getInstance().autowire(this);
		}
		return repository;
	}

	private boolean uploadImage(String uploadDirectory, String fullName, InputStream stream) {
		boolean result = true;
		try {
			if (!getRepositoryService().uploadFileAndCreateFoldersFromStringAsRoot(uploadDirectory, fullName, stream,
					new StringBuffer(MIME_TYPE).append(getFileExtension()).toString())) {
				LOGGER.warning("Error uploading file: ".concat(fullName));
				result = false;
			}
		} catch(RepositoryException e) {
			e.printStackTrace();
			return false;
		} finally {
			IOUtil.close(stream);
		}
		return result;
	}

	private boolean areValidParameters(List<String> urls, List<String> names, String directory, int width, int height) {
		if (urls == null || names == null) {
			return false;
		}
		if (urls.size() != names.size()) {
			return false;
		}
		if (StringUtil.isEmpty(directory)) {
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
}