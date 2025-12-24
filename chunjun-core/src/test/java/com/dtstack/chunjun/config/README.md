# POM Configuration Tests

## Overview

This test suite validates the Maven POM configuration changes, specifically the Log4j2 version update from 2.17.0 to 2.25.3 to address the CVE-2021-45105 security vulnerability.

## Test Files

### 1. PomConfigurationTest.java (15 tests)
Core validation tests for the Log4j2 version update:

- **testRootPomExists()** - Verifies pom.xml exists and is readable
- **testPomXmlIsWellFormed()** - Validates XML structure and parseability
- **testLog4j2VersionPropertyExists()** - Ensures log4j2.version property is defined
- **testLog4j2VersionIsCorrect()** - Verifies version is exactly 2.25.3
- **testLog4j2VersionIsNotVulnerable()** - Checks against list of vulnerable versions
- **testCveCommentExists()** - Verifies CVE-2021-45105 is documented in comments
- **testLog4j2DependenciesUseProperty()** - Ensures dependencies use ${log4j2.version}
- **testModulePomFilesExist()** - Validates module POMs exist and are readable
- **testLog4j2VersionFormat()** - Validates semantic versioning format
- **testNoHardcodedLog4j2Versions()** - Detects hardcoded versions across modules
- **testLog4j2PropertyLineNumber()** - Verifies property location and value
- **testPropertiesSectionExists()** - Ensures properties section is properly structured
- **testLog4j2VersionAgainstKnownCves()** - Tests against specific CVE versions
- **testLog4j2VersionIsNewerThan2_17_0()** - Confirms version > 2.17.0
- **testPomEncodingIsUTF8()** - Validates UTF-8 encoding declaration

### 2. PomIntegrationTest.java (18 tests)
Integration and structural validation tests:

- **testRootPomStructure()** - Validates essential POM elements
- **testPackagingIsPom()** - Verifies multi-module packaging
- **testModulesSection()** - Ensures modules are declared
- **testLog4j2CoreDependencyInChildModules()** - Checks property usage in child modules
- **testAllLog4j2DependenciesConsistency()** - Validates consistency across all modules
- **testPropertyInheritance()** - Verifies property inheritance mechanism
- **testDependencyManagement()** - Validates dependency management section
- **testBuildSection()** - Ensures build configuration exists
- **testJacocoPlugin()** - Verifies code coverage plugin configuration
- **testLog4j2VersionNotInDependencyManagement()** - Checks proper property usage
- **testPomModelVersion()** - Validates Maven model version (4.0.0)
- **testGroupIdConsistency()** - Verifies groupId follows conventions
- **testVersionSnapshot()** - Validates project version format
- **testPropertiesCount()** - Ensures adequate property definitions
- **testLog4j2PropertyLocation()** - Verifies property within properties section
- **testFlinkVersionProperty()** - Validates Flink version property
- **testScalaBinaryVersionProperty()** - Verifies Scala version property
- **testJavaVersionProperty()** - Confirms Java 1.8 target version

## What Was Changed

**File**: `pom.xml` (line 36)
**Change**: Updated Log4j2 version property
```xml
<!-- Before -->
<log4j2.version>2.17.0</log4j2.version>

<!-- After -->
<log4j2.version>2.25.3</log4j2.version>
```

**Reason**: Fix CVE-2021-45105 security vulnerability

## Security Context

### Known Log4j2 CVEs Tested:
- **CVE-2021-44228** (Log4Shell) - Affects versions 2.0-2.14.1
- **CVE-2021-45046** - Affects version 2.15.0
- **CVE-2021-45105** - Affects versions 2.16.0 and 2.17.0

The update to version 2.25.3 ensures protection against all known vulnerabilities.

## Test Framework

- **Framework**: JUnit 4 (4.13.1)
- **Mocking**: Mockito (3.0.0) + PowerMock (2.0.4)
- **XML Parsing**: javax.xml.parsers (built-in)
- **Language**: Java 8

## Running the Tests

```bash
# Run all tests in the package
mvn test -Dtest=com.dtstack.chunjun.config.*

# Run specific test class
mvn test -Dtest=PomConfigurationTest
mvn test -Dtest=PomIntegrationTest

# Run with coverage
mvn test jacoco:report
```

## Test Execution Context

Tests execute from the repository root directory where `pom.xml` is located. All file paths are relative to this root.

## Expected Outcomes

All 33 tests should pass when:
1. Log4j2 version is set to 2.25.3 in root pom.xml
2. CVE-2021-45105 comment is present
3. All Log4j2 dependencies use the ${log4j2.version} property
4. No hardcoded Log4j2 versions exist in module POMs
5. POM structure follows Maven conventions

## Coverage Areas

### Configuration Validation
- ✅ XML well-formedness
- ✅ Property definitions
- ✅ Dependency declarations
- ✅ Build configuration

### Security Validation
- ✅ Version vulnerability checks
- ✅ CVE documentation
- ✅ Version comparison logic

### Structural Validation
- ✅ Multi-module structure
- ✅ Property inheritance
- ✅ Dependency management
- ✅ Module consistency

### Format Validation
- ✅ Semantic versioning
- ✅ Encoding standards
- ✅ Maven conventions

## Maintenance

When updating Log4j2 version in the future:
1. Update `EXPECTED_LOG4J2_VERSION` constant in both test files
2. Add any new CVE versions to `VULNERABLE_LOG4J2_VERSIONS` list
3. Update CVE documentation in test comments
4. Run full test suite to validate changes

## Integration with CI/CD

These tests are automatically executed as part of the standard Maven test lifecycle and will fail the build if any POM configuration violations are detected.

---

**Created**: 2024-12-24
**Purpose**: Validate Log4j2 2.25.3 security update
**Maintainer**: ChunJun Test Suite