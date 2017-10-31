package org.cristalise.kernel.test.persistency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.cristalise.kernel.utils.Logger;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

public class XMLUtils {

    public static final String root = "src/test/data/";

    public static String getXML(String type) throws Exception {
        return new String(Files.readAllBytes(Paths.get(root+type+".xml")));
    }

    public static String getXSD(String type) throws Exception {
        return new String(Files.readAllBytes(Paths.get(root+type+".xsd")));
    }

    /**
     * Compares 2 XML string
     *
     * @param expected the reference XML
     * @param actual the xml under test
     * @return whether the two XMLs are identical or not
     * @throws SAXException every exception
     * @throws IOException every exception
     */
    public static boolean compareXML(String expected, String actual) throws SAXException, IOException {

        Diff diffIdentical = DiffBuilder.compare(expected).withTest(actual)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
                .ignoreComments()
                .ignoreWhitespace()
                .checkForSimilar()
                .build();

        if(diffIdentical.hasDifferences()) Logger.warning(diffIdentical.toString());

        return !diffIdentical.hasDifferences();
    }


}
