import groovy.xml.NamespaceBuilder
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.MarkupBuilder

class NamespaceNodeTest {
    static void main(args) {
        def writer = new StringWriter()
        def builder = new MarkupBuilder(writer)
        def xmlns = new NamespaceBuilder(builder)
//        def xsd = xmlns.declareNamespace(xs: 'http://www.w3.org/2001/XMLSchema')
        def xsd = xmlns.namespace('http://www.w3.org/2001/XMLSchema', 'xs')
        xsd.schema {
            annotation { documentation("Purchase order schema for Example.com.") }
            element(name:'purchaseOrder', type:'PurchaseOrderType')
            element(name:'comment', type:'xs:string')
            complexType(name:'PurchaseOrderType') {
                sequence {
                    element(name:'shipTo', type:'USAddress')
                    element(name:'billTo', type:'USAddress')
                    element(minOccurs:'0', ref:'comment')
                    element(name:'items', type:'Items')
                }
                attribute(name:'orderDate', type:'xs:date')
            }
            complexType(name:'USAddress') {
                sequence {
                    element(name:'name', type:'xs:string')
                    element(name:'street', type:'xs:string')
                    element(name:'city', type:'xs:string')
                    element(name:'state', type:'xs:string')
                    element(name:'zip', type:'xs:decimal')
                }
                attribute(fixed:'US', name:'country', type:'xs:NMTOKEN')
            }
            complexType(name:'Items') {
                sequence {
                    element(maxOccurs:'unbounded', minOccurs:'0', name:'item') {
                        complexType {
                            sequence {
                                element(name:'productName', type:'xs:string')
                                element(name:'quantity') {
                                    simpleType {
                                        restriction(base:'xs:positiveInteger') { maxExclusive(value:'100') }
                                    }
                                }
                                element(name:'USPrice', type:'xs:decimal')
                                element(minOccurs:'0', ref:'comment')
                                element(minOccurs:'0', name:'shipDate', type:'xs:date')
                            }
                            attribute(name:'partNum', type:'SKU', use:'required')
                        }
                    }
                }
            }
            /* Stock Keeping Unit, a code for identifying products */
            simpleType(name:'SKU') {
                restriction(base:'xs:string') { pattern(value:'\\d{3}-[A-Z]{2}') }
            }
        }
        
        println writer.toString()
    }
}