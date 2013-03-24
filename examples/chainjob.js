//
// This example runs an initial map-reduce job to generate the top 10
// daily wind speed maximums (see windspeed.js).  The results are written
// to temporary sequence files.  It then runs a second map-reduce
// job to sort these results using Hadoop's identity reducer.
// 
// This file is part of Eggshell.
// Copyright 2013 George Magiros
// Distributed under the terms of the GNU GPL


function map (key, value) {
  var usaf = value.substring(0, 6),
      wban = value.substring(7, 12),
      month = value.substring(18, 20),
      mxspd = value.substring(88, 93);
  if (mxspd !== "999.9")
    return [ [ Number(month), usaf, wban ], Number(mxspd), 1 ]
}

function setup () {
  var line, field, value, state;
  function trim (s) { return s.substring(1,s.length-1); }
  station = {};  // saved to the global scope
  open("ish-history.csv");
  while ((line = readln()) !== null) {
    field = line.split(",");
    key = trim(field[0]) + trim(field[1]);
    value = trim(field[2]);
    state = trim(field[5]);
    if (state !== '') value += " - " + state;
    value += " [" + trim(field[3]) + "]";
    station[key] = value;
  }
  close();
}

function reduce (key, values) {
  var k, v, sum = 0, list = [];
  while (values.hasNext()) {
    v = values.next();
    sum += v[1];
    list.push(v[0]);
  }
  list.sort(function (a, b) { return a > b ? -1 : (a === b ? 0 : 1) });
  k = [ key[0] ];
  k = k.concat(list.splice(0,10));
  id = key[1] + key[2];
  return [ k, sum, station[id] ];
}

function eggshell (input, output) {
  job = this;

  job
  .input(input)
  .output("temp")
  .name(job.getName() + " (1/2)")
  .map(map)
  .reduce({ 
         setup:setup,
         reduce:reduce, 
       })
  .sequenceFileOutputFormat()
  .waitForCompletion(false);

  stderr.println("-------------------------------------");

  job = new Egg();

  job
  .input("temp")
  .output(output)
  .name(job.getName() + " (2/2)")
  .sequenceFileInputFormat()
  .map(function (key, value) { 
    return [ key, value ];
  })
  .numReduceTasks(1)
  .waitForCompletion(false);
}

