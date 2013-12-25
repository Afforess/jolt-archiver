package com.afforess.jolt.archiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.io.IOUtils;

public abstract class Template {
	private static final String TEMPLATE_END = "<!--Template End-->";
	private static final String TEMPLATE_START = "<!--Template Start-->";
	public static final File TEMPLATE_DIR = new File("./templates/bootstrap/");
	private final Connection conn;
	public Template(Connection conn) {
		this.conn = conn;
	}

	public final Connection getConnection() {
		return conn;
	}

	protected abstract File getOutputFile(File dir);

	protected abstract File getTemplateFile(File dir);

	protected abstract void generateTemplate(String templateHtml, StringBuilder finalHtml) throws IOException, SQLException;

	public final void generate() throws IOException, SQLException {
		File output = getOutputFile(TEMPLATE_DIR);
		File template = getTemplateFile(TEMPLATE_DIR);
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(template);
			final String html = IOUtils.toString(fis);
			int templateStart = html.indexOf(TEMPLATE_START);
			int templateEnd = html.indexOf(TEMPLATE_END);
			final String templateHtml = html.substring(templateStart + TEMPLATE_START.length(), templateEnd);

			StringBuilder builder = new StringBuilder(html.substring(0, templateStart));
			generateTemplate(templateHtml, builder);

			builder.append(html.substring(templateEnd + TEMPLATE_END.length()));
			fos = new FileOutputStream(output);
			IOUtils.write(builder, fos);
		} finally {
			IOUtils.closeQuietly(fis);
			IOUtils.closeQuietly(fos);
		}
	}

}
