package com.didilabs.cc;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.xins.common.MandatoryArgumentChecker;
import org.xins.common.collections.AbstractPropertyReader;
import org.xins.common.text.ParseException;

public final class ServletRequestHeaderPropertyReader extends AbstractPropertyReader {

	public ServletRequestHeaderPropertyReader(ServletRequest request)
		throws NullPointerException {
		super(request.getParameterMap());
	}

	public ServletRequestHeaderPropertyReader(HttpServletRequest request, Set validParams)
		throws IllegalArgumentException, ParseException {

		super(new HashMap(20));

		MandatoryArgumentChecker.check("request", request);

		Map properties = getPropertiesMap();

		// Get the HTTP headers 
		Enumeration headerNames = request.getHeaderNames();

		// Short-circuit if the headers are empty
		if (!headerNames.hasMoreElements()) {
			return;
		}

		// Parse the parameters in the HTTP headers
		try {
			while (headerNames.hasMoreElements()) {
				String headerKey = (String) headerNames.nextElement();
				String headerValue = request.getHeader(headerKey);

				// Add it if it is in validParams or validParams is not defined
				if (validParams == null || validParams.contains(headerKey)) {
					add(properties, headerKey, headerValue);
				}
			}
		} catch (Exception cause) {
			throw new ParseException("Failed to parse HTTP headers.", cause, "Parsing failed.");
		}
	}

	private static void add(Map properties, String key, String value)
		throws NullPointerException, ParseException {

		Object existingValue = properties.get(key);
		if (existingValue != null && ! existingValue.equals(value)) {
			String detail = "Conflicting values found for parameter \""
			+ key
			+ "\": \""
			+ (String) existingValue
			+ "\" vs \""
			+ value
			+ "\".";
			throw new ParseException("Failed to parse HTTP headers.", (Throwable) null, detail);
		}

		properties.put(key, value);
	}
}
