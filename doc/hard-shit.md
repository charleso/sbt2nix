Hard Shit
=========

Some things that may be a step-backward coming from the Maven/SBT in this Nix world.

- [OS-dependent jars](https://github.com/charleso/sbt2nix/issues/6). This is the nature of the beast, but perhaps there
  is a way to circumvent this in Nix?
- If/when people start using Nix the biggest problem I foresee is that maintaining multiple projects is going to suck.
  We have a few options:
  - Maintain a Nix expression for every library we care about in a single place (ala [nixpkgs](https://github.com/NixOS/nixpkgs))
  - Use Git submodules or some form of source-dependency tool to checkout _all_ of the required repositories first, and
    build from there. Not ideal from a consumers perspective.
  - Resurrect and implement a solution using [recursive Nix](https://github.com/NixOS/nix/pull/213). I still don't know
    if this will actually work, but it would be awesome if it did.
