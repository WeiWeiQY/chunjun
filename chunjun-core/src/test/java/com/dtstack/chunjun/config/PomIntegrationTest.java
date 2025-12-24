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
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integration tests for Maven POM configuration validation.
 *
 * <p>This test class performs more comprehensive validation including: - Cross-module dependency
 * consistency - Property inheritance validation - Multi-module POM structure validation
 *
 * @author chunjun test
 * @date 2024/12/24
 */
public class PomIntegrationTest {

    private static final String ROOT_POM_PATH = "pom.xml";
    private static final String EXPECTED_LOG4J2_VERSION = "2.25.3";

    @Test
    public void testRootPomStructure() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        Element root = doc.getDocumentElement();

        Assert.assertEquals("Root element should be 'project'", "project", root.getTagName());

        // Verify essential elements exist
        assertElementExists(doc, "groupId", "Root POM should have groupId");
        assertElementExists(doc, "artifactId", "Root POM should have artifactId");
        assertElementExists(doc, "version", "Root POM should have version");
        assertElementExists(doc, "packaging", "Root POM should have packaging");
    }

    @Test
    public void testPackagingIsPom() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        String packaging = getElementText(doc, "packaging");
        Assert.assertEquals(
                "Root POM packaging should be 'pom' for multi-module project", "pom", packaging);
    }

    @Test
    public void testModulesSection() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        NodeList modules = doc.getElementsByTagName("modules");
        Assert.assertTrue(
                "Root POM should have <modules> section for multi-module project",
                modules.getLength() > 0);
    }

    @Test
    public void testLog4j2CoreDependencyInChildModules() throws Exception {
        // Verify chunjun-core module uses the log4j2.version property
        File coreModulePom = new File("chunjun-core/pom.xml");
        if (coreModulePom.exists()) {
            String content = new String(Files.readAllBytes(Paths.get("chunjun-core/pom.xml")));

            // Check that log4j2 dependencies reference the property
            if (content.contains("org.apache.logging.log4j")) {
                Assert.assertTrue(
                        "chunjun-core module should use ${log4j2.version} property for log4j2 dependencies",
                        content.contains("${log4j2.version}"));
            }
        }
    }

    @Test
    public void testAllLog4j2DependenciesConsistency() throws Exception {
        Map<String, List<String>> log4j2Usages = findAllLog4j2Dependencies();

        if (!log4j2Usages.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : log4j2Usages.entrySet()) {
                String pomFile = entry.getKey();
                List<String> versions = entry.getValue();

                for (String version : versions) {
                    Assert.assertTrue(
                            "All Log4j2 dependencies should use property or inherit from parent in "
                                    + pomFile
                                    + ", found: "
                                    + version,
                            version.contains("${log4j2.version}")
                                    || version.equals(EXPECTED_LOG4J2_VERSION)
                                    || version.isEmpty());
                }
            }
        }
    }

    @Test
    public void testPropertyInheritance() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        String log4j2Version = getPropertyValue(doc, "log4j2.version");

        Assert.assertNotNull(
                "log4j2.version property should be defined in root POM for inheritance",
                log4j2Version);

        // Verify it's available for child modules
        Assert.assertEquals(
                "log4j2.version in root POM should match expected version",
                EXPECTED_LOG4J2_VERSION,
                log4j2Version);
    }

    @Test
    public void testDependencyManagement() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        NodeList depMgmt = doc.getElementsByTagName("dependencyManagement");

        // dependencyManagement is optional but recommended for multi-module projects
        if (depMgmt.getLength() > 0) {
            Element depMgmtElement = (Element) depMgmt.item(0);
            NodeList dependencies = depMgmtElement.getElementsByTagName("dependency");
            Assert.assertTrue(
                    "If dependencyManagement exists, it should contain dependencies",
                    dependencies.getLength() > 0);
        }
    }

    @Test
    public void testBuildSection() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        NodeList build = doc.getElementsByTagName("build");
        Assert.assertTrue("Root POM should have <build> section", build.getLength() > 0);
    }

    @Test
    public void testJacocoPlugin() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        String content = new String(Files.readAllBytes(Paths.get(ROOT_POM_PATH)));

        Assert.assertTrue(
                "POM should configure Jacoco plugin for code coverage",
                content.contains("jacoco-maven-plugin"));
    }

    @Test
    public void testLog4j2VersionNotInDependencyManagement() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        NodeList depMgmt = doc.getElementsByTagName("dependencyManagement");

        if (depMgmt.getLength() > 0) {
            Element depMgmtElement = (Element) depMgmt.item(0);
            NodeList dependencies = depMgmtElement.getElementsByTagName("dependency");

            for (int i = 0; i < dependencies.getLength(); i++) {
                Element dependency = (Element) dependencies.item(i);
                String groupId = getElementTextContent(dependency, "groupId");
                String version = getElementTextContent(dependency, "version");

                if ("org.apache.logging.log4j".equals(groupId) && version != null) {
                    Assert.assertTrue(
                            "Log4j2 version in dependencyManagement should use property reference",
                            version.contains("${log4j2.version}"));
                }
            }
        }
    }

    @Test
    public void testPomModelVersion() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        String modelVersion = getElementText(doc, "modelVersion");
        Assert.assertEquals("POM model version should be 4.0.0", "4.0.0", modelVersion);
    }

    @Test
    public void testGroupIdConsistency() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        String groupId = getElementText(doc, "groupId");
        Assert.assertNotNull("Root POM should have groupId", groupId);
        Assert.assertTrue(
                "GroupId should follow reverse domain naming convention", groupId.contains("."));
    }

    @Test
    public void testVersionSnapshot() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        String version = getElementText(doc, "version");
        Assert.assertNotNull("Root POM should have version", version);
        // Version format validation - should be semantic versioning
        Assert.assertTrue("Version should contain at least one digit", version.matches(".*\\d+.*"));
    }

    @Test
    public void testPropertiesCount() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        NodeList properties = doc.getElementsByTagName("properties");
        Assert.assertTrue("Root POM should have properties section", properties.getLength() > 0);

        Element propertiesElement = (Element) properties.item(0);
        NodeList children = propertiesElement.getChildNodes();
        int propertyCount = 0;
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                propertyCount++;
            }
        }

        Assert.assertTrue(
                "Properties section should contain multiple properties (found: "
                        + propertyCount
                        + ")",
                propertyCount > 3);
    }

    @Test
    public void testLog4j2PropertyLocation() throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(ROOT_POM_PATH));
        int propertyLineIndex = -1;
        int propertiesStartIndex = -1;
        int propertiesEndIndex = -1;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.equals("<properties>")) {
                propertiesStartIndex = i;
            } else if (line.equals("</properties>")) {
                propertiesEndIndex = i;
            } else if (line.contains("<log4j2.version>")) {
                propertyLineIndex = i;
            }
        }

        Assert.assertTrue(
                "log4j2.version property should be within <properties> section",
                propertyLineIndex > propertiesStartIndex && propertyLineIndex < propertiesEndIndex);
    }

    @Test
    public void testFlinkVersionProperty() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        String flinkVersion = getPropertyValue(doc, "flink.version");
        Assert.assertNotNull(
                "flink.version property should exist as this is a Flink-based project",
                flinkVersion);
    }

    @Test
    public void testScalaBinaryVersionProperty() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        String scalaVersion = getPropertyValue(doc, "scala.binary.version");
        Assert.assertNotNull("scala.binary.version property should be defined", scalaVersion);
    }

    @Test
    public void testJavaVersionProperty() throws Exception {
        Document doc = parsePomXml(ROOT_POM_PATH);
        String javaVersion = getPropertyValue(doc, "target.java.version");
        Assert.assertNotNull("target.java.version property should be defined", javaVersion);
        Assert.assertEquals("Java version should be 1.8", "1.8", javaVersion);
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

    private void assertElementExists(Document doc, String tagName, String message) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        Assert.assertTrue(message, nodes.getLength() > 0);
    }

    private String getElementText(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
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

    private Map<String, List<String>> findAllLog4j2Dependencies() throws Exception {
        Map<String, List<String>> result = new HashMap<>();
        findLog4j2InPom(ROOT_POM_PATH, result);

        // Check common module POMs that might have log4j2
        String[] modulePoms = {
            "chunjun-core/pom.xml",
            "chunjun-restore/chunjun-restore-common/pom.xml",
            "chunjun-dirty/chunjun-dirty-log/pom.xml"
        };

        for (String pomPath : modulePoms) {
            File pomFile = new File(pomPath);
            if (pomFile.exists()) {
                findLog4j2InPom(pomPath, result);
            }
        }

        return result;
    }

    private void findLog4j2InPom(String pomPath, Map<String, List<String>> result)
            throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(pomPath)));
        if (content.contains("org.apache.logging.log4j")) {
            List<String> versions = new ArrayList<>();
            Document doc = parsePomXml(pomPath);
            NodeList dependencies = doc.getElementsByTagName("dependency");

            for (int i = 0; i < dependencies.getLength(); i++) {
                Element dependency = (Element) dependencies.item(i);
                String groupId = getElementTextContent(dependency, "groupId");
                if ("org.apache.logging.log4j".equals(groupId)) {
                    String version = getElementTextContent(dependency, "version");
                    versions.add(version != null ? version : "");
                }
            }

            if (!versions.isEmpty()) {
                result.put(pomPath, versions);
            }
        }
    }
}