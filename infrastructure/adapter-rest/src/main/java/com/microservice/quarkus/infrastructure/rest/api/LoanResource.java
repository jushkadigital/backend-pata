package com.microservice.quarkus.infrastructure.rest.api;

import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import com.microservice.quarkus.application.ports.api.LoanAPIService;
import com.microservice.quarkus.domain.model.loan.Loan;
import com.microservice.quarkus.infrastructure.rest.mapper.LoanDTOMapper;
import com.microservice.quarkus.infrastructure.rest.api.LoansAPI;
import com.microservice.quarkus.infrastructure.rest.dto.LoanDTO;
import com.microservice.quarkus.infrastructure.rest.dto.ResponseMessageDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class LoanResource implements LoansAPI {

  LoanAPIService loanService;

  LoanDTOMapper loanMapper;

  @Override
  public Response getAllLoans() {
    log.debug("getAllLoans()");

    Response response;

    var loans = loanService.getAllLoans();

    if (!loans.isEmpty()) {
      response = Response.ok(loans.stream().map(p -> loanMapper.toDto(p)).collect(Collectors.toList())).build();
    } else {
      response = Response.noContent().build();
    }

    return response;
  }

  @Override
  public Response getLoanById(UUID id) {
    log.debug("getLoanById({})", id);

    Loan loan = loanService.getLoan(id.toString());
    Response response;

    if (null == loan) {
      response = Response.status(Response.Status.NOT_FOUND).build();
    } else {
      response = Response.ok(loanMapper.toDto(loan)).build();
    }

    return response;
  }

  @Override
  public Response createLoan(LoanDTO loanDTO) {
    log.debug("createLoan({})", loanDTO);

    loanService.create(loanMapper.toDomain(loanDTO));

    ResponseMessageDTO response = ResponseMessageDTO.builder().message("Loan created successfully").build();

    return Response.status(Response.Status.CREATED).entity(response).build();
  }

  @Override
  public Response updateLoanById(UUID id, LoanDTO loanDTO) {
    log.debug("updateLoanById({})", loanDTO);
    loanService.update(loanMapper.toDomain(loanDTO));
    ResponseMessageDTO response = ResponseMessageDTO.builder().message("Loan update successfully").build();
    return Response.status(Response.Status.ACCEPTED).entity(response).build();
  }

  @Override
  public Response deleteLoanById(UUID id) {
    log.debug("deleteLoanById({})", id);

    loanService.deleteLoan(id.toString());

    ResponseMessageDTO response = ResponseMessageDTO.builder().message("Loan deleted successfully").build();

    return Response.ok(response).build();
  }

}
