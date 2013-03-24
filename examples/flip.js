//
// Flips the key and value from a word count job.  All the counts are
// converted to a number type for sorting purposes.  The output as a
// result will list the words in order of increasing frequency.
//
// This file is part of Eggshell.
// Copyright 2013 George Magiros
// Distributed under the terms of the GNU GPL

function eggshell (input, output, num) {
  this
  .input(input)
  .output(output)
  .keyValueTextInputFormat()
  .map(function (key, value) { 
     return [ +value, key ];
  })
  .numReduceTasks(1)
  .waitForCompletion(false);
}

