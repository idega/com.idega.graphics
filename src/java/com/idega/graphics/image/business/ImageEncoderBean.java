package com.idega.graphics.image.business;

import java.awt.Canvas;
import java.awt.Image;
import java.awt.image.ImageProducer;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.PlanarImage;

import com.idega.business.IBOServiceBean;
import com.idega.graphics.encoder.gif.Gif89Encoder;
import com.idega.io.MemoryFileBuffer;
import com.idega.io.MemoryInputStream;
import com.idega.io.MemoryOutputStream;
import com.sun.jimi.core.Jimi;
import com.sun.jimi.core.JimiException;
import com.sun.jimi.core.JimiReader;
import com.sun.jimi.core.options.GIFOptions;
import com.sun.jimi.core.raster.JimiRasterImage;
import com.sun.media.jai.codec.BMPEncodeParam;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncodeParam;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codec.MemoryCacheSeekableStream;
import com.sun.media.jai.codec.PNGEncodeParam;
import com.sun.media.jai.codec.PNMEncodeParam;
import com.sun.media.jai.codec.TIFFEncodeParam;


/*
  public static void main(String[] args) {
    //Collection coll = Arrays.asList(RegistryMode.getModeNames());
    Collection coll = Arrays.asList(JAI.getDefaultInstance().getOperationRegistry().getDescriptorNames("rendered"));
    Iterator iterator = coll.iterator();
    while (iterator.hasNext())  {
      System.out.println((String)iterator.next());
    }
  }



/**
 *  * 
 * 
 * Title:         idegaWeb
 * Description:   This class provides methodes to encode images.
 *                  
 * Copyright:     Copyright (c) 2003
 * Company:       idega software
 * @author <a href="mailto:thomas@idega.is">Thomas Hilbig</a>
 * @version 1.0
 */
 
 
public class ImageEncoderBean extends IBOServiceBean implements com.idega.graphics.image.business.ImageEncoder {

	private static final long serialVersionUID = 4568985884351622530L;

// for gif images we have a special encoder  
  public final static String GIF = "gif";
  public final static String PNM = "pnm";
  public final static String PNG = "png";
  public final static String TIFF = "tiff";
  public final static String BMP = "bmp";
  public final static String JPEG = "jpeg";
  
  public static final String UNKNOWN_MIME_TYPE = "unknown mime type";
  public static final String INVALID_FILE_EXTENSION = "invalid file extension";
 
 
  public static final String[] FILE_EXTENSIONS = {
    GIF, "gif",
    PNG, "png",
    JPEG, "jpg",
    TIFF, "tif",
    BMP, "bmp",
    PNM, "pnm"
  };
  
  public static final String[] MIME_TYPES_FOR_JAI = {
    PNG, PNM, JPEG, TIFF, BMP };
  
  private static final String[] IMAGES_TYPES = {
    // description, 
    // mime type, mimetype, converted to mimetype
    "GIF",
    "image/gif",  GIF,  GIF,  
//    "X-Windows bitmap (b/w)  xbm",
//        "image/x-xbitmap",  "xbm",  "",    
//    "X-Windows pixelmap (8-bit color)  xpm",
//  "image/x-xpix",     "xpm",      "",     
    "Portable Network Graphics png",
    "image/x-png", PNG,  PNG, 
    "Portable Network Graphics png",
    "image/png",   PNG,  PNG, 
//    "Image Exchange Format (RFC 1314) ief",
//    "image/ief",        "ief",      "",     "", "",
    "JPEG  jpeg jpg jpe pjpeg",
    "image/jpeg",  JPEG, JPEG, 
    "JPEG  jpeg jpg jpe pjpeg",
    "image/pjpeg", JPEG, JPEG, 
    "JPEG  jpeg jpg jpe pjpeg",
    "image/jpg",   JPEG, JPEG, 
    "JPEG  jpeg jpg jpe pjpeg",
    "image/jpe",   JPEG, JPEG,
    "TIFF  tiff tif",
    "image/tiff",  TIFF, TIFF,  
  //  "Macintosh PICT format pict",
  //  "image/x-pict"
  //  "Macintosh PICT format pict",
  //  "image/pict",
    "Microsoft Windows bitmap  bmp",
    "image/x-ms-bmp", BMP,  JPEG,
    "Microsoft Windows bitmap  bmp",
    "image/bmp",      BMP,  JPEG, 
    "Microsoft Windows bitmap  bmp",
    "image/x-bmp",    BMP,  JPEG
  //  "pcx image", "image/pcx",
  //  "iff image", "image/iff",
  //  "ras image", "image/ras",
  //  "portable-bitmap image", "image/x-portable-bitmap",
  //  "portable-graymap image", "image/x-portable-graymap",
  //  "portable-pixmap image", "image/x-portable-pixmap"
   };
  
 
  //1.0 best quality 0.75 high quality 0.5  medium quality  0.25 low quality 0.10 crappy quality 
  private final static float JPEG_QUALITY = 1.00f;

  
   
  private Map mimeTypes;  
  private Map extensionTypes;
  private Map mimeTypesForJai;
 
  
  public ImageEncoderBean() {
    this.extensionTypes = initializeFileExtension();
    this.mimeTypes = initializeMimeTypes();    
    this.mimeTypesForJai = initializeJaiMimeValues();
  } 


  private HashMap initializeJaiMimeValues() {
    HashMap <String, String> mimeTypeForJai = new HashMap<String, String>();
    
    ArrayList <String> list = new ArrayList<String>();
    Enumeration enumer = ImageCodec.getCodecs();
    while (enumer.hasMoreElements())   {
      ImageCodec codec = (ImageCodec) enumer.nextElement();
      list.add(codec.getFormatName());
    }
    int i;
    String mimeTypeKey;
    String jaiMimeType;
    for (i= 0; i < MIME_TYPES_FOR_JAI.length; i++)  {
      mimeTypeKey = MIME_TYPES_FOR_JAI[i];
      Iterator iterator = list.iterator();
      while (iterator.hasNext())  {
        jaiMimeType = (String) iterator.next();
        if (jaiMimeType.indexOf(mimeTypeKey) > -1)  {
          mimeTypeForJai.put(mimeTypeKey, jaiMimeType);
          break;
        }
      }
    }
    return mimeTypeForJai;
  }   
        
        

  private Map initializeMimeTypes() {
    int size = IMAGES_TYPES.length;
    Map <String, String[]> map = new HashMap<String, String[]>(size);
    int i = 0;
    while (i < size)  {
      i++; // skip description
      int key = i++;  // get mime type as key
      String[] shortArray = new String[2];
      shortArray[0] = IMAGES_TYPES[i++];  // get mime type for this class
      shortArray[1] = IMAGES_TYPES[i++];  // get converted to this mime type
      map.put(IMAGES_TYPES[key], shortArray);
    }
    return map;
  }
    

  private Map initializeFileExtension() {
    int size = FILE_EXTENSIONS.length;
    Map <String, String> map = new HashMap<String, String>(size);
    int i = 0;
    while (i < size)  {
      map.put(FILE_EXTENSIONS[i++],FILE_EXTENSIONS[i++]);
    }
    return map;
  }

    

/**
 * @deprecated Use JAI methods instead like encodePlanarImageToInputStream using getPlanarImage(url) first
 */
  @Deprecated
	public void encode(String mimeType, InputStream input, OutputStream output, int width, int heigth) throws IOException {
    
    String resultMime = getResultMimeTypeForInputMimeType(mimeType);
    String formatedInputMime = getFormatedMimeType(mimeType);
  
    if (UNKNOWN_MIME_TYPE.equals(resultMime) || UNKNOWN_MIME_TYPE.equals(formatedInputMime)) {
			throw new IOException("Mime type "+ mimeType + " is not recognized");
		}
		else if (GIF.equals(formatedInputMime)) {
			handleSpecialMimeTypGIF(input, output, width, heigth);
		}
		else {
			handleMimeType(formatedInputMime, resultMime, input, output, width, heigth);
		} 
    output.flush();
  }
 

  /**
  * Method getFormatedMimeType.
  * @param mimeType
  * @return String
  */
  private String getFormatedMimeType(String mimeType) {
    return getValueForMimeType(mimeType, 0, UNKNOWN_MIME_TYPE );
 }
 

  public boolean isInputTypeEqualToResultType(String mimeType)  {
    String formatedMimeType = getFormatedMimeType(mimeType);
    if (UNKNOWN_MIME_TYPE.equals(formatedMimeType)) {
			return false;
		}
    return formatedMimeType.equals(getResultMimeTypeForInputMimeType(mimeType));
  }

  public String getResultMimeTypeForInputMimeType(String inputMimeType) {
    return getValueForMimeType(inputMimeType, 1, UNKNOWN_MIME_TYPE );
 }
    
  public String getResultFileExtensionForInputMimeType(String inputMimeType)  {
    return getExtensionForFormatedMimeType( getResultMimeTypeForInputMimeType(inputMimeType));
 }

    
  private String getValueForMimeType(String mimeType, int index, String errorString) {
    String[] shortArray = (String[]) this.mimeTypes.get(mimeType);
    if (shortArray == null) {
			return errorString;
		}
    return shortArray[index];  
  }        
    
  private String getExtensionForFormatedMimeType(String formatedMimeType) {
    String extension = (String)this.extensionTypes.get(formatedMimeType);  
    if (extension == null) {
			return INVALID_FILE_EXTENSION;
		}    
    return extension;
  }


  private String getMimeTypeForJai(String formatedMimeType) {
    String result = (String) this.mimeTypesForJai.get(formatedMimeType);
    if (result == null) {
			return UNKNOWN_MIME_TYPE;
		}
    return result;
  }


  public static void main(String[] args) {
    //Collection coll = Arrays.asList(RegistryMode.getModeNames());
    //Collection coll = Arrays.asList(JAI.getDefaultInstance().getOperationRegistry().getDescriptorNames("rendered"));
    Enumeration enumer = ImageCodec.getCodecs();
    while (enumer.hasMoreElements())   {
      ImageCodec codec = (ImageCodec) enumer.nextElement();
      String name = codec.getFormatName();
      System.out.println(name);
    }
  }




	/**
	 * Creates new image with the desired width and height and encodes it into a image using the 
   * passed mime type. Writes it to the passed output stream.   
   * The input stream should be a image with the same mimetype. 
   * The implementation uses the JAI library.
	 * @param mimeType
   * @param outputMimeType
	 * @param input
	 * @param output
	 * @param width
	 * @param heigth
	 */
	private void handleMimeType  (String inputMimeType,
    String outputMimeType,
		InputStream input,
		OutputStream output,
		int width,
		int heigth) throws IOException {
  
  
    PlanarImage image = JAI.create("stream",new MemoryCacheSeekableStream(new BufferedInputStream(input)));    
    // scale image
    PlanarImage modifiedImage = createImageWithSize(image,width, heigth);
    
    ImageEncodeParam encodeParam = getEncoderParam(inputMimeType, outputMimeType, image);
    
    com.sun.media.jai.codec.ImageEncoder imageEncoder;
    
    String jaiMimeType = getMimeTypeForJai(outputMimeType);
    if (UNKNOWN_MIME_TYPE.equals(jaiMimeType)) {
			throw new IOException("Mime type "+ outputMimeType + " not recognized by JAI");
		}

    imageEncoder = ImageCodec.createImageEncoder(jaiMimeType , output, encodeParam);
    imageEncoder.encode(modifiedImage);
  }


  private PlanarImage createImageWithSize(PlanarImage originalImage, int width, int height) {
    ParameterBlock pb = new ParameterBlock();
    pb.addSource(originalImage);
    //WTF??
    pb.add(null).add(null).add(null).add(null).add(null);
    RenderableImage ren = JAI.createRenderable("renderable", pb);
    return (PlanarImage) ren.createScaledRendering(width, height, null);
  }



	/**
	 * Creates new image with the desired width and height and encodes it into a GIF image and writes 
   * it to the passed output stream. The input stream must be a GIF image. 
   * Animation and transparency are preserved.
   * The implementation uses the Jimi library and the jmge library.
	 * @param input
	 * @param output
	 * @param width
   * @param heigth
	 */
	private void handleSpecialMimeTypGIF(InputStream input, OutputStream output, int width, int height) throws IOException {
    
    // open the animated picture with a JimiReader for control of individual frames
    JimiReader reader;
    try {
      reader = Jimi.createJimiReader(input);
    }
    catch (JimiException jimiException) {
      System.err.println("Error while reading source " + jimiException.getMessage());
      return;
    }
    

    Gif89Encoder gifenc = new Gif89Encoder();
    Canvas cv = new Canvas();
    Enumeration enumer = reader.getRasterImageEnumeration();
    
    JimiRasterImage rasterImage;
    ImageProducer imageProducer;
    Image image;
    Image scaledImage;
    GIFOptions options;
    int loops = 1;
    int delay = 1;
    boolean notSetYet = true;
    
    while (enumer.hasMoreElements())  {
      rasterImage = (JimiRasterImage) enumer.nextElement();
      imageProducer = rasterImage.getImageProducer();
      image = cv.createImage(imageProducer);
      scaledImage = image.getScaledInstance(width,height,Image.SCALE_DEFAULT);
      gifenc.addFrame(scaledImage);
      
      if (notSetYet)  {
        options = (GIFOptions) rasterImage.getOptions();
        loops = options.getNumberOfLoops();
        delay = options.getFrameDelay();
        notSetYet = false;
      }
    }
    gifenc.setLoopCount(loops);
    gifenc.setUniformDelay(delay);
    gifenc.encode(output);   
  }

  
  private ImageEncodeParam getEncoderParam(String inputMimeType, String outputMimeType, PlanarImage image)  {
    
    ImageEncodeParam para = null; 
    
    if (JPEG.equals(outputMimeType))  {
      para = new JPEGEncodeParam();

    // if the image is converted to jpeg set quality      
      if (! JPEG.equals(inputMimeType)) {
				((JPEGEncodeParam) para).setQuality(JPEG_QUALITY);
			}
      
    }
    
    else if (PNG.equals(outputMimeType)) {
			para = PNGEncodeParam.getDefaultEncodeParam(image);
			((PNGEncodeParam)para).setBitDepth(16);
			//((PNGEncodeParam)para).setBitDepth(24);
		}
		else if (PNM.equals(outputMimeType)) {
			para = new PNMEncodeParam();
		}
		else if (TIFF.equals(outputMimeType)) {
			para = new TIFFEncodeParam();
		}
		else if (BMP.equals(outputMimeType)) {
			para = new BMPEncodeParam();
		}
  
    return para;
  }
  
  
  
  /**
   * JAI (java advanced image) operations. Some useful other just experimental
   */
	public PlanarImage scale(PlanarImage image, float scale) {
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(image);
		pb.add(scale);
		pb.add(scale);
		pb.add(0.0F);
		pb.add(0.0F);
		pb.add(new InterpolationNearest());
		return JAI.create("scale", pb, null);
	}

	public PlanarImage colorToGray(PlanarImage i) {
		double[][] matrix = { {0.114D, 0.587D, 0.299D, 0.0D} };
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(i);
		pb.add(matrix);
		return JAI.create("bandcombine", pb, null);
	}

	/*private static PlanarImage edging(PlanarImage i) {
		return  JAI.create("gradientmagnitude", i,KernelJAI.GRADIENT_MASK_SOBEL_HORIZONTAL, KernelJAI.GRADIENT_MASK_SOBEL_VERTICAL);
	}*/
	
	public PlanarImage thresholding(PlanarImage i) {
		//175 ve altini 0'a esle
		double low[] = { 0d };
		double high[] = { 175d };
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(i);
		pb.add(low);
		pb.add(high);
		pb.add(low);
		return JAI.create("threshold", pb);

	}
	
	public PlanarImage dilation(PlanarImage i) {
		float kernelMatrix[] =
		{
			50,50,50,
			50, 0, 0,
			50, 0, 0 
		};
		KernelJAI kernel = new KernelJAI(3,3,kernelMatrix);
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(i);
		pb.add(kernel);
		return JAI.create("Dilate", pb);
	}
	
	public void writeToFile(PlanarImage i,String filename) {
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(i);
		pb.add(filename);
		pb.add("jpeg");
		JAI.create("filestore", pb);
	}  
  
	/**
	 * Crops the image and fixes the banding issue by translating it into it self.
	 * @param image
	 * @param topX
	 * @param topY
	 * @param width
	 * @param height
	 * @return
	 */
	public PlanarImage crop(PlanarImage image,float topX,float topY, float width, float height){
		// Create a ParameterBlock with information for the cropping.
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(image);
		pb.add(topX);
		pb.add(topY);
		pb.add(width);
		pb.add(height);
		
		// Create the output image by cropping the input image.
		PlanarImage cropped = JAI.create("crop",pb,null);
		
	    // A cropped image will have its origin set to the (x,y) coordinates,
	    // and with the display method we use it will cause bands on the top
	    // and left borders. A simple way to solve this is to shift or
	    // translate the image by (-x,-y) pixels.
	    pb = new ParameterBlock();
	    pb.addSource(cropped);
	    pb.add(-topX);
	    pb.add(-topY);
	    // Create the output image by translating itself.
	    return JAI.create("translate",pb,null);
	}

  /**
   * Uses a MemoryFileBuffer to encode the file into an outputstream and then uses the buffer for that stream to create an MemoryInputStream
   * @param imageType valid JAI image type "JPEG","BMP","PNG","TIFF"...
   */
  public InputStream encodePlanarImageToInputStream(PlanarImage image , String imageType){

		MemoryFileBuffer buff = new MemoryFileBuffer();
		OutputStream outputStream = new MemoryOutputStream(buff);
		
		JAI.create("encode", image, outputStream, imageType , null);
		InputStream s = new MemoryInputStream(buff);
		
		return s;
  }
  
  public PlanarImage getPlanarImage(String URL) throws MalformedURLException{
	  return JAI.create("url", new URL(URL));
  }
  
  
}
