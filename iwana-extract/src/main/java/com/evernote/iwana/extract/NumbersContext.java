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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.evernote.iwana.MessageActions;
import com.evernote.iwana.pb.TN.TNArchives.DocumentArchive;
import com.evernote.iwana.pb.TSP.TSPArchiveMessages.ArchiveInfo;
import com.evernote.iwana.pb.TSP.TSPArchiveMessages.MessageInfo;
import com.evernote.iwana.pb.TST.TSTArchives.TableDataList;
import com.evernote.iwana.pb.TST.TSTArchives.TableDataList.ListEntry;
import com.evernote.iwana.pb.TST.TSTArchives.TableDataList.ListType;
import com.google.protobuf.Message;

/**
 * A Numbers-specific extractor context.
 */
class NumbersContext extends ContextBase {
  private static final MessageActions NUMBERS_ACTIONS = new MessageActions(
      ContextBase.COMMON_ACTIONS);

  static {
    NUMBERS_ACTIONS
        .setAction(1, new StoreObject<DocumentArchive>(DocumentArchive.PARSER));

    NUMBERS_ACTIONS.setAction(new int[] {6005, 6201}, new StoreObject<TableDataList>(
        TableDataList.PARSER) {

      @Override
      protected void onMessage(TableDataList message, ArchiveInfo ai, MessageInfo mi,
          ExtractTextIWAContext context) throws IOException {
        super.onMessage(message, ai, mi, context);

        if (message.getListType() != ListType.STRING) {
          return;
        }

        List<ListEntry> entriesList = new ArrayList<>(message.getEntriesList());
        Collections.sort(entriesList, new Comparator<ListEntry>() {

          @Override
          public int compare(ListEntry o1, ListEntry o2) {
            if (o1.getKey() < o2.getKey()) {
              return -1;
            } else {
              return 1;
            }
          }
        });

        for (ListEntry le : entriesList) {
          // FIXME These list entries are probably not ordered correctly
          context.getTarget()
              .onTextBlock(le.getString(), TextAttributes.DEFAULT_DOCUMENT);
        }
      }

    });
  }

  protected NumbersContext(String documentFilename, ExtractTextCallback target) {
    super(documentFilename, target);
  }

  @Override
  protected MessageActions getMessageTypeActions() {
    return NUMBERS_ACTIONS;
  }

  @Override
  protected void processRootObject(Message obj) {
    // FIXME
  }
}
