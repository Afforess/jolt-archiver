package com.afforess.jolt.archiver;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class HomePage extends Template{
	public HomePage(Connection conn) {
		super(conn);
	}

	@Override
	protected File getOutputFile(File dir, int page) {
		return new File(dir, "index.html");
	}

	@Override
	protected File getTemplateFile(File dir) {
		return new File(dir, "index.template");
	}

	@Override
	protected void generatePageTemplate(String templateHtml, StringBuilder finalHtml, int page) throws SQLException, IOException {
		System.out.println("Generating HomePage Content");
		Connection conn = getConnection();
		PreparedStatement forums = conn.prepareStatement("SELECT title, description, replycount, lastpost, lastpostid, lastposter, threadcount, lastthread, lastthreadid, forumid FROM forum");
		ResultSet result = forums.executeQuery();
		while(result.next()) {
			System.out.println("Found Forum Topic: " + result.getString(1));
			ForumPage forum = new ForumPage(conn, result.getInt(10), result.getString(1), result.getString(2));
			forum.generate();

			finalHtml.append(templateHtml.replaceAll("%FORUM_TITLE%", "<a href=\"" + forum.getFormattedForumName() + "/index.html\">" + result.getString(1) + "</a>")
					.replaceAll("%FORUM_DESCRIPTION%", result.getString(2)).replaceAll("%THREAD_COUNT%", String.valueOf(forum.getThreadCount())));
		}
		System.out.println("Finished Generating HomePage Content");
	}

	@Override
	protected int getNumPages() {
		return 1;
	}

	@Override
	protected String generateBaseTemplate(String templateHtml) throws IOException, SQLException {
		return templateHtml;
	}
}
