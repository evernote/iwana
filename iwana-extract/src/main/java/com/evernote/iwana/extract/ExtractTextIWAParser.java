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

import com.evernote.iwana.IwanaParser;

/**
 * An {@link IwanaParser} that can extract text from Keynote, Pages, Numbers, and
 * potentially other iWork'13-style documents.
 */
class ExtractTextIWAParser extends IwanaParser<ExtractTextCallback> {
  @Override
  protected ExtractTextIWAContext newContext(String documentName,
      ExtractTextCallback target) {
    //FIXME -- the original document name is not actually
    //passed into this by the IWAParser.  So, for now,
    //this is misleading because the ContextBase is always returned
    if (documentName == null) {
      return new ContextBase(documentName, target);
    }
    if (documentName.endsWith(".key")) {
      return new KeynoteContext(documentName, target);
    } else if (documentName.endsWith(".pages")) {
      return new PagesContext(documentName, target);
    } else if (documentName.endsWith(".numbers")) {
      return new NumbersContext(documentName, target);
    } else {
      return new ContextBase(documentName, target);
    }
  }
}
