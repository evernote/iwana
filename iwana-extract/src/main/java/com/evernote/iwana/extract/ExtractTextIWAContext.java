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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.evernote.iwana.IwanaContext;
import com.evernote.iwana.pb.TSD.TSDArchives.GroupArchive;
import com.evernote.iwana.pb.TSP.TSPMessages.Reference;
import com.evernote.iwana.pb.TSWP.TSWPArchives.ObjectAttributeTable.ObjectAttribute;
import com.evernote.iwana.pb.TSWP.TSWPArchives.ShapeInfoArchive;
import com.google.protobuf.Message;

/**
 * Holds information about the status of our text extractor, working on a particular
 * document.
 */
public abstract class ExtractTextIWAContext extends IwanaContext<ExtractTextCallback> {
  protected ExtractTextIWAContext(String documentFilename, ExtractTextCallback target) {
    super(documentFilename, target);
  }

  final Map<Long, TextBlock> objectIdToText = new HashMap<>();
  final Set<Long> ignorableStyles = new HashSet<>();
  final Map<Long, Message> objectStorage = new HashMap<>();

  protected <T extends Message> T getObject(final Reference ref, final Class<T> objectType) {
    return getObject(ref.getIdentifier(), objectType);
  }

  protected <T extends Message> T getObject(final long id, final Class<T> objectType) {
    Message m = objectStorage.get(id);
    if (m == null) {
      // LOG.info("Object " + id + " does not exist / has not been parsed");
      return null;
    }
    if (!objectType.isAssignableFrom(m.getClass())) {

      // poor-man's type inference
      m = tryCast(m, objectType);
      if (m != null) {
        return objectType.cast(m);
      }

      // LOG.info("Object " + id + " cannot be cast to " + objectType);
      return null;
    }
    return objectType.cast(m);
  }

  /**
   * Called whenever we cannot directly cast a message to another type.
   * 
   * This may happen for {@link Message} that are subclasses of another {@link Message}.
   * In this case, we can try getting the proper instance using the message's
   * {@code getSuper()} method.
   * 
   * @param message The message that should be cast.
   * @param objectType The type we want to cast to.
   * @param <T> a subclass of a {@link Message}
   * @return The casted instance, or {@code null} if we are unable to cast.
   */
  protected <T extends Message> T tryCast(final Message message, final Class<T> objectType) {
    return null;
  }

  /**
   * Resolves a list of {@link Reference}s to a list of {@link Message} instances.
   * 
   * Messages that could not be resolved are skipped.
   * 
   * @param refs The list of {@link Reference}s.
   * @param objectType The list of messages that we could resolve, or an empty list.
   * @param <T> a subclass of a {@link Message}
   * @return The resolved list.
   */
  protected <T extends Message> List<T> resolve(final List<Reference> refs,
      final Class<T> objectType) {
    List<T> objects = new ArrayList<>();
    if (refs == null) {
      return objects;
    }
    for (Reference ref : refs) {
      long id = ref.getIdentifier();
      T obj = getObject(id, objectType);
      if (obj != null) {
        objects.add(obj);
      }

      if (objectType == ShapeInfoArchive.class) {
        // check other types to cast
        GroupArchive arc = getObject(id, GroupArchive.class);
        if (arc != null) {
          objects.addAll(resolve(arc.getChildrenList(), objectType));
        }
      }
    }
    return objects;
  }

  TextBlock getTextBlock(final long objectId) {
    TextBlock tb = objectIdToText.get(objectId);
    if (tb == null) {
      tb = new TextBlock();
      objectIdToText.put(objectId, tb);
    }

    return tb;
  }

  @Override
  public void onEndParseIndexZip() {
    // Remove placeholder text
    removePlaceholderText();

    // Order content
    Message obj = objectStorage.get(1L);
    if (obj != null) {
      processRootObject(obj);
    }

    // Dump unreferenced text
    dumpUnreferencedTextBlocks();
  }

  protected void dumpUnreferencedTextBlocks() {
    // Dump the rest
    System.out.println("********");
    for (Map.Entry<Long, TextBlock> en : objectIdToText.entrySet()) {
      TextBlock tb = en.getValue();
      if (tb.done) {
        continue;
      }
      System.out.println(en.getKey() + ":");

      target.onTextBlock(tb.flushText(), TextAttributes.DEFAULT_UNREFERENCED);
    }
  }

  /**
   * Processes the document root message (message with identifier = 1).
   * 
   * Called after reading the entire index.zip file.
   * 
   * @param obj The root message.
   */
  protected abstract void processRootObject(Message obj);

  /**
   * Removes placeholder text from the retrieved text blocks.
   */
  protected void removePlaceholderText() {
    for (Map.Entry<Long, TextBlock> en : objectIdToText.entrySet()) {
      TextBlock tb = en.getValue();
      if (tb.objectAttributes != null) {

        int placeholderStart = -1;
        for (ObjectAttribute oa : IwanaUtil.sortObjectAttributes(tb.objectAttributes)) {
          fixPlaceholder(tb, placeholderStart, oa.getCharacterIndex());

          if (oa.hasObject()) {
            if (ignorableStyles.contains(oa.getObject().getIdentifier())) {
              placeholderStart = oa.getCharacterIndex();
            }
          } else {
            placeholderStart = -1;
          }
        }

        fixPlaceholder(tb, placeholderStart, tb.text.length());
      }
    }
  }

  protected void addContainedStorageTextBlock(Reference containedStorageRef,
      TextAttributes attrs) {
    if (containedStorageRef == null) {
      return;
    }
    long storageArchiveID = containedStorageRef.getIdentifier();
    TextBlock textBlock = objectIdToText.get(storageArchiveID);
    if (textBlock == null || textBlock.done) {
      return;
    }
    textBlock.done = true;

    target.onTextBlock(textBlock.flushText(), attrs);
  }

  private void fixPlaceholder(TextBlock tb, final int placeholderStart,
      final int placeholderEnd) {
    if (placeholderStart == -1) {
      return;
    }
    if (!(tb.text instanceof StringBuilder)) {
      tb.text = new StringBuilder(tb.text);
    }
    StringBuilder sb = (StringBuilder) tb.text;

    for (int c = placeholderStart; c < placeholderEnd; c++) {
      sb.setCharAt(c, '_');
    }
  }

}
