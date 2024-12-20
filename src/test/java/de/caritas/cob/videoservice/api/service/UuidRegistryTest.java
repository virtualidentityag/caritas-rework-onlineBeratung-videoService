package de.caritas.cob.videoservice.api.service;

import static de.caritas.cob.videoservice.api.service.UuidRegistry.GENERATED_UUIDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UuidRegistryTest {

  private UuidRegistry uuidRegistry = new UuidRegistry();

  @Before
  public void setUp() {
    uuidRegistry.cleanUpUuidRegistry();
  }

  @Test
  public void generateUniqueUuid_Should_ReturnValidUuid() {
    String response = uuidRegistry.generateUniqueUuid();

    assertThat(UUID.fromString(response), instanceOf(UUID.class));
  }

  @Test
  public void generateUniqueUuid_Should_AddGeneratedUuidToRegistry() {
    String response = uuidRegistry.generateUniqueUuid();

    assertThat(UUID.fromString(response), instanceOf(UUID.class));
    assertThat(GENERATED_UUIDS.size(), equalTo(1));
  }

  @Test
  public void cleanUpEntireList_Should_cleanListOfUuids() {
    uuidRegistry.cleanUpUuidRegistry();
    assertEquals(0, GENERATED_UUIDS.size());
  }
}
