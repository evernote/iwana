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
package com.evernote.iwana.extract;

/**
 * The scope of a given portion of text.
 */
public class TextAttributes {
  public static enum Scope {
    UNREFERENCED, DOCUMENT, NOTES
  }

  /**
   * The text is an unreferenced part of text and may be ignored.
   */
  static final TextAttributes DEFAULT_UNREFERENCED = new TextAttributes(
      Scope.UNREFERENCED);

  /**
   * The text is part of the main document text.
   */
  static final TextAttributes DEFAULT_DOCUMENT = new TextAttributes(Scope.DOCUMENT);

  /**
   * The text is part of the notes section.
   */
  static final TextAttributes DEFAULT_NOTES = new TextAttributes(Scope.NOTES);

  private final Scope scope;

  /**
   * @param scope The {@link Scope} for the attributes.
   */
  public TextAttributes(final Scope scope) {
    this.scope = scope;
  }

  public Scope getScope() {
    return scope;
  }

  @Override
  public String toString() {
    return scope.toString();
  }
}
