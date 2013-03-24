//
// Generate an inverted citation of the NBER patent data.  This
// example expects cite75_99.txt (without the first line) as the
// input.
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
      var v = "";
      while (values.hasNext()) {
        if (v.length > 0) v += ",";
        v += values.next();
      }
      return [ key, v ];
    })
  .waitForCompletion(true);
}

