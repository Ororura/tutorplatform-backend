package com.tutorplatform.execution.application;

/**
 * Application boundary for isolated code execution.
 *
 * <p>Implementations must delegate untrusted code to an isolated execution service. User code must
 * never be run in the backend JVM or as a child process of it.</p>
 */
public interface ExecutionPort {

    ExecutionResult execute(ExecutionRequest request);
}
