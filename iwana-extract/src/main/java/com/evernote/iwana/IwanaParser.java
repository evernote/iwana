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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
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
	
  /*
   * Lists for valid actions for keynote, pages and numbers iWork files.
   * Taken from the python iWork parser at https://github.com/ChloeTigre/pyiwa
   */
  private final HashSet<Integer> keyActions = new HashSet<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 123, 124, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 10011));
  private final HashSet<Integer> pageActions = new HashSet<Integer>(Arrays.asList(7, 10000, 10001, 10010, 10011, 10012, 10015, 10101, 10102, 10108, 10109, 10110, 10111, 10112, 10113, 10114, 10115, 10116, 10117, 10118, 10119, 10120, 10121, 10125, 10126, 10127, 10128, 10130, 10131, 10132, 10133, 10134, 10140, 10141, 10142, 10143, 10147, 10148, 10149, 10150, 10151, 10152, 10153, 10154, 10155, 10156, 10157));
  private final HashSet<Integer> numActions = new HashSet<Integer>(Arrays.asList(1, 2, 3, 7, 10011, 12002, 12003, 12004, 12005, 12006, 12008, 12009, 12010, 12011, 12012, 12013, 12014, 12015, 12016, 12017, 12018, 12019, 12021, 12024, 12025, 12026, 12027, 12028, 12030));
  private String type = "";
  private String path = "";

	
  public void parse(final File iworkFile, final T target, String paths) throws IOException {
    target.onBeginDocument();
    this.path = paths;
    try {
      if (iworkFile.isDirectory()) {
        this.parseDirectory(iworkFile, target);
      } else {
        try (FileInputStream fin = new FileInputStream(iworkFile)) {
          this.parseInternal(fin, target);
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
      this.parseIndexZip(in, context, target, in);
    }
  }

  /**
   * Parses the given iWork'13 file and adds the parser results to the given target
   * object.
   * 
   * @param zipIn The input stream, a iWork'13 .zip file.
   * @param target The target.
   * @throws IOException
   */
  public void parse(final InputStream zipIn, final T target) throws IOException {
    target.onBeginDocument();
    try {
      this.parseInternal(zipIn, target);
    } finally {
      target.onEndDocument();
    }
  }

  private void parseInternal(final InputStream zipIn, final T target) throws IOException {
    IwanaContext<T> context = null;

    boolean hasIndexDir = false;

    try (ZipInputStream zis = new ZipInputStream(zipIn)) {
      ZipEntry entry;

      while ((entry = zis.getNextEntry()) != null) {
        String name = entry.getName();

        if (context == null && name.endsWith("/Index.zip") && !entry.isDirectory()) {
          int iSlash = name.indexOf('/');
          int iIndex = name.indexOf("/Index.zip");

          if (iSlash == iIndex) {
              context = this.newContext(name.substring(0, iSlash), target);

            this.parseIndexZip(zis, context, target, zipIn);
            break;
          }
        } else if (name.startsWith("Index/") && !entry.isDirectory()) {
          // Index data embedded in single file

          if (context == null) {
            context = this.newContext("yoo." + this.type, target);
            context.onBeginParseIndexZip();
            hasIndexDir = true;
          }

          this.parseIndexZipEntry(zis, entry, context, target, zipIn);
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

  private void parseIndexZip(InputStream indexZipIn, IwanaContext<T> context, T target, InputStream zipIn) throws IOException {


    try (ZipInputStream zis = new ZipInputStream(indexZipIn)) {
      ZipEntry entry;

      context.onBeginParseIndexZip();

      boolean foundIWA = false;
      while ((entry = zis.getNextEntry()) != null) {
        foundIWA |= this.parseIndexZipEntry(zis, entry, context, target, zipIn);
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
  private boolean parseIndexZipEntry(ZipInputStream zis, ZipEntry entry, IwanaContext<T> context, T target, InputStream zipIn) throws IOException {
    if (entry.isDirectory()) {
      return false;
    }
    String name = entry.getName();

    if (name.endsWith(".iwa")) {
      if (context.acceptIWAFile(name)) {
        context.onBeginParseIWAFile(name);
        try {
          context.setCurrentFile(name);
          if (this.type == "") {
              this.type = this.getType(zis, name, context, target, zipIn);
              if (this.type != "") {
                  this.parse(new File(this.path), target, this.path);
              }
          }else 
              this.parseIWA(zis, name, context);
         
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
  
  //Computes the type of the file and starts from the top, building the context properly
  private String getType(InputStream in, String filename, IwanaContext<T> context, T target, InputStream zipIn) throws IOException {
      InputStream bin = new SnappyNoCRCFramedInputStream(in, false);
      RestrictedSizeInputStream rsIn = new RestrictedSizeInputStream(bin, 0L);

      while(!Thread.interrupted()) {
          ArchiveInfo ai = ArchiveInfo.parseDelimitedFrom(bin);
          if (ai == null) {
              break;
          }

          Iterator messages = ai.getMessageInfosList().iterator();

          while(messages.hasNext()) {
              MessageInfo mi = (MessageInfo)messages.next();
              rsIn.setNumBytesReadable((long)mi.getLength());
              if (this.keyActions.contains(mi.getType()) && !this.numActions.contains(mi.getType()) && !this.pageActions.contains(mi.getType())) {
                  return "key";
              }

              if (this.numActions.contains(mi.getType()) && !this.keyActions.contains(mi.getType()) && !this.pageActions.contains(mi.getType())) {
                  return "numbers";
              }

              if (this.pageActions.contains(mi.getType()) && !this.keyActions.contains(mi.getType()) && !this.numActions.contains(mi.getType())) {
                  return "pages";
              }
          }
          rsIn.skipRest();
      }
      return "";
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
