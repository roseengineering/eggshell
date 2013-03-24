
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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.TaskInputOutputContext;


/** An instance of this class encapsulates a Hadoop map-reduce
 *  context.  When a Eggshell map-reduce function is called, its
 *  'this' object will be an instance of this class.  The object
 *  includes properties referencing the standard output and standard
 *  error print streams.  In addition the object has methods to
 *  increment Hadoop metric counters.
 */
public class EggContext extends ScriptableObject 
{
  private static final long serialVersionUID = 7549795420366559595L;

  /* public static fields */

  /** Holds the current map-reduce task context */
  public static TaskInputOutputContext task;  

  /* constructors */

  /** Called when first defined as a Javascript class 
   *  @return This class
   */
  public EggContext () {}

  /** The name of this Javascript class as a string 
   *  @return  The string name of this class
   */
  public String getClassName() 
  { 
    return "EggContext"; 
  }

  /* public methods */

  /** Tell Hadoop that the task is making progress despite what it
   *  assumes.
   */
  @JSFunction
  public void progress ()
  {
    task.progress();
  }

  /** Increment the Hadoop metric counter of the passed group and name
   *  by the specified amount.  If value not given then counter is
   *  incremented by one.
   *  @param group   The counter group
   *  @param name    The counter name
   *  @param value   The amount to increment
   */
  @JSFunction
  public void incrCounter (String group, String name, Object value)
  {
    Counter counter = task.getCounter(group, name);
    if (value instanceof Undefined) 
      counter.increment(1);
    if (value instanceof Double)
      counter.increment(((Double) value).longValue());
  }

  /** Set the Hadoop metric counter of the passed group and name to
   *  specified value.
   *  @param group   The counter group
   *  @param name    The counter name
   *  @param value   The value
   */
  @JSFunction
  public void setCounter (String group, String name, Double value)
  {
    Counter counter = task.getCounter(group, name);
    if (!value.isNaN()) counter.setValue(value.longValue());
  }

  /** Get the current value of the Hadoop metric counter of the
   *  passed group and name
   *  @param group   The counter group
   *  @param name    The counter name
   *  @return        The current value of the counter
   */
  @JSFunction
  public Double counter (String group, String name)
  {
    Counter counter = task.getCounter(group, name);
    return new Double(counter.getValue());
  }

  /* setter methods */

  /** Set the status message of the task.
   *  @param message   The status message
   */
  @JSSetter
  public void setStatus (String message)
  {
    task.setStatus(message);
  }

  /* getter methods */

  /** Get the task attempt ID for the task
   *  @return The Hadoop task attempt ID
   */
  @JSGetter
  public String getTaskAttemptID ()
  {
    return task.getTaskAttemptID().toString();
  }

  /** Get the task context that this object encapsulates
   *  @return The Hadoop task context
   */
  @JSGetter
  public TaskInputOutputContext getContext ()
  {
    return task;
  }

  /** Get the Hadoop Configuration object
   *  @return The Hadoop configuration
   */
  @JSGetter
  public Configuration getConfiguration ()
  {
    return task.getConfiguration();
  }

  /** Get the Job ID for the task
   *  @return The Hadoop job id
   */
  @JSGetter
  public String getJobID ()
  {
    return task.getJobID().toString();
  }

  /** Get the user-specified job name for the task
   *  @return The Hadoop job name
   */
  @JSGetter
  public String getJobName ()
  {
    return task.getJobName();
  }

  /** Get the status message for the task.
   *  @return The status message
   */
  @JSGetter
  public String getStatus ()
  {
    return task.getStatus();
  }
}

 
