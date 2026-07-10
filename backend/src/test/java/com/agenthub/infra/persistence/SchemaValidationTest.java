package com.agenthub.infra.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaValidationTest {

    @Test
    void schemaSqlExistsAndHasCreateTableStatements() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/init/schema.sql");
        assertThat(resource.exists()).isTrue();

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line.trim());
            }
        }

        String content = String.join("\n", lines);
        assertThat(content).contains("CREATE TABLE");
        assertThat(content).contains("agent");
        assertThat(content).contains("function_definition");
        assertThat(content).contains("session");
        assertThat(content).contains("audit_log");
        assertThat(content).contains("cost_record");
    }

    @Test
    void dataSqlExistsAndHasInsertStatements() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/init/data.sql");
        assertThat(resource.exists()).isTrue();

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line.trim());
            }
        }

        String content = String.join("\n", lines);
        assertThat(content).contains("INSERT INTO");
        assertThat(content).contains("tenant");
        assertThat(content).contains("user_account");
        assertThat(content).contains("agent");
        assertThat(content).contains("function_definition");
    }
}
