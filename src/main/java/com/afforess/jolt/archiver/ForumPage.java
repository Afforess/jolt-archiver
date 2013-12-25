package com.afforess.jolt.archiver;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ForumPage extends Template{
	private final int id;
	private final String forumName;
	private final String forumDescription;
	private static final SimpleDateFormat DATE = new SimpleDateFormat("EEE, d MMM yyyy hh:mm aaa");
	private static final SimpleDateFormat HTML_DATETIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	private int threadCount = 0;
	public ForumPage(Connection conn, int id, String forumName, String forumDescription) {
		super(conn);
		this.id = id;
		this.forumName = forumName;
		this.forumDescription = forumDescription;
	}
	
	public int getThreadCount() {
		return threadCount;
	}

	public String getFormattedForumName() {
		return forumName.toLowerCase().replaceAll("\\&|\\(.*\\)|\\?|amp;", "").replaceAll(" ", "-");
	}

	@Override
	protected File getOutputFile(File dir) {
		return new File(dir, getFormattedForumName() + ".html");
	}

	@Override
	protected File getTemplateFile(File dir) {
		return new File(dir, "forumview.template");
	}

	@Override
	protected void generateTemplate(String templateHtml, StringBuilder finalHtml) throws SQLException {
		System.out.println("Generating Forum Content for [ " + forumName + " ]");
		threadCount = 0;
		int start = finalHtml.indexOf("%FORUM_NAME%");
		finalHtml.replace(start, start + "%FORUM_NAME%".length(), forumName);
		start = finalHtml.indexOf("%FORUM_NAME%");
		finalHtml.replace(start, start + "%FORUM_NAME%".length(), forumName);
		start = finalHtml.indexOf("%FORUM_DESCRIPTION%");
		finalHtml.replace(start, start + "%FORUM_DESCRIPTION%".length(), forumDescription);
		
		Connection conn = getConnection();
		PreparedStatement forums = conn.prepareStatement("SELECT threadid, title, lastpost, replycount, postusername, postuserid, lastposter, dateline FROM jolt.thread WHERE forumid = ?");
		forums.setInt(1, id);
		ResultSet result = forums.executeQuery();
		while(result.next()) {
			threadCount++;
			System.out.println("Found Thread Topic: " + result.getString(2));
			Date postTime = new Date(result.getLong(8) * 1000L);
			String time = "<time datetime=\"" + HTML_DATETIME.format(postTime) + "\">" + DATE.format(postTime) + "</time>";
			finalHtml.append(templateHtml.replaceAll("%THREAD_TITLE%", result.getString(2)).replaceAll("%REPLY_COUNT%", String.valueOf(result.getInt(4)))
										.replaceAll("%THREAD_OWNER%", result.getString(5)).replaceAll("%POSTED_DATE%", time)
										.replaceAll("%LAST_REPLY_USER%", result.getString(7)));
		}
	}

}
