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
package com.evernote.iwana;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.evernote.iwana.pb.TSP.TSPArchiveMessages.ArchiveInfo;
import com.evernote.iwana.pb.TSP.TSPArchiveMessages.MessageInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * A registry for {@link MessageAction}s.
 * 
 * A {@link MessageAction} can be called for one or more message types. A type is an
 * application-specific integer value that is defined in an Objective-C
 * {@code TSPRegistry} instance.
 */
public class MessageActions {
  private final Map<Integer, MessageAction<? extends Message, ? extends IwanaContext<?>>> actions =
      new HashMap<>();

  /**
   * Creates a new {@link MessageAction} registry.
   */
  public MessageActions() {
  }

  /**
   * Creates a new {@link MessageAction} registry, copying actions from another registry
   * as a starting point.
   */
  public MessageActions(MessageActions other) {
    actions.putAll(other.actions);
  }

  /**
   * Registers a particular {@link MessageAction} for a given type. Any existing action
   * will be replaced.
   * 
   * @param type The type.
   * @param action The {@link MessageAction}.
   */
  public void setAction(final int type,
      final MessageAction<? extends Message, ? extends IwanaContext<?>> action) {
    actions.put(type, action);
  }

  /**
   * Registers a particular {@link MessageAction} for the given types. Any existing action
   * will be replaced.
   * 
   * @param types The types.
   * @param ma The {@link MessageAction}.
   */
  public void setAction(final int[] types,
      final MessageAction<? extends Message, ? extends IwanaContext<?>> ma) {
    for (int type : types) {
      actions.put(type, ma);
    }
  }

  /**
   * Called by the {@link IwanaParser} for a given {@link MessageInfo}.
   * 
   * If no action is associated with the message type, the message is silently skipped.
   * 
   * @param in The {@link InputStream} containing the payload of the message object.
   * @param ai The {@link ArchiveInfo} that owns this message.
   * @param mi The {@link MessageInfo} that describes this message.
   * @param context The {@link IwanaContext} that holds the parser state for this
   *          document.
   * @throws InvalidProtocolBufferException
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  void onMessage(final InputStream in, final ArchiveInfo ai, final MessageInfo mi,
      IwanaContext<?> context) throws InvalidProtocolBufferException, IOException {
    final MessageAction<? extends Message, ? extends IwanaContext<?>> action =
        actions.get(mi.getType());
    if (action != null) {
      ((MessageAction<Message, IwanaContext<?>>) action).onMessage(in, ai, mi, context);
    }
  }
}
