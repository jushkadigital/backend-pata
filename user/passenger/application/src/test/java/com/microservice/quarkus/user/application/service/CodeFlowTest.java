package com.microservice.quarkus.user.application.service;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.RestAssured;

@QuarkusTest
public class CodeFlowTest {

  KeycloakTestClient keycloakClient = new KeycloakTestClient();

  @Test
  public void testLogInDefaultTenant() throws IOException {
    try (final WebClient webClient = createWebClient()) {
      HtmlPage page = webClient.getPage("http://localhost:8080/default");

      assertEquals("Sign in to quarkus", page.getTitleText());

      HtmlForm loginForm = page.getForms().get(0);

      loginForm.getInputByName("username").setValueAttribute("alice");
      loginForm.getInputByName("password").setValueAttribute("alice");

      page = loginForm.getButtonByName("login").click();

      assertTrue(page.asNormalizedText().contains("tenant"));
    }
  }

  @Test
  public void testLogInTenantAWebApp() throws IOException {
    try (final WebClient webClient = createWebClient()) {
      HtmlPage page = webClient.getPage("http://localhost:8080/tenant-a");

      assertEquals("Sign in to tenant-a", page.getTitleText());

      HtmlForm loginForm = page.getForms().get(0);

      loginForm.getInputByName("username").setValueAttribute("alice");
      loginForm.getInputByName("password").setValueAttribute("alice");

      page = loginForm.getButtonByName("login").click();

      assertTrue(page.asNormalizedText().contains("alice@tenant-a.org"));
    }
  }

  @Test
  public void testLogInTenantABearerToken() throws IOException {
    System.out.println("TOKKEN : " + getAccessToken());
    RestAssured.given().auth().oauth2(getAccessToken()).when()
        .get("/tenant-a/bearer").then().body(containsString("alice@tenant-a.org"));
  }

  private String getAccessToken() {
    return keycloakClient.getRealmAccessToken("tenant-a", "alice", "alice", "multi-tenant-client", "secret");
  }

  private WebClient createWebClient() {
    WebClient webClient = new WebClient();
    webClient.setCssErrorHandler(new SilentCssErrorHandler());
    return webClient;
  }
}
