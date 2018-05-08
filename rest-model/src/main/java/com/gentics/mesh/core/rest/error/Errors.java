package com.gentics.mesh.core.rest.error;

import io.netty.handler.codec.http.HttpResponseStatus;

public enum Errors {

	NAME_CONFLICT(NameConflictException.TYPE, NameConflictException.class),

	GENERAL_ERROR(GenericRestException.TYPE, GenericRestException.class),

	NODE_VERSION_CONFLICT(NodeVersionConflictException.TYPE, NodeVersionConflictException.class);

	private String type;
	private Class<?> clazz;

	private Errors(String type, Class<?> clazz) {
		this.type = type;
		this.clazz = clazz;
	}

	public String getType() {
		return type;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	/**
	 * Create a new http conflict exception.
	 * 
	 * @param conflictingUuid
	 *            Uuid of the object which was part of the conflict
	 * @param conflictingName
	 *            Name field value which caused the conflict
	 * @param i18nMessageKey
	 *            I18n key
	 * @param parameters
	 *            I18n message parameters
	 * @return
	 */
	public static NameConflictException conflict(String conflictingUuid, String conflictingName, String i18nMessageKey, String... parameters) {
		NameConflictException error = new NameConflictException(i18nMessageKey, parameters);
		error.setProperty("conflictingUuid", conflictingUuid);
		error.setProperty("conflictingName", conflictingName);
		return error;
	}

	/**
	 * Create a new http conflict exception.
	 * 
	 * @param conflictingUuid
	 *            Uuid of the object which was part of the conflict
	 * @param conflictingName
	 *            Name field value which caused the conflict
	 * @param conflictingLanguage
	 *            Language which caused the conflict
	 * @param i18nMessageKey
	 *            I18n key
	 * @param parameters
	 *            I18n message parameters
	 * @return
	 */
	public static NameConflictException nodeConflict(String conflictingUuid, String conflictingName, String conflictingLanguage,
			String i18nMessageKey, String... parameters) {
		NameConflictException error = new NameConflictException(i18nMessageKey, parameters);
		error.setProperty("conflictingUuid", conflictingUuid);
		error.setProperty("conflictingName", conflictingName);
		error.setProperty("conflictingLanguage", conflictingLanguage);
		return error;
	}

	/**
	 * Create a i18n translated error exception.
	 * 
	 * @param status
	 *            Http status
	 * @param i18nMessageKey
	 *            i18n key
	 * @param parameters
	 *            i18n parameters
	 * @return
	 */
	public static GenericRestException error(HttpResponseStatus status, String i18nMessageKey, String... parameters) {
		return new GenericRestException(status, i18nMessageKey, parameters);
	}
	
	/**
	 * Create a i18n translated permission error exception.
	 * 
	 * @param elementType
	 * @param elementDescription
	 * @return
	 */
	public static PermissionException missingPerm(String elementType, String elementDescription) {
		return new PermissionException(elementType, elementDescription);
	}

	/**
	 * Create a i18n translated error exception.
	 * 
	 * @param status
	 *            Http status
	 * @param i18nMessageKey
	 *            i18n key
	 * @param t
	 *            Nested exception
	 * @return
	 */
	public static GenericRestException error(HttpResponseStatus status, String i18nMessageKey, Throwable t) {
		return new GenericRestException(status, i18nMessageKey, t);
	}
	
	/**
	 * Creates a generic i18n translated REST error exception.
	 * 
	 * @param status HTTP status.
	 * @param t The cause exception.
	 * @param i18nMessageKey Message key (defined in translations_*.properties).
	 * @param params Parameters for the message.
	 * 
	 * @return The created exception.
	 */
	public static GenericRestException error(HttpResponseStatus status, Throwable t, String i18nMessageKey, String... params) {
		return new GenericRestException(status, t, i18nMessageKey, params);
	}
	
	/**
	 * Creates an i18n REST internal error exception.
	 * 
	 * @param message The message as plain text or i18n message key.
	 * @param parameters Optional message parameters.
	 * 
	 * @return The created exception.
	 */
	public static GenericRestException internalError(String message, String... parameters) {
		return error(HttpResponseStatus.INTERNAL_SERVER_ERROR, message, parameters);
	}
	
	/**
	 * Creates an i18n REST internal error exception.
	 * 
	 * @param i18nMessageKey Message key (defined in translations_*.properties).
	 * @param t The cause exception.
	 * 
	 * @return The created exception.
	 */
	public static GenericRestException internalError(String i18nMessageKey, Throwable t) {
		return error(HttpResponseStatus.INTERNAL_SERVER_ERROR, i18nMessageKey, t);
	}
	
	/**
	 * Creates an i18n REST internal error exception.
	 * 
	 * @param t The cause exception.
	 * @param i18nMessageKey Message key (defined in translations_*.properties).
	 * @param i18nMessageParams Message parameters.
	 * 
	 * @return The created exception.
	 */
	public static GenericRestException internalError(Throwable t, String i18nMessageKey, String... i18nMessageParams) {
		return error(HttpResponseStatus.INTERNAL_SERVER_ERROR, t, i18nMessageKey, i18nMessageParams);
	}
	
	/**
	 * Resolve the given typeName to a registered type.
	 * 
	 * @param typeName
	 * @return
	 */
	public static Errors valueByName(String typeName) {
		for (Errors type : values()) {
			if (type.getType()
					.equals(typeName)) {
				return type;
			}
		}
		return null;
	}

}
