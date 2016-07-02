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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.evernote.iwana.pb.TST.TSTArchives;
import org.apache.log4j.Logger;

import com.evernote.iwana.MessageActions;
import com.evernote.iwana.pb.TSD.TSDArchives.GroupArchive;
import com.evernote.iwana.pb.TSP.TSPArchiveMessages.ArchiveInfo;
import com.evernote.iwana.pb.TSP.TSPArchiveMessages.MessageInfo;
import com.evernote.iwana.pb.TSWP.TSWPArchives.ObjectAttributeTable;
import com.evernote.iwana.pb.TSWP.TSWPArchives.ObjectAttributeTable.ObjectAttribute;
import com.evernote.iwana.pb.TSWP.TSWPArchives.PlaceholderSmartFieldArchive;
import com.evernote.iwana.pb.TSWP.TSWPArchives.StorageArchive;
import com.google.protobuf.Message;

/**
 * Defines common actions for message types that are relevant to text extraction.
 * 
 * @see KeynoteContext For the Keynote-specific implementation.
 * @see PagesContext For the Pages-specific implementation.
 * @see NumbersContext For the Numbers-specific implementation.
 */
class ContextBase extends ExtractTextIWAContext {
  private static final Logger LOG = Logger.getLogger(ContextBase.class);

  /**
   * Constructs a {@link ContextBase} for the given document, using the given target
   * callback.
   * 
   * @param documentFilename The base name of the document itself (usually ends with one
   *          of {@code .pages}, {@code .key}, {@code .numbers})
   * @param target The callback
   */
  protected ContextBase(String documentFilename, ExtractTextCallback target) {
    super(documentFilename, target);
  }

  public static final MessageActions COMMON_ACTIONS = new MessageActions();
  static {
    COMMON_ACTIONS.setAction(2001, new ExtractTextActionBase<StorageArchive>(
        StorageArchive.PARSER) {

      @Override
      protected void onMessage(StorageArchive message, ArchiveInfo ai, MessageInfo mi,
          ExtractTextIWAContext context) throws IOException {

        if (!message.getInDocument()) {
          // not part of the document?
          return;
        }

        switch (message.getTextCount()) {
          case 0:
            // no text, ignore element
            return;
          case 1:
            // should we ever get more than one text block?
            break;
          default:
            LOG.info("Got more than one text block: " + message.getTextCount() + " for "
                + context.getCurrentFile());
        }

        final String text = message.getText(0);
        TextBlock tb = context.getTextBlock(ai.getIdentifier());

        List<ObjectAttribute> attrs = null;
        ObjectAttributeTable tableSmartField = message.getTableSmartfield();
        if (tableSmartField != null) {
          for (ObjectAttribute attr : tableSmartField.getEntriesList()) {
            if (attr.hasCharacterIndex()) {
              if (attrs == null) {
                attrs = new ArrayList<>();
              }
              attrs.add(attr);
            }
          }
        }

        tb.text = text;
        tb.objectAttributes = attrs;
      }
    });

    COMMON_ACTIONS.setAction(2031,
        new ExtractTextActionBase<PlaceholderSmartFieldArchive>(
            PlaceholderSmartFieldArchive.PARSER) {

          @Override
          protected void onMessage(PlaceholderSmartFieldArchive message, ArchiveInfo ai,
              MessageInfo mi, ExtractTextIWAContext context) throws IOException {
            context.ignorableStyles.add(ai.getIdentifier());
          }
        });

    COMMON_ACTIONS.setAction(3008, new StoreObject<GroupArchive>(GroupArchive.PARSER));
  }

  @Override
  protected MessageActions getMessageTypeActions() {
    return COMMON_ACTIONS;
  }

  @Override
  protected void processRootObject(Message obj) {
  }
}
