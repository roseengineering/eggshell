Eggshell
========

Framework for writing JavaScript map-reduce jobs for Hadoop

Overview
--------

Eggshell lets you write and run Hadoop map-reduce functions using
JavaScript.  The software accomplishes this by serializing the
map-reduce functions passed to Eggshell's JavaScript
library functions.  The Eggshell software sends these serialized
JavaScript functions across the network to the nodes of the Hadoop cluster for
deserialization and executation by the map-reduce tasks.

Eggshell leverages the power of the Java written, JavasScript
interpreter Rhino to avoid the additional computing costs of starting
up a separate process to run the interpreter within a map-reduce task.
Instead, deserializing and execution of the JavaScript map reduce
function stays entirely within the JVM of the task.

And since functions are treated by JavaScript like any other object,
Eggshell map-reduce scripts can be easily written in a nice functional
as well as cascaded way.  For example, a Eggshell script to perform a
word count can be written as follows:

```javascript
function eggshell (input, output) {
  this
    .input(input)
    .output(output)
    .map(
      function (key, value) {
        var i, word = value.split(" ");
        for (i = 0; i < word.length; i += 1) {
          yield [ word[i], 1 ];
        }
      })
    .combine(
      function (key, values) {
        var sum = 0;
        while (values.hasNext()) {
           sum = sum + values.next();
        }
        return [ key, sum ];
      })
    .reduce(
      function (key, values) {
        var sum = 0;
        while (values.hasNext()) {
          sum = sum + values.next();
        }
        return [ key, sum ];
      })
    .waitForCompletion(true);
}
```

The Job Scope
-------------

The enclosing function eggshell() is optional.  When Eggshell starts,
it first runs and evaluates the JavaScript script passed to it via the
command line and then calls the function named eggshell if defined.
This function is passed the remaining arguments from the command line.
These arguments are also available through the array, arguments,
defined in the global JavaScript scope.

In addition the 'this' object of the eggshell function is set to an
instance of the JavaScript class Egg.  The Egg object represents a
Hadoop MR2 Job object.  To create another Job just instantiate another
Egg object.  

```javascript
job = this;       // holds a Job object
job = new Egg();  // creates a new Job object
```

The methods of the Egg object directly call the methods of the
encapsulated Job object.  In addition most methods of the Egg object
return their own 'this' object allowing Egg methods to be
cascaded, or chained.  Some chainable methods follow:

```javascript
this.textInputFormat(); // calls setInputFormatClass(TextInputFormat)
this.keyValueTextInputFormat(); // calls setInputFormatClass(KeyValueTextInputFormat)
this.nLineInputFormat(); // calls setInputFormatClass(NLineInputFormat)
this.sequenceFileInputFormat(); // calls setInputFormatClass(SequenceFileInputFormat)

this.textOutputFormat(); // calls setOutputFormatClass(TextOutputFormat)
this.nullOutputFormat(); // calls setOutputFormatClass(NullOutputFormat)
this.sequenceFileOutputFormat(); // calls setOutputFormatClass(SequenceFileOutputFormat)

this.name(name); // calls setJobName(name)
this.addInput(path); // calls FileInputFormat.addInputPath()
this.input(path); // calls FileInputFormat.setInputPaths()
this.output(path); // calls FileOutputFormat.setOutputPath()
this.numReduceTasks(tasks); // calls setNumReduceTasks()
this.map(o); // serializes the mapper function
this.reduce(o); // serializes the reducer function
this.combine(o); // serializes the combiner function

this.mapSpeculativeExecution(enable); // calls setMapSpeculativeExecution(enable)
this.reduceSpeculativeExecution(enable); // calls setReduceSpeculativeExecution(enable)
this.speculativeExecution(enable); // calls setSpeculativeExecution(enable)
```

Some other non-chainable functions and getter methods are:

```javascript
this.kill(); // kill job
this.submit(); // submit job and do not wait
this.waitForCompletion(verbose); // submit job, wait for it to complete, and return success
this.getName(); // calls getJobName()

this.job; // the Job object
this.jar; // the name of the jar file
this.trackingURL; // calls getTrackingURL()
this.isComplete; // calls isComplete()
this.isSuccessful; // calls isSuccessful()
```

The Egg map(), reduce(), and combine() methods can also take objects as
arguments as well as functions.  
Properties of the object reference the map() or reduce() functions that are to be called by the task.
In addition the properties can reference the setup() and cleanup() functions of the task too.

```javascript
job.map({
  setup: function () {},
  map: function (key, value) {},
  cleanup: function () {}
});
```

Global Scope
------------

The JavaScript interpreter provides the following functions as part of its
global scope.  These functions work within the job runner or within a task.

```javascript
load(filename); // loads and evaluates the named JavaScript file
println(string); // prints a line to the standard output
print(string); // print a string without new line to the standard output
stderr; // returns a reference to the Java standard error print stream.
stdout; // returns a reference to the Java standard output print stream.
open(filename); // opens the named file
readln(); // reads a line from the openned file
close(); // closes an openned file
```

Task Scope
----------

Eggshell uses its own sequence file format.  This file format can
written out or read in by the JavaScript map-reduce functions.  The
sequence file format implements a linear array-like data structure
call Tuple, which was inspired by the BSON implementation.  This
'tuple' is able to store raw JavaScript numbers, strings, and
booleans.  When passed as an argument to a map-reduce function the
tuple is converted to an array.  The same thing happens when the
method next() of the custom Eggshell iterator object (which is passed to
the reduce function as the 'values' parameter) is called to get the
next value.

The reverse occurs when arrays are returned by the the JavaScript
functions.  Each array is converted into a tuple with the above data
types preserved.  This allows map-reduce sorting to be performed
at the level of the data types stored in the key tuple.

Eggshell expects the value returned (or yielded) by the map-reduce
function to be a array.  The first element of this array is the key
and the second is the value.  The key can be either a single number,
string, or boolean or an array of them.  The value likewise, however
should it be an array, the array can be flattened into the enclosing
array.  So the following return value is acceptable, [ key, value1,
value2, value3, ... valuen ].

Task called JavaScript functions are supplied with a 'this' object
that encapsulates the current Hadoop task context object.  The context
object provides access to the various status and metric methods of the
task.  For example, the following statements are possible:

```javascript
this.incrCounter(group, name);  // increment the counter by one
this.incrCounter(group, name, 2);  // increment the counter by two
this.setCounter(group, name, 2);  // set counter to two
this.counter(group, name);   // get the counter's value
this.status = "my current status";  // tell task node my current status
this.progress();    // tell hadoop I am still alive and working
```

Chaining Jobs
-------------

It is possible to chain jobs together using Eggshell.  The following code
gives an example:

```javascript
new Egg()
  .input(input)
  .output("temp")
  .map(map)
  .reduce({ 
         setup:setup,
         reduce:reduce, 
       })
  .sequenceFileOutputFormat()
  .waitForCompletion(false);

new Egg()
  .input("temp")
  .output(output)
  .sequenceFileInputFormat()
  .map(function (key, value) { 
    return [ key, value ];
  })
  .numReduceTasks(1)
  .waitForCompletion(false);
```

Running a Eggshell Job
----------------------

Below is an sample Eggshell job:

```bash
$ hadoop fs -mkdir input
$ wget www.usconstitution.net/const.txt
$ hadoop fs -put const.txt input
$ export HADOOP_CLASSPATH=/usr/share/java/js.jar
$ hadoop jar Eggshell.jar Eggshell -libjars /usr/share/java/js.jar wordcount.js input output
```

The first argument is the script name.  While the remaining arguments
are passed to the JavaScript runtime.

Please see the example directory as well for sample Eggshell scripts.

