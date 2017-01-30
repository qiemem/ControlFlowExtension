
# NetLogo ControlFlow extension

This NetLogo extension adds somewhat experimental control-flow primitives. This extension is a place where we can try out new control-flow structures before possibly moving them into core NetLogo.

## Building

Run `sbt package`.

If compilation succeeds, `cf.jar` will be created. This file should then be placed in a folder named `cf` in your NetLogo `extensions` directory.

## Using

The CF extension currently includes primitives that allow you to do things similar to `if`-`else if`-`else` chains you see in other languages, as well as things similar to `switch`es in other languages. However, it does it in a more flexible way than many languages. A few quick examples to get you started:

```
let x 5
let y 7
cf:when
cf:case [ x > y ] [ print "x is bigger than y!" ]
cf:case [ x < y ] [ print "x is less than y!" ]
cf:else           [ print "x is the same as y!" ]
```

```
let my-awesome-number 5
cf:match my-awesome-number
cf:case [ [n] -> n > 7 ] [ print "The number is greater than 7!" ]
cf:case [ [n] -> n < 3 ] [ print "The number is less than 3!" ]
cf:else                  [ print "The number is somewhere in between 3 and 7!" ]
```

## Cases

Central to this extension is the concept of a case. A case is simply a list of two elements, where the first element is a reporter and the second element is either a reporter or a command. The first element, called the condition, *must* report either `true` or `false`. The second argument is called the consequent. If you're curious about how CF works, keep reading this section. Otherwise, feel free to skip to the list of primitives; you don't need to understand the internals to start using CF.

Almost all primitives in the CF extension take a list of cases as an argument. Typically, they go through the cases, looking for a true condition. When they find one, they then run the consequent. Although CF provides primitives that make constructing a list of cases easy, you could actually just build such a list with primitives already in NetLogo, like so:

```
let x 5
let y 7
let list-of-cases (list
  (list task [ x > y ] task [ print "x is greater than y!" ])
  (list task [ x < y ] task [ print "x is less than y!" ])
  (list task [ true  ] task [ print "x is the same as y!" ])
)
```

However, this is not very nice looking. With CF, you can write it like this instead:

```
let x 5
let y 7

let list-of-cases
cf:case [ x > y ] [ print "x is greater than y!" ]
cf:case [ x < y ] [ print "x is less than y!" ]
cf:else           [ print "x is the same as y!" ]
```


## Primitives


### `cf:when`

```NetLogo
cf:when list-of-cases
```


Runs the command task from the first case in the list with a true condition. For instance:

```NetLogo
let x 3
cf:when
cf:case [ x < 2 ] [ print "x is less than 2!" ]
cf:case [ x < 4 ] [ print "x is less than 4!" ]
cf:case [ x < 6 ] [ print "x is less than 6!" ]
cf:else           [ print "x is greater than or equal to 6!" ]
```

The above code will print out `x is less than 4!` since that's the first case with a true condition.

If no true case is found, and no `cf:else` given, `cf:when` will error with a suggestion for a fix.


### `cf:select`

```NetLogo
cf:select list-of-cases
```


Picks the first case in the list with a true condition and reports the result of its consequent. The consequents of the cases in a `cf:select` must be reporter tasks. Thus, `cf:select` is exactly like `cf:when`, except that it reports the value from the true case, rather than just running it. For example:


```NetLogo
let x 3
print cf:select
cf:case [ x < 2 ] [ "x is less than 2!" ]
cf:case [ x < 4 ] [ "x is less than 4!" ]
cf:case [ x < 6 ] [ "x is less than 6!" ]
cf:else           [ "x is greater than or equal to 6!" ]
```

The above code will print out `x is less than 4!` since that's the first case with a true condition.

If no true case is found, and no `cf:else` given, `cf:select` will error with a suggestion for a fix.


### `cf:match`

```NetLogo
cf:match value list-of-cases
```


`cf:match` is like `cf:when`, except that it applies the conditions in its cases to the given value. For instance:

```NetLogo
ask patch 0 0 [ set pcolor red ]
cf:match ([ pcolor ] of patch 0 0)
cf:case [ [c] -> c = green ] [ print "The center patch is green!" ]
cf:case [ [c] -> c = red  ] [ print "The center patch is red!" ]
cf:case [ [c] -> c = blue  ] [ print "The center patch is blue!" ]
cf:else           [ print "I don't know what color the center patch is!" ]
```

The above code will print out `The center patch is green!` since that's the first case with a true condition.

The value is also passed to the consequent of the case. For instance:

```NetLogo
cf:match one-of turtles
cf:case [ [t] -> [color] of t = red  ] [ [t] -> ask t [ show "I'm red!" ] ]
cf:case [ [t] -> [color] of t = blue ] [ [t] -> ask t [ show "I'm blue!" ] ]
cf:else                         [ [t] -> ask t [ show "I'm some other color!" ] ]
```

If no matching case is found, `cf:match` will error with a suggestion for a fix.


### `cf:matching`

```NetLogo
cf:matching value list-of-cases
```


`cf:matching` is like `cf:match`, except that it reports the result of the matching case. `cf:matching` is to `cf:match` as `cf:select` is to `cf:when`. For instance:

```NetLogo
let my-awesome-number 3
print my-awesome-number cf:matching
cf:case [ [num] -> num < 2 ] [ "The number is less than 2!" ]
cf:case [ [num] -> num < 4 ] [ "The number is less than 4!" ]
cf:case [ [num] -> num < 6 ] [ "The number is less than 6!" ]
cf:else           [ "The number is greater than or equal to 6!" ]
```

The above code will print out `The number is less than 4!` since that's the first case with a true condition.
`cf:matching` also applies the consequent of the matching case to the given value, just like `cf:match`:

```NetLogo
print (one-of turtles) cf:matching
cf:case [ [t] -> [color] of t = red  ] [ [t] -> [ "I'm red!" ] of t ]
cf:case [ [t] -> [color] of t = blue ] [ [t] -> [ "I'm blue!" ] of t ]
cf:else                                [ [t] -> [ "I'm some other color!" ] of t ]
```

If no matching case is found, `cf:matching` will error with a suggestion for a fix.


### `cf:case`

```NetLogo
cf:case condition consequent list-of-remaining-cases
```


`cf:case` allows you to construct a list of cases that the other primitives will then pick from. It constructs a new case from the the two given tasks and adds it to the front of the list of remaining cases. Thus, you can chain it together with other instances of `cf:case` to create an arbitrarily long list of cases.

Note that because the condition in a case is just an anonymous reporter, you can check for many common conditions in a very concise manner. For instance, if we want to do something depending on the breed of a turtle, you can do:

```NetLogo
cf:match my-turtle
cf:case is-wolf? [ show "Growl!" ]
cf:case is-a-sheep? [ show "Baah!" ]
cf:case is-dog? [ show "Bark!" ]
cf:case is-cat? [ show "Meow!" ]
cf:else [ show "I'm not sure what sound to make..." ]
```


### `cf:case-is`

```NetLogo
cf:case-is relationship consequent list-of-remaining-cases
```


`cf:case-is` allows you to write some common uses of `cf:case` in `cf:match` or `cf:matching` in a more concise, readable way. The given reporter should be a relationship such as `=`, `<`, or `member?`. `cf:case-is` then fills in the second argument of the reporter with the given value. This is much easier to understand in an example:

```
let x 5
print x cf:matching
cf:case-is = 0 [ "x is 0!" ]
cf:case-is = 1 [ "x is 1!" ]
cf:case-is > 2 [ "x is greater than 2!" ]
cf:case-is member? [ -1 -2 -3] [ "x is either -1, -2, or -3" ]
cf:else [ "x is something else" ]
```

Thus, `cf:case-is` allows you to do something quite similar to `switch` in some other languages, but is also much more flexible.


### `cf:else`

```NetLogo
cf:else command/reporter
```


`cf:else` creates a case where the condition is always true. Thus, it allows you to create a case that will be run if all the other cases fail. You should almost always finish up a chain of cases with `cf:else`. However, if you'd prefer to error rather than have a default case, you can replace `cf:else` with `[]`, like so:

```NetLogo
let x -5
cf:when
cf:case [ 0 < x and x < 10 ] [ print "x is between 0 and 10!" ]
cf:case [ x < 100 ] [ print "x is less than 100!" ]
[]
```

The above code will error, since no matching case will be found.

