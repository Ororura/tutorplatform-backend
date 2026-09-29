package com.tutorplatform.content.application.importpackage;

public record TestCaseImport(
        String inputText, String expectedOutput, Boolean hidden, String comparisonMode) {}
