package me.tgoyal.clearance.parser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the direct dependencies from a Maven {@code pom.xml}.
 *
 * <ul>
 *   <li>Only {@code <project><dependencies>}: not dependencyManagement, not plugin dependencies.</li>
 *   <li>{@code test} scope is skipped, because test dependencies don't ship.</li>
 *   <li>{@code ${property}} versions are resolved from {@code <properties>} and {@code ${project.version}}.</li>
 *   <li>No transitive resolution: that would need Maven itself and a network connection.</li>
 * </ul>
 */
public class MavenPomParser implements DependencyFileParser {

    private static final Pattern PROPERTY = Pattern.compile("\\$\\{([^}]+)}");

    @Override
    public boolean supports(String filename) {
        return "pom.xml".equals(filename);
    }

    @Override
    public Ecosystem ecosystem() {
        return Ecosystem.MAVEN;
    }

    @Override
    public List<Dependency> parse(InputStream input) {
        Element project = readXml(input).getDocumentElement();
        if (!"project".equals(project.getTagName())) {
            throw new DependencyFileException("This is not a pom.xml: the root element must be <project>");
        }

        Map<String, String> properties = readProperties(project);
        List<Dependency> dependencies = new ArrayList<>();

        Element dependenciesElement = child(project, "dependencies");
        if (dependenciesElement == null) {
            return dependencies;
        }

        for (Element dependency : children(dependenciesElement, "dependency")) {
            if ("test".equals(text(dependency, "scope"))) {
                continue;
            }
            String name = text(dependency, "groupId") + ":" + text(dependency, "artifactId");
            String version = text(dependency, "version");
            if (version != null) {
                version = resolveProperties(version, properties);
            }
            dependencies.add(new Dependency(name, version, null));
        }
        return dependencies;
    }

    private static Document readXml(InputStream input) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Security: refuse DOCTYPEs entirely. This blocks XML external entity (XXE)
            // attacks, where an uploaded file asks the server to read its own files.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(input);
        } catch (SAXException e) {
            throw new DependencyFileException("pom.xml is not valid XML: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new DependencyFileException("Could not read pom.xml: " + e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            // Only happens if the JDK's XML parser lacks a feature: a bug, not bad input.
            throw new IllegalStateException("XML parser is misconfigured", e);
        }
    }

    /** Collects {@code <properties>}, plus {@code project.version} which poms often refer to. */
    private static Map<String, String> readProperties(Element project) {
        Map<String, String> properties = new HashMap<>();

        String projectVersion = text(project, "version");
        if (projectVersion == null) {
            // A module may inherit its version from <parent>.
            Element parent = child(project, "parent");
            projectVersion = parent == null ? null : text(parent, "version");
        }
        if (projectVersion != null) {
            properties.put("project.version", projectVersion);
        }

        Element propertiesElement = child(project, "properties");
        if (propertiesElement != null) {
            for (Element property : children(propertiesElement, null)) {
                properties.put(property.getTagName(), property.getTextContent().trim());
            }
        }
        return properties;
    }

    /** Replaces each {@code ${name}} with its value. Unknown names are left exactly as written. */
    private static String resolveProperties(String value, Map<String, String> properties) {
        Matcher matcher = PROPERTY.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String replacement = properties.getOrDefault(matcher.group(1), matcher.group());
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    // Small DOM helpers. They look only at direct children, so a <version> inside
    // <parent> is never mistaken for the project's own <version>.

    /** Direct child elements, optionally only those with the given tag name. */
    private static List<Element> children(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && (tagName == null || tagName.equals(element.getTagName()))) {
                result.add(element);
            }
        }
        return result;
    }

    private static Element child(Element parent, String tagName) {
        List<Element> matches = children(parent, tagName);
        return matches.isEmpty() ? null : matches.get(0);
    }

    /** Trimmed text of a direct child, or {@code null} if there is no such child or it is empty. */
    private static String text(Element parent, String tagName) {
        Element element = child(parent, tagName);
        if (element == null) {
            return null;
        }
        String text = element.getTextContent().trim();
        return text.isEmpty() ? null : text;
    }
}
