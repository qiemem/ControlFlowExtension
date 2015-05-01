Examples:

```
(cf:cond
  cf:case [ x > y ] [ print "x is bigger than y!" ]
  cf:case [ x < y ] [ print "x is less than y!" ]
  cf:else [ print "I don't know what's going on..." ])
```

```
(cf:match x
  cf:case [ ? > 7 ] [ print "x is greater than 7!" ]
  cf:case [ ? < 3 ] [ print "x is less than 3!" ]
  cf:else [ print "What's an x?" ])
```

```
(cf:match x
  cf:= 3 [ print "x is 3!" ]
  cf:= 8 [ print "x is 8!" ]
  cf:= turtle 0 [ print "x is turtle 0 for some reason" ]
  cf:else [ print "poor x" ])
```

```
(cf:match x
  cf:case is-string? [ print "x is a string!" ]
  cf:case is-number? [ print "x is a number!" ]
  cf:case is-turtle? [ print "x is a turtle!" ]
  cf:else [ print "x is some kind of alien." ])
```

`cf:cond-value` and `cf:match-value` do the same thing, but return stuff like `ifelse-value`.

