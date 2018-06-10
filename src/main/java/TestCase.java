import java.util.List;

public class TestCase {

    private String number = "";
    private String title = "";
    private String description = "";

    private List<String> steps;
    private List<String> expectations;

    public TestCase(){ }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addSteps(final List<String> steps) {
        this.steps = steps;
    }

    public List<String> getSteps() {
        return steps;
    }



}
