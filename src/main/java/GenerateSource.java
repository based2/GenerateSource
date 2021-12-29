import org.apache.commons.lang3.StringUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.apache.tika.sax.XHTMLContentHandler;
import org.apache.tika.sax.xpath.Matcher;
import org.apache.tika.sax.xpath.MatchingContentHandler;
import org.apache.tika.sax.xpath.XPathParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

// https://github.com/apache/tika/blob/master/tika-parsers/src/test/java/org/apache/tika/parser/microsoft/WordParserTest.java
// https://stackoverflow.com/questions/10250617/java-apache-poi-can-i-get-clean-text-from-ms-word-doc-files
// https://stackoverflow.com/questions/6800509/are-there-apis-for-text-analysis-mining-in-java
// https://github.com/topics/text-analysis?l=java
// https://www.ibm.com/developerworks/opensource/tutorials/os-apache-tika/index.html
// https://boilerpipe-web.appspot.com/
// http://rtw.ml.cmu.edu/rtw/ NELL: Never-Ending Language Learning
// https://www.hascode.com/2012/12/running-categorized-tests-using-junit-maven-and-annotated-test-suites/
// https://www.hascode.com/2012/12/content-detection-metadata-and-content-extraction-with-apache-tika/
// https://stanbol.apache.org/ set of reusable components for semantic content management
// https://github.com/apache/tika/blob/master/tika-core/src/main/java/org/apache/tika/sax/PhoneExtractingContentHandler.java
// https://github.com/Axel-Girard/tika_poc
// https://blog.cloudera.com/blog/2013/07/morphlines-the-easy-way-to-build-and-integrate-etl-apps-for-apache-hadoop/
public class GenerateSource {

    private final static Logger LOGGER = LogManager.getLogger(GenerateSource.class);

    private final static String DIR_SOURCE = "./tests/spec/NewTests.doc";
    private final static String DIR_TARGET = "./tests/generated/";

    private final static String PACKAGE = "com.github.based2";
    private final static String AUTHOR = "AUTHOR";
    private final static String INPORTS = "assertj";
    private static String date = "20180609";

    private static JavaClassTestWritter javaClassTestWritter  = new JavaClassTestWritter(
            PACKAGE, AUTHOR, date, DIR_TARGET);
    /** Jsoup HTML parser document */
    private Document document;

    private List<String> previousLines = new ArrayList<>();
    private List<String> previousChapter = new ArrayList<>();
    private String line;
    private TestCase testCase;

    private String number;
    private String title;
    private String description;

    private List<String> steps = new ArrayList<>();
    private List<String> expectations = new ArrayList<>();

    private Status status;

    enum Status {
        START, TITLE, STEP_EXPECT;
    }

    public void start(){
        String text = "";

        // Reads functional specification file MS Word .doc file
       final URL url = getClass().getResource(DIR_SOURCE);
        try (final InputStream name = url.openStream();
             final TikaInputStream stream = TikaInputStream.get(name)) {
            //XPathParser xhtmlParser = new XPathParser("xhtml", XHTMLContentHandler.XHTML);
           // Matcher divContentMatcher = xhtmlParser.parse("/xhtml:html/xhtml:body/xhtml:table/descendant::node()");
           // ContentHandler xmlhandler = new MatchingContentHandler(new ToXMLContentHandler(), divContentMatcher);
            /* https://issues.apache.org/jira/browse/TIKA-1774
            org.xml.sax.SAXException: Namespace http://www.w3.org/1999/xhtml not declared
            The incoming SAX events are expected to be well-formed (properly nested, etc.) and valid HTML.
            */
            ContentHandler xmlhandler = new ToXMLContentHandler();
            new OfficeParser().parse(stream, xmlhandler, new Metadata(), new ParseContext());
            text = xmlhandler.toString();
        } catch (IOException | TikaException | SAXException e) {
            e.printStackTrace();
        }

        LOGGER.debug(text);

        // Parse HTML to testCases
        document = Jsoup.parse(text);
        final Elements header1s = document.getElementsByTag("h1");
        //final Elements tables = document.getElementsByTag("table");

        int testCaseCount = 1;
        for (Element header1 : header1s) {
            testCase = new TestCase();
            line = header1.text();
            if (isNextTestCaseChapter()) { // testCase.setTitle
                final Element table = next(header1, "table");
                testCase.setSteps(
                        texts(table, "tbody > tr > td:nth-child(1) > p") );
                testCase.setExpectations(
                        texts(table,"tbody > tr > td:nth-child(2) > p") );
            }
            javaClassTestWritter.write(testCase);
            testCaseCount++;
        }

        // Split to test cases -> launch javaClassTestWritter.write(testCase);
        /*status = Status.START;
        new BufferedReader(new StringReader(text)).lines().forEach(
                (line) -> processLine(line)
        );
        addNewTestCase();*/
    }

    private Element next(final Element current, final String tagName) {
        Element node = current.nextElementSibling();
        while (node != null && !node.tagName().equalsIgnoreCase(tagName)) {
            node = node.nextElementSibling();
        }
        return node;
    }

    private List<String> texts(final Element parent, final String cssSelectors){
        final List<String> texts = new ArrayList<>();
        final Elements elements = parent.select(cssSelectors);
        for (Element element : elements) {
            texts.add(element.text());
        }
        return texts;
    }

    private void processLine(final String line){
        this.line = line.trim();

        if (status==Status.START && isNextTestCaseChapter()) status = Status.TITLE;
        if ((status==Status.TITLE || status==Status.STEP_EXPECT)
            && isNextStepExpectation()) {
            status = Status.STEP_EXPECT;
        }
    }

    private boolean isNextTestCaseChapter(){
        if (StringUtils.isBlank(line) || !isStartsWithDigit()) return false;

        // isStartsWithDigit()
        // TODO improve number separated by dots.
        List<String> numbers = StringExtractor.extractNumbers(line);
        if (numbers.isEmpty()) return false;
        if (isNextChapter(numbers)) {
            testCase.setNumber( concat(previousChapter) );
            // extracts title
            int posAfterChapter = line.lastIndexOf( getLatest(previousChapter) );
            testCase.setTitle( line.substring(posAfterChapter + 4) ); // ' : '
            return true;
        }

        return false;
    }

    private boolean isNextChapter(final List<String> numbers){
        if (null==previousChapter) {
            testCase = new TestCase();
            return true;
        }
        if (previousChapter.size()==0
                || (previousChapter.size()==numbers.size()
            && getLatestInt(previousChapter)==(getLatestInt(numbers)+1))){
            previousChapter = numbers;
            return true;
        }
        return false;
    }

    private void addNewTestCase() {
        if (null!=testCase) {
            testCase.setSteps(steps);
            javaClassTestWritter.write(testCase);
            steps.clear();
            expectations.clear();
        }

        testCase = new TestCase();

        //testCase.addExpectations(expectations);
        //previousLines.clear();
        //line = null;
    }

    private String getLatest(final List<String> list){
        return list.get(list.size()-1);
    }

    private int getLatestInt(final List<String> list){
        return Integer.valueOf(list.get(list.size()));
    }

    private boolean isStartsWithDigit(){
       return Character.isDigit(line.charAt(0));
    }

    private boolean isNextStepExpectation(){
        if (StringUtils.isBlank(line)) return false;
        // TODO add potential text filters

        // TODO find heuristic to split lines
        steps.add(line);
        return true;
    }

    private String concat(final List<String> list){
        final StringBuilder sb = new StringBuilder();
        for(String item : list){
            sb.append(item);
        }
        return sb.toString();
    }

    public static void main(String[] argv) {
        final GenerateSource generateSource = new GenerateSource();
        generateSource.start();
    }
}
