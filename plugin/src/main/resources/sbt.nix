{ pkgs ? import <nixpkgs> {}, scala ? pkgs.scala_2_11, jdk ? pkgs.openjdk }:
let
  stdenv = pkgs.stdenv;
  self = _self;
  _self = with self; {

  artifact = {org, jarname, version, sha256}: stdenv.mkDerivation (rec {
    name = "${org}.${jarname}.${version}";
    orgpath = stdenv.lib.replaceChars ["."] ["/"] org;
    maven = "${orgpath}/${jarname}/${version}/${jarname}-${version}.jar";
    jar = pkgs.fetchurl { urls = [
      "http://central.maven.org/maven2/${maven}"
      "http://oss.sonatype.org/content/repositories/releases/${maven}"
      "http://oss.sonatype.org/content/repositories/public/${maven}"
      "http://repo.typesafe.com/typesafe/releases/${maven}"
    ]; sha256 = sha256; };
    phases = "installPhase fixupPhase";
    installPhase = ''
      mkdir -p $out/share/java
      ln -s $jar $out/share/java
    '';
  });

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
    src = sources;
    sourceRoot = ".";
    nativeBuildInputs = [ jdk scala ];
    buildInputs = [ pkgs.unzip pkgs.zip ] ++ buildDepends ++ modules;
    # TODO Don't call javac if there are no java files
    buildPhase = ''
      mkdir -p target/classes
      scalac $scalacOptions -d target/classes $(find $sources -name \*.scala -or -name \*.java)
      javac -d target/classes -classpath target/classes $(find $sources -name \*.java) || echo
      . ${<nixpkgs/pkgs/build-support/release/functions.sh>}
      { cd target/classes; jar cfv $pname.jar $(find . -name \*.class); canonicalizeJar $pname.jar; }
    '';
    installPhase = ''
      mkdir -p $out/share/java
      cp $pname.jar $out/share/java
    '';
  });

  # We may want to add more to this later on
  callPackage = pkgs.callPackage;
}; in self
