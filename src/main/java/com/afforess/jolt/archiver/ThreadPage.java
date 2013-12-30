package com.afforess.jolt.archiver;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.dbutils.DbUtils;

public class ThreadPage extends Template{
	private final int id;
	private final String threadName;
	private final String forumName;
	private static final SimpleDateFormat DATE = new SimpleDateFormat("EEE, d MMM yyyy hh:mm aaa");
	private static final SimpleDateFormat HTML_DATETIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	public ThreadPage(Connection conn, File baseDir, int id, String threadName, String forumName) {
		super(conn, baseDir);
		this.id = id;
		this.threadName = threadName;
		this.forumName = forumName;
	}

	public String getFormattedThreadName() {
		return threadName.toLowerCase().replaceAll("\\&|\\(.*\\)|\\?|amp;|quot;|:|\\.|,|!|\\[|\\]", "").replaceAll(" +", "-");
	}

	@Override
	protected File getOutputFile(File dir, int page) {
		return new File(new File(dir, getFormattedThreadName()), page == 0 ? "index.html" : "page-" + page + ".html");
	}

	@Override
	protected File getTemplateFile(File dir) {
		return new File(dir, "thread.template");
	}

	@Override
	protected void generatePageTemplate(String templateHtml, StringBuilder finalHtml, int page) throws SQLException {
		if (JoltArchiver.VERBOSE) System.out.println("Generating Thread Content for [ " + threadName + " ]");
		Connection conn = getConnection();
		ResultSet result = null;
		PreparedStatement forums = null;
		try {
			forums = conn.prepareStatement("SELECT postid, username, dateline, pagetext FROM post WHERE threadid = ? ORDER BY dateline DESC LIMIT ?, ?");
			forums.setInt(1, id);
			forums.setInt(2, page * ITEMS_PER_PAGE);
			forums.setInt(3, ITEMS_PER_PAGE);
			result = forums.executeQuery();
			while(result.next()) {
				Date postTime = new Date(result.getLong(3) * 1000L);
				String time = "<time datetime=\"" + HTML_DATETIME.format(postTime) + "\">" + DATE.format(postTime) + "</time>";
				finalHtml.append(templateHtml.replaceAll("%POST_USERNAME%", String.valueOf(result.getString(2))).replaceAll("%POST_TIME%", time).replaceAll("%POST_CONTENT%", parseBBCode(result.getString(4))));
			}
		} finally {
			DbUtils.closeQuietly(result);
			DbUtils.closeQuietly(forums);
		}
	}
	
	private static final Map<Pattern, String> bbcodeMap = new HashMap<Pattern, String>();
	static {
		bbcodeMap.put(Pattern.compile("\\[(b|B)\\](.+?)\\[/(b|B)\\]"), "<strong>$2</strong>");
		bbcodeMap.put(Pattern.compile("\\[(i|I)\\](.+?)\\[/(i|I)\\]"), "<span class='italic'>$2</span>");
		bbcodeMap.put(Pattern.compile("\\[(u|U)\\](.+?)\\[/(u|U)\\]"), "<span class='underline'>$2</span>");
		bbcodeMap.put(Pattern.compile("\\[(h|H)1\\](.+?)\\[/(h|H)1\\]"), "<h1>$2</h1>");
		bbcodeMap.put(Pattern.compile("\\[(h|H)2\\](.+?)\\[/(h|H)2\\]"), "<h2>$2</h2>");
		bbcodeMap.put(Pattern.compile("\\[(h|H)3\\](.+?)\\[/(h|H)3\\]"), "<h3>$2</h3>");
		bbcodeMap.put(Pattern.compile("\\[(h|H)4\\](.+?)\\[/(h|H)4\\]"), "<h4>$2</h4>");
		bbcodeMap.put(Pattern.compile("\\[(h|H)5\\](.+?)\\[/(h|H)5\\]"), "<h5>$2</h5>");
		bbcodeMap.put(Pattern.compile("\\[(h|H)6\\](.+?)\\[/(h|H)6\\]"), "<h6>$2</h6>");
		bbcodeMap.put(Pattern.compile("\\[(quote|QUOTE)\\]"), "<blockquote>");
		bbcodeMap.put(Pattern.compile("\\[/(quote|QUOTE)\\]"), "</blockquote>");
		bbcodeMap.put(Pattern.compile("\\[(quote|QUOTE)=(.+?)\\]"), "<div class='quote'>$2</div><blockquote>");
		bbcodeMap.put(Pattern.compile("\\[(center|CENTER)\\](.+?)\\[/(center|CENTER)\\]"), "<div align='center'>$2</div>");
		bbcodeMap.put(Pattern.compile("\\[(align|ALIGN)=(.+?)\\](.+?)\\[/(align|ALIGN)\\]"), "<div align='$2'>$3</div>");
		bbcodeMap.put(Pattern.compile("\\[(color|COLOR)=(.+?)\\](.+?)\\[/(color|COLOR)\\]"), "<span style='color:$2;'>$3</span>");
		bbcodeMap.put(Pattern.compile("\\[(size|SIZE)=(.+?)\\](.+?)\\[/(size|SIZE)\\]"), "<span style='font-size:$2;'>$3</span>");
		bbcodeMap.put(Pattern.compile("\\[(img|IMG)\\](.+?)\\[/(img|IMG)\\]"), "<img src='$2' />");
		bbcodeMap.put(Pattern.compile("\\[(url|URL)\\](.+?)\\[/(url|URL)\\]"), "<a href='$2'>$2</a>");
		bbcodeMap.put(Pattern.compile("\\[(url|URL)=(.+?)\\](.+?)\\[/(url|URL)\\]"), "<a href='$2'>$3</a>");
	}

	private static String parseBBCode(String text) {
		for (Map.Entry<Pattern, String> entry : bbcodeMap.entrySet()) {
			text = entry.getKey().matcher(text).replaceAll(entry.getValue());
		}

		return text;
	}

	@Override
	protected int getNumPages() throws SQLException {
		Connection conn = getConnection();
		ResultSet result = null;
		PreparedStatement forums = null;
		try {
			forums = conn.prepareStatement("SELECT count(*) FROM post WHERE threadid = ?");
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
		return super.generateBaseTemplate(templateHtml.replaceAll("%THREAD_NAME%", threadName)).replaceAll("%FORUM_NAME%", forumName);
	}
}
