{ pkgs ? import <nixpkgs> {}, scala ? pkgs.scala_2_11 }:
let
  stdenv = pkgs.stdenv;
  self = _self;
  _self = with self; {

  artifact = {org, name, version, file, sha256}: stdenv.mkDerivation {
    name = "${org}.${name}.${version}";
    phases = "installPhase fixupPhase";
    jar = pkgs.fetchurl {
      url = file;
      sha256 = sha256;
    };
    installPhase = ''
      mkdir -p $out/share/java
      ln -s $jar $out/share/java
    '';
  };

  mkDerivation = args@{
      pname,
      version,
      scalacOptions ? "",
      sources ? [],
      modules ? [],
      buildDepends ? [],
      ...
    }: stdenv.mkDerivation(args // {
    name = "${pname}";
    nativeBuildInputs = [ pkgs.openjdk scala ];
    buildInputs = buildDepends ++ modules;
    # TODO Don't call javac if there are no java files
    buildPhase = ''
      mkdir -p target/classes
      scalac $scalacOptions -d target/classes $(find $sources -name \*.scala -or -name \*.java)
      javac -d target/classes -classpath target/classes $(find $sources -name \*.java) || echo
      { cd target/classes; jar cfv $pname.jar $(find . -name \*.class); }
    '';
    installPhase = ''
      mkdir -p $out/share/java
      cp $pname.jar $out/share/java
    '';
  });

  # We may want to add more to this later on
  callPackage = pkgs.callPackage;
}; in self
