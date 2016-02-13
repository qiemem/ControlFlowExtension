Examples:

```
cf:when
cf:case [ x > y ] [ print "x is bigger than y!" ]
cf:case [ x < y ] [ print "x is less than y!" ]
cf:else [ print "I don't know what's going on..." ]
```

```

cf:match x
cf:case [ ? > 7 ] [ print "x is greater than 7!" ]
cf:case [ ? < 3 ] [ print "x is less than 3!" ]
cf:else [ print "What's an x?" ]
```

```
cf:match x
cf:case-is = 3 [ print "x is 3!" ]
cf:case-is = 8 [ print "x is 8!" ]
cf:case-is = turtle 0 [ print "x is turtle 0 for some reason" ]
cf:else [ print "poor x" ]
```

```
cf:match x
cf:case is-string? [ print "x is a string!" ]
cf:case is-number? [ print "x is a number!" ]
cf:case is-turtle? [ print "x is a turtle!" ]
cf:else [ print "x is some kind of alien." ]
```

```
let comparison cf:select
cf:case [ x < y ] [ -1 ]
cf:case [ x > y ] [  1 ]
cf:else           [  0 ]
```

```
let description x cf:matching
cf:case-is > 3 [ "greater than 3!" ]
cf:case [ ? mod 2 = 0 ] [ "at most 3, but even!" ]
cf:else [ "something else!" ]
```
