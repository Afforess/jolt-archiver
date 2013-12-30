package com.afforess.jolt.archiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;

import org.apache.commons.io.IOUtils;

public abstract class Template {
	private static final String TEMPLATE_END = "<!--Template End-->";
	private static final String TEMPLATE_START = "<!--Template Start-->";
	public static final File TEMPLATE_DIR = new File("./templates/bootstrap/");
	public static final int ITEMS_PER_PAGE = 20;
	private final Connection conn;
	private final File baseDir;
	public Template(Connection conn, File baseDir) {
		this.conn = conn;
		this.baseDir = baseDir;
		baseDir.mkdirs();
	}

	public final Connection getConnection() {
		return conn;
	}

	protected abstract File getOutputFile(File dir, int page);

	protected abstract File getTemplateFile(File dir);

	protected String generateBaseTemplate(String templateHtml) throws IOException, SQLException {
		final int pages = getNumPages();
		if (pages <= 1) {
			return templateHtml.replaceAll("%PAGINATION%", "");
		} else {
			return templateHtml.replaceAll("%PAGINATION%", Matcher.quoteReplacement("<script type='text/javascript'>var options = {bootstrapMajorVersion: 3, currentPage: %CURRENT_PAGE%, totalPages: " + pages + ", shouldShowPage: function(type, page, current){ if (type == 'first' || type == 'last') return false; return true;}, pageUrl: function(type, page, current) { return (page == 1 ? \"index.html\" : \"page-\" + (page - 1) + \".html\"); }}; $('#pagination').bootstrapPaginator(options);</script>"));
		}
	}

	protected abstract void generatePageTemplate(String templateHtml, StringBuilder finalHtml, int page) throws IOException, SQLException;

	protected abstract int getNumPages() throws SQLException;

	public final void generate() throws IOException, SQLException {
		File template = getTemplateFile(TEMPLATE_DIR);
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream(template);
			final String html = generateBaseTemplate(IOUtils.toString(fis));

			int templateStart = html.indexOf(TEMPLATE_START);
			int templateEnd = html.indexOf(TEMPLATE_END);
			final String templateHtml = html.substring(templateStart + TEMPLATE_START.length(), templateEnd);

			final int pages = getNumPages();
			for (int i = 0; i < pages; i++) {
				try {
					StringBuilder builder = new StringBuilder(html.substring(0, templateStart));

					File output = getOutputFile(baseDir, i);
					output.getParentFile().mkdirs();
					generatePageTemplate(templateHtml, builder, i);
	
					builder.append(html.substring(templateEnd + TEMPLATE_END.length()).replaceAll("%CURRENT_PAGE%", String.valueOf(i + 1)));
					fos = new FileOutputStream(output);
					IOUtils.write(builder, fos);
				} finally {
					IOUtils.closeQuietly(fos);
				}
			}
		} finally {
			IOUtils.closeQuietly(fis);
		}
	}
}
