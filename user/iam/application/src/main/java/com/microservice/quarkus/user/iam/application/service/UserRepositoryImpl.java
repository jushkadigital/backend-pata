package com.microservice.quarkus.user.iam.application.service;

import java.util.List;

import com.microservice.quarkus.user.iam.application.api.UserApiService;
import com.microservice.quarkus.user.iam.domain.EmailAddress;
import com.microservice.quarkus.user.iam.domain.User;
import com.microservice.quarkus.user.iam.domain.UserId;
import com.microservice.quarkus.user.iam.domain.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserRepositoryImpl implements UserApiService {

  UserRepository userRepository;

  public UserRepositoryImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User findById(UserId id) {
    return userRepository.findById(id);
  }

  public User findByEmail(EmailAddress email) {
    return userRepository.findByEmail(email);
  }

  @Override
  public void delete(String id) {

    userRepository.delete(id);
  }

  @Override
  public List<User> getAll() {
    return userRepository.getAll();
  }

  @Override
  public void save(User loan) {

    userRepository.save(loan);

  }

  @Override
  public void update(User loan) {

    userRepository.update(loan);
  }
}
