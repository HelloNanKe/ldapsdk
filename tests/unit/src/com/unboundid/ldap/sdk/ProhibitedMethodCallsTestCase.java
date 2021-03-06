/*
 * Copyright 2019 Ping Identity Corporation
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2019 Ping Identity Corporation
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
package com.unboundid.ldap.sdk;



import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.unboundid.util.StaticUtils;



/**
 * This class ensures that the LDAP SDK code does not call any prohibited
 * methods that may cause problems under certain cases.
 */
public final class ProhibitedMethodCallsTestCase
       extends LDAPSDKTestCase
{
  /**
   * Examines all source code files to ensure that there are no inappropriate
   * uses of the {@code System.getProperties} method.
   *
   * @param  f  The source file to examine.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test(dataProvider = "sourceCodeFiles")
  public void testSystemGetProperties(final File f)
         throws Exception
  {
    final Map<String,Set<Integer>> allowedExceptions = StaticUtils.mapOf(
         "StaticUtils.java", StaticUtils.setOf(3990, 3992, 4005, 4014, 4030));

    final Map<Integer,String> unwrappedLines =
         unwrapSourceLines(readFileLines(f));
    for (final Map.Entry<Integer,String> e : unwrappedLines.entrySet())
    {
      final int lineNumber = e.getKey();
      final String line = e.getValue();
      if (line.contains("System.getProperties"))
      {
        final Set<Integer> allowedLineNumbers =
             allowedExceptions.get(f.getName());
        if ((allowedLineNumbers != null) && allowedLineNumbers.contains(
             lineNumber))
        {
          // This is an explicitly allowed use of the method.  Don't fail
          // because of it.
          continue;
        }

        fail("Source code file " + f.getAbsolutePath() +
             " contains a forbidden use of System.getProperties, which may " +
             "fail under certain security managers.  You should replace the " +
             "call with StaticUtils.getSystemProperties.  The offense is on " +
             "the following line (at or near line " + lineNumber + "):" +
             StaticUtils.EOL + StaticUtils.EOL + line);
      }
    }
  }



  /**
   * Examines all source code files to ensure that there are no inappropriate
   * uses of the {@code System.getProperty} method.
   *
   * @param  f  The source file to examine.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test(dataProvider = "sourceCodeFiles")
  public void testSystemGetProperty(final File f)
         throws Exception
  {
    final Map<String,Set<Integer>> allowedExceptions = StaticUtils.mapOf(
         "StaticUtils.java", StaticUtils.setOf(4045, 4072, 4076, 4105, 4109));

    final Map<Integer,String> unwrappedLines =
         unwrapSourceLines(readFileLines(f));
    for (final Map.Entry<Integer,String> e : unwrappedLines.entrySet())
    {
      final int lineNumber = e.getKey();
      final String line = e.getValue();
      if (line.contains("System.getProperty"))
      {
        final Set<Integer> allowedLineNumbers =
             allowedExceptions.get(f.getName());
        if ((allowedLineNumbers != null) && allowedLineNumbers.contains(
             lineNumber))
        {
          // This is an explicitly allowed use of the method.  Don't fail
          // because of it.
          continue;
        }

        fail("Source code file " + f.getAbsolutePath() +
             " contains a forbidden use of System.getProperty, which may " +
             "fail under certain security managers.  You should replace the " +
             "call with StaticUtils.getSystemProperty.  The offense is on " +
             "the following line (at or near line " + lineNumber + "):" +
             StaticUtils.EOL + StaticUtils.EOL + line);
      }
    }
  }



  /**
   * Examines all source code files to ensure that there are no inappropriate
   * uses of the {@code System.setProperty} method.
   *
   * @param  f  The source file to examine.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test(dataProvider = "sourceCodeFiles")
  public void testSystemSetProperty(final File f)
         throws Exception
  {
    final Map<String,Set<Integer>> allowedExceptions = StaticUtils.mapOf(
         "StaticUtils.java", StaticUtils.setOf(4143, 4148));

    final Map<Integer,String> unwrappedLines =
         unwrapSourceLines(readFileLines(f));
    for (final Map.Entry<Integer,String> e : unwrappedLines.entrySet())
    {
      final int lineNumber = e.getKey();
      final String line = e.getValue();
      if (line.contains("System.setProperty"))
      {
        final Set<Integer> allowedLineNumbers =
             allowedExceptions.get(f.getName());
        if ((allowedLineNumbers != null) && allowedLineNumbers.contains(
             lineNumber))
        {
          // This is an explicitly allowed use of the method.  Don't fail
          // because of it.
          continue;
        }

        fail("Source code file " + f.getAbsolutePath() +
             " contains a forbidden use of System.setProperty, which may " +
             "fail under certain security managers.  You should replace the " +
             "call with StaticUtils.setSystemProperty.  The offense is on " +
             "the following line (at or near line " + lineNumber + "):" +
             StaticUtils.EOL + StaticUtils.EOL + line);
      }
    }
  }



  /**
   * Examines all source code files to ensure that there are no inappropriate
   * uses of the {@code System.clearProperty} method.
   *
   * @param  f  The source file to examine.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test(dataProvider = "sourceCodeFiles")
  public void testSystemClearProperty(final File f)
         throws Exception
  {
    final Map<String,Set<Integer>> allowedExceptions = StaticUtils.mapOf(
         "StaticUtils.java", StaticUtils.setOf(4139, 4149, 4175, 4179));

    final Map<Integer,String> unwrappedLines =
         unwrapSourceLines(readFileLines(f));
    for (final Map.Entry<Integer,String> e : unwrappedLines.entrySet())
    {
      final int lineNumber = e.getKey();
      final String line = e.getValue();
      if (line.contains("System.clearProperty"))
      {
        final Set<Integer> allowedLineNumbers =
             allowedExceptions.get(f.getName());
        if ((allowedLineNumbers != null) && allowedLineNumbers.contains(
             lineNumber))
        {
          // This is an explicitly allowed use of the method.  Don't fail
          // because of it.
          continue;
        }

        fail("Source code file " + f.getAbsolutePath() +
             " contains a forbidden use of System.clearProperty, which may " +
             "fail under certain security managers.  You should replace the " +
             "call with StaticUtils.clearSystemProperty.  The offense is on " +
             "the following line (at or near line " + lineNumber + "):" +
             StaticUtils.EOL + StaticUtils.EOL + line);
      }
    }
  }



  /**
   * Examines all source code files to ensure that there are no inappropriate
   * uses of the {@code System.getenv} method.
   *
   * @param  f  The source file to examine.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test(dataProvider = "sourceCodeFiles")
  public void testSystemGetEnv(final File f)
         throws Exception
  {
    final Map<String,Set<Integer>> allowedExceptions = StaticUtils.mapOf(
         "StaticUtils.java", StaticUtils.setOf(4201, 4205, 4229, 4233));

    final Map<Integer,String> unwrappedLines =
         unwrapSourceLines(readFileLines(f));
    for (final Map.Entry<Integer,String> e : unwrappedLines.entrySet())
    {
      final int lineNumber = e.getKey();
      final String line = e.getValue();
      if (line.contains("System.getenv"))
      {
        final Set<Integer> allowedLineNumbers =
             allowedExceptions.get(f.getName());
        if ((allowedLineNumbers != null) && allowedLineNumbers.contains(
             lineNumber))
        {
          // This is an explicitly allowed use of the method.  Don't fail
          // because of it.
          continue;
        }

        fail("Source code file " + f.getAbsolutePath() +
             " contains a forbidden use of System.getenv, which may " +
             "fail under certain security managers.  You should replace the " +
             "call with StaticUtils.getEnvironmentVariable or " +
             "StaticUtils.getEnvironmentVariables.  The offense is on the " +
             "following line (at or near line " + lineNumber + "):" +
             StaticUtils.EOL + StaticUtils.EOL + line);
      }
    }
  }



  /**
   * Examines all source code files to ensure that there are no inappropriate
   * uses of the {@code Logger.setLevel} method.
   *
   * @param  f  The source file to examine.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @Test(dataProvider = "sourceCodeFiles")
  public void testLoggerLogLevel(final File f)
         throws Exception
  {
    final Map<String,Set<Integer>> allowedExceptions = StaticUtils.mapOf(
         "Debug.java", StaticUtils.setOf(72),
         "StaticUtils.java", StaticUtils.setOf(4255, 4278));

    final Map<Integer,String> unwrappedLines =
         unwrapSourceLines(readFileLines(f));
    for (final Map.Entry<Integer,String> e : unwrappedLines.entrySet())
    {
      final int lineNumber = e.getKey();
      final String line = e.getValue();
      if (line.contains("setLevel"))
      {
        final Set<Integer> allowedLineNumbers =
             allowedExceptions.get(f.getName());
        if ((allowedLineNumbers != null) && allowedLineNumbers.contains(
             lineNumber))
        {
          // This is an explicitly allowed use of the method.  Don't fail
          // because of it.
          continue;
        }

        fail("Source code file " + f.getAbsolutePath() +
             " looks like it might contain a forbidden use of " +
             "Logger.setLevel or Handler.setLevel, which may fail under " +
             "certain security managers.  You should replace the call with " +
             "StaticUtils.setLoggerLevel or StaticUtils.setLogHandlerLevel.  " +
             "The offense is on the following line (at or near line " +
             lineNumber + "):" + StaticUtils.EOL + StaticUtils.EOL + line);
      }
    }
  }



  /**
   * Retrieves an iterator that may be used to access all of the files in the
   * LDAP SDK source code.
   *
   * @return  An iterator that may be used to access all of the files in the
   *          LDAP SDK source code.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @DataProvider(name = "sourceCodeFiles")
  public Iterator<Object[]> getSourceCodeFiles()
         throws Exception
  {
    final List<Object[]> sourceCodeFiles = new ArrayList<>(100);

    final File baseDir = new File(System.getProperty("basedir"));
    assertNotNull(baseDir);
    assertTrue(baseDir.exists());
    assertTrue(baseDir.isDirectory());
    assertTrue(baseDir.listFiles().length > 0);

    final File srcDir = new File(baseDir, "src");
    assertNotNull(srcDir);
    assertTrue(srcDir.exists());
    assertTrue(srcDir.isDirectory());
    assertTrue(srcDir.listFiles().length > 0);

    getSourceFiles(srcDir, sourceCodeFiles);

    return sourceCodeFiles.iterator();
  }



  /**
   * Recursively adds all of the source files in the specified directory to the
   * provided list.
   *
   * @param  dir    The directory containing the files to examine.
   * @param  files  The list to which all source code files should be added.
   *                Each array should contain a single non-{@code null} item,
   *                which is a {@code File} object.
   *
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  private static void getSourceFiles(final File dir, final List<Object[]> files)
          throws Exception
  {
    for (final File f : dir.listFiles())
    {
      if (f.isDirectory())
      {
        getSourceFiles(f, files);
      }
      else if (f.getName().endsWith(".java"))
      {
        files.add(new Object[] { f });
      }
    }
  }



  /**
   * Unwraps long lines in the specified list of source code lines so that
   * method calls should not be split across multiple lines.
   *
   * @param  sourceLines  The list of lines to be examined.
   *
   * @return  A list of unwrapped lines.
   */
  private static Map<Integer,String> unwrapSourceLines(
                                          final List<String> sourceLines)
  {
    final Map<Integer,String> unwrappedLines = new LinkedHashMap<>(
         StaticUtils.computeMapCapacity(sourceLines.size()));

    int currentLineNumber = 1;
    int currentStatementStartingLineNumber = 1;
    final StringBuilder completeLine = new StringBuilder();
    for (final String line : sourceLines)
    {
      if (line.isEmpty())
      {
        if (completeLine.length() == 0)
        {
          currentStatementStartingLineNumber++;
        }
        currentLineNumber++;
        continue;
      }

      if (line.endsWith(";") || line.endsWith("{") || line.endsWith("}"))
      {
        completeLine.append(line);
        unwrappedLines.put(currentStatementStartingLineNumber,
             completeLine.toString());
        completeLine.setLength(0);
        currentStatementStartingLineNumber = currentLineNumber + 1;
      }

      currentLineNumber++;
    }

    assertEquals(completeLine.length(), 0);
    return unwrappedLines;
  }
}
