package com.mazlove.backend.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class SchemaScriptTest {
  @Test
  void springKeepsTheOneTimeMigrationConditionInTheSameStatement() throws Exception {
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    when(connection.createStatement()).thenReturn(statement);
    ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/schema.sql"));
    var commands = ArgumentCaptor.forClass(String.class);
    verify(statement, atLeastOnce()).execute(commands.capture());
    assertTrue(commands.getAllValues().stream().anyMatch(sql ->
        sql.startsWith("UPDATE sessions SET status='IN_PROGRESS'")
            && sql.contains("AND NOT EXISTS") && sql.contains("column_name='started_at'")));
  }
}
