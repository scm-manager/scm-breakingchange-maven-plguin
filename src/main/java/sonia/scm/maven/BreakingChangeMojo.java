package sonia.scm.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import java.io.File;

public abstract class BreakingChangeMojo extends AbstractMojo {

  @Component
  protected MavenProject project;

  @Parameter(defaultValue = "${localRepository}")
  private ArtifactRepository localRepository;

  @Component
  private ArtifactResolver artifactResolver;

  @Component
  protected RepositorySystem system;

  protected File resolve(Artifact artifact) {
    ArtifactResolutionRequest request = new ArtifactResolutionRequest();

    request.setArtifact(artifact);
    request.setRemoteRepositories(project.getRemoteArtifactRepositories());
    request.setLocalRepository(localRepository);

    artifactResolver.resolve(request);

    return artifact.getFile();
  }
}
