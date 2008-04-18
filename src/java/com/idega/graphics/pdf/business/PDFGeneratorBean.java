package com.idega.graphics.pdf.business;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;

import javax.faces.component.UIComponent;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jdom.output.XMLOutputter;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.idega.business.IBOLookup;
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.graphics.generator.business.PDFGenerator;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.presentation.IWContext;
import com.idega.presentation.Page;
import com.idega.slide.business.IWSlideService;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;

public class PDFGeneratorBean implements PDFGenerator {

	private BuilderService builder = null;
	private DocumentBuilderFactory documentBuilder = null;
	
	public boolean generatePDF(IWContext iwc, Document doc, String fileName, String uploadPath) {
		if (doc == null || fileName == null || uploadPath == null) { 
			return false;
		}
		
		//	Rendering PDF
		byte[] buffer = null;
		ByteArrayOutputStream os = null;
		try {
			os = new ByteArrayOutputStream();
			ITextRenderer renderer = new ITextRenderer();
			renderer.setDocument(doc, iwc.getServerURL());
			renderer.layout();
			renderer.createPDF(os);
			buffer = os.toByteArray();
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			closeOutputStream(os);
		}
		
		//	Checking result of rendering process
		if (buffer == null) {
			return false;
		}
		
		if (!uploadPath.startsWith(CoreConstants.SLASH)) {
			uploadPath = new StringBuilder(CoreConstants.SLASH).append(uploadPath).toString();
		}
		if (!uploadPath.endsWith(CoreConstants.SLASH)) {
			uploadPath = new StringBuilder(uploadPath).append(CoreConstants.SLASH).toString();
		}
		//	Uploading PDF
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(buffer);
			IWSlideService slide = (IWSlideService) IBOLookup.getServiceInstance(iwc, IWSlideService.class);
			return slide.uploadFileAndCreateFoldersFromStringAsRoot(uploadPath, fileName, is, "application/pdf", true);
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			closeInputStream(is);
		}
		
		return false;
	}
	
	private void closeInputStream(InputStream is) {
		if (is == null) {
			return;
		}
		
		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void closeOutputStream(OutputStream os) {
		if (os == null) {
			return;
		}
		
		try {
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private BuilderService getBuilderService(IWApplicationContext iwac) {
		if (builder == null) {
			try {
				builder = BuilderServiceFactory.getBuilderService(iwac);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return builder;
	}

	public boolean generatePDF(IWContext iwc, UIComponent component, String fileName, String uploadPath, boolean cleanHtml) {
		if (component == null) {
			return false;
		}
		
		BuilderService builder = getBuilderService(iwc);
		if (builder == null) {
			return false;
		}
		
		org.jdom.Document doc = builder.getRenderedComponent(iwc, component, cleanHtml);
		if (doc == null) {
			return false;
		}
		
		byte[] buffer = null; 
		ByteArrayOutputStream os = null;
		try {
			os = new ByteArrayOutputStream();
			XMLOutputter outputter = new XMLOutputter();
			outputter.output(doc, os);
			buffer = os.toByteArray();
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			closeOutputStream(os);
		}
		if (buffer == null) {
			return false;
		}
		
		Document document = null;
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(buffer);
			
			if (documentBuilder == null) {
				documentBuilder = DocumentBuilderFactory.newInstance();
				documentBuilder.setNamespaceAware(false);
				documentBuilder.setValidating(false);
			}
			
			document = documentBuilder.newDocumentBuilder().parse(is);
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			closeInputStream(is);
		}
		
		return generatePDF(iwc, document, fileName, uploadPath);
	}

	public boolean generatePDFFromComponent(String componentId, String fileName, String uploadPath, boolean cleanHtml) {
		if (componentId == null) {
			return false;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return false;
		}
		
		BuilderService builder = getBuilderService(iwc);
		if (builder == null) {
			return false;
		}
		
		UIComponent component = builder.findComponentInPage(iwc, String.valueOf(iwc.getCurrentIBPageID()), componentId);
		return generatePDF(iwc, component, fileName, uploadPath, cleanHtml);
	}

	public boolean generatePDFFromPage(String pageUri, String fileName, String uploadPath, boolean cleanHtml) {
		if (pageUri == null) {
			return false;
		}
		
		IWContext iwc = CoreUtil.getIWContext();
		if (iwc == null) {
			return false;
		}
		
		BuilderService builder = getBuilderService(iwc);
		Page page = null;
		try {
			page = builder.getPage(builder.getPageKeyByURI(pageUri));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return generatePDF(iwc, page, fileName, uploadPath, cleanHtml);
	}

}
