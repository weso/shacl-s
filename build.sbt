lazy val scala212 = "2.12.12"
lazy val scala213 = "2.13.3"
lazy val supportedScalaVersions = List(scala212, scala213)

// Local dependencies
lazy val srdfVersion           = "0.1.75"
lazy val utilsVersion          = "0.1.69"

// Dependency versions
lazy val antlrVersion          = "4.7.1"
lazy val catsVersion           = "2.2.0"
lazy val commonsTextVersion    = "1.8"
lazy val circeVersion          = "0.14.0-M1"
lazy val diffsonVersion        = "4.0.0"
// lazy val effVersion            = "4.6.1"
lazy val jenaVersion           = "3.13.1"
lazy val jgraphtVersion        = "1.3.1"
lazy val logbackVersion        = "1.2.3"
lazy val loggingVersion        = "3.9.2"
lazy val rdf4jVersion          = "3.0.0"
lazy val scalacheckVersion     = "1.14.0"
lazy val scalacticVersion      = "3.2.0"
lazy val scalaTestVersion      = "3.2.0"
lazy val scalaGraphVersion     = "1.11.5"
lazy val scalatagsVersion      = "0.6.7"
lazy val scallopVersion        = "3.3.1"
lazy val seleniumVersion       = "2.35.0"
lazy val sextVersion           = "0.2.6"
lazy val typesafeConfigVersion = "1.3.4"
lazy val xercesVersion         = "2.12.0"

// Compiler plugin dependency versions
lazy val simulacrumVersion    = "1.0.0"
// lazy val kindProjectorVersion = "0.9.5"
lazy val scalaMacrosVersion   = "2.1.1"

// Dependency modules
lazy val antlr4            = "org.antlr"                  % "antlr4"               % antlrVersion
lazy val catsCore          = "org.typelevel"              %% "cats-core"           % catsVersion
lazy val catsKernel        = "org.typelevel"              %% "cats-kernel"         % catsVersion
// lazy val catsMacros        = "org.typelevel"              %% "cats-macros"         % catsVersion
lazy val circeCore         = "io.circe"                   %% "circe-core"          % circeVersion
lazy val circeGeneric      = "io.circe"                   %% "circe-generic"       % circeVersion
lazy val circeParser       = "io.circe"                   %% "circe-parser"        % circeVersion
lazy val commonsText       = "org.apache.commons"         %  "commons-text"        % commonsTextVersion
lazy val diffsonCirce      = "org.gnieh"                  %% "diffson-circe"       % diffsonVersion
// lazy val eff               = "org.atnos"                  %% "eff"                 % effVersion
lazy val jgraphtCore       = "org.jgrapht"                % "jgrapht-core"         % jgraphtVersion
lazy val logbackClassic    = "ch.qos.logback"             % "logback-classic"      % logbackVersion
lazy val jenaArq           = "org.apache.jena"            % "jena-arq"             % jenaVersion
lazy val jenaFuseki        = "org.apache.jena"            % "jena-fuseki-main"     % jenaVersion
lazy val rdf4j_runtime     = "org.eclipse.rdf4j"          % "rdf4j-runtime"        % rdf4jVersion
lazy val srdf              = "es.weso"                    %% "srdf"            % srdfVersion
lazy val srdfJena          = "es.weso"                    %% "srdfjena"        % srdfVersion
lazy val srdf4j            = "es.weso"                    %% "srdf4j"          % srdfVersion
lazy val utils             = "es.weso"                    %% "utils"           % utilsVersion
lazy val typing            = "es.weso"                    %% "typing"          % utilsVersion
lazy val validating        = "es.weso"                    %% "validating"      % utilsVersion

lazy val scalaLogging      = "com.typesafe.scala-logging" %% "scala-logging"       % loggingVersion
lazy val scallop           = "org.rogach"                 %% "scallop"             % scallopVersion
lazy val scalactic         = "org.scalactic"              %% "scalactic"           % scalacticVersion
lazy val scalacheck        = "org.scalacheck"             %% "scalacheck"          % scalacheckVersion
lazy val scalaTest         = "org.scalatest"              %% "scalatest"           % scalaTestVersion
lazy val scalatags         = "com.lihaoyi"                %% "scalatags"           % scalatagsVersion
lazy val selenium          = "org.seleniumhq.selenium"    % "selenium-java"        % seleniumVersion
// lazy val htmlUnit          = "org.seleniumhq.selenium"    % "htmlunit-driver"      % seleniumVersion
lazy val sext              = "com.github.nikita-volkov"   % "sext"                 % sextVersion
lazy val typesafeConfig    = "com.typesafe"               % "config"               % typesafeConfigVersion
lazy val xercesImpl        = "xerces"                     % "xercesImpl"           % xercesVersion
lazy val simulacrum        = "org.typelevel" %% "simulacrum"     % simulacrumVersion


lazy val shacl_s = project
  .in(file("."))
  .enablePlugins(ScalaUnidocPlugin, 
    SiteScaladocPlugin, 
    AsciidoctorPlugin, 
    SbtNativePackager, 
    WindowsPlugin, 
    JavaAppPackaging, 
    LauncherJarPlugin)
  .disablePlugins(RevolverPlugin)
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
      logbackClassic,
      scalaLogging,
      scallop,
      typesafeConfig,
    ),
    cancelable in Global      := true,
    fork                      := true,
//    parallelExecution in Test := false,
    crossScalaVersions := supportedScalaVersions,
    publishArtifact := false,
    ThisBuild / turbo := true
  )

lazy val shacl = project
  .in(file("modules/shacl"))
  .disablePlugins(RevolverPlugin)
  .settings(commonSettings, publishSettings)
  .dependsOn()
  .settings(
    logBuffered in Test       := false,
    parallelExecution in Test := false,
    fork in Test              := true,
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Seq(
      typesafeConfig % Test,
      catsCore,
      sext,
      utils,
      typing,
      validating,
      catsKernel,
      // catsMacros, 
      srdf,
      srdf4j % Test,
      srdfJena % Test
      )
  )

lazy val utilsTest = project
  .in(file("modules/utilsTest"))
  .disablePlugins(RevolverPlugin)
  .settings(commonSettings, noPublishSettings)
  .settings(
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Seq(
      circeCore,
      circeGeneric,
      circeParser,
      catsCore,
      catsKernel,
//      catsMacros,
      diffsonCirce,
      xercesImpl,
      commonsText,
      scalaTest
    )
  )

/* ********************************************************
 ******************** Grouped Settings ********************
 **********************************************************/

lazy val noDocProjects = Seq[ProjectReference](
)

lazy val noPublishSettings = Seq(
//  publish := (),
//  publishLocal := (),
  publishArtifact := false,
  licenses        := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))
)

lazy val sharedDependencies = Seq(
  libraryDependencies ++= Seq(
    scalactic,
    scalaTest % Test
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
  // format: off
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions ++= Seq(
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.  "-encoding", "UTF-8",
    "-language:_",
    "-target:jvm-1.8",
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-Xlint",
    "-Yrangepos",
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    // "-Xfatal-warnings",
    "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
//    "-Ymacro-annotations"
  )
  // format: on
)

lazy val wixSettings = Seq(
  wixProductId        := "39b564d5-d381-4282-ada9-87244c76e14b",
  wixProductUpgradeId := "6a710435-9af4-4adb-a597-98d3dd0bade1"
// The same numbers as in the docs?
// wixProductId := "ce07be71-510d-414a-92d4-dff47631848a",
// wixProductUpgradeId := "4552fb0e-e257-4dbd-9ecb-dba9dbacf424"
)

lazy val ghPagesSettings = Seq(
  git.remoteRepo := "git@github.com:labra/shaclex.git"
)

lazy val commonSettings = compilationSettings ++ sharedDependencies ++ Seq(
  organization := "es.weso",
  resolvers ++= Seq(
    Resolver.bintrayRepo("labra", "maven"),
    Resolver.bintrayRepo("weso", "weso-releases"),
    Resolver.sonatypeRepo("snapshots")
  ), 
 // coverageHighlighting := priorTo2_13(scalaVersion.value), 
 // coverageEnabled := priorTo2_13(scalaVersion.value)
)

def antlrSettings(packageName: String) = Seq(
  antlr4GenListener in Antlr4 := true,
  antlr4GenVisitor in Antlr4  := true,
  antlr4Dependency in Antlr4  := antlr4,
  antlr4PackageName in Antlr4 := Some(packageName),
)

lazy val publishSettings = Seq(
  maintainer      := "Jose Emilio Labra Gayo <labra@uniovi.es>",
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
  scalacOptions in doc ++= Seq(
    "-diagrams-debug",
    "-doc-source-url",
    scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath",
    baseDirectory.in(LocalRootProject).value.getAbsolutePath,
    "-diagrams",
  ),
  publishMavenStyle              := true,
  bintrayRepository in bintray   := "weso-releases",
  bintrayOrganization in bintray := Some("weso")
)

def priorTo2_13(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 => true
    case _                              => false
  }
