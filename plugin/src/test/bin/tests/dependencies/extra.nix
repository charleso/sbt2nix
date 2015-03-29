{ sbt ? import ./sbt.nix {} }:
{

commons-lang_commons-lang_2_6 = sbt.artifact {
  org = "commons-lang";
  jarname = "commons-lang";
  version = "2.6";
  sha256 = "177llblvmkzhq1hqcr2g0ksrr4lgs93kyii4dzar9hkpz04ipwah";
};

}