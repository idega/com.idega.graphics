package com.idega.graphics.image.business;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

import javax.media.jai.PlanarImage;

import com.idega.business.IBOService;

public interface ImageEncoder extends IBOService {
	
  
	/**
	 * @see com.idega.graphics.image.business.ImageEncoderBean#encode
	 */
	public void encode(String mimeType, InputStream input, OutputStream output,
			int width, int heigth) throws IOException, RemoteException;

	/**
	 * @see com.idega.graphics.image.business.ImageEncoderBean#isInputTypeEqualToResultType
	 */
	public boolean isInputTypeEqualToResultType(String mimeType)
			throws RemoteException;

	/**
	 * @see com.idega.graphics.image.business.ImageEncoderBean#getResultMimeTypeForInputMimeType
	 */
	public String getResultMimeTypeForInputMimeType(String inputMimeType)
			throws RemoteException;

	/**
	 * @see com.idega.graphics.image.business.ImageEncoderBean#getResultFileExtensionForInputMimeType
	 */
	public String getResultFileExtensionForInputMimeType(String inputMimeType)
			throws RemoteException;

	/**
	 * @see com.idega.graphics.image.business.ImageEncoderBean#scale
	 */
	public PlanarImage scale(PlanarImage image, float scale)
			throws RemoteException;

	/**
	 * @see com.idega.graphics.image.business.ImageEncoderBean#colorToGray
	 */
	public PlanarImage colorToGray(PlanarImage i) throws RemoteException;

	/**
	 * @see com.idega.graphics.image.business.ImageEncoderBean#thresholding
	 */
	public PlanarImage thresholding(PlanarImage i) throws RemoteException;

	/**
	 * @see com.idega.graphics.image.business.ImageEncoderBean#dilation
	 */
	public PlanarImage dilation(PlanarImage i) throws RemoteException;

	/**
	 * @see com.idega.graphics.image.business.ImageEncoderBean#writeToFile
	 */
	public void writeToFile(PlanarImage i, String filename)
			throws RemoteException;

	/**
	 * @see com.idega.graphics.image.business.ImageEncoderBean#getCroppedImage
	 */
	public PlanarImage crop(PlanarImage image, float topX,
			float topY, float width, float height) throws RemoteException;

	/**
	 * @see com.idega.graphics.image.business.ImageEncoderBean#encodePlanarImageToInputStream
	 */
	public InputStream encodePlanarImageToInputStream(PlanarImage image,
			String imageType) throws RemoteException;

	/**
	 * @see com.idega.graphics.image.business.ImageEncoderBean#getPlanarImage
	 */
	public PlanarImage getPlanarImage(String URL) throws MalformedURLException,
			RemoteException;
}