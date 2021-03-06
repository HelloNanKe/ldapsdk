/*
 * Copyright 2018-2019 Ping Identity Corporation
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2018-2019 Ping Identity Corporation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package com.unboundid.util;



import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;



/**
 * This class provides a background thread that will be used to read data from
 * a file and make it available for consumption by a
 * {@link StreamFileValuePatternComponent} instance.  This thread will
 * automatically close the associated file and exit after a period of
 * inactivity.
 */
final class StreamFileValuePatternReaderThread
      extends Thread
{
  // A value that tracks the position at which the next line of data should be
  // read from the file.
  private final AtomicLong nextReadPosition;

  // A reference that holds this thread and makes it available to the associated
  // StreamFileValuePatternComponent.
  private final AtomicReference<StreamFileValuePatternReaderThread> threadRef;

  // The queue that will be used to hold the lines of data read from the file.
  private final LinkedBlockingQueue<String> lineQueue;

  // The maximum length of time in milliseconds that an attempt to offer a
  // string to the queue will be allowed to block before the associated reader
  // thread will exit.
  private final long maxOfferBlockTimeMillis;

  // The random-access file from which the data will be read.
  private final RandomAccessFile randomAccessFile;



  /**
   * Creates a new reader thread instance that will read data from the specified
   * file.
   *
   * @param  file                     The file from which the data is to be
   *                                  read.  It must not be {@code null}, and it
   *                                  must reference a file that exists.
   * @param  lineQueue                The queue that will be used to hold the
   *                                  lines of data read from the file.  It must
   *                                  not be {@code null}.
   * @param  maxOfferBlockTimeMillis  The maximum length of time in milliseconds
   *                                  that an attempt to offer a string into the
   *                                  queue will be allowed to block before the
   *                                  associated reader thread will exit.  It
   *                                  must be greater than zero.
   * @param  nextReadPosition         A value that tracks the position at which
   *                                  the next line of data should be read from
   *                                  the file.  It must not be {@code null}.
   * @param  threadRef                An object that will be used to hold a
   *                                  reference to this thread from within the
   *                                  associated
   *                                  {@link StreamFileValuePatternComponent}.
   *                                  This thread will clear out the reference
   *                                  when it exits as a signal that a new
   *                                  thread may need to be created.
   *
   * @throws  IOException  If a problem is encountered while attempting to open
   *                       the specified file for reading.
   */
  StreamFileValuePatternReaderThread(final File file,
       final LinkedBlockingQueue<String> lineQueue,
       final long maxOfferBlockTimeMillis,
       final AtomicLong nextReadPosition,
       final AtomicReference<StreamFileValuePatternReaderThread> threadRef)
       throws IOException
  {
    setName("StreamFileValuePatternReaderThread for file '" +
         file.getAbsolutePath() + '\'');
    setDaemon(true);

    this.lineQueue = lineQueue;
    this.maxOfferBlockTimeMillis = maxOfferBlockTimeMillis;
    this.nextReadPosition = nextReadPosition;
    this.threadRef = threadRef;

    randomAccessFile = new RandomAccessFile(file, "r");
    randomAccessFile.seek(nextReadPosition.get());
  }



  /**
   * Operates in a loop, reading data from the specified file and offering it to
   * the provided queue.  If the offer attempt blocks for longer than the
   * configured maximum offer block time, the file will be closed and the thread
   * will exit.
   */
  @Override()
  public void run()
  {
    try
    {
      while (true)
      {
        // Read the next line of data from the file.  If we get an error, or if
        // we hit the end of the file, then we'll reset the next read position
        // to zero and the thread will exit.
        final String line;
        try
        {
          line = randomAccessFile.readLine();
          if (line == null)
          {
            nextReadPosition.set(0L);
            return;
          }
        }
        catch (final Exception e)
        {
          Debug.debugException(e);
          nextReadPosition.set(0L);
          return;
        }


        // Offer the line that we read to the queue.  If it succeeds, then
        // update the next read position to reflect our current position in the
        // file.  If it times out, or if we encounter an error, then exit this
        // thread without updating the position, which will cause the next read
        // attempt from the next thread instance to pick up where this one left
        // off.
        try
        {
          if (lineQueue.offer(line, maxOfferBlockTimeMillis,
               TimeUnit.MILLISECONDS))
          {
            nextReadPosition.set(randomAccessFile.getFilePointer());
          }
          else
          {
            return;
          }
        }
        catch (final Exception e)
        {
          Debug.debugException(e);
          return;
        }
      }
    }
    finally
    {
      // Clear the reference to this thread from the associated value pattern
      // component.
      threadRef.set(null);


      // Close the file.
      try
      {
        randomAccessFile.close();
      }
      catch (final Exception e)
      {
        Debug.debugException(e);
      }
    }
  }
}
