/**
 * Boundary for execution of untrusted user programs. The backend only prepares requests and calls
 * the isolated execution worker over an internal protocol. Local process execution APIs do not
 * belong in this capability.
 */
package com.tutorplatform.execution;
