/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.FileCopyUtils;

import android.test.suitebuilder.annotation.MediumTest;

public abstract class AbstractHttpRequestFactoryTestCase extends TestCase {

	protected ClientHttpRequestFactory factory;

	protected static String baseUrl;

	private static Server jettyServer;

	@Override
	protected void setUp() throws Exception {
		setUpJetty();
		this.factory = createRequestFactory();
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		this.factory = null;
		super.tearDown();
	}

	@Override
	protected void finalize() throws Throwable {
		if (jettyServer != null) {
			jettyServer.stop();
		}
		super.finalize();
	}

	private void setUpJetty() throws Exception {
		if (jettyServer == null) {
			int port = 8080;
			jettyServer = new Server(port);
			baseUrl = "http://localhost:" + port;
			Context jettyContext = new Context(jettyServer, "/");
			jettyContext.addServlet(new ServletHolder(new EchoServlet()), "/echo");
			jettyContext.addServlet(new ServletHolder(new StatusServlet(200)), "/status/ok");
			jettyContext.addServlet(new ServletHolder(new StatusServlet(404)), "/status/notfound");
			jettyContext.addServlet(new ServletHolder(new MethodServlet("DELETE")), "/methods/delete");
			jettyContext.addServlet(new ServletHolder(new MethodServlet("GET")), "/methods/get");
			jettyContext.addServlet(new ServletHolder(new MethodServlet("HEAD")), "/methods/head");
			jettyContext.addServlet(new ServletHolder(new MethodServlet("OPTIONS")), "/methods/options");
			jettyContext.addServlet(new ServletHolder(new PostServlet()), "/methods/post");
			jettyContext.addServlet(new ServletHolder(new MethodServlet("PUT")), "/methods/put");
			jettyServer.start();
		}
	}

	protected abstract ClientHttpRequestFactory createRequestFactory();


	@MediumTest
	public void testStatus() throws Exception {
		URI uri = new URI(baseUrl + "/status/notfound");
		ClientHttpRequest request = factory.createRequest(uri, HttpMethod.GET);
		assertEquals("Invalid HTTP method", HttpMethod.GET, request.getMethod());
		assertEquals("Invalid HTTP URI", uri, request.getURI());
		ClientHttpResponse response = request.execute();
		assertEquals("Invalid status code", HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@MediumTest
	public void testEcho() throws Exception {
		ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/echo"), HttpMethod.PUT);
		assertEquals("Invalid HTTP method", HttpMethod.PUT, request.getMethod());
		String headerName = "MyHeader";
		String headerValue1 = "value1";
		request.getHeaders().add(headerName, headerValue1);
		String headerValue2 = "value2";
		request.getHeaders().add(headerName, headerValue2);
		byte[] body = "Hello World".getBytes("UTF-8");
		request.getHeaders().setContentLength(body.length);
		FileCopyUtils.copy(body, request.getBody());
		ClientHttpResponse response = request.execute();
		try {
			assertNotNull(response.getStatusText());
			assertEquals("Invalid status code", HttpStatus.OK, response.getStatusCode());
			assertTrue("Header not found", response.getHeaders().containsKey(headerName));
			assertEquals("Header value not found", Arrays.asList(headerValue1, headerValue2), response.getHeaders().get(headerName));
			byte[] result = FileCopyUtils.copyToByteArray(response.getBody());
			assertTrue("Invalid body", Arrays.equals(body, result));
		} finally {
			response.close();
		}
	}

	@MediumTest
	public void testMultipleWrites() throws Exception {
		boolean success = false;
		try {
			ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/echo"), HttpMethod.POST);
			byte[] body = "Hello World".getBytes("UTF-8");
			FileCopyUtils.copy(body, request.getBody());
			ClientHttpResponse response = request.execute();
			try {
				FileCopyUtils.copy(body, request.getBody());
			} finally {
				response.close();
			}
		} catch (IllegalStateException e) {
			success = true;
		}
		assertTrue("Expected IllegalStateException", success);
	}

	@MediumTest
	public void testHeadersAfterExecute() throws Exception {
		boolean success = false;
		try {
			ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/echo"), HttpMethod.POST);
			request.getHeaders().add("MyHeader", "value");
			byte[] body = "Hello World".getBytes("UTF-8");
			FileCopyUtils.copy(body, request.getBody());
			ClientHttpResponse response = request.execute();
			try {
				request.getHeaders().add("MyHeader", "value");
			} finally {
				response.close();
			}
		} catch (UnsupportedOperationException e) {
			success = true;
		}
		assertTrue("Expected UnsupportedOperationException", success);
	}

	@MediumTest
	public void testHttpMethods() throws Exception {
		assertHttpMethod("get", HttpMethod.GET);
		assertHttpMethod("head", HttpMethod.HEAD);
		assertHttpMethod("post", HttpMethod.POST);
		assertHttpMethod("put", HttpMethod.PUT);
		assertHttpMethod("options", HttpMethod.OPTIONS);
		assertHttpMethod("delete", HttpMethod.DELETE);
	}

	private void assertHttpMethod(String path, HttpMethod method) throws Exception {
		ClientHttpResponse response = null;
		try {
			ClientHttpRequest request = factory.createRequest(new URI(baseUrl + "/methods/" + path), method);
			response = request.execute();
			assertEquals("Invalid response status", HttpStatus.OK, response.getStatusCode());
			assertEquals("Invalid method", path.toUpperCase(Locale.ENGLISH), request.getMethod().name());
		} finally {
			if (response != null) {
				response.close();
			}
		}
	}

	/**
	 * Servlet that sets a given status code.
	 */
	private static class StatusServlet extends GenericServlet {

		private static final long serialVersionUID = 1L;

		private final int sc;

		private StatusServlet(int sc) {
			this.sc = sc;
		}

		@Override
		public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
			((HttpServletResponse) response).setStatus(sc);
		}
	}

	private static class MethodServlet extends GenericServlet {

		private static final long serialVersionUID = 1L;

		private final String method;

		private MethodServlet(String method) {
			this.method = method;
		}

		@Override
		public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
			HttpServletRequest httpReq = (HttpServletRequest) req;
			assertEquals("Invalid HTTP method", method, httpReq.getMethod());
			res.setContentLength(0);
			((HttpServletResponse) res).setStatus(200);
		}
	}

	private static class PostServlet extends MethodServlet {

		private static final long serialVersionUID = 1L;

		private PostServlet() {
			super("POST");
		}

		@Override
		public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
			super.service(req, res);
			long contentLength = req.getContentLength();
			if (contentLength != -1) {
				InputStream in = req.getInputStream();
				long byteCount = 0;
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = in.read(buffer)) != -1) {
					byteCount += bytesRead;
				}
				assertEquals("Invalid content-length", contentLength, byteCount);
			}
		}
	}

	private static class EchoServlet extends HttpServlet {

		private static final long serialVersionUID = 1L;

		@Override
		protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			echo(req, resp);
		}

		private void echo(HttpServletRequest request, HttpServletResponse response) throws IOException {
			response.setStatus(HttpServletResponse.SC_OK);
			for (Enumeration<?> e1 = request.getHeaderNames(); e1.hasMoreElements();) {
				String headerName = (String) e1.nextElement();
				for (Enumeration<?> e2 = request.getHeaders(headerName); e2.hasMoreElements();) {
					String headerValue = (String) e2.nextElement();
					response.addHeader(headerName, headerValue);
				}
			}
			FileCopyUtils.copy(request.getInputStream(), response.getOutputStream());
		}
	}

}