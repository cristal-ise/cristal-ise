import javax.xml.parsers.DocumentBuilderFactory
import org.codehaus.groovy.tools.xml.DomToGroovy

def builder     = DocumentBuilderFactory.newInstance().newDocumentBuilder()
def document    = builder.parse(new FileInputStream(new File("../dslforge/src/test/data/soap.xml")))
def output      = new StringWriter()
def converter   = new DomToGroovy(new PrintWriter(output))

converter.print(document)
println output.toString()