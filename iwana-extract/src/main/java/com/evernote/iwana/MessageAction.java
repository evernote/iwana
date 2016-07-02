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

import com.evernote.iwana.pb.TSP.TSPArchiveMessages.ArchiveInfo;
import com.evernote.iwana.pb.TSP.TSPArchiveMessages.MessageInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

/**
 * Defines a message handler that performs an action whenever a particular message type is
 * encountered.
 * 
 * @see MessageActions The registry that maps message types to {@link MessageAction}s.
 */
public abstract class MessageAction<T extends Message, C extends IwanaContext<?>> {
  private final Parser<T> parser;

  /**
   * Constructs a new {@link MessageAction} that parses protobuf messages using the given
   * {@link Parser}.
   * 
   * @param parser The parser to use.
   */
  protected MessageAction(final Parser<T> parser) {
    this.parser = parser;
  }

  /**
   * Called by the {@link IwanaParser} when encountering a particular message that matches
   * this {@link MessageAction} (as defined by {@link MessageAction}, for example).
   * 
   * The default implementation calls this instance's parser to read the message from the
   * {@link InputStream} and to convert it into a protobuf Message, then calls
   * {@link #onMessage(Message, ArchiveInfo, MessageInfo, IwanaContext)}.
   * 
   * @param in The {@link InputStream} containing the payload of the message object.
   * @param ai The {@link ArchiveInfo} that owns this message.
   * @param mi The {@link MessageInfo} that describes this message.
   * @param context The {@link IwanaContext} that holds the parser state for this
   *          document.
   * @throws InvalidProtocolBufferException
   * @throws IOException
   */
  void onMessage(InputStream in, final ArchiveInfo ai, final MessageInfo mi,
      final C context) throws InvalidProtocolBufferException, IOException {
    T message = parser.parseFrom(in);
    onMessage(message, ai, mi, context);
  }

  /**
   * Called upon encountering a particular message that matches this {@link MessageAction}
   * (as defined by {@link MessageAction}, for example).
   * 
   * @param message The {@link Message}, parsed from the input using the parser set for
   *          this instance.
   * @param ai The {@link ArchiveInfo} that owns this message.
   * @param mi The {@link MessageInfo} that describes this message.
   * @param context The {@link IwanaContext} that holds the parser state for this
   *          document.
   * @throws IOException
   */
  protected abstract void onMessage(T message, final ArchiveInfo ai,
      final MessageInfo mi, final C context) throws IOException;
}
