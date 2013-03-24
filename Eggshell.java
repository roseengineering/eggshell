
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

import java.util.Arrays;
import java.io.IOException;

// hadoop job
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

// hadoop file systems
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;

// hadooop runner
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.conf.Configuration;

/** Class that processes the command-line options
 *  from Hadoop and submits the job.
 */
public class Eggshell extends Configured implements Tool 
{
  /* public static fields */

  /** The name of the file containing the Eggshell Javascript library */
  public static final String LIBRARY_FILE = "eggshell.js"; 
  /** HDFS temporary directory */
  public static final String SCRIPT_DIR = ".eggshell";
  /** The name of the file containing the serialized Javascript map function */ 
  public static final String MAP_FILE = "map.js";
  /** The name of the file containing the serialized Javascript reduce function */ 
  public static final String REDUCE_FILE = "reduce.js";
  /** The name of the file containing the serialized Javascript combine function */ 
  public static final String COMBINE_FILE = "combine.js";

  /* private fields */
  
  /** Holds the Javascript interpreter's state */
  private Script script;

  /* public methods */

  /** In charge of setting up and submitting the Hadoop job
   *  The method receives the remaining command-line arguments
   *  after first being processed by Hadoop.
   *
   * @param args        Hadoop processed command-line arguments
   * @return            Returns 0 for sucess
   */
  public int run (String[] args) 
    throws Exception
  {
    String name = args[0];
    String[] params = Arrays.copyOfRange(args, 1, args.length);
    Object[] arguments = Arrays.copyOf(params, params.length, Object[].class);
    
    script = new Script();    // start the Javascript interpreter
    script.putProperty("arguments", script.newArray(arguments));

    EggGlobal.script = script;
    Egg.script = script;
    Egg.name = name;
    Egg.conf = getConf();

    Scriptable global = script.newObject("EggGlobal", null);
    script.setGlobalScope(global);

    script.evalLibrary();     // load the Eggshell Javascript library
    script.evalFile(name);    // load the javascript job file

    /* create a temporary directory in hdfs to hold the seralized functions */
    FileSystem fs = FileSystem.get(getConf());
    Path dir = new Path(SCRIPT_DIR);
    if (fs.exists(dir)) fs.delete(dir, true);
    fs.mkdirs(dir);

    /* call the 'eggshell' function */
    Object o = script.getProperty("eggshell");
    if (o instanceof Function) {
      Scriptable thisObj = script.newObject("Egg", null);
      Function f = (Function) o;
      o = script.callFunction(f, thisObj, params); 
      script.exit();
      
      /* return the result of the 'eggshell' function */
      if (o instanceof NativeJavaObject) o = ((NativeJavaObject) o).unwrap();
      if (o instanceof Boolean) return (Boolean) o ? 0 : 1;
      if (o instanceof Integer) return (Integer) o;
      if (o instanceof Double) return ((Double) o).intValue();
    }
    return 0;
  }

  /** The static main method for the class
   *
   * @param args  The raw command-line
   */
  public static void main (String[] args) 
    throws Exception 
  {
    int ret = ToolRunner.run(new Eggshell(), args);
    System.exit(ret);
  }
}


