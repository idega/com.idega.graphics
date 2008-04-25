package com.idega.graphics.pdf.business;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.List;

import javax.faces.component.UIComponent;

import org.htmlcleaner.HtmlCleaner;
import org.jaxen.JaxenException;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.builder.business.BuilderService;
import com.idega.core.builder.business.BuilderServiceFactory;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.graphics.generator.business.PDFGenerator;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.presentation.IWContext;
import com.idega.presentation.Page;
import com.idega.slide.business.IWSlideService;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.xml.XmlUtil;

@Scope("session")
@Service(CoreConstants.SPRING_BEAN_NAME_PDF_GENERATOR)
public class PDFGeneratorBean implements PDFGenerator {

	private IWSlideService slide = null;
	private BuilderService builder = null;
	
	private ITextRenderer renderer = null;
	private XMLOutputter outputter = null;
//	private TransformerFactory transformerFactory = null;
	
	private String nameSpaceId = "xmlns";
	private String nameSpace = "http://www.w3.org/1999/xhtml";
	
	public PDFGeneratorBean() {
		renderer = new ITextRenderer();
		outputter = new XMLOutputter(Format.getPrettyFormat());
//		transformerFactory = TransformerFactory.newInstance();
	}
	
	private boolean generatePDF(IWContext iwc, Document doc, String fileName, String uploadPath) {
		if (doc == null || fileName == null || uploadPath == null) { 
			return false;
		}
		
		//	Rendering PDF
		byte[] memory = null;
		ByteArrayOutputStream os = null;
		try {
			os = new ByteArrayOutputStream();
			renderer.setDocument(doc, iwc.getServerURL());
			renderer.layout();
			renderer.createPDF(os);
			memory = os.toByteArray();
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			closeOutputStream(os);
		}
		
		return upload(iwc, memory, fileName, uploadPath);
	}
	
	private boolean upload(IWContext iwc, byte[] memory, String fileName, String uploadPath) {
		//	Checking result of rendering process
		if (memory == null) {
			return false;
		}
		
		//	Checking file name and upload path
		if (!fileName.toLowerCase().endsWith(".pdf")) {
			fileName += ".pdf";
		}
		if (!uploadPath.startsWith(CoreConstants.SLASH)) {
			uploadPath = CoreConstants.SLASH + uploadPath;
		}
		if (!uploadPath.endsWith(CoreConstants.SLASH)) {
			uploadPath = uploadPath + CoreConstants.SLASH;
		}
		
		//	Uploading PDF
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(memory);
			return getSlideService(iwc).uploadFileAndCreateFoldersFromStringAsRoot(uploadPath, fileName, is, MimeTypeUtil.MIME_TYPE_PDF_1, true);
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			closeInputStream(is);
		}
		return false;
	}
	
	public boolean generatePDF(IWContext iwc, UIComponent component, String fileName, String uploadPath, boolean replaceInputs) {
		if (component == null) {
			return false;
		}
		
		BuilderService builder = getBuilderService(iwc);
		if (builder == null) {
			return false;
		}
		
		org.jdom.Document doc = builder.getRenderedComponent(iwc, component, true, false, false);
		if (doc == null) {
			return false;
		}
		
		//	JDOM transform
		//doc = getTransformedDocument(iwc.getIWMainApplication(), doc);
		if (replaceInputs) {
			doc = getDocumentWithoutInputs(doc);
		}
		
		byte[] buffer = getDocumentWithFixedMediaType(doc);
		if (buffer == null) {
			return false;
		}
		
		Document document = null;
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(buffer);
			document = XmlUtil.getDocumentBuilder(false).parse(is);
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			closeInputStream(is);
		}
		
		return generatePDF(iwc, document, fileName, uploadPath);
	}

	public boolean generatePDFFromComponent(String componentUUID, String fileName, String uploadPath, boolean replaceInputs) {
		if (componentUUID == null) {
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
		
		UIComponent component = builder.findComponentInPage(iwc, String.valueOf(iwc.getCurrentIBPageID()), componentUUID);
		return generatePDF(iwc, component, fileName, uploadPath, replaceInputs);
	}

	public boolean generatePDFFromPage(String pageUri, String fileName, String uploadPath, boolean replaceInputs) {
		//	TODO:	remove
//		pageUri = "/pages";
//		fileName = "page";
//		uploadPath = "/files/public";
		
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
		
		return generatePDF(iwc, page, fileName, uploadPath, replaceInputs);
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
	
	private IWSlideService getSlideService(IWApplicationContext iwac) {
		if (slide == null) {
			try {
				slide = (IWSlideService) IBOLookup.getServiceInstance(iwac, IWSlideService.class);
			} catch (IBOLookupException e) {
				e.printStackTrace();
			}
		}
		return slide;
	}
	
	private byte[] getDocumentWithFixedMediaType(org.jdom.Document document) {
		List<Element> styles = getDocumentElements("link", document);
		if (styles != null) {
			String mediaAttrName = "media";
			String mediaAttrValue = "all";
			String typeAttrName = "type";
			String typeAttrValue = "text/css";
			Attribute type = null;
			Attribute media = null;
			for (Element style: styles) {
				type = style.getAttribute(typeAttrName);
				if (type != null) {
					if (typeAttrValue.equals(type.getValue())) {
						media = style.getAttribute(mediaAttrName);
						if (media == null) {
							media = new Attribute(mediaAttrName, mediaAttrValue);
							style.setAttribute(media);
						}
						else {
							media.setValue(mediaAttrValue);
						}
					}
				}
			}
		}
		
		String content = outputter.outputString(document);
		HtmlCleaner cleaner = new HtmlCleaner(content);
		cleaner.setOmitDoctypeDeclaration(false);
		cleaner.setOmitHtmlEnvelope(false);
		cleaner.setOmitComments(true);
		cleaner.setOmitXmlDeclaration(true);
		try {
			cleaner.clean();
			content = cleaner.getPrettyXmlAsString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	
//		System.out.println(content);
		return content.getBytes();
	}
	
	private org.jdom.Document getDocumentWithoutInputs(org.jdom.Document document) {
		List<Element> inputs = getDocumentElements("input", document);
		if (inputs != null) {
			String classAttrName = "class";
			String className = "replaceForInputStyle";
			String valueAttributeName = "value";
			Attribute valueAttribute = null;
			String value = null;
			for (Element input: inputs) {
				value = null;
				
				valueAttribute = input.getAttribute(valueAttributeName);
				value = valueAttribute == null ? CoreConstants.EMPTY : valueAttribute.getValue();
				if (value != null) {
					input.setText(value);
				}
				
				Attribute classAttr = new Attribute(classAttrName, className);
				input.setAttribute(classAttr);
				input.setName("p");
			}
		}
		
		return document;
		/*List<AdvancedProperty> regexs = new ArrayList<AdvancedProperty>();
		regexs.add(new AdvancedProperty("<input.+value=\"", "<p class=\"replaceForInputStyle\">"));	//	Start for <input>
//		regexs.add(new AdvancedProperty("<p class=\"replaceForInputStyle\">.+/>", "</p>"));								//	End for <input>
		
		Pattern pattern = null;
		Matcher matcher = null;
		for(AdvancedProperty prop: regexs) {
			try {
				pattern = Pattern.compile(prop.getId());
				matcher = pattern.matcher(content);
				while (matcher.find()) {
					matcher.start();
					matcher.end();
				}
				//content = matcher.replaceAll(prop.getValue());
			} catch(Exception e) {
			}
		}
		*/
	}
	
	@SuppressWarnings("unchecked")
	private List<Element> getDocumentElements(String tagName, org.jdom.Document doc) {
		String xpathExpr = "//"+nameSpaceId+":" + tagName;
		
		JDOMXPath xp = null;
		try {
			xp = new JDOMXPath(xpathExpr);
			xp.addNamespace(nameSpaceId, nameSpace);
			return xp.selectNodes(doc);
		} catch (JaxenException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/*private org.jdom.Document getTransformedDocument(IWMainApplication iwma, org.jdom.Document sourceDoc) {
		if (sourceDoc == null) {
			return null;
		}
		
		PipedInputStream resultIn = null;
		InputStream sourceDocumentStream = null;
		try {
			//	Set up the XSLT stylesheet for use with Xalan
			String xsl = null;
//			xsl = "XFormTransformer.xsl";
			xsl = "xhtml2fo.xsl";
			String uri = iwma.getBundle(GraphicsConstants.IW_BUNDLE_IDENTIFIER).getVirtualPathWithFileNameString("xsl/" + xsl);
//			File stylesheetFile = IWBundleResourceFilter.copyResourceFromJarToWebapp(iwma, uri);
			URL url = new URL("http://127.0.0.1:8080" + uri);
			InputStream temp = url.openStream();
			Templates stylesheet = transformerFactory.newTemplates(new StreamSource(temp));
			Transformer processor = stylesheet.newTransformer();
			closeInputStream(temp);

			//	Use I/O streams for output files
			resultIn = new PipedInputStream();
			PipedOutputStream resultOut = new PipedOutputStream(resultIn);

			//	Convert the output target for use in Xalan-J 2
			StreamResult result = new StreamResult(resultOut);
			XMLOutputter xmlOutputter = new XMLOutputter();
			sourceDocumentStream = StringHandler.getStreamFromString(xmlOutputter.outputString(sourceDoc));
			//	Use I/O streams for source files
			StreamSource source = new StreamSource(sourceDocumentStream);

			//	Feed the resultant I/O stream into the XSLT processor
			processor.transform(source, result);
			resultOut.close();

			// Convert the resultant transformed document back to JDOM
			SAXBuilder builder = new SAXBuilder();
			org.jdom.Document resultDoc = builder.build(resultIn);
			return resultDoc;
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			closeInputStream(sourceDocumentStream);
			closeInputStream(resultIn);
		}
		return null;
	}*/

}
