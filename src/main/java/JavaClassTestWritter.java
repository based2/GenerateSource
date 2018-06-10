import net.sourceforge.jenesis4java.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 *
 * http://jenesis4java.sourceforge.net/index.html
 */
public class JavaClassTestWritter {

    private final static Logger LOGGER = LogManager.getLogger(JavaClassTestWritter.class);

    private String _package;
    private String author;
    private String date;
    private String targetDirectory;

    public JavaClassTestWritter(final String _package,
                                final String author, final String date,
                                final String targetDirectory){
        this._package = _package;
        this.author = author;
        this.date = date;
        this.targetDirectory = targetDirectory;
    }

    public void write(final TestCase testCase) {
        final String methodName = testCase.getNumber() + "_" + StringUtils.deleteWhitespace(testCase.getTitle());
        final String className = "TEST_" + methodName;
        final String description = testCase.getDescription();
        write(className, methodName, description, testCase.getSteps());
    }

    public void write(final String className,
                      final String methodName,
                      final String description,
                      final List<String> steps){

        // Get the VirtualMachine implementation.
        final VirtualMachine vm = VirtualMachine.getVirtualMachine();

        // Instantiate a new CompilationUnit.
        // The argument to the compilation unit is the "codebase"
        // or directory where the compilation unit should be written.
        //
        // Make a new compilation unit rooted to the given sourcepath.
        CompilationUnit unit = vm.newCompilationUnit(targetDirectory + className);

        // Set the package namespace.
        unit.setNamespace(_package);

        // Add an import statement.
        unit.addImport("test.import");

        // Comment the package with a javadoc (DocumentationComment).
        unit.setComment(Comment.DOCUMENTATION, "@author " + author + "\n@since " + date);

       // unit.addAnnotation();
        //VirtualMachine.newAnnotation(Test.classs);
        
        // Make a new class.
        PackageClass cls = unit.newClass(className);

        // Make it a public class.
        cls.setAccess(Access.PUBLIC);
        // Extend Object just.
        //cls.setExtends("Object");
        // Implement serializable just.
        //cls.addImplements("Serializable");
        // Comment the class with a javadoc (DocumentationComment).
        unit.setComment(Comment.DOCUMENTATION, description);

        // Make a new Method in the Class having type VOID and name "main".
       /* ClassMethod method = cls.newMethod(vm.newType(Type.VOID), "main");
        // Make it a public method.
        method.setAccess(Access.PUBLIC);
        // Make it a static method
        method.isStatic(true);
        // Add the "String[] argv" formal parameter.
        method.addParameter(vm.newArray("String", 1), "argv");

        // Create a new Method Invocation expression.
        Invoke println = vm.newInvoke("System.out", "println");
        // Add the Hello World string literal as the sole argument.
        println.addArg(vm.newString("Hello World!"));
        // Add this expression to the method in a statement.
        method.newStmt(println);*/

        ClassMethod method2 = cls.newMethod(vm.newType(Type.VOID), methodName);

        method2.setAccess(Access.PUBLIC);
        ClassMethod method3 = cls.newMethod(vm.newType("String"), "set" );//+ capitalize("name"));
        method3.setAccess(Access.PUBLIC);

        method3.addParameter(vm.newType("int"), "input");
        Let letx = method3.newLet(vm.newType(Type.LONG));
        letx.addAssign("x", vm.newBinary(Binary.ADD, vm.newVar("input"), vm.newLong(12L)));
        method3.newReturn().setExpression(vm.newVar("x"));

        // Encode the file, compile it, and load the class
        System.out.println(cls);

        try {
            Files.write(Paths.get(className), cls.toString().getBytes()); // , StandardCharsets.UTF_8)
        } catch (IOException e) {
            e.printStackTrace();
        }

        // java.lang.Class hello = cls.load();

        // Get the main method
        // java.lang.reflect.Method main = hello.getMethod("main", new Class[] {
        // String[].class });

        // Invoke it
        // main.invoke(hello.newInstance(), new Object[] { new String[] {} });
    }
}
