package com.afforess.jolt.archiver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.IOUtils;

public class JoltArchiver {
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		createTemplates();
		if (args.length != 5) {
			System.err.println("Missing arguments. Must be (5) arguments: (hostname) (port) (schema) (username) (password).");
			return;
		}
		final String hostname = args[0];
		final String port = args[1];
		final String schema = args[2];
		final String username = args[3];
		final String password = args[4];
		Class.forName("com.mysql.jdbc.Driver");
		Connection conn = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + schema + "?user=" + username + "&password=" + password);
		conn.setSchema(schema);
		
		HomePage home = new HomePage(conn);
		home.generate();
		
		DbUtils.closeQuietly(conn);
	}

	public static void createTemplates() throws IOException {
		File inputDir = new File("./templates");
		inputDir.mkdirs();
		URL jar = JoltArchiver.class.getProtectionDomain().getCodeSource().getLocation();
		ZipInputStream zip = new ZipInputStream(jar.openStream());
		ZipEntry entry = zip.getNextEntry();
		do {
			if (entry.getName().startsWith("bootstrap")) {
				if (entry.isDirectory()) {
					(new File(inputDir, entry.getName())).mkdirs();
				} else {
					File out = new File(inputDir, entry.getName());
					if (!out.exists()) {
						FileOutputStream fos = null;
						try {
							fos = new FileOutputStream(out);
							IOUtils.copy(zip, fos);
						} finally {
							IOUtils.closeQuietly(fos);
						}
					}
				}
			}
			entry = zip.getNextEntry();
		} while (entry != null);
		IOUtils.closeQuietly(zip);
	}

}
