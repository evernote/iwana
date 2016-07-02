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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.evernote.iwana.pb.TSWP.TSWPArchives.ObjectAttributeTable.ObjectAttribute;

/**
 * Some helper methods.
 */
final class IwanaUtil {

  private IwanaUtil() {
    throw new IllegalStateException("No instances");
  }

  private static final Comparator<ObjectAttribute> COMPARATOR_OBJECTATTRIBUTE =
      new Comparator<ObjectAttribute>() {
        @Override
        public int compare(ObjectAttribute o1, ObjectAttribute o2) {
          int c1 = o1.getCharacterIndex();
          int c2 = o2.getCharacterIndex();

          if (c1 < c2) {
            return -1;
          } else if (c1 == c2) {
            int h1 = o1.hashCode();
            int h2 = o2.hashCode();
            if (h1 < h2) {
              return -1;
            } else {
              return 1;
            }
          } else {
            return 1;
          }
        }
      };

  public static List<ObjectAttribute> sortObjectAttributes(
      final List<ObjectAttribute> list) {
    Collections.sort(list, COMPARATOR_OBJECTATTRIBUTE);
    return list;
  }

}
