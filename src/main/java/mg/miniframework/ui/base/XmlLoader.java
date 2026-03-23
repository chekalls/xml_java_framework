package mg.miniframework.ui.base;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import jakarta.servlet.ServletContext;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class XmlLoader {
    private static final Pattern STYLE_PATTERN = Pattern
            .compile("(?s)<style>\\s*(?:<!\\[CDATA\\[)?(.*?)(?:]]>)?\\s*</style>");

    private static Class<?>[] COMPONENT_CLASSES;
    private static java.util.Map<String, Class<?>> COMPONENT_BY_ROOT_NAME;

    static {
        loadComponent();
    }

    public static String getXMLTagContent(String filePath, String tagName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(filePath));
            NodeList list = doc.getElementsByTagName(tagName);
            if (list.getLength() > 0) {
                return list.item(0).getTextContent();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static void loadComponent() {
        try (InputStream in = XmlLoader.class.getClassLoader()
                .getResourceAsStream("config/xml-component-register.xml")) {
            if (in == null) {
                System.err.println("Attention : config/xml-component-register.xml non trouvé");
                COMPONENT_CLASSES = new Class<?>[0];
                COMPONENT_BY_ROOT_NAME = new java.util.HashMap<>();
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document doc = factory.newDocumentBuilder().parse(in);

            NodeList componentNodes = doc.getElementsByTagName("component");
            COMPONENT_CLASSES = new Class<?>[componentNodes.getLength()];
            COMPONENT_BY_ROOT_NAME = new java.util.HashMap<>();

            for (int i = 0; i < componentNodes.getLength(); i++) {
                Element componentEl = (Element) componentNodes.item(i);
                String className = componentEl.getElementsByTagName("class").item(0).getTextContent().trim();
                String rootElement = componentEl.getElementsByTagName("rootElement").item(0).getTextContent().trim();

                try {
                    Class<?> clazz = Class.forName(className);
                    COMPONENT_CLASSES[i] = clazz;
                    COMPONENT_BY_ROOT_NAME.put(rootElement, clazz);
                    System.out.println("Component registered: " + className + " (root: " + rootElement + ")");
                } catch (ClassNotFoundException e) {
                    System.err.println("Erreur : classe " + className + " non trouvée : " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du registre de composants : " + e.getMessage());
            e.printStackTrace();
            COMPONENT_CLASSES = new Class<?>[0];
            COMPONENT_BY_ROOT_NAME = new java.util.HashMap<>();
        }
    }

    public static Element getRootElement(String xmlContent) throws Exception {
        if (xmlContent == null || xmlContent.isBlank()) {
            throw new IllegalArgumentException("XML content cannot be null or empty");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document doc = factory.newDocumentBuilder().parse(
                    new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            return doc.getDocumentElement();
        } catch (org.xml.sax.SAXException | ParserConfigurationException e) {
            throw new IOException("Erreur lors du parsing XML : " + e.getMessage(), e);
        }
    }

    public static Class<? extends HtmlComponent> getComponentClassByRootName(Element rootElement) {
        if (rootElement == null) {
            return null;
        }
        String rootName = rootElement.getNodeName();
        Class<?> clazz = COMPONENT_BY_ROOT_NAME.get(rootName);
        return (Class<? extends HtmlComponent>) clazz;
    }

    public static String getXMLFileContentAsString(String resourcePath, ServletContext context) {
        String realPath = context.getRealPath(resourcePath);
        return getXMLFileContentAsString(realPath);
    }

    public static String getXMLFileContentAsString(String resourceOrPath, Pattern pattern) {
        String xml = readXmlContentAsString(resourceOrPath);
        return extractComponent(xml, pattern);
    }

    public static String getXMLFileContentAsString(String resourceOrPath) {
        String xml = readXmlContentAsString(resourceOrPath);
        return extractStyle(xml);
    }

    private static String readXmlContentAsString(String resourceOrPath) {
        if (resourceOrPath == null || resourceOrPath.isBlank()) {
            return "";
        }

        try (InputStream in = XmlLoader.class.getClassLoader().getResourceAsStream(resourceOrPath)) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture de la ressource : " + e.getMessage());
        }

        try {
            if (Files.exists(Paths.get(resourceOrPath))) {
                return Files.readString(Paths.get(resourceOrPath), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.err.println("Impossible de lire le fichier sur le disque : " + e.getMessage());
        }

        if (HtmlComponent.getServletContext() != null) {
            String webPath = resourceOrPath.startsWith("/") ? resourceOrPath : "/" + resourceOrPath;
            try (InputStream in = HtmlComponent.getServletContext().getResourceAsStream(webPath)) {
                if (in != null) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                System.err.println("Erreur lors de la lecture via ServletContext : " + e.getMessage());
            }
        }

        return "";
    }

    // public static HtmlComponent loadFrom(String xmlPath ,ServletContext
    // context,Class<? extends HtmlComponent> clazz) throws Exception{

    // }

    public static HtmlComponent loadFrom(String xmlPath, ServletContext context) throws Exception {
        String realPath = context.getRealPath(xmlPath);
        if (realPath != null && Files.exists(Paths.get(realPath))) {
            return loadFrom(realPath);
        }
        try (InputStream in = context.getResourceAsStream(xmlPath)) {
            if (in == null) {
                throw new FileNotFoundException("Le fichier XML n'a pas été trouvé : " + xmlPath);
            }
            String xmlContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Element rootElement = getRootElement(xmlContent);
            Class<? extends HtmlComponent> componentClass = getComponentClassByRootName(rootElement);
            if (componentClass == null) {
                throw new IllegalArgumentException(
                        "Aucun composant enregistré pour l'élément racine : " + rootElement.getNodeName());
            }
            JAXBContext jaxbContext = JAXBContext.newInstance(componentClass);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Object result = unmarshaller
                    .unmarshal(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            if (result instanceof HtmlComponent) {
                return (HtmlComponent) result;
            }
            throw new IllegalArgumentException("Le XML ne correspond pas à un composant HTML connu.");
        }
    }

    public static HtmlComponent loadFrom(String xmlPath) throws Exception {
        if (!Files.exists(Paths.get(xmlPath))) {
            throw new FileNotFoundException("Le fichier XML n'a pas été trouvé : " + xmlPath);
        }
        String xmlContent = Files.readString(Paths.get(xmlPath), StandardCharsets.UTF_8);
        Element rootElement = getRootElement(xmlContent);
        Class<? extends HtmlComponent> componentClass = getComponentClassByRootName(rootElement);
        if (componentClass == null) {
            throw new IllegalArgumentException(
                    "Aucun composant enregistré pour l'élément racine : " + rootElement.getNodeName());
        }
        JAXBContext jaxbContext = JAXBContext.newInstance(componentClass);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Object result = unmarshaller.unmarshal(new File(xmlPath));
        if (result instanceof HtmlComponent) {
            return (HtmlComponent) result;
        }
        throw new IllegalArgumentException("Le XML ne correspond pas à un composant HTML connu.");
    }

    public static HtmlComponent loadFrom(String resource, Class<? extends HtmlComponent> clazz) {
        try (InputStream in = XmlLoader.class.getClassLoader()
                .getResourceAsStream(resource)) {
            if (in == null) {
                throw new FileNotFoundException(resource);
            }
            JAXBContext ctx = JAXBContext.newInstance(clazz);
            Unmarshaller u = ctx.createUnmarshaller();
            return (HtmlComponent) u.unmarshal(in);
        } catch (JAXBException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String extractComponent(String xmlContent, Pattern pattern) {
        Matcher m = pattern.matcher(xmlContent);
        return m.find() ? m.group(1).trim() : "";
    }

    private static String extractStyle(String xmlContent) {
        Matcher m = STYLE_PATTERN.matcher(xmlContent);
        return m.find() ? m.group(1).trim() : "";
    }

    /**
     * Extrait un template spécifique par type depuis le fichier centralisé
     * html-fields.xml
     * 
     * @param templateType  Le type du template (ex: "text-field", "textarea-field",
     *                      "select-field")
     * @param fieldsXmlPath Chemin vers le fichier html-fields.xml (ex:
     *                      "/WEB-INF/views/templates/html-fields.xml")
     * @return Le contenu du template ou "" si non trouvé
     */
    public static String getTemplateByType(String templateType, String fieldsXmlPath) {
        if (templateType == null || templateType.isBlank() || fieldsXmlPath == null) {
            return "";
        }

        try {
            String xmlContent = readXmlContentAsString(fieldsXmlPath);
            if (xmlContent == null || xmlContent.isEmpty()) {
                System.err.println("Fichier XML de templates vide : " + fieldsXmlPath);
                return "";
            }

            Element rootElement = getRootElement(xmlContent);
            if (rootElement == null) {
                return "";
            }

            NodeList groups = rootElement.getElementsByTagName("group");
            for (int i = 0; i < groups.getLength(); i++) {
                Element group = (Element) groups.item(i);
                NodeList typeNodes = group.getElementsByTagName("type");
                NodeList styleNodes = group.getElementsByTagName("style");
                if (typeNodes.getLength() == 0 || styleNodes.getLength() == 0) {
                    continue;
                }

                String type = typeNodes.item(0).getTextContent();
                if (type != null && type.trim().equalsIgnoreCase(templateType.trim())) {
                    String styleContent = styleNodes.item(0).getTextContent();
                    return styleContent != null ? styleContent.trim() : "";
                }
            }

            // Chercher le template avec l'attribut type
            NodeList templates = rootElement.getElementsByTagName("template");
            for (int i = 0; i < templates.getLength(); i++) {
                Element template = (Element) templates.item(i);
                String type = template.getAttribute("type");
                if (type.equals(templateType)) {
                    // Extraire et retourner le contenu du style
                    NodeList styleNodes = template.getElementsByTagName("style");
                    if (styleNodes.getLength() > 0) {
                        String styleContent = styleNodes.item(0).getTextContent();
                        return styleContent != null ? styleContent.trim() : "";
                    }
                }
            }

            System.err.println("Template de type '" + templateType + "' non trouvé dans " + fieldsXmlPath);
            return "";
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture du template '" + templateType + "' : " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }
}
