package io.student.rangiffler.data.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.ValidateResult;
import org.flywaydb.core.api.pattern.ValidatePattern;

import javax.sql.DataSource;

public final class Migrations {
  private final String schema;
  private final DataSource dataSource;
  private final Path location;

  public Migrations(Databases database, String location) {
    this(database, Path.of(location));
  }

  public Migrations(Databases database, Path location) {
    this(database.schema(), database.dataSource(), location);
  }

  public Migrations(String schema, DataSource dataSource, Path location) {
    this.schema = schema;
    this.dataSource = dataSource;
    this.location = location;
  }

  public Optional<String> mismatch() {
    if (!Files.isDirectory(location)) {
      throw new IllegalStateException(
          "Migrations directory " + location.toAbsolutePath() + " of schema " + schema + " does not exist");
    }
    ValidateResult result = Flyway.configure()
        .dataSource(dataSource)
        .schemas(schema)
        .locations("filesystem:" + location.toAbsolutePath())
        .baselineOnMigrate(true)
        .baselineVersion("0")
        .ignoreMigrationPatterns(new ValidatePattern[0])
        .load()
        .validateWithResult();
    return result.validationSuccessful
        ? Optional.empty()
        : Optional.of(schema + ": " + String.join("; ", problems(result)));
  }

  public Optional<String> currentVersion() {
    try {
      MigrationInfo current = Flyway.configure()
          .dataSource(dataSource)
          .schemas(schema)
          .locations("filesystem:" + location.toAbsolutePath())
          .load()
          .info()
          .current();
      return Optional.of(current == null ? "none" : current.getVersion().getVersion());
    } catch (FlywayException exception) {
      return Optional.empty();
    }
  }

  private List<String> problems(ValidateResult result) {
    return Stream.concat(
            result.invalidMigrations.stream().map(migration -> migration.errorDetails),
            Stream.ofNullable(result.errorDetails))
        .filter(Objects::nonNull)
        .map(details -> details.errorMessage.lines().findFirst().orElse(String.valueOf(details.errorCode)))
        .distinct()
        .toList();
  }
}
