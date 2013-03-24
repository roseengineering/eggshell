
/*
This file is part of Eggshell.
Copyright 2013 George Magiros

Eggshell is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or (at
your option) any later version.

Eggshell is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License
along with Eggshell.  If not, see <http://www.gnu.org/licenses/>.
*/

import org.mozilla.javascript.*;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import java.io.IOException;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;

/** Provides the global scope and includes properties that reference
 *  the standard output and standard error print streams.  Also has
 *  methods to read files and load javascript source.
 */
public class EggGlobal extends ScriptableObject 
{
  private static final long serialVersionUID = 2902986993382947684L;

  /* private fields */

  /** Used by the class's open, close and readln routines */
  private BufferedReader reader;
      
  /* public static fields */

  /** Holds the current Javascript interpreter */
  public static Script script;  

  /* constructors */

  /** Called when first defined as a Javascript class 
   *  @return This class
   */
  public EggGlobal () { }

  /** The name of this Javascript class as a string 
   *  @return  The string name of this class
   */
  public String getClassName () 
  { 
    return "EggGlobal"; 
  }

  /* public methods */

  /** Write a line to the standard output
   *  @param o    The line
   */
  @JSFunction
  public void println (Object o)
  {
    String line = "";
    if (o != null && !(o instanceof Undefined)) line = o.toString();
    System.out.println(line);
  }

  /** Write message to the standard output.  No trailing line feed is
   *  added
   *  @param o   The message
   */
  @JSFunction
  public void print (Object o)
  {
    String line = "";
    if (o != null && !(o instanceof Undefined)) line = o.toString();
    System.out.print(line);
  }

  /** Load the JavaScript source file.
   *  @param name    The JavaScript source file name
   *  @return        The result of the evaluation
   */
  @JSFunction
  public Object load (String name)
    throws IOException
  {
    return script.evalFile(name);
  }

  /** Open a file for reading
   *  @param name    The file name
   */
  @JSFunction
  public void open (String name)
    throws IOException
  {
    InputStream stream = new FileInputStream(name);
    reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
  }

  /** Close the input stream
   */
  @JSFunction
  public void close ()
    throws IOException
  {
    reader.close();
  }

  /** Read a line from the input stream
   *  @return    The line read
   */
  @JSFunction
  public String readln ()
    throws IOException
  {
    return reader.readLine();
  }

  /* getter methods */

  /** Get the standard error print stream
   *  @return A print stream
   */
  @JSGetter
  public PrintStream getStderr ()
  {
    return System.err;
  }

  /** Get the standard output print stream
   *  @return A print stream
   */
  @JSGetter
  public PrintStream getStdout ()
  {
    return System.out;
  }
}

