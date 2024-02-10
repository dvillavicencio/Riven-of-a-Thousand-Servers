package com.danielvm.destiny2bot.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.danielvm.destiny2bot.ResourceUtils;
import com.danielvm.destiny2bot.dto.discord.Attachment;
import com.danielvm.destiny2bot.dto.discord.InteractionResponseData;
import com.danielvm.destiny2bot.enums.InteractionResponseType;
import com.danielvm.destiny2bot.exception.ImageProcessingException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;

public class HttpUtilTest {

  @Test
  @DisplayName("Prepare multipart payload has all correct parts for /raid_map")
  public void prepareMultipartPayloadIsCorrectForRaidMapCommand() throws IOException {
    // given: An interaction response and an indexed map of classpath resources
    Map<Long, Resource> resources = Map.of(
        0L, ResourceUtils.createPartialResource("kalli-plates.jpg", 1024L * 1024L, false),
        1L, ResourceUtils.createPartialResource("kalli-dmg-phase.jpg", 1024L, false)
    );

    List<Attachment> attachments = List.of(
        Attachment.builder()
            .id(0).filename(resources.get(0L).getFilename())
            .size(Math.toIntExact(resources.get(0L).contentLength())).build(),
        Attachment.builder()
            .id(1).filename(resources.get(1L).getFilename())
            .size(Math.toIntExact(resources.get(1L).contentLength()))
            .build()
    );

    var data = InteractionResponseData.builder()
        .attachments(attachments)
        .build();

    var interactionResponse = com.danielvm.destiny2bot.dto.discord.InteractionResponse.builder()
        .type(InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE.getType())
        .data(data)
        .build();

    // when: prepareMultipartResponse is called
    var response = HttpUtil.prepareMultipartPayload(interactionResponse, resources);

    // then: the MultiValueMap created has the appropriate parameters and headers
    Condition<HttpEntity<?>> correctJsonPayloadHeaders = new Condition<>(
        httpEntity ->
            Objects.equals(httpEntity.getHeaders().getContentDisposition().getName(),
                "payload_json") &&
            Objects.equals(httpEntity.getHeaders().getContentType(),
                MediaType.valueOf("application/json")),
        "Contains all needed headers");

    Condition<HttpEntity<?>> firstImageAssertions = new Condition<>(
        h ->
            Objects.equals(h.getHeaders().getContentDisposition().getFilename(), "kalli-plates.jpg")
            &&
            Objects.equals(h.getHeaders().getContentDisposition().getName(), "files[0]") &&
            Objects.equals(h.getHeaders().getContentType(), MediaType.valueOf("image/jpg")) &&
            Objects.equals(h.getHeaders().getContentDisposition().isFormData(), true),
        "First image part contains all correct headers");

    Condition<HttpEntity<?>> secondImageAssertions = new Condition<>(
        he ->
            Objects.equals(he.getHeaders().getContentDisposition().getFilename(),
                "kalli-dmg-phase.jpg") &&
            Objects.equals(he.getHeaders().getContentDisposition().getName(), "files[1]") &&
            Objects.equals(he.getHeaders().getContentType(), MediaType.valueOf("image/jpg")) &&
            Objects.equals(he.getHeaders().getContentDisposition().isFormData(), true),
        "Second image part contains all correct headers");

    assertThat(response.get("payload_json").get(0)).is(correctJsonPayloadHeaders);
    assertThat(response.get("payload_json").get(0).getBody()).isEqualTo(interactionResponse);

    assertThat(response.get("files[0]").get(0)).is(firstImageAssertions);
    assertThat(response.get("files[1]").get(0)).is(secondImageAssertions);

    // Assert the bytes in the image bodies
    try {
      assertThat(response.get("files[0]").get(0).getBody()).isEqualTo(
          resources.get(0L).getContentAsByteArray());
      assertThat(response.get("files[1]").get(0).getBody()).isEqualTo(
          resources.get(1L).getContentAsByteArray());
    } catch (IOException e) {
      throw new RuntimeException("Reading mocked content bytes was unsuccessful");
    }
  }

  @Test
  @DisplayName("Prepare multipart payload should throw the appropriate exception in case of I/O problems")
  public void shouldThrowAppropriateExceptionsInIOErrors() throws IOException {
    // given: an InteractionResponse and faulty files which might cause I/O processing errors
    var firstResource = ResourceUtils.createPartialResource("kalli-plates.jpg", 1024L * 1024L, true);
    var secondResource = ResourceUtils.createPartialResource("kalli-dmg-phase.jpg", 1024L, true);
    Map<Long, Resource> resources = Map.of(0L, firstResource, 1L, secondResource);

    List<Attachment> attachments = List.of(
        Attachment.builder()
            .id(0).filename(firstResource.getFilename())
            .size(Math.toIntExact(firstResource.contentLength())).build(),
        Attachment.builder()
            .id(1).filename(secondResource.getFilename())
            .size(Math.toIntExact(secondResource.contentLength()))
            .build()
    );

    var data = InteractionResponseData.builder()
        .attachments(attachments)
        .build();

    var interactionResponse = com.danielvm.destiny2bot.dto.discord.InteractionResponse.builder()
        .type(InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE.getType())
        .data(data)
        .build();

    // when: prepare multipart payload is called
    // then: the appropriate ImageProcessingException is thrown
    assertThatThrownBy(() -> HttpUtil.prepareMultipartPayload(interactionResponse, resources))
        .isInstanceOf(ImageProcessingException.class);
  }
}
