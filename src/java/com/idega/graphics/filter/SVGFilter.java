/*
 * Created on 18.7.2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
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
 * @author tryggvil
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class SVGFilter implements Filter {
	
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		if (req instanceof HttpServletRequest) {
			HttpServletRequest request = (HttpServletRequest) req;
			HttpServletResponse response = (HttpServletResponse) res;
			//String ae = request.getHeader("accept-encoding");
			//if (ae != null && ae.indexOf("gzip") != -1) {
				//System.out.println("GZIP supported, compressing.");
				SVGFilterResponseWrapper wrappedResponse = new SVGFilterResponseWrapper(response);
				chain.doFilter(req, wrappedResponse);
				wrappedResponse.finishResponse();
				//return;
			//}
			//chain.doFilter(req, res);
		}
	}

	public void emitSVG(HttpServletRequest request,
			HttpServletResponse response, String svgString) {
		response.setContentType("image/svg+xml");
		try {
			response.getWriter().write(svgString);
			response.getWriter().flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	public void emitJPG(HttpServletRequest request,
			HttpServletResponse response, String svgString) {
		response.setContentType("image/jpeg");
		JPEGTranscoder t = new JPEGTranscoder();
		t.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(.8));
		TranscoderInput input = new TranscoderInput(new StringReader(svgString));
		try {
			TranscoderOutput output = new TranscoderOutput(response
					.getOutputStream());
			t.transcode(input, output);
			response.getOutputStream().close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void emitPNG(HttpServletRequest request,
			HttpServletResponse response, String svgString) {
		response.setContentType("image/png");
		PNGTranscoder t = new PNGTranscoder();
		TranscoderInput input = new TranscoderInput(new StringReader(svgString));
		try {
			TranscoderOutput output = new TranscoderOutput(response
					.getOutputStream());
			t.transcode(input, output);
			response.getOutputStream().close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	*/
}