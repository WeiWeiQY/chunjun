/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.chunjun.config;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tests for validating Maven POM configuration changes, specifically Log4j2 version updates.
 *
 * <p>This test class ensures that: - Log4j2 version is correctly set to 2.25.3 (fixing
 * CVE-2021-45105) - POM XML is well-formed and valid - Version property is consistently used
 * across modules - No vulnerable Log4j2 versions are present
 *
 * @author chunjun test
 * @date 2024/12/24
 */
public class PomConfigurationTest {

    private static final String ROOT_POM_PATH = "pom.xml";
    private static final String EXPECTED_LOG4J2_VERSION = "2.25.3";
    private static final List<String> VULNERABLE_LOG4J2_VERSIONS =
            Arrays.asList(
                    "2.0", "2.1", "2.2", "2.3", "2.4", "2.5", "2.6", "2.7", "2.8", "2.9", "2.10",
                    "2.11", "2.12", "2.13", "2.14", "2.15", "2.16.0", "2.17.0");

    @Test
    public void testRootPomExists() {
        File pomFile = new File(ROOT_POM_PATH);
        Assert.assertTrue(
                "Root pom.xml should exist at project root: " + ROOT_POM_PATH, pomFile.exists());
        Assert.assertTrue("Root pom.xml should be a file", pomFile.isFile());
        Assert.assertTrue("Root pom.xml should be readable", pomFile.canRead());
    }

    @Test
    public void testPomXmlIsWellFormed() throws Exception {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(ROOT_POM_PATH));
            Assert.assertNotNull("POM XML document should be parseable", document);
            Assert.assertNotNull(
                    "POM XML should have a root element", document.getDocumentElement());
        } catch (SAXException e) {
            Assert.fail("POM XML is not well-formed: " + e.getMessage());
        } catch (ParserConfigurationException | IOException e) {
            Assert.fail("Error parsing POM XML: " + e.getMessage());
        }
    }

    @Test
    public void testLog4j2VersionPropertyExists() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        String version = getPropertyValue(doc, "log4j2.version");
        Assert.assertNotNull(
                "log4j2.version property should be defined in root pom.xml", version);
        Assert.assertFalse(
                "log4j2.version property should not be empty", version.trim().isEmpty());
    }

    @Test
    public void testLog4j2VersionIsCorrect() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        String actualVersion = getPropertyValue(doc, "log4j2.version");
        Assert.assertEquals(
                "Log4j2 version should be updated to " + EXPECTED_LOG4J2_VERSION,
                EXPECTED_LOG4J2_VERSION,
                actualVersion);
    }

    @Test
    public void testLog4j2VersionIsNotVulnerable() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        String actualVersion = getPropertyValue(doc, "log4j2.version");

        for (String vulnerableVersion : VULNERABLE_LOG4J2_VERSIONS) {
            Assert.assertNotEquals(
                    "Log4j2 version should not be vulnerable version: " + vulnerableVersion,
                    vulnerableVersion,
                    actualVersion);
        }

        // Ensure version is at least 2.17.1 (first version after CVE-2021-45105 fix)
        String[] versionParts = actualVersion.split("\\.");
        Assert.assertTrue(
                "Log4j2 version should have at least 2 parts (major.minor)",
                versionParts.length >= 2);

        int major = Integer.parseInt(versionParts[0]);
        int minor = Integer.parseInt(versionParts[1]);

        Assert.assertTrue(
                "Log4j2 major version should be 2 or higher (actual: " + major + ")", major >= 2);
        if (major == 2) {
            Assert.assertTrue(
                    "Log4j2 minor version should be at least 17 for version 2.x (actual: "
                            + minor
                            + ")",
                    minor >= 17);
            if (minor == 17) {
                int patch = versionParts.length > 2 ? Integer.parseInt(versionParts[2]) : 0;
                Assert.assertTrue(
                        "Log4j2 patch version should be at least 1 for version 2.17.x (actual: "
                                + patch
                                + ")",
                        patch >= 1);
            }
        }
    }

    @Test
    public void testCveCommentExists() throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(ROOT_POM_PATH)));
        Assert.assertTrue(
                "POM should contain CVE-2021-45105 reference comment",
                content.contains("CVE-2021-45105") || content.contains("fix CVE-2021-45105"));
    }

    @Test
    public void testLog4j2DependenciesUseProperty() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        NodeList dependencies = doc.getElementsByTagName("dependency");

        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dependency = (Element) dependencies.item(i);
            String groupId = getElementTextContent(dependency, "groupId");
            String artifactId = getElementTextContent(dependency, "artifactId");

            if ("org.apache.logging.log4j".equals(groupId)) {
                String version = getElementTextContent(dependency, "version");
                Assert.assertTrue(
                        "Log4j2 dependency "
                                + artifactId
                                + " should use ${log4j2.version} property",
                        version == null || version.contains("${log4j2.version}"));
            }
        }
    }

    @Test
    public void testModulePomFilesExist() {
        Document doc = parsePomXml(ROOT_POM_PATH);
        NodeList modules = doc.getElementsByTagName("module");

        List<String> moduleNames = new ArrayList<>();
        for (int i = 0; i < modules.getLength(); i++) {
            String moduleName = modules.item(i).getTextContent().trim();
            if (!moduleName.isEmpty()) {
                moduleNames.add(moduleName);
            }
        }

        Assert.assertTrue("Root POM should declare at least one module", moduleNames.size() > 0);

        for (String moduleName : moduleNames) {
            File modulePom = new File(moduleName + "/pom.xml");
            if (modulePom.exists()) {
                Assert.assertTrue(
                        "Module POM should be readable: " + modulePom.getPath(),
                        modulePom.canRead());
            }
        }
    }

    @Test
    public void testLog4j2VersionFormat() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        String version = getPropertyValue(doc, "log4j2.version");

        Pattern versionPattern = Pattern.compile("^\\d+\\.\\d+(\\.\\d+)?$");
        Matcher matcher = versionPattern.matcher(version);
        Assert.assertTrue(
                "Log4j2 version should follow semantic versioning format (major.minor.patch): "
                        + version,
                matcher.matches());
    }

    @Test
    public void testNoHardcodedLog4j2Versions() throws Exception {
        // Check root POM
        String rootPomContent = new String(Files.readAllBytes(Paths.get(ROOT_POM_PATH)));
        checkForHardcodedLog4j2Version(rootPomContent, ROOT_POM_PATH);

        // Check module POMs that use log4j2
        List<String> log4j2Poms =
                Arrays.asList(
                        "chunjun-core/pom.xml",
                        "chunjun-restore/chunjun-restore-common/pom.xml",
                        "chunjun-dirty/chunjun-dirty-log/pom.xml");

        for (String pomPath : log4j2Poms) {
            File pomFile = new File(pomPath);
            if (pomFile.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(pomPath)));
                checkForHardcodedLog4j2Version(content, pomPath);
            }
        }
    }

    @Test
    public void testLog4j2PropertyLineNumber() throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(ROOT_POM_PATH));
        boolean foundProperty = false;
        int lineNumber = -1;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("<log4j2.version>")) {
                foundProperty = true;
                lineNumber = i + 1;
                String versionLine = line.trim();
                Assert.assertTrue(
                        "Log4j2 version line should contain the expected version "
                                + EXPECTED_LOG4J2_VERSION
                                + " at line "
                                + lineNumber,
                        versionLine.contains(EXPECTED_LOG4J2_VERSION));
                break;
            }
        }

        Assert.assertTrue("log4j2.version property should be found in pom.xml", foundProperty);
        Assert.assertTrue("Line number should be positive", lineNumber > 0);
    }

    @Test
    public void testPropertiesSectionExists() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        NodeList propertiesNodes = doc.getElementsByTagName("properties");
        Assert.assertTrue(
                "Root POM should contain <properties> section", propertiesNodes.getLength() > 0);

        Element properties = (Element) propertiesNodes.item(0);
        NodeList propertyNodes = properties.getChildNodes();
        int propertyCount = 0;
        for (int i = 0; i < propertyNodes.getLength(); i++) {
            if (propertyNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                propertyCount++;
            }
        }
        Assert.assertTrue(
                "Properties section should contain at least one property", propertyCount > 0);
    }

    @Test
    public void testLog4j2VersionAgainstKnownCves() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        String version = getPropertyValue(doc, "log4j2.version");

        // Test against known CVE-affected versions
        Assert.assertNotEquals(
                "Version should not be 2.0-2.14.1 (CVE-2021-44228)", "2.14.1", version);
        Assert.assertNotEquals(
                "Version should not be 2.15.0 (CVE-2021-45046)", "2.15.0", version);
        Assert.assertNotEquals(
                "Version should not be 2.16.0 (CVE-2021-45105)", "2.16.0", version);
        Assert.assertNotEquals(
                "Version should not be 2.17.0 (CVE-2021-45105)", "2.17.0", version);
    }

    @Test
    public void testLog4j2VersionIsNewerThan2_17_0() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        String version = getPropertyValue(doc, "log4j2.version");

        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

        boolean isNewer =
                major > 2
                        || (major == 2 && minor > 17)
                        || (major == 2 && minor == 17 && patch > 0);

        Assert.assertTrue(
                "Log4j2 version should be newer than 2.17.0 (found: " + version + ")", isNewer);
    }

    @Test
    public void testPomEncodingIsUTF8() throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(ROOT_POM_PATH)));
        Assert.assertTrue(
                "POM should declare UTF-8 encoding",
                content.contains("encoding=\"UTF-8\"")
                        || content.contains("project.build.sourceEncoding"));
    }

    // Helper methods

    private Document parsePomXml(String pomPath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new File(pomPath));
        } catch (Exception e) {
            Assert.fail("Failed to parse POM XML at " + pomPath + ": " + e.getMessage());
            return null;
        }
    }

    private String getPropertyValue(Document doc, String propertyName) {
        NodeList properties = doc.getElementsByTagName("properties");
        if (properties.getLength() == 0) {
            return null;
        }

        Element propertiesElement = (Element) properties.item(0);
        NodeList propertyNodes = propertiesElement.getElementsByTagName(propertyName);
        if (propertyNodes.getLength() == 0) {
            return null;
        }

        return propertyNodes.item(0).getTextContent().trim();
    }

    private String getElementTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }

    private void checkForHardcodedLog4j2Version(String content, String filePath) {
        // Look for hardcoded log4j2 versions (not using property)
        Pattern hardcodedPattern =
                Pattern.compile(
                        "<version>2\\.(\\d+)(\\.\\d+)?</version>\\s*</dependency>",
                        Pattern.MULTILINE);
        Matcher matcher = hardcodedPattern.matcher(content);

        while (matcher.find()) {
            String contextBefore =
                    content.substring(Math.max(0, matcher.start() - 200), matcher.start());
            if (contextBefore.contains("org.apache.logging.log4j")) {
                Assert.fail(
                        "Found hardcoded Log4j2 version in "
                                + filePath
                                + ": "
                                + matcher.group()
                                + ". Should use ${log4j2.version} property instead.");
            }
        }
    }
}