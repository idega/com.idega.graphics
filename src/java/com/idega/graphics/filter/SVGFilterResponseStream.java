/*
 * Created on 18.7.2004
 */
package com.idega.graphics.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;

/**
 * @author tryggvil
 */
public class SVGFilterResponseStream extends ServletOutputStream {

	//protected ByteArrayOutputStream baos = null;
	//protected GZIPOutputStream gzipstream = null;
	protected ByteArrayOutputStream buffer = null;
	protected boolean closed = false;
	protected HttpServletResponse response = null;
	protected HttpServletRequest request;
	protected ServletOutputStream output = null;
	protected String outputFormat=SVGFilter.FORMAT_PNG;

	public SVGFilterResponseStream(HttpServletResponse response,HttpServletRequest request,String outputFormat) throws IOException {
		super();
		closed = false;
		this.response = response;
		this.request = request;
		this.output = response.getOutputStream();
		//baos = new ByteArrayOutputStream();
		//gzipstream = new GZIPOutputStream(baos);
		buffer = new ByteArrayOutputStream();
		this.outputFormat=outputFormat;
	}

	public void close() throws IOException {
		if (closed) {
			throw new IOException("This output stream has already been closed");
		}
		//gzipstream.finish();

		//byte[] bytes = buffer.toByteArray();
		//String svgString = new String(bytes);
		
		
		//response.addHeader("Content-Length", Integer.toString(bytes.length));
		//response.setContentType("image/png");
		//response.addHeader("Content-Encoding", "gzip");
		
		if(outputFormat.equals(SVGFilter.FORMAT_PNG)){
			emitPNG(output);
		}
		//else if(outputFormat.equals(SVGFilter.FORMAT_SVG)){
		//	emitSVG(output);
		//}
		else if(outputFormat.equals(SVGFilter.FORMAT_JPEG)){
			emitJPEG(output);
		}		
		
		/*output.write(bytes);
		output.flush();
		output.close();*/
		closed = true;
	}
	
	public void emitPNG(ServletOutputStream output) {
		response.setContentType("image/png");
		PNGTranscoder t = new PNGTranscoder();
		//TranscoderInput input = new TranscoderInput(new StringReader(svgString));
		String requestUri = getRequestedUri();
		TranscoderInput input = new TranscoderInput(requestUri);
		try {
			TranscoderOutput tOutput = new TranscoderOutput(output);
			t.transcode(input, tOutput);
			output.flush();
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void emitJPEG(ServletOutputStream output) {
		response.setContentType("image/jpeg");
		JPEGTranscoder t = new JPEGTranscoder();
		String requestUri = getRequestedUri();
		TranscoderInput input = new TranscoderInput(requestUri);
		//TranscoderInput input = new TranscoderInput(new StringReader(svgString));
		try {
			TranscoderOutput tOutput = new TranscoderOutput(output);
			t.transcode(input, tOutput);
			output.flush();
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
	/*
	public void emitSVG(ServletOutputStream output, String svgString) {
		response.setContentType("image/svg+xml");
		try {
			//response.getWriter().write(svgString);
			//response.getWriter().flush();
			
			OutputStreamWriter writer = new OutputStreamWriter(output);
			writer.write(svgString);
			//output.write(new StringReader(svgString));
			writer.flush();
			output.flush();
			output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/
	
	protected String getRequestedUri(){
		StringBuffer url = request.getRequestURL();
		return url.toString();
	}
	
	public void flush() throws IOException {
		if (closed) {
			throw new IOException("Cannot flush a closed output stream");
		}
		buffer.flush();
	}

	public void write(int b) throws IOException {
		if (closed) {
			throw new IOException("Cannot write to a closed output stream");
		}
		buffer.write((byte) b);
	}

	public void write(byte b[]) throws IOException {
		write(b, 0, b.length);
	}

	public void write(byte b[], int off, int len) throws IOException {
		//System.out.println("writing...");
		if (closed) {
			throw new IOException("Cannot write to a closed output stream");
		}
		buffer.write(b, off, len);
	}

	public boolean closed() {
		return (this.closed);
	}

	public void reset() {
		//noop
	}
}