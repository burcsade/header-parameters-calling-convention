package com.didilabs.cc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xins.common.collections.InvalidPropertyValueException;
import org.xins.common.collections.MissingRequiredPropertyException;
import org.xins.common.collections.PropertyReader;
import org.xins.common.io.IOReader;
import org.xins.common.manageable.InitializationException;
import org.xins.common.spec.FunctionSpec;
import org.xins.common.text.TextUtils;
import org.xins.server.API;
import org.xins.server.CustomCallingConvention;
import org.xins.server.FunctionNotSpecifiedException;
import org.xins.server.FunctionRequest;
import org.xins.server.FunctionResult;
import org.xins.server.InvalidRequestException;

public class HeaderParametersCallingConvention extends CustomCallingConvention {

	private static final String TEMPLATES_CACHE_PROPERTY = "templates.cache";
	private static final String TEMPLATES_SOURCE_PROPERTY = "templates.source";
	private static final String TEMPLATES_TAG_PROPERTY = "templates.tag.property";
	
	private String _baseTemplateDir;
	private String _tagProperty;
	private boolean _cacheTemplates;
	private final API _api;	
	
	private Map<String, String> _templateCache = new HashMap<String, String>();
	
	public LandingPageCallingConvention(API api) throws IllegalArgumentException {
		_api = api;
	}
   
	protected void initImpl(PropertyReader runtimeProperties) 
		throws MissingRequiredPropertyException,
		   	InvalidPropertyValueException,
		   	InitializationException {
	   
		// Get the base directory of the templates
		String templatesProperty = TEMPLATES_SOURCE_PROPERTY;
		_baseTemplateDir = runtimeProperties.get(templatesProperty);
		if (_baseTemplateDir == null) {
			throw new MissingRequiredPropertyException(templatesProperty);
		}
	      
		_tagProperty  = runtimeProperties.get(TEMPLATES_TAG_PROPERTY);
		if (_tagProperty == null) {
			throw new MissingRequiredPropertyException(TEMPLATES_TAG_PROPERTY);
		}
	      
		Properties systemProps = System.getProperties();
		templatesProperty = TextUtils.replace(templatesProperty, systemProps, "${", "}");
		templatesProperty = templatesProperty.replace('\\', '/');

		// Determine if the template cache should be enabled
		String cacheEnabled = runtimeProperties.get(TEMPLATES_CACHE_PROPERTY);
		initCacheEnabled(cacheEnabled);
	}
   
	private void initCacheEnabled(String cacheEnabled) {
		if (cacheEnabled == null) {
			_cacheTemplates = true;
		} else {
			cacheEnabled = cacheEnabled.trim();
			if ("false".equals(cacheEnabled)) {
				_cacheTemplates = false;
			} else {
				_cacheTemplates = true;
			}
		}
	}
	   
	protected FunctionRequest convertRequestImpl(HttpServletRequest httpRequest)
		throws InvalidRequestException, FunctionNotSpecifiedException {

		// Determine which function should be invoked
		String page = httpRequest.getParameter("page");

		// The function name must be specified
		if (page == null || "".equals(page)) {
			throw new FunctionNotSpecifiedException();
		}

		Set funcParams = null;

		try {
			FunctionSpec function = _api.getAPISpecification().getFunction(page);
			funcParams = function.getInputParameters().keySet();
		} catch (Exception ex) {
			throw new FunctionNotSpecifiedException();
		}      

		// Directly convert the parameters to a PropertyReader object
		PropertyReader params;
		try {
			params = new ServletRequestHeaderPropertyReader(httpRequest, funcParams);
		} catch (Exception exception) {
			throw new InvalidRequestException("Failed to parse request.", exception);
		}

		// Return an appropriate XINS request object
		return new FunctionRequest(page, params, null);
	}   

	/*
	* If enabled, get the template from the cache. 
	* Otherwise read from the file location and put into the cache
	*/
	private String getTemplate(String page, String tag) throws IOException {
		String templateLocation = _baseTemplateDir + page + "_" + tag + ".html";

		String template;
		if (_cacheTemplates && _templateCache.containsKey(templateLocation)) {
			template = _templateCache.get(templateLocation);
		} else {
			FileInputStream fstream = new FileInputStream(templateLocation);
			template = IOReader.readFully(fstream);
			if (_cacheTemplates) {
				_templateCache.put(templateLocation, template);
			}
		}

		return template;
	}

	/*
	* Gets template tag from template parameter 
	* and returns the template with filename command_tag.html in _baseTemplateDir 
	*/
	protected void convertResultImpl(FunctionResult xinsResult,
		HttpServletResponse httpResponse,
		HttpServletRequest  httpRequest)
		throws IOException {

		String page = httpRequest.getParameter("page");
		String templateTag = xinsResult.getParameter(_tagProperty);

		if (page != null && !page.isEmpty() 
			&& templateTag != null && !templateTag.isEmpty()) {
			String htmlSource = getTemplate(page, templateTag);
			httpResponse.setContentType("text/html;charset=ISO-8859-1");
			httpResponse.setStatus(HttpServletResponse.SC_OK);
			Writer output = httpResponse.getWriter();
			output.write(htmlSource);
			output.close();
			return;
		}
	}
}
