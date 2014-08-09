sbt2nix
=======

Generate Nix build instructions from a SBT project

## Warning

This is (currently) and empty/stub project to force myself (and hopefully) others to get cracking on building a set of tools for generating and consuming Nix expressions for Scala (and Java by extension, but who cares). It _shouldn't_ be that hard (in theory).

## Why Nix?

> Build tools should use a build cache, emphasis on the cache, for propagating results from one component to another. A cache is an abstraction that allows computing a function more quickly based on partial results computed in the past. The function, in this case, is for turning source code into a binary. 
> A cache does nothing except speed things up. You could remove a cache entirely and the surrounding system would work the same, just more slowly. A cache has no side effects, either. No matter what you've done with a cache in the past, a given query to the cache will give back the same value to the same query in the future.

From Lex Spoon's [Recursive Maven Considered Harmful.](http://blog.lexspoon.org/2012/12/recursive-maven-considered-harmful.html), which I discovered while reading the excellent [Ultimate Build Tool](http://blog.ltgt.net/in-quest-of-the-ultimate-build-tool/). And finally my own little [rant](https://bitbucket.org/cofarrell/one-build-tool/src/master/README.md).

## Help wanted

We need _you_!

See the list of [Issues](https://github.com/charleso/sbt2nix/issues) for where to start.

Please contact me if you're interested. As a word of warning I'm quite lazy and this repository may remain empty for some time/forever.
