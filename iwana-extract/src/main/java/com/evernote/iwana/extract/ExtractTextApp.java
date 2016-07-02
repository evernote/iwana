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

import java.io.File;
import java.io.IOException;

/**
 * A demo application.
 */
public class ExtractTextApp {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Syntax: ExtractTextApp <filename>");
      System.exit(1);
    }

    ExtractTextCallback target = new ExtractTextCallback() {

      @Override
      public void onTextBlock(String text, TextAttributes scope) {
        System.out.println(text);
        System.out.println();
      }

    };

    ExtractTextIWAParser parser = new ExtractTextIWAParser();
    parser.parse(new File(args[0]), target);
  }
}
