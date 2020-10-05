package sonia.scm.maven;

import com.google.common.base.Joiner;
import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.exception.JApiCmpException;
import japicmp.model.JApiClass;
import japicmp.model.JApiCompatibilityChange;
import japicmp.model.JApiSemanticVersionLevel;
import japicmp.output.incompatible.IncompatibleErrorOutput;
import japicmp.util.Optional;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.List;

@Mojo(name = "core-api", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class CoreApiMojo extends BreakingChangeMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    JApiCmpArchive baseArchive = createBaseArchive();
    JApiCmpArchive archive = createArchive();
    try {
      check(baseArchive, archive);
    } catch (DependencyResolutionRequiredException e) {
      throw new MojoExecutionException("failed to resolve dependencies", e);
    }
  }

  private JApiCmpArchive createBaseArchive() {
    String version = Versions.findBaseVersion(project.getVersion());
    File baseCoreApiJar = resolveCoreApi(version);
    return new JApiCmpArchive(baseCoreApiJar, version);
  }

  private JApiCmpArchive createArchive() {
    Artifact artifact = project.getArtifact();
    File coreApiJar = artifact.getFile();
    return new JApiCmpArchive(coreApiJar, project.getVersion());
  }

  private void check(JApiCmpArchive baseArchive, JApiCmpArchive archive) throws MojoFailureException, MojoExecutionException, DependencyResolutionRequiredException {
    getLog().info("compare scm-core of " + archive.getVersion().getStringVersion() + " with " + baseArchive.getVersion().getStringVersion());
    String classpath = Joiner.on(File.pathSeparator).join(project.getRuntimeClasspathElements());

    Options options = Options.newDefault();
    options.setErrorOnSemanticIncompatibility(false);

    Optional<String> optionalClasspath = Optional.of(classpath);
    options.setNewClassPath(optionalClasspath);
    // TODO set real classpath to catch breaking changes in transitive dependencies
    options.setOldClassPath(optionalClasspath);
    options.setIgnoreMissingClasses(true);
    options.setErrorOnBinaryIncompatibility(true);

    JarArchiveComparatorOptions comparatorOptions = JarArchiveComparatorOptions.of(options);
    comparatorOptions.addOverrideCompatibilityChange(new JarArchiveComparatorOptions.OverrideCompatibilityChange(
            JApiCompatibilityChange.METHOD_NEW_DEFAULT,
            true,
            true,
            JApiSemanticVersionLevel.PATCH
    ));
    JarArchiveComparator jarArchiveComparator = new JarArchiveComparator(comparatorOptions);
    List<JApiClass> jApiClasses = jarArchiveComparator.compare(baseArchive, archive);

    IncompatibleErrorOutput errorOutput = new IncompatibleErrorOutput(options, jApiClasses, jarArchiveComparator) {
      @Override
      protected void warn(String msg, Throwable error) {
        getLog().warn(msg, error);
      }
    };

    try {
      errorOutput.generate();
    } catch (JApiCmpException e) {
      if (e.getReason() == JApiCmpException.Reason.IncompatibleChange) {
        throw new MojoFailureException(e.getMessage());
      } else {
        throw new MojoExecutionException("Error while checking for incompatible changes", e);
      }
    }

  }

  private File resolveCoreApi(String version) {
    Artifact artifact = system.createArtifact("sonia.scm", "scm-core", version, "", "jar");
    return resolve(artifact);
  }

}
