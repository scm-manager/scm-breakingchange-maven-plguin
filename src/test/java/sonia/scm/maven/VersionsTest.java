package sonia.scm.maven;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersionsTest {

  @Test
  void shouldResolveBaseVersion() {
    assertThat(Versions.findBaseVersion("2.7.0-SNAPSHOT")).isEqualTo("2.6.0");
    assertThat(Versions.findBaseVersion("2.6.2-SNAPSHOT")).isEqualTo("2.6.1");
  }

}
