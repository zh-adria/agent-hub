package com.agenthub.infra.persistence.entity;

import javax.persistence.*;

@Entity
@Table(name = "ah_evaluation_case_result")
public class EvaluationCaseResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "case_key", nullable = false, length = 128)
    private String caseKey;

    @Column(name = "input", nullable = false, columnDefinition = "TEXT")
    private String input;

    @Column(name = "expected", columnDefinition = "TEXT")
    private String expected;

    @Column(name = "actual", columnDefinition = "TEXT")
    private String actual;

    @Column(name = "passed", nullable = false)
    private Boolean passed;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
    public String getCaseKey() { return caseKey; }
    public void setCaseKey(String caseKey) { this.caseKey = caseKey; }
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }
    public String getActual() { return actual; }
    public void setActual(String actual) { this.actual = actual; }
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
