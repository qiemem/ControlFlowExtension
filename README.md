
# NetLogo ControlFlow extension

This NetLogo extension adds somewhat experimental control-flow primitives. Currently, this extension contains a preview of the future version of the NetLogo primitives `ifelse` and `ifelse-value` that allow more than two branches.

## Building

Run `sbt package`.

If compilation succeeds, `cf.jar` will be created. This file should then be placed in a folder named `cf` in your NetLogo `extensions` directory.

## Primitives


### `cf:ifelse`

```NetLogo
cf:ifelse condition consequent conditions/consequents optional-else
```


Runs the first command block following a true condition:

```NetLogo
let x 3
(cf:ifelse
  x < 2 [ print "x is less than 2!" ]
  x < 4 [ print "x is less than 4!" ]
  x < 6 [ print "x is less than 6!" ]
        [ print "x is greater than or equal to 6!" ])
```

The above code will print out `x is less than 4!` since that's the first case with a true condition.

A final command block without a matching condition may be provided, in which case it will run if no other command blocks do. If no such command block is provided and no conditions are true, nothing will happen.

The default number of arguments is 3, so that if you only have one condition, a consequent, and an else block (like a regular NetLogo `ifelse`), you do not need parentheses:

```NetLogo
cf:ifelse 0 < 1 [ print "hi" ] [ print "bye" ]
```



### `cf:ifelse-value`

```NetLogo
cf:ifelse-value condition consequent conditions/consequents else-reporter
```


Runs the first reporter following a true condition and reports its value:

```NetLogo
(cf:ifelse-value
  x < 2 [ "x is less than 2!" ]
  x < 4 [ "x is less than 4!" ]
  x < 6 [ "x is less than 6!" ]
        [ "x is greater than or equal to 6!" ])
```

The above code will report `x is less than 4!` since that's the first case with a true condition.

Unlike `cf:ifelse`, the else-block is required in `cf:ifelse-value`. If no condition is true, the result of the else block will be reported.

Note that `cf:ifelse-value` has somewhat different associativity than NetLogo's `ifelse`, making it so that you don't need to put parentheses around the conditions, as in the above example.

The default number of arguments is 3, so that if you only have one condition, a consequent, and an else block (like a regular NetLogo `ifelse-value`), you do not need parentheses:

```NetLogo
cf:ifelse-value 0 < 1 [ "hi" ] [ "bye" ]
```


