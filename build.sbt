lazy val scala212 = "2.12.14"
lazy val scala213 = "2.13.5"
lazy val scala3   = "3.0.0-RC2"

lazy val supportedScalaVersions = List(
  scala212, 
  scala213
)

val Java11 = "adopt@1.11"  

// Local dependencies
lazy val srdfVersion           = "0.1.101"
lazy val utilsVersion          = "0.1.94"

// Dependency versions
// lazy val antlrVersion          = "4.7.1"
lazy val catsVersion           = "2.5.0"
lazy val catsEffectVersion     = "3.0.2"

lazy val circeVersion          = "0.14.0"
lazy val jenaVersion           = "3.16.0"
lazy val logbackVersion        = "1.2.3"
lazy val loggingVersion        = "3.9.3"
lazy val munitVersion          = "0.7.22"
lazy val munitEffectVersion    = "0.13.1"

lazy val typesafeConfigVersion = "1.3.4"

lazy val catsCore          = "org.typelevel"              %% "cats-core"           % catsVersion
lazy val catsKernel        = "org.typelevel"              %% "cats-kernel"         % catsVersion
lazy val catsEffect        = "org.typelevel"              %% "cats-effect"         % catsEffectVersion

lazy val circeCore         = "io.circe"                   %% "circe-core"          % circeVersion
lazy val circeGeneric      = "io.circe"                   %% "circe-generic"       % circeVersion
lazy val circeParser       = "io.circe"                   %% "circe-parser"        % circeVersion
lazy val logbackClassic    = "ch.qos.logback"             % "logback-classic"      % logbackVersion
lazy val munit          = "org.scalameta"     %% "munit"           % munitVersion
lazy val munitEffect    = "org.typelevel"     %% "munit-cats-effect-3" % munitEffectVersion
lazy val MUnitFramework = new TestFramework("munit.Framework")

lazy val srdf              = "es.weso"                    %% "srdf"            % srdfVersion
lazy val srdfJena          = "es.weso"                    %% "srdfjena"        % srdfVersion
lazy val srdf4j            = "es.weso"                    %% "srdf4j"          % srdfVersion
lazy val utils             = "es.weso"                    %% "utils"           % utilsVersion
lazy val typing            = "es.weso"                    %% "typing"          % utilsVersion
lazy val validating        = "es.weso"                    %% "validating"      % utilsVersion

lazy val scalaLogging      = "com.typesafe.scala-logging" %% "scala-logging"       % loggingVersion
lazy val typesafeConfig    = "com.typesafe"               % "config"               % typesafeConfigVersion

ThisBuild / githubWorkflowJavaVersions := Seq(Java11)


lazy val shacl_s = project
  .in(file("."))
  .enablePlugins(ScalaUnidocPlugin, 
    SiteScaladocPlugin, 
    AsciidoctorPlugin, 
    SbtNativePackager, 
    WindowsPlugin, 
    JavaAppPackaging, 
    LauncherJarPlugin)
//  .disablePlugins(RevolverPlugin)
  .settings(commonSettings, packagingSettings, publishSettings, ghPagesSettings, wixSettings)
  .aggregate(shacl)
  .dependsOn(shacl)
  .settings(
    siteSubdirName in ScalaUnidoc := "scaladoc/latest",
    addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc),
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(noDocProjects: _*),
    mappings in makeSite ++= Seq(
      file("src/assets/favicon.ico") -> "favicon.ico"
    ),
    libraryDependencies ++= Seq(
   //   logbackClassic,
   //   scalaLogging,
   //   scallop,
      typesafeConfig,
      munit % Test, 
      munitEffect % Test
    ),
    testFrameworks += MUnitFramework,
    cancelable in Global      := true,
    fork                      := true,
    crossScalaVersions := supportedScalaVersions,
    skip in publish := true,
    ThisBuild / turbo := true
  )

lazy val shacl = project
  .in(file("modules/shacl"))
  .settings(commonSettings, publishSettings)
  .dependsOn()
  .settings(
    fork in Test              := true,
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Seq(
      catsCore,
     // sext,
      utils,
      typing,
      validating,
      catsKernel,
      scalaLogging,
      // catsMacros, 
      srdf,
      typesafeConfig % Test,
      srdf4j % Test,
      srdfJena % Test,
      munit % Test, 
      munitEffect % Test
      ),
    testFrameworks += MUnitFramework  
  )

lazy val utilsTest = project
  .in(file("modules/utilsTest"))
//  .disablePlugins(RevolverPlugin)
  .settings(commonSettings, noPublishSettings)
  .settings(
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Seq(
      circeCore,
      circeGeneric,
      circeParser,
      catsCore,
      catsKernel,
    )
  )

/* ********************************************************
 ******************** Grouped Settings ********************
 **********************************************************/

lazy val noDocProjects = Seq[ProjectReference](
)

lazy val noPublishSettings = Seq(
  publishArtifact := false,
  licenses        := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))
)

lazy val sharedDependencies = Seq(
  libraryDependencies ++= Seq(
  )
)

lazy val packagingSettings = Seq(
  mainClass in Compile        := Some("es.weso.shacl-s.Main"),
  mainClass in assembly       := Some("es.weso.shacl-s.Main"),
  test in assembly            := {},
  assemblyJarName in assembly := "shacl-s.jar",
  packageSummary in Linux     := name.value,
  packageSummary in Windows   := name.value,
  packageDescription          := name.value
)

lazy val compilationSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.  "-encoding", "UTF-8",
    "-language:_",
   // "-target:jvm-1.8",
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    // "-Xfatal-warnings",
    "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
//    "-Ymacro-annotations"
  )
  // format: on
)

lazy val wixSettings = Seq()

lazy val ghPagesSettings = Seq(
)

lazy val commonSettings = compilationSettings ++ sharedDependencies ++ Seq(
  organization := "es.weso",
  resolvers ++= Seq(
//    Resolver.githubPackages("weso"),
//    Resolver.sonatypeRepo("snapshots")
  ), 
  coverageHighlighting := true,
//  githubOwner := "weso", 
//  githubRepository := "shacl-s"
)

/*def antlrSettings(packageName: String) = Seq(
  antlr4GenListener in Antlr4 := true,
  antlr4GenVisitor in Antlr4  := true,
  antlr4Dependency in Antlr4  := antlr4,
  antlr4PackageName in Antlr4 := Some(packageName),
)*/

lazy val publishSettings = Seq(
  homepage        := Some(url("https://github.com/weso/shacl-s")),
  licenses        := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  scmInfo         := Some(ScmInfo(url("https://github.com/weso/shacl-s"), "scm:git:git@github.com:weso/shacl-s.git")),
  autoAPIMappings := true,
  apiURL          := Some(url("http://weso.github.io/shacl-s/latest/api/")),
  pomExtra        := <developers>
                       <developer>
                         <id>labra</id>
                         <name>Jose Emilio Labra Gayo</name>
                         <url>https://github.com/labra/</url>
                       </developer>
                     </developers>,
  publishMavenStyle              := true,
)

def priorTo2_13(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 => true
    case _                              => false
  }
