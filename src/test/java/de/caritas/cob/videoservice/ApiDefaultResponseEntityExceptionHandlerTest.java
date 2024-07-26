package de.caritas.cob.videoservice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

@ExtendWith(MockitoExtension.class)
class ApiDefaultResponseEntityExceptionHandlerTest {

  @InjectMocks private ApiDefaultResponseEntityExceptionHandler exceptionHandler;

  @Test
  void handleInternal_Should_respondWithStatusInternalServerError_When_exceptionIsGiven() {
    RuntimeException exception = new RuntimeException("error");

    ResponseEntity<Object> objectResponseEntity =
        this.exceptionHandler.handleInternal(exception, mock(WebRequest.class));

    assertThat(objectResponseEntity.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
  }
}
