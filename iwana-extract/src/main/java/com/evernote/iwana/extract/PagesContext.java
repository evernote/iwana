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
import com.evernote.iwana.MessageActions;
import com.evernote.iwana.pb.TP.TPArchives.DocumentArchive;
import com.evernote.iwana.pb.TSP.TSPArchiveMessages.ArchiveInfo;
import com.evernote.iwana.pb.TSP.TSPArchiveMessages.MessageInfo;

/**
 * A Pages-specific extractor context.
 */
class PagesContext extends ContextBase {
  
  private static final MessageActions PAGES_ACTIONS;
  static {
      PAGES_ACTIONS = new MessageActions(ContextBase.COMMON_ACTIONS);
      PAGES_ACTIONS.setAction(10000, new ExtractTextActionBase<DocumentArchive>(DocumentArchive.PARSER) {
    	  @Override
          protected void onMessage(DocumentArchive message, ArchiveInfo ai, MessageInfo mi, ExtractTextIWAContext context) throws IOException {
              ((ExtractTextCallback)context.getTarget()).onMetaBlock("FooterMargin", String.valueOf(message.getFooterMargin()));
              ((ExtractTextCallback)context.getTarget()).onMetaBlock("HeaderMargin", String.valueOf(message.getHeaderMargin()));
              ((ExtractTextCallback)context.getTarget()).onMetaBlock("PageHeight", String.valueOf(message.getPageHeight()));
              ((ExtractTextCallback)context.getTarget()).onMetaBlock("PageWidth", String.valueOf(message.getPageWidth()));
              ((ExtractTextCallback)context.getTarget()).onMetaBlock("PageScale", String.valueOf(message.getPageScale()));
              ((ExtractTextCallback)context.getTarget()).onMetaBlock("RightMargin", String.valueOf(message.getRightMargin()));
              ((ExtractTextCallback)context.getTarget()).onMetaBlock("TopMargin", String.valueOf(message.getTopMargin()));
              ((ExtractTextCallback)context.getTarget()).onMetaBlock("BottomMargin", String.valueOf(message.getBottomMargin()));
              ((ExtractTextCallback)context.getTarget()).onMetaBlock("CitationRecordsCount", String.valueOf(message.getCitationRecordsCount()));
              ((ExtractTextCallback)context.getTarget()).onMetaBlock("Orientation", String.valueOf(message.getOrientation()));
              ((ExtractTextCallback)context.getTarget()).onMetaBlock("PreventImageConversionOnOpenning", String.valueOf(message.getSuper().getSuper().getPreventImageConversionOnOpen()));
              ((ExtractTextCallback)context.getTarget()).onMetaBlock("getNeedsMovieCompatibilityUpgrade", String.valueOf(message.getSuper().getNeedsMovieCompatibilityUpgrade()));
          }
      });
  }

  protected PagesContext(String documentFilename, ExtractTextCallback target) {
    super(documentFilename, target);
  }

  @Override
  protected MessageActions getMessageTypeActions() {
    return PAGES_ACTIONS;
  }
}
