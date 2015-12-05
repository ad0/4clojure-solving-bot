A 4clojure automatic solver
===========================

[4clojure](http://www.4clojure.com/) is a really nice platform to experiment with the clojure language and to try to solve small exercises but test cases are not always well-chosen.

Warning!
--------

I take no responsibility whatsoever for those who cheat on this really good problems platform!

Moreover, be aware that testing this bot on a 4clojure account will lead to overwriting existing solutions...

How to use?
-----------

The only step is to put login/password at the beginning of the main clj source file.
Then, simply:

    lein run

How it works?
-------------

When looking at some test cases for exercise validation, I thought "hey, what prevents me to just give a function that returns the expected answer for given parameters?" Well actually, for many exercises, nothing.

Since I don't want to cheat, I wrote a bot that can do the dirty tasks: it requests for 4clojure problems and try to solve them automatically, by building functions that have the following forms :

    (fn [arg1 arg2]
      (cond
        (and (= arg1 *param1*) (= arg2 *param2*)) *expected*
        (and (= arg1 *param1*) (= arg2 *param2*)) *expected*
        ...
      )
    )

Results
-------

At the time of writing this, the bot solved **92** of **156** problems, with the following distribution by difficulty:

![ditribution](https://raw.githubusercontent.com/ad0/4clojure-solving-bot/master/resources/4clojurebot-results.png)

I guess that more patterns can be learned to solve more exercises (especially by exploiting parsing power of lisp-like langages like clojure), but, well, the PoC is here.

I understand that there's no point of cheating on this kind of challenges site. The goal is to learn the clojure langage. Anyway, I wrote this bot using clojure!
