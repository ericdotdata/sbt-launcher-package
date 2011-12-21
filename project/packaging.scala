import sbt._
import com.typesafe.packager.Keys._
import sbt.Keys._
import com.typesafe.packager.PackagerPlugin._

object Packaging {

  val sbtLaunchJarUrl = SettingKey[String]("sbt-launch-jar-url")
  val sbtLaunchJarLocation = SettingKey[File]("sbt-launch-jar-location")  
  val sbtLaunchJar = TaskKey[File]("sbt-launch-jar", "Resolves SBT launch jar")
  
  val settings: Seq[Setting[_]] = packagerSettings ++ Seq(
    sbtLaunchJarUrl <<= sbtVersion apply ("http://typesafe.artifactoryonline.com/typesafe/ivy-releases/org.scala-tools.sbt/sbt-launch/"+_+"/sbt-launch.jar"),
    sbtLaunchJarLocation <<= target apply (_ / "sbt-launch.jar"),
    sbtLaunchJar <<= (sbtLaunchJarUrl, sbtLaunchJarLocation) map { (uri, file) =>
      import dispatch._
      if(!file.exists) {
         val writer = new java.io.BufferedOutputStream(new java.io.FileOutputStream(file))
         try Http(url(uri) >>> writer)
         finally writer.close()
      }
      // TODO - GPG Trust validation.
      file
    },
    // GENERAL LINUX PACKAGING STUFFS
    maintainer := "Josh Suereth <joshua.suereth@typesafe.com>",
    packageDescription := """Simple Build Tool
 This script provides a native way to run the Simple Build Tool,
 a build tool for Scala software, also called SBT.""",
    linuxPackageMappings <+= (baseDirectory) map { bd =>
      (packageMapping((bd / "sbt") -> "/usr/bin/sbt")
       withUser "root" withGroup "root" withPerms "0755")
    },
    linuxPackageMappings <+= (sourceDirectory) map { bd =>
      (packageMapping(
        (bd / "linux" / "usr/share/man/man1/sbt.1") -> "/usr/share/man/man1/sbt.1.gz"
      ) withPerms "0644" gzipped) asDocs()
    },
    linuxPackageMappings <+= (sourceDirectory in Linux) map { bd =>
      packageMapping(
        (bd / "usr/share/doc/sbt/copyright") -> "/usr/share/doc/sbt/copyright"
      ) withPerms "0644" asDocs()
    },   
    linuxPackageMappings <+= (sourceDirectory in Linux) map { bd =>
      packageMapping(
        (bd / "usr/share/doc/sbt") -> "/usr/share/doc/sbt"
      ) asDocs()
    },
    linuxPackageMappings <+= (sourceDirectory in Linux) map { bd =>
      packageMapping(
        (bd / "etc/sbt") -> "/etc/sbt"
      ) withConfig()
    },
    linuxPackageMappings <+= (sourceDirectory in Linux) map { bd =>
      packageMapping(
        (bd / "etc/sbt/sbtopts") -> "/etc/sbt/sbtopts"
      ) withPerms "0644" withConfig()
    },
    linuxPackageMappings <+= (sbtLaunchJar, sourceDirectory in Linux, sbtVersion) map { (jar, dir, v) =>
      packageMapping(dir -> "/usr/lib/sbt",
                     dir -> ("/usr/lib/sbt/" + v),
                     jar -> ("/usr/lib/sbt/"+v+"/sbt-launch.jar")) withPerms "0755"
    },
    // DEBIAN SPECIFIC    
    name in Debian := "sbt",
    version in Debian <<= (version, sbtVersion) apply { (v, sv) =>       
      sv + "-build-" + (v split "\\." map (_.toInt) dropWhile (_ == 0) map ("%02d" format _) mkString "")
    },
    debianPackageDependencies in Debian ++= Seq("curl", "java2-runtime", "bash (>= 2.05a-11)"),
    debianPackageRecommends in Debian += "git",
    linuxPackageMappings in Debian <+= (sourceDirectory) map { bd =>
      (packageMapping(
        (bd / "debian/changelog") -> "/usr/share/doc/sbt/changelog.gz"
      ) withUser "root" withGroup "root" withPerms "0644" gzipped) asDocs()
    },
    
    // RPM SPECIFIC
    name in Rpm := "sbt",
    version in Rpm <<= sbtVersion.identity,
    rpmRelease := "1",
    rpmVendor := "typesafe",
    rpmUrl := Some("http://github.com/paulp/sbt-extras"),
    rpmSummary := Some("Simple Build Tool for Scala-driven builds."),
    rpmLicense := Some("BSD")   
  )
}