/*
 * Created on 30.3.2004
 */
package com.idega.graphics.filter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * @author laddi
 */
public class SVGFilterResponseWrapper extends HttpServletResponseWrapper {

	protected HttpServletResponse origResponse = null;
	protected HttpServletRequest request;
	protected ServletOutputStream stream = null;
	protected PrintWriter writer = null;
	private String outputFormat;

	public SVGFilterResponseWrapper(HttpServletResponse response,HttpServletRequest request,String outputFormat) {
		super(response);
		this.request=request;
		origResponse = response;
		this.outputFormat=outputFormat;
		
		//cache the content for 3600 seconds:
		this.addHeader("Cache-Control","max-age=3600, must-revalidate");
		//this.addHeader("Expires","Fri, 30 Oct 2006 14:19:41 GMT");
		//this.setHeader("Last-Modified","Fri, 30 Oct 2000 14:19:41 GMT");
		//origResponse.setHeader("Last-Modified","Fri, 30 Oct 2000 14:19:41 GMT");
		
		
	}

	public ServletOutputStream createOutputStream() throws IOException {
		return (new SVGFilterResponseStream(origResponse,request,outputFormat));
	}

	public void finishResponse() {
		try {
			if (writer != null) {
				writer.close();
			}
			else {
				if (stream != null) {
					stream.close();
				}
			}
		}
		catch (IOException e) {
		}
	}

	public void flushBuffer() throws IOException {
		stream.flush();
	}

	public ServletOutputStream getOutputStream() throws IOException {
		if (writer != null) {
			throw new IllegalStateException("getWriter() has already been called!");
		}

		if (stream == null)
			stream = createOutputStream();
		return (stream);
	}

	public PrintWriter getWriter() throws IOException {
		if (writer != null) {
			return (writer);
		}

		if (stream != null) {
			throw new IllegalStateException("getOutputStream() has already been called!");
		}

		stream = createOutputStream();
		writer = new PrintWriter(new OutputStreamWriter(stream, origResponse.getCharacterEncoding()));
		return (writer);
	}

	public void setContentLength(int length) {
	}
}