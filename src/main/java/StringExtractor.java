import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringExtractor {

    private final static Pattern PATTERN_NUMBERS = Pattern.compile("-?\\d+");

    public static List<String> extractNumbers(final String text) {
        final List<String> numbers = new ArrayList<>();
        final Matcher m = PATTERN_NUMBERS.matcher(text);
        while (m.find()) {
            numbers.add(m.group());
        }
        return numbers;
    }
}
