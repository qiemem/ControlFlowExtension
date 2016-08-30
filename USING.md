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

