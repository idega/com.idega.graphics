/*
 * $Id: SVGFilter.java,v 1.9 2008/11/05 16:40:02 laddi Exp $
 * Created on 18.7.2004 by Tryggvi Larusson
 *
 * Copyright (C) 2004-2005 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 */
package com.idega.graphics.filter;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * <p>
 * Filter that acts as a renderer of SVG files and uses Batik to render either a PNG or JPEG image.<br>
 * This filter is mapped by default on urls with the patterns *.psvg and *.jsvg , and the default behaviour is to try
 * to render the image out to PNG even if the browser accepts viewing svg.
 * </p>
 *  Last modified: $Date: 2008/11/05 16:40:02 $ by $Author: laddi $
 * 
 * @author <a href="mailto:tryggvil@idega.com">Tryggvi Larusson</a>
 * @version $Revision: 1.9 $
 */
public class SVGFilter implements Filter {
	
	public static final String FORMAT_PNG = "png";
	public static final String FORMAT_SVG = "svg";
	public static final String FORMAT_JPEG = "jpg";
	
	public static final String BATIK_USERAGENT_START="Batik";
	public static final String JAVA_USERAGENT_START="Java";
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	public void init(FilterConfig arg0) throws ServletException {
		Logger.getLogger(this.getClass().getName()).info("[idegaWebApp] : Starting SVGFilter");
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Filter#destroy()
	 */
	public void destroy() {
		// TODO Auto-generated method stub
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		if (req instanceof HttpServletRequest) {
			HttpServletRequest request = (HttpServletRequest) req;
			HttpServletResponse response = (HttpServletResponse) res;
			String outputFormat = getOutputFormatForClient(request);
			if(outputFormat.equals(FORMAT_SVG)){
				//just bypass this filter to do the default handing:
				response.setContentType("image/svg+xml");
				chain.doFilter(req,res);
				response.setContentType("image/svg+xml");
			}
			else{
				//else wrap the response:
				SVGFilterResponseWrapper wrappedResponse = new SVGFilterResponseWrapper(response,request,outputFormat);
				chain.doFilter(req, wrappedResponse);
				wrappedResponse.finishResponse();
			}
		}
	}

	/**
	 * @param req
	 * @return
	 */
	private String getOutputFormatForClient(HttpServletRequest request) {
		// TODO implement better client detection
		//String userAgent = request.getHeader("User-agent");
		String accept = request.getHeader("Accept");
		/*Enumeration enumeration = request.getHeaderNames();
		System.out.println("SVGFilter: Printing out headers:");
		while (enum.hasMoreElements()) {
			String header = (String) enum.nextElement();
			System.out.println("Header: "+header+": "+request.getHeader(header));
		}*/
		
		if(accept.indexOf("svg")!=-1){
		//if(true){
		//if(userAgent.startsWith(BATIK_USERAGENT_START)){
			//Special case for not rendering out to png when Batik itself is fetching the file.
			return FORMAT_SVG;
		}
		//else if(userAgent.startsWith(JAVA_USERAGENT_START)){
			//Special case for not rendering out to png when Batik itself is fetching the file.
		//	return FORMAT_SVG;
		//}
		/*else if(accept.indexOf("png")!=-1){
			return FORMAT_PNG;
		}
		else{
			return FORMAT_JPEG;
		}*/
		return FORMAT_PNG;
	}
}