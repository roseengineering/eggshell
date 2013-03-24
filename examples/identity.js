//
// Run the input through identity mappers and identity reducers (if
// the number of reducers is set to greater than zero).  The third and last 
// command line argument sets the number of reducers.
//
// This file is part of Eggshell.
// Copyright 2013 George Magiros
// Distributed under the terms of the GNU GPL


identityreducer = function (key, values) {
  var v;
  while (values.hasNext()) {
    v = values.next();
    yield [ key, v ];
  }
};

identitymapper = function (key, value) { 
  return [ key, value ];
};

function eggshell (input, output, num) {
  this
  .input(input)
  .output(output)
  .map(identitymapper)
  .reduce(identityreducer)
  .numReduceTasks(num)
  .waitForCompletion(false);
}

