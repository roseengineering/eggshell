
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
import java.io.FileNotFoundException;

// java stream reading classes
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;

// hadoop task context
import org.apache.hadoop.mapreduce.TaskInputOutputContext;

// hadoop filesystem classes
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;


/** Class that starts the javascript and handles the evaluation of
 *  scripts including those serialized over the distributed cache
 */
class Script
{
  /* private fields */

  /** The global scope object */
  private Scriptable globalScope;

  /** The Javascript interpreter's context object */
  private Context cx;

  /* constructors */

  /** Starts the javascript interpreter and defines the base classes
   *  used by eggshell in the global scope.
   *  @return             The new script object
   *  @thows IOException  Failed creating the Javascript environment
   */
  public Script ()
    throws IOException
  {
    try {
      cx = Context.enter();
      cx.setLanguageVersion(Context.VERSION_1_7);
      globalScope = cx.initStandardObjects();
      ScriptableObject.defineClass(globalScope, EggGlobal.class);
      ScriptableObject.defineClass(globalScope, EggIterator.class);
      ScriptableObject.defineClass(globalScope, EggContext.class);
      ScriptableObject.defineClass(globalScope, Egg.class);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  /* public methods */

  /** Exit the javascript interpreter
   */
  public void exit ()
  {
    cx.exit();
  }

  /** Set the global scope.
   *  @param scope    The new global scope
   */
  public void setGlobalScope (Scriptable scope)
  {
    globalScope = scope;
  }

  /** Create a Javascript array from a Java Object[] array.
   *  @param         The Java array
   *  @return        The new array
   */
  public Scriptable newArray (Object[] array)
  {
    return cx.newArray(globalScope, array);
  }

  /** Create a Javascript object from a class.  The constructor is
   *  searched for in the global scope.
   *  @param name       The name of the class
   *  @param args       The arguments to pass to the class's constructor
   *  @return           The new object
   */
  public Scriptable newObject (String name, 
                               Object[] args)
  {
    return cx.newObject(globalScope, name, args);
  }

  /** Return the Javascript global scope property referenced by the
   *  name.  If the property is not found then return null.
   *  @param name       The name of the property
   *  @return           The Javascript object or null
   */
  public Object getProperty (String name)
  {
    Object ret = globalScope.get(name, globalScope);
    return (ret != UniqueTag.NOT_FOUND) ? ret : null;
  }

  /** Return the Javascript object property referenced by the name.
   *  If the property is not found then return null.
   *  @param object     The object to look up the property in
   *  @param name       The name of the property
   *  @return           The Javascript object or null
   */
  public Object getProperty (String name,
                             Scriptable object)
  {
    Object ret = object.get(name, object);
    return (ret != UniqueTag.NOT_FOUND) ? ret : null;
  }

  /** Add the property to the Javascript global scope.
   *  @param name       The name of the property
   *  @param name       The scriptable object
   */
  public void putProperty (String name, Object object)
  {
    globalScope.put(name, globalScope, object);
  }

  /** Call a Javascript function.  If a Javascript exception occurs
   *  return null as the result of the function call.
   *  @param func       The Javascript function
   *  @param thisObj    The 'this' object to pass to the function
   *  @param args       The arguments to pass to the function
   *  @return           The result of the Javascript function call
   */
  public Object callFunction (Function func, 
                              Scriptable thisObj,
                              Object[] args)
  {
    if (thisObj == null) thisObj = globalScope;
    Object ret;
    try {
      ret = func.call(cx, thisObj.getParentScope(), thisObj, args);
      if (ret instanceof Undefined) ret = null;
    } catch (JavaScriptException e) { 
      ret = null; 
    }
    return ret;
  }

  /** Evaluates the Javascript expression contained within a string,
   *  performing the evaluation within the global scope.
   *  @param buf        String to evaluate
   *  @return           The result of the Javascript evaluation
   */
  public Object evalString (String buf)
  {
    return cx.evaluateString(globalScope, buf, this.getClass().toString(), 1, null);
  }

  /** Evaluates the Javascript expressions found in the eggshell
   *  Javascript library file.
   *  @return           The result of the Javascript evaluation
   */
  public Object evalLibrary () 
    throws IOException
  {
    ClassLoader cl = this.getClass().getClassLoader();
    InputStream stream = cl.getResourceAsStream(Eggshell.LIBRARY_FILE);
    BufferedReader reader = 
      new BufferedReader(new InputStreamReader(stream, "UTF-8"));
    String line, buf = "";
    while ((line = reader.readLine()) != null) buf += line + "\n";
    reader.close();
    return evalString(buf);
  }

  /** Evaluates the Javascript expressions contained in a file.
   *  @param name       The name of the file
   *  @return           The result of the Javascript evaluation
   */
  public Object evalFile (String name) 
    throws IOException
  {
    InputStream stream = new FileInputStream(name);
    BufferedReader reader = 
      new BufferedReader(new InputStreamReader(stream, "UTF-8"));
    String line, buf = "";
    while ((line = reader.readLine()) != null) buf += line + "\n";
    reader.close();
    return evalString(buf);
  }

  /** Evaluates the Javascript expressions contained in a
   *  DataInputStream serialized file and passed over the distributed
   *  cache.
   *  @param conf       The Hadoop configuration object
   *  @param pathString The path string of the cached file
   *  @param name       The name of the file added to the cache
   *  @return           The result of the Javascript evaluation
   */
  public Object evalCache (Configuration conf, 
                           String pathString, 
                           String name) 
    throws IOException
  {
    FSDataInputStream in;
    FileSystem fs = FileSystem.getLocal(conf);
    try {
      Path path = new Path(pathString);
      in = fs.open(path);
    } catch (FileNotFoundException e) {  // must be running in standalone mode
      Path path = new Path(Eggshell.SCRIPT_DIR + "/" + name);
      in = fs.open(path);  // read it from the eggshell script directory instead
    }
    String buf = in.readUTF();
    in.close();
    return evalString(buf);
  }

  /** Call a map-reduce Javascript function, saving the result in
   *  key and value Hadoop custom writable 'Tuples'.
   *  @param func          The map-reduce Javascript function
   *  @param thisObj       The 'this' object to pass to the function
   *  @param args          The arguments to pass to the function
   *  @return              The result of the Javascript function call
   *  @throws IOException  The map-reduce function returned a bad result
   */
  public Object callMapReduce (Function func, 
                               Scriptable thisObj,
                               Object[] args,
                               Tuple key,
                               Tuple value)
    throws IOException
  {
    Object ret = callFunction(func, thisObj, args);

    // handle generators
    if (ret == null || ret instanceof NativeGenerator) return ret;

    // handle a map-reduce result
    if (!(ret instanceof NativeArray)) throw new IOException();
    NativeArray array = (NativeArray) ret;

    key.clear();         // get the key Tuple and fill it
    ret = array.get(0);
    if (!(ret instanceof NativeArray)) key.add(ret);
    else {
      NativeArray subarray = (NativeArray) ret;
      for (int i = 0; i < subarray.size(); i++) key.add(subarray.get(i));
    }

    value.clear();       // get the value Tuple and fill with the rest
    for (int n = 1; n < array.size(); n++) {
      ret = array.get(n);
      if (!(ret instanceof NativeArray)) value.add(ret);
      else {
        NativeArray subarray = (NativeArray) ret;
        for (int i = 0; i < subarray.size(); i++) value.add(subarray.get(i));
      }
    }
    return array;
  }

  /** Call the map-reduce Javascript function with the given
   *  arguments.  Save the key-value result in the task's context
   *  @param context   Task context
   *  @param script    The Javascript interpreter
   *  @param f         The map-reduce javascript object
   *  @param args      The key-value arguments
   */
  @SuppressWarnings("unchecked")
  public void dispatchMapReduce (TaskInputOutputContext context,
                                 Function f,
                                 Scriptable thisObj,
                                 Object[] args,
                                 Tuple key,
                                 Tuple value)
    throws IOException, InterruptedException
  {
    Object ret = callMapReduce(f, thisObj, args, key, value);
    if (ret instanceof NativeGenerator) {
      NativeGenerator gen = (NativeGenerator) ret;
      Function next = (Function) gen.getProperty(gen, "next");
      while (callMapReduce(next, gen, null, key, value) != null)
        context.write(key, value);
    } else if (ret != null) {
      context.write(key, value);
    }
  }

  /** Serialize the Javascript object into a file on HDFS and then add
   *  the file to the distributed cache.
   *  @param conf       The Hadoop configuration object
   *  @param o          The Javascript object to serialize
   *  @param name       The name of file to save the serialized object to
   */
  public void serialize (Configuration conf, 
                         Object o, 
                         String name)
    throws IOException
  {
    FileSystem hdfs = FileSystem.get(conf);
    Path path = new Path(Eggshell.SCRIPT_DIR + "/" + name);
    FSDataOutputStream out = hdfs.create(path); // create the file
    String buf;
    if (!(o instanceof NativeObject)) {
      buf = cx.toString(o); // serialize
      if (o instanceof NativeArray)  buf = "[" + buf + "]"; // if array
    }
    else {
      buf = "{";
      NativeObject obj = (NativeObject) o;
      Object[] propIds = obj.getPropertyIds(obj);
      for(Object propId: propIds) {
        String key = propId.toString();
        Object value = obj.getProperty(obj, key);
        buf += key + ":" + cx.toString(value) + ",";
      }
      buf += "}";
    }
    buf = "(" + buf + ")"; // force evaluation
    out.writeUTF(buf);
    out.close();
    DistributedCache.addCacheFile(path.toUri(), conf);
  }

  /** Deserialize the Javascript function from the distributed cache.
   *  @param conf    The Hadoop configuration
   *  @return        The Javascript function
   */
  public Object deserialize (Configuration conf, String file)
    throws IOException
  {
    Path [] cacheFiles = DistributedCache.getLocalCacheFiles(conf);
    if (null != cacheFiles && cacheFiles.length > 0) {
      for (Path path : cacheFiles) {        // loop through cache files
        if (path.getName().equals(file)) {  // find this file
          return evalCache(conf, path.toString(), file);
        }
      }
    }
    return null;
  }
}


