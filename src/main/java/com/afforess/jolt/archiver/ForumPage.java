package com.afforess.jolt.archiver;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.dbutils.DbUtils;

public class ForumPage extends Template{
	private final int id;
	private final String forumName;
	private final String forumDescription;
	private static final SimpleDateFormat DATE = new SimpleDateFormat("EEE, d MMM yyyy hh:mm aaa");
	private static final SimpleDateFormat HTML_DATETIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	private int threadCount = 0;
	public ForumPage(Connection conn, File baseDir, int id, String forumName, String forumDescription) {
		super(conn, baseDir);
		this.id = id;
		this.forumName = forumName;
		this.forumDescription = forumDescription;
	}

	public int getThreadCount() {
		return threadCount;
	}

	public String getFormattedForumName() {
		return forumName.toLowerCase().replaceAll("\\&|\\(.*\\)|\\?|amp;|quot;|:|\\.|,|!|\\[|\\]", "").replaceAll(" +", "-");
	}

	@Override
	protected File getOutputFile(File dir, int page) {
		return new File(new File(dir, getFormattedForumName()), page == 0 ? "index.html" : "page-" + page + ".html");
	}

	@Override
	protected File getTemplateFile(File dir) {
		return new File(dir, "forum.template");
	}

	@Override
	protected void generatePageTemplate(String templateHtml, StringBuilder finalHtml, int page) throws SQLException, IOException {
		if (JoltArchiver.VERBOSE) System.out.println("Generating Forum Content for [ " + forumName + " ]");
		Connection conn = getConnection();
		ResultSet result = null;
		PreparedStatement forums = null;
		try {
			forums = conn.prepareStatement("SELECT threadid, title, lastpost, replycount, postusername, postuserid, lastposter, dateline FROM thread WHERE forumid = ? ORDER BY dateline DESC LIMIT ?, ?");
			forums.setInt(1, id);
			forums.setInt(2, page * ITEMS_PER_PAGE);
			forums.setInt(3, ITEMS_PER_PAGE);
			result = forums.executeQuery();
			while(result.next()) {
				threadCount++;
				if (JoltArchiver.VERBOSE) {
					System.out.println("Found Thread Topic: " + result.getString(2));
				}
				
				ThreadPage posts = new ThreadPage(conn, new File(TEMPLATE_DIR, "threads"), result.getInt(1), result.getString(2), "<span class='breadcrumb-parent'><a class='breadcrumb' href=\"../../forums/" + getFormattedForumName() + "/index.html\"><span style='font-weight:bold;' class='breadcrumb-text'>" + forumName + "</span></a><span class='arrow'><span></span></span></span>");
				posts.generate();
				
				Date postTime = new Date(result.getLong(8) * 1000L);
				String time = "<time datetime=\"" + HTML_DATETIME.format(postTime) + "\">" + DATE.format(postTime) + "</time>";
				finalHtml.append(templateHtml.replaceAll("%THREAD_TITLE%", "<a href=\"../../threads/" + posts.getFormattedThreadName() + "/index.html\">" + result.getString(2) + "</a>").replaceAll("%REPLY_COUNT%", String.valueOf(result.getInt(4)))
											.replaceAll("%THREAD_OWNER%", result.getString(5)).replaceAll("%POSTED_DATE%", time)
											.replaceAll("%LAST_REPLY_USER%", result.getString(7)));
			}
		} finally {
			DbUtils.closeQuietly(result);
			DbUtils.closeQuietly(forums);
		}
	}

	@Override
	protected int getNumPages() throws SQLException {
		Connection conn = getConnection();
		ResultSet result = null;
		PreparedStatement forums = null;
		try {
			forums = conn.prepareStatement("SELECT count(*) FROM thread WHERE forumid = ?");
			forums.setInt(1, id);
			result = forums.executeQuery();
			result.next();
			return result.getInt(1) / ITEMS_PER_PAGE + 1;
		} finally {
			DbUtils.closeQuietly(result);
			DbUtils.closeQuietly(forums);
		}
	}

	@Override
	protected String generateBaseTemplate(String templateHtml) throws IOException, SQLException {
		return super.generateBaseTemplate(templateHtml.replaceAll("%FORUM_NAME%", forumName).replaceAll("%FORUM_DESCRIPTION%", forumDescription));
	}
}
