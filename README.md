sbt2nix
=======

Generate Nix build instructions from a SBT project

[![Build Status](https://travis-ci.org/charleso/sbt2nix.png)](https://travis-ci.org/charleso/sbt2nix)

## Warning

This is (currently) an *experimental* project to force myself (and hopefully others) to get cracking on building a set
of tools for generating and consuming Nix expressions for Scala (and Java by extension, but who cares).
It _shouldn't_ be that hard (in theory).

## SBT Plugin - Experimental

### Running

Firstly build and publish this project with the usual `sbt publishLocal`.
Then add the following to your `project/plugins.sbt`:

    addSbtPlugin("sbt2nix" % "sbt2nix" % "0.1.0-SNAPSHOT")

Run the following to generate a series of `nix` files for each module (see below).

    $ sbt nix

Once that's done, to build everything with `nix`:

    $ nix-build

Or specific modules:

    $ nix-build -A foo

The result should be a jar in `result/share/java/$name.jar`.
Ideally we would like to symlink the dependent jars and add an SBT resolver to enable `nix-shell` to bootstrap `sbt`.

### Output

In the top-level aggregate project (if you have one) there will be a `default.nix`:

```nix
{ sbt ? import ./sbt.nix {}, deps ? import ./deps.nix { inherit sbt; } }:
{
  foo = sbt.callPackage ./my-foo {};
  bar = sbt.callPackage ./my-bar {};
}
```

In each module there is a `default.nix`:

```nix
{ sbt ? import ../sbt.nix {}, deps ? import ../deps.nix { inherit sbt; } }:
let
bar = import ../bar { inherit sbt; };

in sbt.mkDerivation {
  pname = "my-foo";
  version = "0.1.0-SNAPSHOT";
  src = ./.;
  sources = [ ./src/main/scala ];
  modules = [ bar ];
  scalacOptions = "";
  buildDepends = [
    deps.commons-io_commons-io_2_4
  ];
  meta = {
    description = "my-foo";
  };
}
```

And lastly a single `deps.nix` will be generated:

```nix
{ sbt }:
{
  commons-io_commons-io_2_4 = sbt.artifact {
    org = "commons-io";
    jarname = "commons-io";
    version = "2.4";
    sha256 = "108mw2v8ncig29kjvzh8wi76plr01f4x5l3b1929xk5a7vf42snc";
  };
}
```

Also, a utility `sbt.nix` will be extracted from the plugin and placed in the root.

## Why Nix?

> Build tools should use a build cache, emphasis on the cache, for propagating results from one component to another.
> A cache is an abstraction that allows computing a function more quickly based on partial results computed in the past.
> The function, in this case, is for turning source code into a binary.

> A cache does nothing except speed things up. You could remove a cache entirely and the surrounding system would work
> the same, just more slowly. A cache has no side effects, either. No matter what you've done with a cache in the past,
> a given query to the cache will give back the same value to the same query in the future.

From Lex Spoon's [Recursive Maven Considered Harmful](http://blog.lexspoon.org/2012/12/recursive-maven-considered-harmful.html),
which I discovered while reading the excellent [Ultimate Build Tool](http://blog.ltgt.net/in-quest-of-the-ultimate-build-tool/).
This is a good post on using [Nix for Haskell development](https://ocharles.org.uk/blog/posts/2014-02-04-how-i-develop-with-nixos.html).
And finally my own little [rant](https://bitbucket.org/cofarrell/one-build-tool/src/master/README.md).

## Help wanted

We need _you_! Come and chat on Freenode `##nix-sbt` if you want to know more.

See the list of [Issues](https://github.com/charleso/sbt2nix/issues) for an overview on what needs to happen.

The current code is something hacked together in roughly a day, so please don't judge it too harshly.
The important thing at the moment is the generated Nix output, which will need to be improved in a number of ways,
as outlined in various issues.
If you have any experience in Nix, your input and suggestions on what we needs to happen will be much appreciated.
