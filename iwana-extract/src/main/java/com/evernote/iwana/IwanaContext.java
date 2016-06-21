/**
 * Copyright 2014,2015 Evernote Corporation.
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
package com.evernote.iwana;

import java.io.InputStream;

/**
 * Holds the state of an iWork'13 document that is being parsed using an
 * {@link IwanaParser}.
 */
public abstract class IwanaContext<T extends IwanaParserCallback> {
  protected final T target;

  private final String documentFilename;
  private String currentFile;

  /**
   * Creates a new {@link IwanaContext} instance.
   * 
   * @param documentFilename The base name of the document itself (usually ends with one
   *          of {@code .pages}, {@code .key}, {@code .numbers})
   */
  protected IwanaContext(final String documentFilename, T target) {
    this.documentFilename = documentFilename;
    this.target = target;
  }

  /**
   * Called by the parser to check whether a given {@code .iwa} file should be considered
   * at all for parsing.
   * 
   * @param name The name of the {@code .iwa} file
   * @return {@code} true if acceptable.
   */
  public boolean acceptIWAFile(final String name) {
    return true;
  }

  /**
   * Returns the name of the current file (e.g., an {@code .iwa} file) that is being
   * parsed.
   * 
   * @return The name of the file.
   */
  public String getCurrentFile() {
    return currentFile;
  }

  /**
   * Sets the name of the current file (e.g., an {@code .iwa} file) that is being parsed.
   * 
   * @param currentFile The name of the file.
   */
  void setCurrentFile(String currentFile) {
    this.currentFile = currentFile;
  }

  /**
   * Called when the parser beings parsing the named {@code .iwa} file.
   * 
   * @param name The name of the file.
   */
  public void onBeginParseIWAFile(String name) {
  }

  /**
   * Called when the parser has finished parsing the named {@code .iwa} file.
   * 
   * @param name The name of the file.
   */
  public void onEndParseIWAFile(String name) {
  }

  /**
   * Called when the parser skips the named file (e.g., an {@code .iwa} file where
   * {@link #acceptIWAFile(String)} returned false, or any another resource
   * file)
   * 
   * @param name The name of the file.
   * @param in The InputStream to read the uncompressed file from (don't close!)
   */
  public void onSkipFile(String name, InputStream in) {
  }

  /**
   * Called when the parser begins parsing the {@code index.zip} archive.
   */
  public void onBeginParseIndexZip() {
  }

  /**
   * Called when the parser has finished parsing the {@code index.zip} archive.
   */
  public void onEndParseIndexZip() {
  }

  /**
   * Returns the base filename of the document being parsed.
   * 
   * @return The file name.
   */
  public String getDocumentFilename() {
    return documentFilename;
  }

  /**
   * Returns the {@link MessageActions} that are registered for this parser.
   * 
   * @return The registered {@link MessageActions}.
   */
  protected abstract MessageActions getMessageTypeActions();

  /**
   * Returns the {@link IwanaParserCallback} target.
   * 
   * @return The target.
   */
  public T getTarget() {
    return target;
  }
}
