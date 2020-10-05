package sonia.scm.maven;

import com.qdesrame.openapi.diff.core.OpenApiCompare;
import com.qdesrame.openapi.diff.core.model.ChangedOpenApi;
import com.qdesrame.openapi.diff.core.output.ConsoleRender;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;

@Mojo(name = "openapi-webapp", defaultPhase = LifecyclePhase.VERIFY)
public class OpenApiWebAppMojo extends BreakingChangeMojo {

  private static final String OPENAPI_SPEC = "WEB-INF/classes/META-INF/scm/openapi.json";

  @Override
  public void execute() throws MojoFailureException {
    String baseVersion = Versions.findBaseVersion(project.getVersion());
    getLog().info("check for breaking openapi changes between " + baseVersion + " and " + project.getVersion());

    String oldContent = resolveOldContent(baseVersion);
    String newContent = resolveNewContent();

    ChangedOpenApi openApi = OpenApiCompare.fromContents(oldContent, newContent);
    if (!openApi.isCompatible()) {
      ConsoleRender consoleRender = new ConsoleRender();
      String output = consoleRender.render(openApi);

      System.out.println(output);

      throw new MojoFailureException("incompatible rest api changes detected");
    }
  }

  private String resolveNewContent() {
    Artifact artifact = project.getArtifact();
    return resolveContent(artifact.getFile());
  }

  private String resolveOldContent(String baseVersion) {
    Artifact artifact = system.createArtifact("sonia.scm", "scm-webapp", baseVersion, "", "war");
    File war = resolve(artifact);
    return resolveContent(war);
  }

  private String resolveContent(File war) {
    byte[] bytes = ZipUtil.unpackEntry(war, OPENAPI_SPEC);
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
