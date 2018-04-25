package com.gentics.mesh.parameter;

public interface UploadParameters extends ParameterProvider {
	
	public static final String LANG_QUERY_PARAM_KEY = "lang";
	
	public static final String VER_QUERY_PARAM_KEY = "ver";
	
	default String getLanguage() {
		return getParameter(LANG_QUERY_PARAM_KEY);
	}
	
	default UploadParameters setLanguage(String language) {
		setParameter(LANG_QUERY_PARAM_KEY, language);
		return this;
	}
	
	default String getVersion() {
		return getParameter(VER_QUERY_PARAM_KEY);
	}
	
	default UploadParameters setVersion(String version) {
		setParameter(VER_QUERY_PARAM_KEY, version);
		return this;
	}

}
