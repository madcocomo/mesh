package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.UploadParameters;

public class UploadParametersImpl extends AbstractParameters implements UploadParameters {
	
	public UploadParametersImpl() {
		super();
	}

	public UploadParametersImpl(ActionContext ac) {
		super(ac);
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();
		
		// lang
		QueryParameter langParameter = new QueryParameter();
		langParameter.setDescription(
				"ISO 639-1 language tag of the language which should be loaded. "
				+ "Fallback handling can be applied by specifying multiple languages in a comma-separated list. "
				+ "The first matching language will be returned. "
				+ "If omitted or the requested language is not available then the _defaultLanguage_ as configured in _mesh.yml_ will be returned.");
		langParameter.setExample("en");
		langParameter.setRequired(false);
		langParameter.setType(ParamType.STRING);
		parameters.put(LANG_QUERY_PARAM_KEY, langParameter);
		
		// ver
		QueryParameter verParameter = new QueryParameter();
		verParameter.setDescription("Current version of the field");
		verParameter.setExample("0.2");
		verParameter.setRequired(true);
		verParameter.setType(ParamType.STRING);
		parameters.put(VER_QUERY_PARAM_KEY, verParameter);
		
		return parameters;
	}

	@Override
	public String getName() {
		return "Binary upload parameters";
	}

}
