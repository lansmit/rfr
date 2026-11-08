package io.student.rangiffler.data.db;

import io.student.rangiffler.config.TestConfig;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

public enum Databases {

  AUTH("rangiffler-auth", TestConfig.authJdbcUrl()),
  API("rangiffler-api", TestConfig.apiJdbcUrl());

  private final String schema;
  private final DataSource dataSource;
  private final PlatformTransactionManager transactionManager;

  Databases(String schema, String jdbcUrl) {
    this.schema = schema;
    this.dataSource = new DriverManagerDataSource(jdbcUrl, TestConfig.dbUsername(), TestConfig.dbPassword());
    this.transactionManager = new DataSourceTransactionManager(dataSource);
  }

  public String schema() {
    return schema;
  }

  public DataSource dataSource() {
    return dataSource;
  }

  public PlatformTransactionManager transactionManager() {
    return transactionManager;
  }
}
