package com.example;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import java.io.BufferedWriter;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Example implements  HttpFunction {
@Override
  public void service(HttpRequest request, HttpResponse response) throws Exception {
    createConnectionPool();
    BufferedWriter writer = response.getWriter();
    writer.write("Hello. I have successfully completed your work - created table in SQL Server!");
  }
  // Saving credentials in environment variables is convenient, but not secure - consider a more
  // secure solution such as https://cloud.google.com/kms/ to help keep secrets safe.
  private static final String INSTANCE_CONNECTION_NAME ="YOUR_PROJECT_ID:LOCATION1:INSTANCE_NAME";
  private static final String DB_USER = "YOUR_USERNAME";
  private static final String DB_PASS = "YOUR_PASSWORD";
  private static final String DB_NAME = "YOUR_DB";
 private HikariDataSource connectionPool;
  private String tableName;

  public void createConnectionPool() throws SQLException {
    HikariConfig config = new HikariConfig();
    config
        .setDataSourceClassName("com.microsoft.sqlserver.jdbc.SQLServerDataSource");
    config.setUsername(DB_USER); // e.g. "root", "mysql"
    config.setPassword(DB_PASS); // e.g. "my-password"
    config.addDataSourceProperty("databaseName", DB_NAME);
    config.addDataSourceProperty("socketFactoryClass",
        "com.google.cloud.sql.sqlserver.SocketFactory");
    config.addDataSourceProperty("socketFactoryConstructorArg", INSTANCE_CONNECTION_NAME);
    config.addDataSourceProperty("encrypt", "true");
    config.addDataSourceProperty("trustServerCertificate","true");

    config.setMaximumPoolSize(5); 
    config.setMinimumIdle(5);
    config.setConnectionTimeout(10000); // 10 seconds
    config.setIdleTimeout(600000); // 10 minutes 
    config.setMaxLifetime(1800000); // 30 minutes 
    DataSource pool = new HikariDataSource(config); 

    
    this.connectionPool = new HikariDataSource(config);
    this.tableName = String.format("books_%s", UUID.randomUUID().toString().replace("-", ""));

    // Create table
    try (Connection conn = connectionPool.getConnection()) {
      String stmt = String.format("CREATE TABLE %s (", this.tableName)
          + "  ID CHAR(20) NOT NULL,"
          + "  TITLE TEXT NOT NULL"
          + ");";
      try (PreparedStatement createTableStatement = conn.prepareStatement(stmt)) {
        createTableStatement.execute();
      }
    }
  }

}
