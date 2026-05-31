package com.microservice.quarkus.user.identity.application.api;

import java.util.List;
import java.util.Map;

public interface GroupIdentityProvider {
  Map<String, String> createGroupHierarchy(List<List<String>> groupHierarchies);

  String findGroupByPath(String groupPath);

  void assignUserToGroup(String userId, String groupId);
}
