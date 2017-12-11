package com.gentics.mesh.core.field.html;

import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;

public interface HtmlListFieldHelper {

	public static final String TEXT1 = "<i>one</i>";

	public static final String TEXT2 = "<b>two</b>";

	public static final String TEXT3 = "<u>three</u>";

	public static final DataProvider FILLTEXT = (container, name) -> {
		HtmlGraphFieldList field = container.createHTMLList(name);
		field.setList(TEXT1, TEXT2, TEXT3);
	};

	public static final DataProvider FILLNUMBERS = (container, name) -> {
		HtmlGraphFieldList field = container.createHTMLList(name);
		field.setList("1", "0");
	};

	public static final DataProvider CREATE_EMPTY = (container, name) -> container.createHTMLList(name);

	public static final DataProvider FILLTRUEFALSE = (container, name) -> {
		HtmlGraphFieldList field = container.createHTMLList(name);
		field.setList("true", "false");
	};

	public static final FieldFetcher FETCH = (container, name) -> container.getHTMLList(name);

}
