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

import com.evernote.iwana.IwanaParserCallback;

/**
 * The callback handler that is called for extracted text.
 */
public abstract class ExtractTextCallback extends IwanaParserCallback {

  /**
   * Called for a portion of text extracted from the document.
   * 
   * @param text The text block.
   * @param attrs Some text attributes
   */
  public abstract void onTextBlock(final String text, TextAttributes attrs);
}
