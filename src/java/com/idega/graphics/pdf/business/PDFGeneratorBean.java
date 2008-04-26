package com.idega.graphics.pdf.business;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.faces.component.UIComponent;

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
import com.idega.util.ListUtil;
import com.idega.util.xml.XmlUtil;

@Scope("session")
@Service(CoreConstants.SPRING_BEAN_NAME_PDF_GENERATOR)
public class PDFGeneratorBean implements PDFGenerator {

	private IWSlideService slide = null;
	private BuilderService builder = null;
	
	private ITextRenderer renderer = null;
	private XMLOutputter outputter = null;
	
	private String nameSpaceId = "xmlns";
	private String nameSpace = "http://www.w3.org/1999/xhtml";
	
	public PDFGeneratorBean() {
		renderer = new ITextRenderer();
		outputter = new XMLOutputter(Format.getPrettyFormat());
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
		
		if (replaceInputs) {
			doc = getDocumentWithoutInputs(doc);
		}
		
		byte[] memory = getDocumentWithFixedMediaType(iwc, doc);
		if (memory == null) {
			return false;
		}
		
		Document document = null;
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(memory);
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
	
	private byte[] getDocumentWithFixedMediaType(IWApplicationContext iwac, org.jdom.Document document) {
		List<Element> styles = getDocumentElements("link", document);
		if (styles != null) {
			String mediaAttrName = "media";
			String mediaAttrValue = "all";
			String typeAttrName = "type";
			List<String> expextedValues = ListUtil.convertStringArrayToList(new String[] {"text/css"});
			for (Element style: styles) {
				if (doElementHasAttribute(style, typeAttrName, expextedValues)) {
					setCustomAttribute(style, mediaAttrName, mediaAttrValue);
				}
			}
		}
		
		String htmlContent = getBuilderService(iwac).getCleanedHtmlContent(outputter.outputString(document), false, false, true);
	
//		System.out.println(htmlContent);
		return htmlContent.getBytes();
	}
	
	private org.jdom.Document getDocumentWithoutInputs(org.jdom.Document document) {
		String divTag = "div";
		String classAttrName = "class";
		List<Element> needlessElements = new ArrayList<Element>();
		
		//	<input>
		List<Element> inputs = getDocumentElements("input", document);
		String typeAttrName = "type";
		String checkedAttrName = "checked";
		String className = "replaceForInputStyle";
		String valueAttrName = "value";
		Attribute valueAttr = null;
		String value = null;
//		String yesLocalizedText = iwrb.getLocalizedString("yes", "Yes");
		boolean needReplace = true;
		//	Inputs we don't want to be replaced
		List<String> typeAttrValues = ListUtil.convertStringArrayToList(new String[] {"button", "hidden", "image", "password", "reset", "submit"});
		List<String> textTypeValue = ListUtil.convertStringArrayToList(new String[] {"text"});
		List<String> checkedAttrValues = ListUtil.convertStringArrayToList(new String[] {"checked", Boolean.TRUE.toString(), CoreConstants.EMPTY});
		for (Element input: inputs) {
			needReplace = !doElementHasAttribute(input, typeAttrName, typeAttrValues);
			
			if (needReplace) {
				if (doElementHasAttribute(input, typeAttrName, textTypeValue)) {
					//	Text inputs
					valueAttr = input.getAttribute(valueAttrName);
					value = valueAttr == null ? null : valueAttr.getValue();
					if (value != null) {
						input.setText(value);
						input.setName(divTag);
						setCustomAttribute(input, classAttrName, className);
					}
				}
				else {
					//	Radio button or check box
					if (doElementHasAttribute(input, checkedAttrName, checkedAttrValues)) {
						//	Is checked
						//	TODO: do we need some special handling?
//						input.setText(yesLocalizedText);
					}
				}
			}
		}
		
		//	<select>
		List<Element> selects = getDocumentElements("select", document);
		String multipleAtrrName = "multiple";
		String selectAttrName = "selected";
		String singleSelectClass = "replaceForSelectSingle";
		String multiSelectClass = "replaceForSelectMulti";
		String listTag = "ul";
		String listItemTag = "li";
		String optionTag = "option";
		
		List<String> multipleAttrValues = ListUtil.convertStringArrayToList(new String[] {Boolean.TRUE.toString(), multipleAtrrName});
		List<String> selectedAttrValues = ListUtil.convertStringArrayToList(new String[] {Boolean.TRUE.toString(), selectAttrName});
		for (Element select: selects) {
			if (doElementHasAttribute(select, multipleAtrrName, multipleAttrValues)) {	//	Is multiple?
				//	Will create list: <ul><li></li>...</ul>
				select.setName(listTag);
				setCustomAttribute(select, classAttrName, multiSelectClass);
				
				List<Element> options = getDocumentElements(optionTag, select);
				for (Element option: options) {
					if (doElementHasAttribute(option, selectAttrName, selectedAttrValues)) {
						option.setName(listItemTag);
					}
					else {
						needlessElements.add(option);
					}
				}
			}
			else {
				//	Will convert to <div>
				select.setName(divTag);
				setCustomAttribute(select, classAttrName, singleSelectClass);
			}
		}
		
		//	Removing needless elements
		for (Iterator<Element> it = needlessElements.iterator(); it.hasNext();) {
			it.next().detach();
		}
		
		return document;
	}
	
	private boolean doElementHasAttribute(Element e, String attrName, List<String> expextedValues) {
		if (e == null || attrName == null || expextedValues == null) {
			return false;
		}
		
		Attribute a = e.getAttribute(attrName);
		if (a == null) {
			return false;
		}
		
		String attrValue = a.getValue();
		if (attrValue == null) {
			return false;
		}
		
		return expextedValues.contains(attrValue);
	}
	
	private void setCustomAttribute(Element e, String attrName, String attrValue) {
		if (e == null || attrName == null || attrValue == null) {
			return;
		}
		
		Attribute a = e.getAttribute(attrName);
		if (a == null) {
			a = new Attribute(attrName, attrValue);
			e.setAttribute(a);
		}
		else {
			a.setValue(attrValue);
		}
	}
	
	@SuppressWarnings("unchecked")
	private List<Element> getDocumentElements(String tagName, Object node) {
		String xpathExpr = "//"+nameSpaceId+":" + tagName;
		
		JDOMXPath xp = null;
		try {
			xp = new JDOMXPath(xpathExpr);
			xp.addNamespace(nameSpaceId, nameSpace);
			return xp.selectNodes(node);
		} catch (JaxenException e) {
			e.printStackTrace();
		}
		
		return new ArrayList<Element>();	//	Fake, to avoid NullPointer
	}

}
