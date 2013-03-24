
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

// hadoop classes
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.Reducer;

// hadoop input formats
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.NLineInputFormat;

// hadoop output formats
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;

/** An instance of this Javascript class representates a Hadoop
 *  Job. This object includes properties that reference the standard
 *  output and standard error print streams.  A constructor call to
 *  this class with no arguments creates a new Hadoop Job.  A
 *  construct call with a Hadoop Job instance creates an object which
 *  encapsulates that job.  When the function 'eggshell' is invoked by
 *  the run method of the Hadoop tool runner, an instance of this
 *  class is used as the function's 'this' object.
 */
public class Egg extends ScriptableObject 
{
  private static final long serialVersionUID = 2069490938230603150L;

  /* public static fields */

  /** Holds the javascript interpreter */
  public static Script script;  
  /** Holds the initial Hadoop configuration object */
  public static Configuration conf;
  /** Holds the name of the script */
  public static String name;

  /* private fields */

  /** Holds a Hadoop job object for the instance */
  private Job job;

  /* constructors */

  /** Called when first defined as a Javascript class 
   *  @return This class
   */
  public Egg () { }

  /** The name of this Javascript class as a string 
   *  @return  The string name of this class
   */
  public String getClassName () 
  { 
    return "Egg"; 
  }

  /** Creates a Hadoop job with a default configuration of
   *  TextInputFormat and TextOutputFormat.  If invoked with no
   *  parameters, uses the initially created job as the parent to
   *  spawn a new job.  The name of the parent job is used as the name
   *  of the child job.  The object is used a the 'this' object of the
   *  eggshell function
   *  @param o    The Hadoop Job
   */
  @JSConstructor
  public Egg (Object o) 
    throws IOException
  {
    Configuration cf = conf; // new Configuration(conf);
    job = new Job(cf, name);
    job.setJarByClass(this.getClass());               // set jar file
    job.setInputFormatClass(TextInputFormat.class);
    job.setOutputFormatClass(TextOutputFormat.class);
    job.setOutputKeyClass(Tuple.class);               // K2
    job.setOutputValueClass(Tuple.class);             // V2
    job.setMapperClass(Payload.TextMap.class);
    job.setReducerClass(Reducer.class);
    job.setCombinerClass(Reducer.class);
  }

  /* chainable public methods */

  /** Defines how to read data from a file into the Mapper instances.
   *  This method sets the input format to the 'TextInputFormat'
   *  implementation.
   *  @return The 'this' object
   */
  @JSFunction
  public Egg textInputFormat ()
  {
    job.setInputFormatClass(TextInputFormat.class);
    return this;
  }

  /** Defines how to read data from a file into the Mapper instances.
   *  This method sets the input format to the
   *  'KeyValueTextInputFormat' implementation.
   *  @return The 'this' object
   */
  @JSFunction
  public Egg keyValueTextInputFormat ()
  {
    job.setInputFormatClass(KeyValueTextInputFormat.class);
    return this;
  }

  /** Defines how to read data from a file into the Mapper instances.
   *  This method sets the input format to the
   *  'SequenceFileInputFormat' implementation.
   *  @return The 'this' object
   */
  @JSFunction
  public Egg sequenceFileInputFormat ()
  {
    job.setInputFormatClass(SequenceFileInputFormat.class);
    job.setMapperClass(Payload.TupleMap.class);
    return this;
  }

  /** Defines how to read data from a file into the Mapper instances.
   *  This method sets the input format to the
   *  'NLineInputFormat' implementation.
   *  @return The 'this' object
   */
  @JSFunction
  public Egg nLineInputFormat ()
  {
    job.setInputFormatClass(NLineInputFormat.class);
    return this;
  }

  /** Defines how to write the results of a job back into a file.
   *  This method sets the output format to the 'TextOutputFormat'
   *  implementation.
   *  @return The 'this' object
   */
  @JSFunction
  public Egg textOutputFormat ()
  {
    job.setOutputFormatClass(TextOutputFormat.class);
    return this;
  }

  /** Defines how to write the results of a job back into a file.
   *  This method sets the output format to the 'NullOutputFormat'
   *  implementation.
   *  @return The 'this' object
   */
  @JSFunction
  public Egg nullOutputFormat ()
  {
    job.setOutputFormatClass(NullOutputFormat.class);
    return this;
  }

  /** Defines how to write the results of a job back into a file.
   *  This method sets the output format to the
   *  'SequenceFileOutputFormat' implementation.
   *  @return The 'this' object
   */
  @JSFunction
  public Egg sequenceFileOutputFormat ()
  {
    job.setOutputFormatClass(SequenceFileOutputFormat.class);
    return this;
  }

  /** Set the user-specified job name.
   *  @param name The job name
   *  @return The 'this' object
   */
  @JSFunction
  public Egg name (String name)
  {
    job.setJobName(name);
    return this;
  }

  /** Adds a path to the list of inputs for the map-reduce job
   *  @param pathString  The name of the path
   *  @return            The 'this' object
   */
  @JSFunction
  public Egg addInput (String pathString)
    throws IOException
  {
    Path path = new Path(pathString);
    FileInputFormat.addInputPath(job, path);
    return this;
  }

  /** Sets the list of inputs for the map-reduce job to the path
   *  @param pathString  The name of the path
   *  @return            The 'this' object
   */
  @JSFunction
  public Egg input (String pathString)
    throws IOException
  {
    Path path = new Path(pathString);
    FileInputFormat.setInputPaths(job, path);
    return this;
  }

  /** Sets the output for the map-reduce job to the path
   *  @param pathString  The name of the path
   *  @return            The 'this' object
   */
  @JSFunction
  public Egg output (String pathString)
  {
    Path path = new Path(pathString);
    FileOutputFormat.setOutputPath(job, path);
    return this;
  }

  /** Sets the number of reduce tasks for the map-reduce job
   *  @param tasks       The number of reduce tasks
   *  @return            The 'this' object
   */
  @JSFunction
  public Egg numReduceTasks (int tasks)
  {
    job.setNumReduceTasks(tasks);
    return this;
  }

  /** Turns speculative execution on or off for the map tasks
   *  @param enable      On or off
   *  @return            The 'this' object
   */
  @JSFunction
  public Egg mapSpeculativeExecution (boolean enable)
  {
    job.setMapSpeculativeExecution(enable);
    return this;
  }

  /** Turns speculative execution on or off for the reduce tasks
   *  @param enable      On or off
   *  @return            The 'this' object
   */
  @JSFunction
  public Egg reduceSpeculativeExecution (boolean enable)
  {
    job.setReduceSpeculativeExecution(enable);
    return this;
  }

  /** Turns speculative execution on or off for the map-reduce job
   *  @param enable      On or off
   *  @return            The 'this' object
   */
  @JSFunction
  public Egg speculativeExecution (boolean enable)
  {
    job.setSpeculativeExecution(enable);
    return this;
  }

  /** Sets the mapper function for the job
   *  @param o  The javascript map function
   *  @return   The 'this' object
   */
  @JSFunction
  public Egg map (Object o)
    throws IOException
  {
    script.serialize(job.getConfiguration(), o, Eggshell.MAP_FILE);
    return this;
  }

  /** Sets the reducer function for the job
   *  @param o  The javascript reduce function
   *  @return   The 'this' object
   */
  @JSFunction
  public Egg reduce (Object o)
    throws IOException
  {
    script.serialize(job.getConfiguration(), o, Eggshell.REDUCE_FILE);
    job.setReducerClass(Payload.Reduce.class);
    return this;
  }

  /** Sets the combiner function for the job
   *  @param o  The javascript combine function
   *  @return   The 'this' object
   */
  @JSFunction
  public Egg combine (Object o)
    throws IOException
  {
    script.serialize(job.getConfiguration(), o, Eggshell.COMBINE_FILE);
    job.setCombinerClass(Payload.Combine.class);
    return this;
  }

  /* non-chainable public methods */

  /** Kill the running job
   */
  @JSFunction
  public void kill ()
    throws IOException
  {
    job.killJob();
  }

  /** Submit the job to the cluster and return immediately
   */
  @JSFunction
  public void submit ()
    throws IOException, InterruptedException, ClassNotFoundException
  {
    job.submit();
  }

  /** Submit the job to the cluster and wait for it to finish
   *  @param verbose  Verbose output or not
   *  @return         Job completion sucess
   */
  @JSFunction
  public boolean waitForCompletion (boolean verbose)
    throws IOException, InterruptedException, ClassNotFoundException
  {
    return job.waitForCompletion(verbose);
  }

  /** Get the user specified job name
   *  @return The job name
   */
  @JSFunction
  public String getName ()
  {
    return job.getJobName();
  }

  /* getter methods */

  /** Get the encapsulated job object
   *  @return The job object
   */
  @JSGetter
  public Job getJob ()
  {
    return job;
  }

  /** Get the pathname of the job's jar
   *  @return The pathname
   */
  @JSGetter
  public String getJar ()
  {
    return job.getJar();
  }

  /** Get the URL for tracking the job's progress
   *  @return The URL
   */
  @JSGetter
  public String getTrackingURL ()
  {
    return job.getTrackingURL();
  }

  /** Check if the job finished or not
   *  @return Job completion status
   */
  @JSGetter
  public Boolean getIsComplete ()
    throws IOException
  {
    return job.isComplete();
  }

  /** Check if the job finished successfully or not
   *  @return Job success status
   */
  @JSGetter
  public Boolean getIsSuccessful ()
    throws IOException
  {
    return job.isSuccessful();
  }

  /** Get the progress of the job's map tasks
   *  @return  A progress value between 0.0 and 1.0.
   */
  @JSGetter
  public Double getMapProgress ()
    throws IOException
  {
    return (double) job.mapProgress();
  }

  /** Get the progress of the job's reduce tasks
   *  @return  A progress value between 0.0 and 1.0.
   */
  @JSGetter
  public Double getReduceProgress ()
    throws IOException
  {
    return (double) job.reduceProgress();
  }

  /** Get the progress of the job's setup
   *  @return  A progress value between 0.0 and 1.0.
   */
  @JSGetter
  public Double getSetupProgress ()
    throws IOException
  {
    return (double) job.setupProgress();
  }
}

