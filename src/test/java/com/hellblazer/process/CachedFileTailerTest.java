package com.hellblazer.process;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by saptarshi.roy on 9/29/14.
 */
public class CachedFileTailerTest {

    File testFile;

    @Before
    public void setupTestFile() throws IOException {
        testFile = File.createTempFile("testFile", ".txt");
    }

    @After
    public void tearDown() {
        testFile.delete();
    }

    void populateTestFile(int numLines) throws IOException {
        populateTestFile(numLines, 0, false);
    }

    void populateTestFile(int numLines, int start, boolean append) throws IOException {

        try (FileWriter fw = new FileWriter(testFile, append)) {
            for (int i = 1; i <= numLines; i++) {
                int lineLabelNum = start + i;
                fw.write(String.format("Line : %d\n", lineLabelNum));
            }
        }

    }

    @Test
    public void testNormalFileWithNumlinesBoundaries() throws IOException {
        populateTestFile(10);

        CachedFileTailer cft = new CachedFileTailer(testFile, TimeUnit.SECONDS, 1, 6);
        List<String> reversedLines = cft.getTailLines(6);

        assertTrue(reversedLines.get(0).endsWith("5"));
        assertTrue(reversedLines.get(5).endsWith("10"));
        assertEquals(6, reversedLines.size());

        reversedLines = cft.getTailLines(100);
        assertEquals(6, reversedLines.size());

        reversedLines = cft.getTailLines(3);
        assertEquals(3, reversedLines.size());
        assertTrue(reversedLines.get(0).endsWith("8"));
        assertTrue(reversedLines.get(2).endsWith("10"));

        reversedLines = cft.getTailLines(0);
        assertEquals(0, reversedLines.size());

        reversedLines = cft.getTailLines(-5);
        assertEquals(0, reversedLines.size());
    }

    @Test
    public void testShortFile() throws IOException {
        populateTestFile(10);

        CachedFileTailer cft = new CachedFileTailer(testFile, TimeUnit.SECONDS, 1, 20);
        List<String> reversedLines = cft.getTailLines(20);

        assertTrue(reversedLines.get(0).endsWith("1"));
        assertTrue(reversedLines.get(9).endsWith("10"));
        assertEquals(10, reversedLines.size());
    }

    @Test
    public void testEmptyFile() throws IOException {
        CachedFileTailer cft = new CachedFileTailer(testFile, TimeUnit.SECONDS, 1, 20);
        List<String> reversedLines = cft.getTailLines(20);

        assertEquals(0, reversedLines.size());
    }

    @Test
    public void testChangingFile() throws IOException, InterruptedException {
        populateTestFile(10);

        CachedFileTailer cft = new CachedFileTailer(testFile, TimeUnit.SECONDS, 1, 6);
        List<String> reversedLines = cft.getTailLines(6);

        assertTrue(reversedLines.get(0).endsWith("5"));
        assertTrue(reversedLines.get(5).endsWith("10"));
        assertEquals(6, reversedLines.size());

        populateTestFile(20, 10, true);

        // Everything should still be the same at first
        reversedLines = cft.getTailLines(6);
        assertTrue(reversedLines.get(0).endsWith("5"));
        assertTrue(reversedLines.get(5).endsWith("10"));
        assertEquals(6, reversedLines.size());

        // Let's sleep for an interval then recheck
        TimeUnit.SECONDS.sleep(2);

        reversedLines = cft.getTailLines(6);
        assertTrue(reversedLines.get(0).endsWith("25"));
        assertTrue(reversedLines.get(5).endsWith("30"));
        assertEquals(6, reversedLines.size());
    }
}
