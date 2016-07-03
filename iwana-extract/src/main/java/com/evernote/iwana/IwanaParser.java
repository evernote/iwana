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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.evernote.iwana.pb.TSP.TSPArchiveMessages.ArchiveInfo;
import com.evernote.iwana.pb.TSP.TSPArchiveMessages.MessageInfo;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * The base class used to implement a document parser.
 */
public abstract class IwanaParser<T extends IwanaParserCallback> {
  /**
   * Parses the given iWork'13 file and adds the parser results to the given target
   * object.
   * 
   * @param iworkFile The input file.
   * @param target The target.
   * @throws IOException
   */
  public void parse(final File iworkFile, final T target) throws IOException {
    target.onBeginDocument();
    try {
      if (iworkFile.isDirectory()) {
        parseDirectory(iworkFile, target);
      } else {
        try (FileInputStream fin = new FileInputStream(iworkFile)) {
          parseInternal(fin, iworkFile.getName(), target);
        }
      }
    } finally {
      target.onEndDocument();
    }
  }

  /**
   * Parses the given iWork'13 file's contents and adds the parser results to the given
   * target object.
   * 
   * @param dir The input file.
   * @param target The target.
   * @throws IOException
   */
  private void parseDirectory(final File dir, final T target) throws IOException {
    final IwanaContext<T> context = newContext(dir.getName(), target);

    final File indexZip = new File(dir, "Index.zip");
    if (!indexZip.isFile()) {
      throw new FileNotFoundException("Could not find Index.zip: " + indexZip);
    }

    try (FileInputStream in = new FileInputStream(indexZip)) {
      parseIndexZip(in, context);
    }
  }

  /**
   * Parses the given iWork'13 file and adds the parser results to the given target
   * object.
   * 
   * @param zipIn The input stream, a iWork'13 .zip file.
   * @param fileName The fileName for the input stream. Used to perform file format
   *   specific parsing.
   * @param target The target.
   * @throws IOException
   */
  public void parse(final InputStream zipIn, String fileName, final T target)
      throws IOException {
    target.onBeginDocument();
    try {
      parseInternal(zipIn, fileName, target);
    } finally {
      target.onEndDocument();
    }
  }

  private void parseInternal(final InputStream zipIn, final String fileName,
      final T target) throws IOException {
    IwanaContext<T> context = null;

    boolean hasIndexDir = false;

    try (ZipInputStream zis = new ZipInputStream(zipIn)) {
      ZipEntry entry;

      while ((entry = zis.getNextEntry()) != null) {
        String name = entry.getName();

        if (context == null && name.endsWith("/Index.zip") && !entry.isDirectory()) {
          // Index data embedded in single file
          context = newContext(fileName, target);
          parseIndexZip(zis, context);
          break;
        } else if (name.startsWith("Index/") && !entry.isDirectory()) {
          if (context == null) {
            context = newContext(fileName, target);
            context.onBeginParseIndexZip();
            hasIndexDir = true;
          }
          parseIndexZipEntry(zis, entry, context);
        }
      }

      if (context == null) {
        throw new IOException("Could not find Index.zip archive");
      }

      if (hasIndexDir) {
        context.onEndParseIndexZip();
      }
    }
  }

  private void parseIndexZip(final InputStream indexZipIn, final IwanaContext<T> context)
      throws IOException {

    try (ZipInputStream zis = new ZipInputStream(indexZipIn)) {
      ZipEntry entry;

      context.onBeginParseIndexZip();

      boolean foundIWA = false;
      while ((entry = zis.getNextEntry()) != null) {
        foundIWA |= parseIndexZipEntry(zis, entry, context);
      }

      if (!foundIWA) {
        throw new IOException("Index.zip does not contain any .iwa files");
      }
    } finally {
      context.onEndParseIndexZip();
    }
  }

  /**
   * Processes an .iwa file, provided as a zip entry in a {@link ZipInputStream}.
   * 
   * @param zis The input stream.
   * @param entry The zip entry.
   * @param context Our parser context.
   * @return {@code true} if the entry was a valid *.iwa file.
   * @throws IOException
   */
  private boolean parseIndexZipEntry(final ZipInputStream zis, final ZipEntry entry,
      final IwanaContext<T> context) throws IOException {
    if (entry.isDirectory()) {
      return false;
    }
    String name = entry.getName();

    if (name.endsWith(".iwa")) {
      if (context.acceptIWAFile(name)) {
        context.onBeginParseIWAFile(name);
        try {
          context.setCurrentFile(name);
          parseIWA(zis, name, context);
        } finally {
          context.onEndParseIWAFile(name);
        }
      } else {
        context.onSkipFile(name, zis);
      }

      return true;
    } else {
      context.onSkipFile(name, zis);

      return false;
    }
  }

  private void parseIWA(final InputStream in, final String filename,
      final IwanaContext<T> context) throws IOException {
    final MessageActions actions = context.getMessageTypeActions();
    final InputStream bin = new SnappyNoCRCFramedInputStream(in, false);
    final RestrictedSizeInputStream rsIn = new RestrictedSizeInputStream(bin, 0);

    while (!Thread.interrupted()) {
      ArchiveInfo ai;
      ai = ArchiveInfo.parseDelimitedFrom(bin);
      if (ai == null) {
        break;
      }

      for (MessageInfo mi : ai.getMessageInfosList()) {
        rsIn.setNumBytesReadable(mi.getLength());
        try {
          actions.onMessage(rsIn, ai, mi, context);
        } catch (InvalidProtocolBufferException e) {
          handleInvalidProtocolBufferException(ai, mi, e);
        } finally {
          rsIn.skipRest();
        }
      }
    }
  }

  /**
   * Called upon experiencing a {@link InvalidProtocolBufferException} while parsing.
   * 
   * The default operation is to throw the exception. Other parsers may want to skip over
   * the message under certain circumstances and may override this method.
   * 
   * @param ai The {@link ArchiveInfo} that owns the message that caused the exception.
   * @param mi The {@link MessageInfo} that describes the message that caused the
   *          exception.
   * @param e The caught exception.
   */
  protected void handleInvalidProtocolBufferException(ArchiveInfo ai, MessageInfo mi,
      InvalidProtocolBufferException e) throws InvalidProtocolBufferException {
    throw e;
  }

  /**
   * Creates a new parser context.
   * 
   * @param documentName The document name (parsed).
   * @param target The target object.
   * @return The context.
   */
  protected abstract IwanaContext<T> newContext(String documentName, T target);
}
