package com.idega.graphics.pdf.business;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.xml.sax.InputSource;

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
import com.idega.util.StringUtil;
import com.idega.util.xml.XmlUtil;

@Scope("session")
@Service(CoreConstants.SPRING_BEAN_NAME_PDF_GENERATOR)
public class PDFGeneratorBean implements PDFGenerator {

	private IWSlideService slide = null;
	private BuilderService builder = null;
	
	private ITextRenderer renderer = null;
	private XMLOutputter outputter = null;
	
	private static final String XMLNS_NAME_SPACE_ID = "xmlns";
	private static final String XHTML_NAME_SPACE = "http://www.w3.org/1999/xhtml";
	private static final String TAG_DIV = "div";
	private static final String ATTRIBUTE_CLASS = "class";
	private static final String ATTRIBUTE_STYLE = "style";
	private static final String ATTRIBUTE_VALUE_DISPLAY_NONE = "display: none;";
	
	public PDFGeneratorBean() {
		try {
			renderer = new ITextRenderer();
		} catch(Exception e) {
			Logger.getLogger(PDFGeneratorBean.class.getName()).log(Level.SEVERE, "Error creating bean!", e);
		}
		outputter = new XMLOutputter(Format.getPrettyFormat());
	}
	
	private boolean generatePDF(IWContext iwc, Document doc, String fileName, String uploadPath) {
		return upload(iwc, getPDFBytes(iwc, doc), fileName, uploadPath);
	}
	
	private byte[] getPDFBytes(IWContext iwc, Document doc) {
		if (renderer == null || doc == null) {
			return null;
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
			return null;
		} finally {
			closeOutputStream(os);
		}
		
		return memory;
	}
	
	private boolean upload(IWContext iwc, byte[] memory, String fileName, String uploadPath) {
		//	Checking result of rendering process
		if (memory == null || StringUtil.isEmpty(fileName) || StringUtil.isEmpty(uploadPath)) {
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
	
	public boolean generatePDF(IWContext iwc, UIComponent component, String fileName, String uploadPath, boolean replaceInputs, boolean checkCustomTags) {
		Document document = getDocumentToConvertToPDF(iwc, component, replaceInputs, checkCustomTags);
		if (document == null) {
			return false;
		}
		
		return generatePDF(iwc, document, fileName, uploadPath);
	}
	
	private Document getDocumentToConvertToPDF(IWContext iwc, UIComponent component, boolean replaceInputs, boolean checkCustomTags) {
		if (component == null) {
			return null;
		}
		
		BuilderService builder = getBuilderService(iwc);
		if (builder == null) {
			return null;
		}
		
		org.jdom.Document doc = builder.getRenderedComponent(iwc, component, true, false, false);
		if (doc == null) {
			return null;
		}
//		XMLOutputter output = new XMLOutputter(Format.getPrettyFormat());
//		System.out.println(output.outputString(doc));
		
		if (replaceInputs) {
			doc = getDocumentWithoutInputs(doc);
		}
		if (checkCustomTags) {
			doc = getDocumentWithModifiedTags(doc);
		}
		
		byte[] memory = getDocumentWithFixedMediaType(iwc, doc);
		if (memory == null) {
			return null;
		}
		
		Document document = null;
		InputStream stream = null;
		Reader reader = null;
		try {
			stream = new ByteArrayInputStream(memory);
			reader = new InputStreamReader(stream, CoreConstants.ENCODING_UTF8);
			document = XmlUtil.getDocumentBuilder().parse(new InputSource(reader));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			closeInputStream(stream);
			closeReader(reader);
		}
		
		return document;
	}

	public boolean generatePDFFromComponent(String componentUUID, String fileName, String uploadPath, boolean replaceInputs, boolean checkCustomTags) {
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
		return generatePDF(iwc, component, fileName, uploadPath, replaceInputs, checkCustomTags);
	}

	public boolean generatePDFFromPage(String pageUri, String fileName, String uploadPath, boolean replaceInputs, boolean checkCustomTags) {		
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
		
		return generatePDF(iwc, page, fileName, uploadPath, replaceInputs, checkCustomTags);
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
	
	private void closeReader(Reader reader) {
		if (reader == null) {
			return;
		}
		
		try {
			reader.close();
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
		if (!ListUtil.isEmpty(styles)) {
			String mediaAttrName = "media";
			String mediaAttrValue = "all";
			String typeAttrName = "type";
			List<String> expectedValues = ListUtil.convertStringArrayToList(new String[] {"text/css"});
			for (Element style: styles) {
				if (doElementHasAttribute(style, typeAttrName, expectedValues)) {
					setCustomAttribute(style, mediaAttrName, mediaAttrValue);
				}
			}
		}
		
		String htmlContent = getBuilderService(iwac).getCleanedHtmlContent(outputter.outputString(document), false, false, true);
		
		try {
			return htmlContent.getBytes(CoreConstants.ENCODING_UTF8);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private org.jdom.Document getDocumentWithoutInputs(org.jdom.Document document) {
		List<Element> needlessElements = new ArrayList<Element>();
		
		//	<input>
		List<Element> inputs = getDocumentElements("input", document);
		String typeAttrName = "type";
		String checkedAttrName = "checked";
		String className = "replaceForInputStyle";
		String valueAttrName = "value";
		Attribute valueAttr = null;
		String value = null;
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
						input.setName(TAG_DIV);
						setCustomAttribute(input, ATTRIBUTE_CLASS, className);
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
				setCustomAttribute(select, ATTRIBUTE_CLASS, multiSelectClass);
				
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
				select.setName(TAG_DIV);
				setCustomAttribute(select, ATTRIBUTE_CLASS, singleSelectClass);
			}
		}
		
		//	Removing needless elements
		for (Iterator<Element> it = needlessElements.iterator(); it.hasNext();) {
			it.next().detach();
		}
		
		return document;
	}
	
	private org.jdom.Document getDocumentWithModifiedTags(org.jdom.Document document) {
		List<String> expectedValues = null;
		
		//	<div>
		List<Element> divs = getDocumentElements(TAG_DIV, document);
		if (!ListUtil.isEmpty(divs)) {
			expectedValues = ListUtil.convertStringArrayToList(new String[] {"deselected-case"});
			List<String> buttonAreaClassValue = ListUtil.convertStringArrayToList(new String[] {"fbc_button_area"});
			for (Element div: divs) {
				if (doElementHasAttribute(div, ATTRIBUTE_CLASS, expectedValues)) {
					setCustomAttribute(div, ATTRIBUTE_STYLE, "display: block;");
				}
				
				if (doElementHasAttribute(div, ATTRIBUTE_CLASS, buttonAreaClassValue)) {
					setCustomAttribute(div, ATTRIBUTE_STYLE, ATTRIBUTE_VALUE_DISPLAY_NONE);
				}
			}
		}
		
		//	<legend>
		List<Element> legends = getDocumentElements("legend", document);
		if (!ListUtil.isEmpty(legends)) {
			expectedValues = ListUtil.convertStringArrayToList(new String[] {"label"});
			for (Element legend: legends) {
				if (doElementHasAttribute(legend, ATTRIBUTE_CLASS, expectedValues)) {
					setCustomAttribute(legend, ATTRIBUTE_STYLE, ATTRIBUTE_VALUE_DISPLAY_NONE);
				}
			}
		}
		
		//	<span>
		List<Element> spans = getDocumentElements("span", document);
		if (!ListUtil.isEmpty(spans)) {
			expectedValues = ListUtil.convertStringArrayToList(new String[] {"help-text"});
			for (Element span: spans) {
				if (doElementHasAttribute(span, ATTRIBUTE_CLASS, expectedValues)) {
					setCustomAttribute(span, ATTRIBUTE_STYLE, ATTRIBUTE_VALUE_DISPLAY_NONE);
				}
			}
		}
		
		return document;
	}
	
	private boolean doElementHasAttribute(Element e, String attrName, List<String> expectedValues) {
		if (e == null || attrName == null || expectedValues == null) {
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
		
		if (expectedValues.contains(attrValue)) {
			return true;
		}
		
		for (String expectedValue: expectedValues) {
			if (attrValue.indexOf(expectedValue) != -1) {
				return true;
			}
		}
		
		return false;
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
		String xpathExprStart = "//";
		String xpathExprNameSpacePart = XMLNS_NAME_SPACE_ID + ":";
		
		JDOMXPath xp = null;
		List<Element> elements = null;
		try {
			xp = new JDOMXPath(new StringBuilder(xpathExprStart).append(xpathExprNameSpacePart).append(tagName).toString());
			xp.addNamespace(XMLNS_NAME_SPACE_ID, XHTML_NAME_SPACE);
			elements = xp.selectNodes(node);
			if (ListUtil.isEmpty(elements)) {
				xp = new JDOMXPath(new StringBuilder(xpathExprStart).append(tagName).toString());
				xp.addNamespace(XMLNS_NAME_SPACE_ID, XHTML_NAME_SPACE);
				elements = xp.selectNodes(node);
			}
		} catch (JaxenException e) {
			e.printStackTrace();
		}
		
		return ListUtil.isEmpty(elements) ? new ArrayList<Element>(0) : elements;
	}

	public InputStream getStreamToGeneratedPDF(IWContext iwc, UIComponent component, boolean replaceInputs, boolean checkCustomTags) {
		byte[] pdfMemory = getPDFBytes(iwc, getDocumentToConvertToPDF(iwc, component, replaceInputs, checkCustomTags));
		if (pdfMemory == null) {
			return null;
		}
		
		return new ByteArrayInputStream(pdfMemory);
	}

}
