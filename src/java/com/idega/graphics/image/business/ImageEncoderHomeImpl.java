package com.idega.graphics.image.business;


import javax.ejb.CreateException;
import com.idega.business.IBOHomeImpl;

public class ImageEncoderHomeImpl extends IBOHomeImpl implements
		ImageEncoderHome {
	public Class getBeanInterfaceClass() {
		return ImageEncoder.class;
	}

	public ImageEncoder create() throws CreateException {
		return (ImageEncoder) super.createIBO();
	}
}