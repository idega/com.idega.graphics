/*
 * $Id: SVGFilter.java,v 1.6 2005/01/26 03:26:07 tryggvil Exp $
 * Created on 18.7.2004 by Tryggvi Larusson
 *
 * Copyright (C) 2004-2005 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 */
package com.idega.graphics.filter;

import java.io.IOException;
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
 * This filter is mapped by default on urls with the patterns *.svg,*.jsvg,*.svg.jsp and *.svg.jspx
 * </p>
 *  Last modified: $Date: 2005/01/26 03:26:07 $ by $Author: tryggvil $
 * 
 * @author <a href="mailto:tryggvil@idega.com">Tryggvi Larusson</a>
 * @version $Revision: 1.6 $
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
		System.out.println("[idegaWebApp] : Starting SVGFilter");
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
				chain.doFilter(req,res);
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
		String userAgent = request.getHeader("User-agent");
		if(userAgent.startsWith(BATIK_USERAGENT_START)){
			//Special case for not rendering out to png when Batik itself is fetching the file.
			return FORMAT_SVG;
		}
		else if(userAgent.startsWith(JAVA_USERAGENT_START)){
			//Special case for not rendering out to png when Batik itself is fetching the file.
			return FORMAT_SVG;
		}
		return FORMAT_PNG;
	}
}