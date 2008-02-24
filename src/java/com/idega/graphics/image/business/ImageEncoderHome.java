package com.idega.graphics.image.business;


import javax.ejb.CreateException;
import com.idega.business.IBOHome;
import java.rmi.RemoteException;

public interface ImageEncoderHome extends IBOHome {
	public ImageEncoder create() throws CreateException, RemoteException;
}