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
		this.origResponse = response;
		this.outputFormat=outputFormat;
		
		//cache the content for 3600 seconds:
		this.addHeader("Cache-Control","max-age=3600, must-revalidate");
		//this.addHeader("Expires","Fri, 30 Oct 2006 14:19:41 GMT");
		//this.setHeader("Last-Modified","Fri, 30 Oct 2000 14:19:41 GMT");
		//origResponse.setHeader("Last-Modified","Fri, 30 Oct 2000 14:19:41 GMT");
		
		
	}

	public ServletOutputStream createOutputStream() throws IOException {
		return (new SVGFilterResponseStream(this.origResponse,this.request,this.outputFormat));
	}

	public void finishResponse() {
		try {
			if (this.writer != null) {
				this.writer.close();
			}
			else {
				if (this.stream != null) {
					this.stream.close();
				}
			}
		}
		catch (IOException e) {
		}
	}

	public void flushBuffer() throws IOException {
		this.stream.flush();
	}

	public ServletOutputStream getOutputStream() throws IOException {
		if (this.writer != null) {
			throw new IllegalStateException("getWriter() has already been called!");
		}

		if (this.stream == null) {
			this.stream = createOutputStream();
		}
		return (this.stream);
	}

	public PrintWriter getWriter() throws IOException {
		if (this.writer != null) {
			return (this.writer);
		}

		if (this.stream != null) {
			throw new IllegalStateException("getOutputStream() has already been called!");
		}

		this.stream = createOutputStream();
		this.writer = new PrintWriter(new OutputStreamWriter(this.stream, this.origResponse.getCharacterEncoding()));
		return (this.writer);
	}

	public void setContentLength(int length) {
	}
}