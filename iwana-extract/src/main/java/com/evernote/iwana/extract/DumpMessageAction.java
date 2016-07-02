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

import org.apache.log4j.Logger;

import com.evernote.iwana.IwanaContext;
import com.evernote.iwana.MessageAction;
import com.evernote.iwana.pb.TSP.TSPArchiveMessages.ArchiveInfo;
import com.evernote.iwana.pb.TSP.TSPArchiveMessages.MessageInfo;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

/**
 * Only used for quick debugging during development.
 */
class DumpMessageAction<T extends Message> extends MessageAction<T, IwanaContext<?>> {
  private static final Logger LOG = Logger.getLogger(DumpMessageAction.class);

  protected DumpMessageAction(Parser<T> parser) {
    super(parser);
  }

  @Override
  protected void onMessage(T message, final ArchiveInfo ai, final MessageInfo mi,
      final IwanaContext<?> context) throws IOException {
    LOG.info(message);
  }
}
