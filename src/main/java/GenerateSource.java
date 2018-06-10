import org.apache.commons.lang.StringUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.microsoft.OfficeParser;
import org.apache.tika.sax.BodyContentHandler;
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

   // private List<TestCase> testCases = new ArrayList<>();

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
            final ContentHandler handler = new BodyContentHandler(); // TODO use ToXMLContentHandler and jsoup
            new OfficeParser().parse(stream, handler, new Metadata(), new ParseContext());
            text = handler.toString();
            handler.
        } catch (IOException | TikaException | SAXException e) {
            e.printStackTrace();
        }

        LOGGER.debug(text);

        // Split to test cases -> launch javaClassTestWritter.write(testCase);
        status = Status.START;
        new BufferedReader(new StringReader(text)).lines().forEach(
                (line) -> processLine(line)
        );
        addNewTestCase();
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
            addNewTestCase();
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
            testCase.addSteps(steps);
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
        final StringBuffer sb = new StringBuffer();
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