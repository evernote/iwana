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
import java.util.HashMap;
import java.util.Map;

/**
 * A demo application. Returns text and metadata from the document in a map.
 */
public class ExtractTextApp {
	private String texts = "";
    private Map<String, String> metaDatas = new HashMap();

    public ExtractTextApp() {
    }
    public Map<String, String> parse(String[] args) throws IOException {
        this.texts = "";
        this.metaDatas = new HashMap();
        if (args.length != 1) {
            System.err.println("Syntax: ExtractTextApp <filename>");
            System.exit(1);
        }
        ExtractTextCallback target = new ExtractTextCallback() {
            public void onTextBlock(String text, TextAttributes scope) {
                ExtractTextApp.this.texts = ExtractTextApp.this.texts + "  " + text;
            }
            public void onMetaBlock(String key, String value) {
                String prev = "";
                if (!ExtractTextApp.this.metaDatas.containsKey(key)) {
                    ExtractTextApp.this.metaDatas.put(key, value);
                } else if (key == "Comments") {
                    prev = (String)ExtractTextApp.this.metaDatas.get(key);
                    ExtractTextApp.this.metaDatas.remove(key);
                    ExtractTextApp.this.metaDatas.put(key, prev + "; " + value);
                }
            }
        };
        ExtractTextIWAParser parser = new ExtractTextIWAParser();
        parser.parse(new File(args[0]), target, args[0]);
        this.metaDatas.put("text", this.texts);
        return this.metaDatas;
    }      
}
