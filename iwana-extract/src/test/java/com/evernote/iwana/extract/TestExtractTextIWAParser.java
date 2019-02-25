/**
 * Copyright 2014-2016 Evernote Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evernote.iwana.extract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Initial basic tests for text extraction.
 */
public class TestExtractTextIWAParser {

  @Test
  public void testNumbers() throws Exception {
    String contents = getText("testNumbers2013.numbers");

    //header
    assertContains("Checking Account: 300545668", contents);
    //note
    assertContains("Try adding", contents);
    //cell contents
    assertContains("Debit Card", contents);

    //table in upper right, DataList-17
    assertContains("Total", contents);

    //DataList-19 column "Description"
    assertContains("Night on the town", contents);

  }

  @Test
  @Ignore("Not yet extracting these")
  public void testNumbersIgnored() throws Exception {
    String contents = getText("testNumbers2013.numbers");

    //DataList-7, column "Category" in table
    assertContains("Deposit", contents);//home food gas

    //sheet name...exists in the Document.iwa file
    assertContains("Second sheet", contents);

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
  
  //Test meta data extraction.
  
  @Test
  public void testPagesMetaData() throws Exception {
      String[] args = new String[]{Paths.get(this.getClass().getResource("/test-documents/testPages2013.pages").toURI()).toString()};
      ExtractTextApp extractTextApp1 = new ExtractTextApp();
      Map<String, String> resultPages = extractTextApp1.parse(args);
      assertTrue((resultPages.get("Content-Type")).contains("pages"));
      assertEquals((resultPages.get("TopMargin")),("56.692917"));
  }

  @Test
  public void testKeyMetaData() throws Exception {
      String[] args = new String[]{Paths.get(this.getClass().getResource("/test-documents/testKeynote2013.key").toURI()).toString()};
      ExtractTextApp extractTextApp = new ExtractTextApp();
      Map<String, String> resultKey = extractTextApp.parse(args);
      assertTrue((resultKey.get("Content-Type")).contains("key"));
      assertEquals((resultKey.get("Comments")), "A nice comment; A nice comment");
      
  }
  
  @Test
  public void testNumMetaData() throws Exception {
      String[] args = new String[]{Paths.get(this.getClass().getResource("/test-documents/testNumbers2013.numbers").toURI()).toString()};
      ExtractTextApp extractTextApp = new ExtractTextApp();
      Map<String, String> resultNum = extractTextApp.parse(args);
      assertTrue((resultNum.get("Content-Type")).contains("num"));
      assertEquals((resultNum.get("NumberOfSheets")), "2");      
  }

  private void assertContains(String needle, String haystack) {
    int i = haystack.indexOf(needle);
    if (i < 0) {
      fail("Couldn't find >" + needle + "< in >" + haystack+"<");
    }
  }
  
  private String getText(String testFileName) throws Exception {
	    ExtractTextIWAParser parser = new ExtractTextIWAParser();
	    SimpleExtractTextCallback target = new SimpleExtractTextCallback();
	    File f = getTestFile(testFileName);
	    parser.parse(f, target, Paths.get(this.getClass().getResource("/test-documents/"+testFileName).toURI()).toString());
	    return target.toString();
	  }

  private File getTestFile(String testFileName) throws URISyntaxException {
    return Paths.get(
        this.getClass().getResource("/test-documents/" + testFileName).toURI()).toFile();

  }

  private final static class SimpleExtractTextCallback extends ExtractTextCallback {
    StringBuilder sb = new StringBuilder();

    @Override
    public void onTextBlock(String text, TextAttributes scope) {
      this.sb.append(text);
      this.sb.append("\n");
    }

    @Override
    public String toString() {
      return sb.toString();
    }
    
    @Override
    public void onMetaBlock(String key, String value) {
        this.sb.append("\n");
    }

  }

}
