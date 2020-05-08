package org.cristalise.dsl.test.csv

import java.time.LocalDate

import org.cristalise.dsl.csv.CSVGroovyParser
import org.junit.Test

import groovy.xml.MarkupBuilder

/**
 * 
 */
class NewCSVGroovyParserTest {

    @Test
    public void noHeaderValues() {
        use(CSVGroovyParser) {
            new File("src/test/data/noHeaderAddress.csv").csvEachRow(headerRows:0) { address, i ->
                assert address[0]
                assert address[1]
                assert address[2]
            }
        }
    }

    @Test
    public void singleLineHeaderValues() {
        use(CSVGroovyParser) {
            new File("src/test/data/address.csv").csvEachRow(headerRows:1) { address, i ->
                assert address.name
                assert address.postal
                assert address.email
            }
        }
    }

    @Test
    public void allTypeConverts() {
        use(CSVGroovyParser) {
            new File("src/test/data/convertedTypes.csv").csvEachRow(headerRows:1, dateFormater:'yyyy/MM/dd') { types, i ->
                assert types
                
                assert types.empty == ''
                assert types.string == 'customer'
                assert types.integer == 34
                assert types.date == LocalDate.parse('1969/02/23', 'yyyy/MM/dd')
                assert types.bigdecimal == 89.6
                assert types.biginteger == 12345678901234567890
            }
        }
    }

    @Test
    public void multiLineHeader() {
        use(CSVGroovyParser) {
            def header = new File("src/test/data/multiHeader.csv").csvHeader(headerRows:2)

            assert header

            assert header[0] == ["kind"]
            assert header[6] == ["sex"]

            assert header[7] == ["address","purpose"]
            assert header[14] == ["address","deliveryInfo"]

            assert header[15] == ["phone","purpose"]
            assert header[17] == ["phone","number"]

            assert header[18] == ["email","purpose"]
            assert header[19] == ["email","address"]
        }
    }

    @Test
    public void multiLineHeaderValues() {
        use(CSVGroovyParser) {
            new File("src/test/data/multiHeader.csv").csvEachRow(headerRows:2) { user, i ->
                assert user.kind
                assert user.sex

                assert user.address
                assert user.address.purpose
                assert user.address.deliveryInfo

                assert user.phone
                assert user.phone.purpose
                assert user.phone.number

                assert user.email
                assert user.email.purpose
                assert user.email.address
            }
        }
    }

    def multiLineHeaderTestClosure = { user, i ->
        assert user.kind
        assert user.sex
        assert user.age

        assert user.contacts.address[0].purpose
        assert user.contacts.address[0].deliveryInfo

        assert user.contacts.phone[0].purpose
        assert user.contacts.phone[0].number

        assert user.contacts.email[0].purpose
        assert user.contacts.email[0].address

        if (i == 0) {
            assert user.contacts.email[1].purpose == "secondary"
            assert user.contacts.email[1].address == "customer2@nowhere.com"
        }
        else {
            assert user.contacts.email[1].purpose == ""
            assert user.contacts.email[1].address == ""
        }
    }

    def multiLineHeaderFile = "src/test/data/multiHeaderWithRepeat.csv"


    def checkMultiLineHeader(header) {
        assert header

        assert header[0] == ["kind"]
        assert header[1] == ["userid"]
        assert header[2] == ["password"]
        assert header[3] == ["title"]
        assert header[4] == ["firstName"]
        assert header[5] == ["lastName"]
        assert header[6] == ["sex"]
        assert header[7] == ["age"]
        assert header[8] == ["contacts", "address[0]", "purpose"]
        assert header[9] == ["contacts", "address[0]", "country"]
        assert header[10] == ["contacts", "address[0]", "countryCode"]
        assert header[11] == ["contacts", "address[0]", "province"]
        assert header[12] == ["contacts", "address[0]", "address1"]
        assert header[13] == ["contacts", "address[0]", "city"]
        assert header[14] == ["contacts", "address[0]", "postalCode"]
        assert header[15] == ["contacts", "address[0]", "deliveryInfo"]
        assert header[16] == ["contacts", "phone[0]", "purpose"]
        assert header[17] == ["contacts", "phone[0]", "areaCode"]
        assert header[18] == ["contacts", "phone[0]", "number"]
        assert header[19] == ["contacts", "email[0]", "purpose"]
        assert header[20] == ["contacts", "email[0]", "address"]
        assert header[21] == ["contacts", "email[1]", "purpose"]
        assert header[22] == ["contacts", "email[1]", "address"]
    }

    @Test
    public void multiLineHeaderWithRepeatValues() {
        def i=0

        use(CSVGroovyParser) {
            new File(multiLineHeaderFile).csvEachRow([headerRows:3], multiLineHeaderTestClosure)
        }
    }


    @Test
    public void multiLineHeaderWithExternalHeader() {
        def i=0

        use(CSVGroovyParser) {
            def header = new File(multiLineHeaderFile).csvHeader(headerRows:3)

            checkMultiLineHeader(header)

            new File(multiLineHeaderFile).csvEachRow(header:header, skipRows:3, multiLineHeaderTestClosure)
        }
    }

    @Test
    public void multiLineStringHeader() {
        def c = "contacts" //tests GString expression
        def headerText = """\
,,,,,,,,$c,,,,,,,,,,,,,,
,,,,,,,,address[0],,,,,,,,phone[0],,,email[0],,email[1],
kind,userid,password,title,firstName,lastName,sex,age,purpose,country,countryCode,province,address1,city,postalCode,deliveryInfo,purpose,areaCode,number,purpose,address,purpose,address
"""
        use(CSVGroovyParser) {
            def header = new String(headerText).csvHeader(headerRows:3)

            checkMultiLineHeader(header)

            new File(multiLineHeaderFile).csvEachRow(header:header, skipRows:3, multiLineHeaderTestClosure)
        }
    }

    @Test
    public void wikiTableString() {
        def wikiTable ="""\
                | Num | Status | Action | Who | When | Progress |
                | 1 | C | Chose new colours | John | 1-Dec-02 | Done |
                | 2 | X | Release | John | 1-Apr-02 | |
                | 3 |   | Get feedback | Anne | 1-Feb-02 | |
                | 4 | C | Spec error handling | Jack | 1-Dec-02 | |
                | 5 |   | Abc | John | | |"""

        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        writer << '<?xml version="1.0" encoding="UTF-8"?>\n'

        use(CSVGroovyParser) {
            xml.calendar
            {
                new String(wikiTable).csvEachRow([headerRows:1, separatorChar:'|', skipLeftCols:1, skipRightCols:1, trimData:true])
                { 
                    row, i ->

                    assert row.Num
                    assert row.Action
                    assert row.Who

                    if(i==2 || i==4 ) {assert !row.Status}
                    else {assert row.Status}

                    if(i==4) {assert !row.When }
                    else {assert row.When}

                    if(i==0) {assert row.Progress }
                    else {assert !row.Progress }

                    task(num:row.Num) {
                        status(row.Status)
                        action(row.Action)
                        who(row.Who)
                        when(row.When)
                        if(row.Progress) {
                            progress(row.Progress)
                        }
                    }
                }
            }
            println writer.toString()
        }
    }
}
