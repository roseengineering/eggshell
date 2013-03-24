
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

import java.io.IOException;

// hadoop classes
import org.apache.hadoop.io.Text;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.mapreduce.TaskInputOutputContext;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;


/** An Eggshell Payload class for map-reduce methods
 */
class Payload 
{
  /* private static fields */

  /** The key tuple result */
  private static Tuple keyout = new Tuple();  
  /** The value tuple result */
  private static Tuple valueout = new Tuple(); 
  /** Holds Javascript interpreter */
  private static Script script;      
  /** Holds the 'this' EggContext object */
  private static Scriptable thisObj;

  /* stores the Javascript functions */

  private static Function fmapred; 
  private static Function fmap;
  private static Function freduce;
  private static Function fsetup;
  private static Function fcleanup;

  /** Shadow setup function
   */
  private static void setup (TaskInputOutputContext task, String name) 
    throws IOException
  {
    script = new Script();
    EggGlobal.script = script;
    EggIterator.script = script;
    EggContext.task = task;

    Scriptable global = script.newObject("EggGlobal", null);
    script.setGlobalScope(global);
    thisObj = script.newObject("EggContext", null);

    Object o = script.deserialize(task.getConfiguration(), name);
    if (o instanceof NativeObject) {
      NativeObject obj = (NativeObject) o;
      fmap = (Function) script.getProperty("map", obj); 
      freduce = (Function) script.getProperty("reduce", obj); 
      fsetup = (Function) script.getProperty("setup", obj); 
      fcleanup = (Function) script.getProperty("cleanup", obj); 
      if (fsetup != null) script.callFunction(fsetup, thisObj, null);
    } else if (o instanceof Function) {
      fmapred = (Function) o;
    }
  }

  /** Shadow cleanup function
   */
  private static void cleanup () 
  {
    if (fcleanup != null) script.callFunction(fcleanup, thisObj, null);
    script.exit();
  }

  /** The text input format mapper class
   */
  static class TextMap extends Mapper<Object, Text, Tuple, Tuple> 
  {
    protected void setup (Context context) 
      throws IOException
    {
      Payload.setup(context, Eggshell.MAP_FILE);
      if (fmap == null) fmap = fmapred;
    }
    
    protected void cleanup (Context context) 
      throws IOException
    {
      Payload.cleanup();
    }
    
    protected void map (Object key, Text value, Context context) 
      throws IOException, InterruptedException 
    {
      Object[] args = { key.toString(), value.toString() };
      script.dispatchMapReduce(context, fmap, thisObj, args, keyout, valueout);
    }
  }
  
  /** The sequence file input format mapper class
   */
  static class TupleMap extends Mapper<Tuple, Tuple, Tuple, Tuple> 
  {
    protected void setup (Context context) 
      throws IOException
    {
      Payload.setup(context, Eggshell.MAP_FILE);
      if (fmap == null) fmap = fmapred;
    }
    
    protected void cleanup (Context context) 
      throws IOException
    {
      Payload.cleanup();
    }
    
    protected void map (Tuple key, Tuple value, Context context) 
      throws IOException, InterruptedException 
    {
      Object[] args = { key.toParams(script), value.toParams(script) };
      script.dispatchMapReduce(context, fmap, thisObj, args, keyout, valueout);
    }
  }
  
  /** The reducer class
   */
  static class Reduce extends Reducer<Tuple, Tuple, Tuple, Tuple> 
  {
    protected void setup (Context context) 
      throws IOException
    {
      Payload.setup(context, Eggshell.REDUCE_FILE);
      if (freduce == null) freduce = fmapred;
    }
    
    protected void cleanup (Context context) 
      throws IOException
    {
      Payload.cleanup();
    }
    
    protected void reduce (Tuple key, Iterable<Tuple> values, Context context) 
      throws IOException, InterruptedException 
    {
      Object args[] = new Object[]{ values.iterator() };
      Scriptable itr = script.newObject("EggIterator", args); 
      args = new Object[]{ key.toParams(script), itr };
      script.dispatchMapReduce(context, freduce, thisObj, args, keyout, valueout);
    }
  }

  /** The combiner class
   */
  static class Combine extends Reducer<Tuple, Tuple, Tuple, Tuple> 
  {
    protected void setup (Context context) 
      throws IOException
    {
      Payload.setup(context, Eggshell.COMBINE_FILE);
      if (freduce == null) freduce = fmapred;
    }
    
    protected void cleanup (Context context) 
      throws IOException
    {
      Payload.cleanup();
    }
    
    protected void reduce (Tuple key, Iterable<Tuple> values, Context context) 
      throws IOException, InterruptedException 
    {
      Object args[] = new Object[]{ values.iterator() };
      Scriptable itr = script.newObject("EggIterator", args); 
      args = new Object[]{ key.toParams(script), itr };
      script.dispatchMapReduce(context, freduce, thisObj, args, keyout, valueout);
    }
  }
}
