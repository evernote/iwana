package com.evernote.iwana.extract;

import com.evernote.iwana.extract.ExtractTextCallback;
import com.evernote.iwana.extract.ExtractTextIWAParser;
import com.evernote.iwana.extract.TextAttributes;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.junit.Assert.fail;

/**
 * Initial basic tests for text extraction.
 * More refactoring on its way...
 */
public class TestExtractTextIWAParser {

    @Test
    public void testNumbers() throws Exception {
        String contents = getText("testNumbers2013.numbers");

        //header
        assertContains("Checking Account: 300545668", contents);
        //note
        assertContains("Try adding", contents);

    }

    @Test
    @Ignore("Not yet extracting these")
    public void testNumbersIgnored() throws Exception {
        String contents = getText("testNumbers2013.numbers");

        //DataList-7, column "Category" in table
        assertContains("Deposit", contents);//home food gas

        //cell contents
        assertContains("Debit Card", contents);

        //sheet name...exists in the Document.iwa file
        assertContains("Second sheet", contents);

        //table in upper right, DataList-17
        assertContains("Total", contents);

        //DataList-19 column "Description"
        assertContains("Night on the town", contents);
    }

    @Test
    public void testKeynote() throws Exception {
        String contents = getText("testKeynote2013.key");
        assertContains("nice note", contents);
        assertContains("nice comment", contents);
        assertContains("A sample presentation", contents);
        assertContains("Apache Tika project", contents);
        assertContains("Some random text for the sake of testability", contents);
    }

    @Test
    public void testPages() throws Exception {
        String contents = getText("testPages2013.pages");
        assertContains("A text box with text", contents);
        assertContains("Some plain text to parse", contents);
        assertContains("Sample pages document", contents);
        assertContains("A second page...", contents);

    }

    private void assertContains(String needle, String haystack) {
        int i = haystack.indexOf(needle);
        if (i < 0) {
            fail("Couldn't find >"+needle+"< in >"+haystack);
        }
    }

    private String getText(String testFileName) throws Exception {
        ExtractTextIWAParser parser = new ExtractTextIWAParser();
        SimpleExtractTextCallback target = new SimpleExtractTextCallback();
        File f = getTestFile(testFileName);
        parser.parse(f, target);
        return target.toString();
    }
    
    private File getTestFile(String testFileName) throws URISyntaxException {
        return Paths.get(
                this.getClass().getResource("/test-documents/"+testFileName).toURI()).toFile();

    }

    private final static class SimpleExtractTextCallback extends ExtractTextCallback {
        StringBuilder sb = new StringBuilder();
        @Override
        public void onTextBlock(String text, TextAttributes scope) {
            sb.append(text);
            sb.append("\n");
        }

        @Override
        public String toString() {
            return sb.toString();
        }

    };
}
