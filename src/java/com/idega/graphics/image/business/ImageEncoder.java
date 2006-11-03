package com.idega.graphics.image.business;


public interface ImageEncoder extends com.idega.business.IBOService
{
  
  public static final String UNKNOWN_MIME_TYPE = "unknown mime type";
  
  public static final String INVALID_FILE_EXTENSION = "invalid file extension";
  
  public boolean isInputTypeEqualToResultType(String mimeType);
  
  public String getResultFileExtensionForInputMimeType(String inputMimeType);
  
  public String getResultMimeTypeForInputMimeType(java.lang.String inputMimeType);
  
  public void encode(java.lang.String p0, java.io.InputStream p1,java.io.OutputStream p2, int p3, int p4) throws java.rmi.RemoteException, java.io.IOException;
}
