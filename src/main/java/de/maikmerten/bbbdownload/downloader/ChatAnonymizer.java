// This class has originally been provided by Benjamin Bogusch.
// Modified and published with permission.
package de.maikmerten.bbbdownload.downloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ChatAnonymizer {

    public static InputStream anonymizeChat(InputStream input) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException, TransformerFactoryConfigurationError, TransformerException {

        // Map to hold anonymized person names
        Map<String, Integer> persons = new HashMap<>();

        // Read XML document from input stream
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(new InputSource(input));

        // Locate nodes in XML document containing names
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xpath.evaluate("/popcorn/chattimeline",
                doc, XPathConstants.NODESET);

        // Replace names in chat timeline
        for (int idx = 0; idx < nodes.getLength(); idx++) {
            Node node = nodes.item(idx).getAttributes().getNamedItem("name");
            String val = node.getNodeValue();

            // replace names with anonymized names
            if (persons.containsKey(val)) {
                node.setNodeValue("Person-" + persons.get(val));
            } else {
                persons.put(val, persons.size() + 1);
                node.setNodeValue("Person-" + persons.get(val));
            }
        }

        // Save modified XML document into memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Transformer xformer = TransformerFactory.newInstance().newTransformer();
        xformer.transform(new DOMSource(doc), new StreamResult(baos));

        // return XML doc data as InputStream
        byte[] xmlData = baos.toByteArray();
        return new ByteArrayInputStream(xmlData);
    }

}
