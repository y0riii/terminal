import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

public class TestShell {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private Shell shell;

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outContent));
        shell = new Shell();
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    public void testEcho() {
        shell.handleCommand("echo Hello, World!");

        String output = outContent.toString().trim();
        Assertions.assertEquals("Hello, World!", output);
    }

    @Test
    public void testPwd() {
        shell.handleCommand("pwd");

        String output = outContent.toString().trim();
        Assertions.assertEquals(System.getProperty("user.dir"), output);
    }

    @Test
    public void testCd() {
        shell.handleCommand("cd src/test");

        outContent.reset();

        shell.handleCommand("pwd");

        String output = outContent.toString().trim();
        String expectedPwd = System.getProperty("user.dir") + "\\src\\test";
        Assertions.assertEquals(expectedPwd, output);
    }

    @Test
    public void testLs() {
        shell.handleCommand("ls src/test");

        String output = outContent.toString().trim();
        Assertions.assertEquals("helpContent.txt\ntest.txt", output);

        shell.handleCommand("cd src/test");

        outContent.reset();

        shell.handleCommand("ls");

        output = outContent.toString().trim();
        Assertions.assertEquals("helpContent.txt\ntest.txt", output);
    }

    @Test
    public void testCat() {
        shell.handleCommand("cd src/test");

        outContent.reset();

        shell.handleCommand("cat test.txt");

        String output = outContent.toString().trim();
        Assertions.assertEquals("this is a test file", output);
    }

    @Test
    public void testTouch() {
        shell.handleCommand("cd src/test");
        shell.handleCommand("touch myTest.txt");

        File testFile = new File("src/test/myTest.txt");
        Assertions.assertTrue(testFile.exists() && testFile.isFile());
        testFile.delete();
    }

    @Test
    public void TestRm() {
        shell.handleCommand("cd src/test");
        shell.handleCommand("touch myTest.txt");
        shell.handleCommand("rm myTest.txt");

        File testFile = new File("src/test/myTest.txt");
        Assertions.assertFalse(testFile.exists() && testFile.isFile());
    }

    @Test
    public void TestMkdir() {
        shell.handleCommand("cd src/test");
        shell.handleCommand("mkdir myTestDir");

        File testDir = new File("src/test/myTestDir");
        Assertions.assertTrue(testDir.exists() && testDir.isDirectory());
        testDir.delete();
    }

    @Test
    public void TestRmdir() {
        shell.handleCommand("cd src/test");
        shell.handleCommand("mkdir myTestDir");
        shell.handleCommand("rmdir myTestDir");

        File testDir = new File("src/test/myTestDir");
        Assertions.assertFalse(testDir.exists() && testDir.isDirectory());
    }

    @Test
    public void TestMv() {
        shell.handleCommand("touch myTest.txt");
        shell.handleCommand("mv myTest.txt src/test");

        File testOriginal = new File("myTest.txt");
        File testMv = new File("src/test/myTest.txt");
        Assertions.assertTrue(!testOriginal.exists() && testMv.exists());
        testMv.delete();
    }

    @Test
    public void TestMvRename() {
        shell.handleCommand("cd src/test");
        shell.handleCommand("touch myTest.txt");
        shell.handleCommand("mv myTest.txt myMvTest.txt");

        File testOriginal = new File("src/test/myTest.txt");
        File testMv = new File("src/test/myMvTest.txt");
        Assertions.assertTrue(!testOriginal.exists() && testMv.exists());
        testMv.delete();
    }

    @Test
    public void TestRedirectOutput() {
        shell.handleCommand("cd src/test");
        shell.handleCommand("echo hello world > myTest.txt");

        outContent.reset();

        shell.handleCommand("cat myTest.txt");

        String output = outContent.toString().trim();
        File testFile = new File("src/test/myTest.txt");

        Assertions.assertEquals("hello world", output);

        shell.handleCommand("echo \"this is line 2\" >> myTest.txt");

        outContent.reset();

        shell.handleCommand("cat myTest.txt");

        output = outContent.toString().trim();

        String expectedOutput = "hello world\nthis is line 2";

        Assertions.assertEquals(expectedOutput, output);

        shell.handleCommand("echo all above lines should be deleted > myTest.txt");

        outContent.reset();

        shell.handleCommand("cat myTest.txt");

        output = outContent.toString().trim();
        Assertions.assertEquals("all above lines should be deleted", output);

        testFile.delete();
    }

    @Test
    public void TestGrep() {
        shell.handleCommand("grep new src/test/helpContent.txt");

        String grepOutput = outContent.toString().trim();

        String expectedOutput = "mkdir <directory_name>          Create a new directory\n  touch <file_name>               Create a new file";

        Assertions.assertEquals(expectedOutput, grepOutput);
    }

    @Test
    public void TestPipe() {
        shell.handleCommand("help | grep new");

        String PipeOutput = outContent.toString().trim();

        String expectedOutput = "mkdir <directory_name>          Create a new directory\n  touch <file_name>               Create a new file";

        Assertions.assertEquals(expectedOutput, PipeOutput);
    }
}
