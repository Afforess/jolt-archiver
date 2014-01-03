package com.afforess.jolt.archiver;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.dbutils.DbUtils;

import static java.util.regex.Matcher.quoteReplacement;

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
		return String.valueOf(id);
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
			forums = conn.prepareStatement("SELECT postid, username, dateline, pagetext FROM post WHERE visible = 1 AND threadid = ? ORDER BY dateline ASC LIMIT ?, ?");
			forums.setInt(1, id);
			forums.setInt(2, page * ITEMS_PER_PAGE);
			forums.setInt(3, ITEMS_PER_PAGE);
			result = forums.executeQuery();
			while(result.next()) {
				Date postTime = new Date(result.getLong(3) * 1000L);
				String time = "<time datetime=\"" + HTML_DATETIME.format(postTime) + "\">" + DATE.format(postTime) + "</time>";
				final String postContent = parseBBCode(result.getString(4));
				finalHtml.append(templateHtml.replaceAll("%POST_USERNAME%", quoteReplacement(String.valueOf(result.getString(2))))
											.replaceAll("%POST_TIME%", quoteReplacement(time)).replaceAll("%POST_CONTENT%", quoteReplacement(postContent))
											.replaceAll("%POST_ID%", String.valueOf(result.getInt(1))));
			}
		} finally {
			DbUtils.closeQuietly(result);
			DbUtils.closeQuietly(forums);
		}
	}

	private static final Map<Pattern, String> bbcodeMap = new LinkedHashMap<Pattern, String>();
	static {
		bbcodeMap.put(Pattern.compile("http://forums.jolt.co.uk/showthread.php\\?t=([0-9]+)", Pattern.CASE_INSENSITIVE), "../$1/index.html");
		
		bbcodeMap.put(Pattern.compile("\n"), "<br>");
//      bbcodeMap.put(Pattern.compile("\r\n"), "<p>");

		bbcodeMap.put(Pattern.compile("\\[b\\]", Pattern.CASE_INSENSITIVE), "<strong>");
		bbcodeMap.put(Pattern.compile("\\[/b\\]", Pattern.CASE_INSENSITIVE), "</strong>");
		bbcodeMap.put(Pattern.compile("\\[i\\]", Pattern.CASE_INSENSITIVE), "<span class='italic'>");
		bbcodeMap.put(Pattern.compile("\\[/i\\]", Pattern.CASE_INSENSITIVE), "</span>");
		bbcodeMap.put(Pattern.compile("\\[u\\]", Pattern.CASE_INSENSITIVE), "<span class='underline'>");
		bbcodeMap.put(Pattern.compile("\\[/u\\]", Pattern.CASE_INSENSITIVE), "</span>");
		bbcodeMap.put(Pattern.compile("\\[list\\]", Pattern.CASE_INSENSITIVE), "<ul style=\"list-style-type: disc;\">");
		bbcodeMap.put(Pattern.compile("\\[/list\\]", Pattern.CASE_INSENSITIVE), "</ul>");
		bbcodeMap.put(Pattern.compile("\\[list=[0-9]\\]", Pattern.CASE_INSENSITIVE), "<ul style=\"list-style-type: decimal;\">");
		bbcodeMap.put(Pattern.compile("\\[\\*\\]", Pattern.CASE_INSENSITIVE), "</li><li>");
		bbcodeMap.put(Pattern.compile("\\[h([0-6])\\]", Pattern.CASE_INSENSITIVE), "<h$1>");
		bbcodeMap.put(Pattern.compile("\\[/h([0-6])\\]", Pattern.CASE_INSENSITIVE), "</h$1>");
		bbcodeMap.put(Pattern.compile("\\[indent\\]", Pattern.CASE_INSENSITIVE), "<span style='padding-left:30px;'>");
		bbcodeMap.put(Pattern.compile("\\[/indent\\]", Pattern.CASE_INSENSITIVE), "</span>");
		bbcodeMap.put(Pattern.compile("\\[code\\]", Pattern.CASE_INSENSITIVE), "<pre>");
		bbcodeMap.put(Pattern.compile("\\[/code\\]", Pattern.CASE_INSENSITIVE), "</pre>");
		bbcodeMap.put(Pattern.compile("\\[quote\\]", Pattern.CASE_INSENSITIVE), "<blockquote>");
		bbcodeMap.put(Pattern.compile("\\[/quote\\]", Pattern.CASE_INSENSITIVE), "</blockquote>");
		bbcodeMap.put(Pattern.compile("\\[quote=(.+?);([0-9]+)\\]", Pattern.CASE_INSENSITIVE), "<div postid='$2' class='quote'>$1</div><blockquote>");
		bbcodeMap.put(Pattern.compile("\\[quote=(.+?)\\]", Pattern.CASE_INSENSITIVE), "<div class='quote'>$1</div><blockquote>");
		bbcodeMap.put(Pattern.compile("\\[center\\]", Pattern.CASE_INSENSITIVE), "<div align='center'>");
		bbcodeMap.put(Pattern.compile("\\[/center\\]", Pattern.CASE_INSENSITIVE), "</div>");
		bbcodeMap.put(Pattern.compile("\\[align=(.+?)\\]", Pattern.CASE_INSENSITIVE), "<div align='$1'>");
		bbcodeMap.put(Pattern.compile("\\[/align\\]", Pattern.CASE_INSENSITIVE), "</div>");
		bbcodeMap.put(Pattern.compile("\\[color=\"(.+?)\"\\]", Pattern.CASE_INSENSITIVE), "<span style='color:$1;'>");
		bbcodeMap.put(Pattern.compile("\\[color=(.+?)\\]", Pattern.CASE_INSENSITIVE), "<span style='color:$1;'>");
		bbcodeMap.put(Pattern.compile("\\[/color\\]", Pattern.CASE_INSENSITIVE), "</span>");
		bbcodeMap.put(Pattern.compile("\\[size=\"([1-7])\"\\]", Pattern.CASE_INSENSITIVE), "<span class='bbcode-size-$1'>");
		bbcodeMap.put(Pattern.compile("\\[size=([1-7])\\]", Pattern.CASE_INSENSITIVE), "<span class='bbcode-size-$1'>");
		bbcodeMap.put(Pattern.compile("\\[size=\"(.+?)\"\\]", Pattern.CASE_INSENSITIVE), "<span style='font-size:$1;'>");
		bbcodeMap.put(Pattern.compile("\\[size=(.+?)\\]", Pattern.CASE_INSENSITIVE), "<span style='font-size:$1;'>");
		bbcodeMap.put(Pattern.compile("\\[/size\\]", Pattern.CASE_INSENSITIVE), "</span>");
		bbcodeMap.put(Pattern.compile("\\[font=(.+?)\\]", Pattern.CASE_INSENSITIVE), "<span style='font-family:$1'>");
		bbcodeMap.put(Pattern.compile("\\[/font\\]", Pattern.CASE_INSENSITIVE), "</span>");
		bbcodeMap.put(Pattern.compile("\\[left\\]", Pattern.CASE_INSENSITIVE), "<span class=\"left-align\">");
		bbcodeMap.put(Pattern.compile("\\[/left\\]", Pattern.CASE_INSENSITIVE), "</span>");
		bbcodeMap.put(Pattern.compile("\\[img\\](.+?)\\[/img\\]", Pattern.CASE_INSENSITIVE), "<img src='$1' />");
		bbcodeMap.put(Pattern.compile("\\[url=\"(.+?)\"\\](.+?)\\[/url\\]", Pattern.CASE_INSENSITIVE), "<a href='$1'>$2</a>");
		bbcodeMap.put(Pattern.compile("\\[url=(.+?)\\](.+?)\\[/url\\]", Pattern.CASE_INSENSITIVE), "<a href='$1'>$2</a>");
		bbcodeMap.put(Pattern.compile("\\[url\\](.+?)\\[/url\\]", Pattern.CASE_INSENSITIVE), "<a href='$1'>$1</a>");
	}

	private String parseBBCode(String text) throws SQLException {
		for (Map.Entry<Pattern, String> entry : bbcodeMap.entrySet()) {
			text = entry.getKey().matcher(text).replaceAll(entry.getValue());
		}

		//link post ids
		int start = text.indexOf("<div postid='");
		while(start > -1) {
			int end = text.indexOf("'", start + "<div postid='".length());
			try {
				int postId = Integer.parseInt(text.substring(start + "<div postid='".length(), end));
				Connection conn = getConnection();
				PreparedStatement threadLookup = null;
				PreparedStatement postCount = null;
				ResultSet result = null;
				ResultSet set = null;
				try {
					threadLookup = conn.prepareStatement("SELECT threadid FROM post WHERE postid = ?");
					threadLookup.setInt(1, postId);
					set = threadLookup.executeQuery();
					if (set.next()) {
						int threadId = set.getInt(1);
						postCount = conn.prepareStatement("SELECT count(*) FROM post WHERE threadid = ? AND postid < ?");
						postCount.setInt(1, threadId);
						postCount.setInt(2, postId);
						result = postCount.executeQuery();
						if (result.next()) {
							int count = result.getInt(1);
							int tagEnd = text.indexOf(">", end);
							int tagStart = text.indexOf("</div>", tagEnd + 1);
							StringBuilder builder = new StringBuilder(text.substring(0, start));
							builder.append("<div class='quote'><a href='../").append(threadId).append("/").append(count < ITEMS_PER_PAGE ? "index.html#" : "page-" + (count / ITEMS_PER_PAGE) + ".html#").append(postId).append("'>").append(text.substring(tagEnd, tagStart)).append("</a>").append(text.substring(tagStart));
							text = builder.toString();
						}  else break;
					} else break;
				} finally {
					DbUtils.closeQuietly(set);
					DbUtils.closeQuietly(result);
					DbUtils.closeQuietly(postCount);
					DbUtils.closeQuietly(threadLookup);
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			start = text.indexOf("<div postid='");
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
		return super.generateBaseTemplate(templateHtml.replaceAll("%THREAD_NAME%", quoteReplacement(threadName)).replaceAll("%FORUM_NAME%", quoteReplacement(forumName)));
	}
}
