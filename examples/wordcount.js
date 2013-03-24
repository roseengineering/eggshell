//
// Generate a word count from the input and write the results to the
// output.
//
// This file is part of Eggshell.
// Copyright 2013 George Magiros
// Distributed under the terms of the GNU GPL

function eggshell (input, output) {
  this
  .input(input)
  .output(output)
  .map(
    function (key, value) {
      var word = value.split(" ");
      for (var i = 0; i < word.length; i += 1) {
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
