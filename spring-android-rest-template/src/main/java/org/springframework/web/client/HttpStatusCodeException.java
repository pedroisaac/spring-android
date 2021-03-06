/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.web.client;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.springframework.http.HttpStatus;

/**
 * Abstract base class for exceptions based on an {@link HttpStatus}.
 *
 * @author Arjen Poutsma
 * @author Roy Clarkson
 * @since 1.0
 */
public abstract class HttpStatusCodeException extends RestClientException {

	private static final long serialVersionUID = 1L;

	private static final Charset DEFAULT_CHARSET = Charset.forName("ISO-8859-1");

	private final HttpStatus statusCode;

	private final String statusText;

	private final byte[] responseBody;

	private final Charset responseCharset;

	/**
	 * Construct a new instance of {@code HttpStatusCodeException} based on a {@link HttpStatus}.
	 *
	 * @param statusCode the status code
	 */
	protected HttpStatusCodeException(HttpStatus statusCode) {
		this(statusCode, statusCode.name(), null, null);
	}

	/**
	 * Construct a new instance of {@code HttpStatusCodeException} based on a {@link HttpStatus} and status text.
	 *
	 * @param statusCode the status code
	 * @param statusText the status text
	 */
	protected HttpStatusCodeException(HttpStatus statusCode, String statusText) {
		this(statusCode, statusText, null, null);
	}

	/**
	 * Construct a new instance of {@code HttpStatusCodeException} based on a {@link HttpStatus}, status text, and
	 * response body content.
	 *
	 * @param statusCode	  the status code
	 * @param statusText	  the status text
	 * @param responseBody	the response body content, may be {@code null}
	 * @param responseCharset the response body charset, may be {@code null}
	 */
	protected HttpStatusCodeException(HttpStatus statusCode,
			String statusText,
			byte[] responseBody,
			Charset responseCharset) {
		super(statusCode.value() + " " + statusText);
		this.statusCode = statusCode;
		this.statusText = statusText;
		this.responseBody = responseBody != null ? responseBody : new byte[0];
		this.responseCharset = responseCharset != null ? responseCharset : DEFAULT_CHARSET;
	}

	/**
	 * Returns the HTTP status code.
	 */
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}

	/**
	 * Returns the HTTP status text.
	 */
	public String getStatusText() {
		return this.statusText;
	}

	/**
	 * Returns the response body as a byte array.
	 */
	public byte[] getResponseBodyAsByteArray() {
		return responseBody;
	}

	/**
	 * Returns the response body as a string.
	 */
	public String getResponseBodyAsString() {
		try {
			return new String(responseBody, responseCharset.name());
		}
		catch (UnsupportedEncodingException ex) {
			// should not occur
			throw new InternalError(ex.getMessage());
		}
	}
}
