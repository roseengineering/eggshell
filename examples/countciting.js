//
// Count the number of citing patents for a given cited parent using
// the NBER patent data.  This example expects cite75_99.txt (without
// the first line) as the input.
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
      field = value.split(",");
      return [ field[1], field[0] ];
    })
  .reduce(
    function (key, values) {
      var count = 0;
      while (values.hasNext()) {
        count += 1;
        values.next();
      }
      return [ key, count ];
    })
  .waitForCompletion(true);
}

