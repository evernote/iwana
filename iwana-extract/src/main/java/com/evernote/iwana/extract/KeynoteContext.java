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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.evernote.iwana.MessageActions;
import com.evernote.iwana.pb.KN.KNArchives.DocumentArchive;
import com.evernote.iwana.pb.KN.KNArchives.NoteArchive;
import com.evernote.iwana.pb.KN.KNArchives.PlaceholderArchive;
import com.evernote.iwana.pb.KN.KNArchives.ShowArchive;
import com.evernote.iwana.pb.KN.KNArchives.SlideArchive;
import com.evernote.iwana.pb.KN.KNArchives.SlideNodeArchive;
import com.evernote.iwana.pb.TSD.TSDArchives.DrawableArchive;
import com.evernote.iwana.pb.TSD.TSDArchives.ShapeArchive;
import com.evernote.iwana.pb.TSP.TSPMessages.Reference;
import com.evernote.iwana.pb.TSWP.TSWPArchives.ShapeInfoArchive;
import com.evernote.iwana.pb.TSP.TSPArchiveMessages.ArchiveInfo;
import com.evernote.iwana.pb.TSP.TSPArchiveMessages.MessageInfo;
import com.google.protobuf.Message;

/**
 * A Keynote-specific extractor context.
 */
class KeynoteContext extends ContextBase {
  private static final Logger LOG = Logger.getLogger(KeynoteContext.class);

  private static final MessageActions KEYNOTE_ACTIONS = new MessageActions(
      ContextBase.COMMON_ACTIONS);
  static {
	KEYNOTE_ACTIONS.setAction(1, new ExtractTextActionBase<DocumentArchive>(DocumentArchive.PARSER) {
			@Override
			protected void onMessage(DocumentArchive message, ArchiveInfo ai, MessageInfo mi, ExtractTextIWAContext context)
					throws IOException {
				((ExtractTextCallback)context.getTarget()).onMetaBlock("PreventImageConversionOnOpenning", String.valueOf(message.getSuper().getSuper().getPreventImageConversionOnOpen()));
	            ((ExtractTextCallback)context.getTarget()).onMetaBlock("getNeedsMovieCompatibilityUpgrade", String.valueOf(message.getSuper().getNeedsMovieCompatibilityUpgrade()));

			}
	    });
    KEYNOTE_ACTIONS.setAction(2, new StoreObject<ShowArchive>(ShowArchive.PARSER));
    KEYNOTE_ACTIONS.setAction(4, new StoreObject<SlideNodeArchive>(
        SlideNodeArchive.PARSER));
    KEYNOTE_ACTIONS.setAction(5, new StoreObject<SlideArchive>(SlideArchive.PARSER));
    KEYNOTE_ACTIONS.setAction(6, new StoreObject<SlideArchive>(SlideArchive.PARSER));
    KEYNOTE_ACTIONS.setAction(7, new StoreObject<PlaceholderArchive>(
        PlaceholderArchive.PARSER));
    KEYNOTE_ACTIONS.setAction(15, new StoreObject<NoteArchive>(NoteArchive.PARSER));
    KEYNOTE_ACTIONS.setAction(2011, new StoreObject<ShapeInfoArchive>(
        ShapeInfoArchive.PARSER));
  }

  protected KeynoteContext(String documentFilename, ExtractTextCallback target) {
    super(documentFilename, target);
  }

  @Override
  public boolean acceptIWAFile(String name) {
    if (name.contains("/MasterSlide")) {
      // skip master slides (short cut)
      return false;
    }
    return super.acceptIWAFile(name);
  }

  @Override
  protected MessageActions getMessageTypeActions() {
    return KEYNOTE_ACTIONS;
  }

  @Override
  protected <T extends Message> T tryCast(final Message message, final Class<T> objectType) {
    if (message instanceof PlaceholderArchive) {
      ShapeInfoArchive m1 = ((PlaceholderArchive) message).getSuper();
      if (objectType.isAssignableFrom(m1.getClass())) {
        return objectType.cast(m1);
      }
      return tryCast(m1, objectType);
    } else if (message instanceof ShapeInfoArchive) {
      ShapeArchive m1 = ((ShapeInfoArchive) message).getSuper();
      if (objectType.isAssignableFrom(m1.getClass())) {
        return objectType.cast(m1);
      }
      return tryCast(m1, objectType);
    } else if (message instanceof ShapeArchive) {
      DrawableArchive m1 = ((ShapeArchive) message).getSuper();
      if (objectType.isAssignableFrom(m1.getClass())) {
        return objectType.cast(m1);
      }
      return tryCast(m1, objectType);
    }

    return super.tryCast(message, objectType);
  }

  @Override
  protected void processRootObject(Message obj) {
    if (!(obj instanceof DocumentArchive)) {
      LOG.info("Unsupported root object message: " + obj.getClass());
      return;
    }

    DocumentArchive root = (DocumentArchive) obj;
    ShowArchive showArchive = getObject(root.getShow(), ShowArchive.class);

    final long rootSlideNoteId =
        showArchive.getSlideTree().getRootSlideNode().getIdentifier();
    SlideNodeArchive slideNodeId = getObject(rootSlideNoteId, SlideNodeArchive.class);
    LinkedHashMap<Long, SlideNodeArchive> nodes = new LinkedHashMap<>();
    nodes.put(rootSlideNoteId, slideNodeId);

    processSlideNodes(nodes);
  }

  /**
   * @param nodes
   */
  private void processSlideNodes(LinkedHashMap<Long, SlideNodeArchive> nodes) {
    LinkedHashMap<Long, SlideNodeArchive> children = new LinkedHashMap<>();

    Set<Long> seenIds = new HashSet<Long>();

    while (!nodes.isEmpty()) {
      for (Map.Entry<Long, SlideNodeArchive> en : nodes.entrySet()) {
        if (!seenIds.add(en.getKey())) {
          LOG.info("Circular reference detected: id=" + en.getKey());
          continue;
        }

        SlideNodeArchive sna = en.getValue();
        if (sna == null) {
          continue;
        }
        for (Reference ref : sna.getChildrenList()) {
          final long childId = ref.getIdentifier();
          SlideNodeArchive child = getObject(childId, SlideNodeArchive.class);
          if (child != null) {
            children.put(childId, child);
          }
        }

        // FIXME we could skip hidden slides using sna.getIsHidden(); / collapsed

        SlideArchive slide = getObject(sna.getSlide(), SlideArchive.class);
        if (slide != null) {
          // process note objects

          List<GeometryObject> geoms = new ArrayList<>();

          {
            ShapeInfoArchive sia =
                getObject(slide.getTitlePlaceholder(), ShapeInfoArchive.class);
            if (sia != null && sia.hasContainedStorage()) {
              geoms.add(new GeometryObject(sia));
            }
          }
          {
            ShapeInfoArchive sia =
                getObject(slide.getBodyPlaceholder(), ShapeInfoArchive.class);
            if (sia != null && sia.hasContainedStorage()) {
              // that's mostly crap content; defer to the end
              // geoms.add(new GeometryObject(sia));
            }
          }
          {
            ShapeInfoArchive sia =
                getObject(slide.getObjectPlaceholder(), ShapeInfoArchive.class);
            if (sia != null && sia.hasContainedStorage()) {
              geoms.add(new GeometryObject(sia));
            }
          }

          for (ShapeInfoArchive sia : resolve(slide.getOwnedDrawablesList(),
              ShapeInfoArchive.class)) {
            if (sia.hasContainedStorage()) {
              geoms.add(new GeometryObject(sia));
            }
          }

          // FIXME this currently assumes top-to-bottom, left-to-right document
          // orientation
          Collections.sort(geoms);

          for (GeometryObject go : geoms) {
            ShapeInfoArchive sia = (ShapeInfoArchive) go.message;

            addContainedStorageTextBlock(sia.getContainedStorage(),
                TextAttributes.DEFAULT_DOCUMENT);
          }

          // process slide notes
          NoteArchive arc = getObject(slide.getNote(), NoteArchive.class);
          if (arc != null) {
            addContainedStorageTextBlock(arc.getContainedStorage(),
                TextAttributes.DEFAULT_NOTES);
          }
        }

      }

      nodes.clear();
      LinkedHashMap<Long, SlideNodeArchive> other = nodes;
      nodes = children;
      children = other;
    }
  }

  @Override
  protected void dumpUnreferencedTextBlocks() {
    // don't dump unreferenced text blocks
  }
}
