package io.student.rangiffler.data.db;

import io.student.rangiffler.config.TestConfig;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

public enum Databases {

  AUTH(TestConfig.authJdbcUrl()),
  API(TestConfig.apiJdbcUrl());

  private final DataSource dataSource;
  private final PlatformTransactionManager transactionManager;

  Databases(String jdbcUrl) {
    this.dataSource = new DriverManagerDataSource(jdbcUrl, TestConfig.dbUsername(), TestConfig.dbPassword());
    this.transactionManager = new DataSourceTransactionManager(dataSource);
  }

  public DataSource dataSource() {
    return dataSource;
  }

  public PlatformTransactionManager transactionManager() {
    return transactionManager;
  }
}
