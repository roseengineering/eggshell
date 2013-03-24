//
// Count the number of lines in the input using Hadoop counters.  The
// total count is found through the tracker web interface.  No output
// is generated since the output format is set to null.  The example
// also sets the tracker status message, writes to each tracker's
// standard output, invokes the task context's progess method, outputs
// the tracking URL, and uses the job's submit method.
//
// This file is part of Eggshell.
// Copyright 2013 George Magiros
// Distributed under the terms of the GNU GPL


function eggshell (input) {
  this
  .input(input)
  .nullOutputFormat()
  .map(function (key, value) {
    var status,
        group = "good",
        name = "lines";
    this.incrCounter(group, name);
    status = "map: counter = " + this.counter(group, name);
    this.status = status;
    this.progress();
  })
  .numReduceTasks(1) // fails if number of reducer tasks is set to 0
  .submit();

  print("\nTracking URL: ");
  println(this.trackingURL);
  println();

  while (!this.isComplete) ;
  println("Successfull? " + this.isSuccessful);
}


