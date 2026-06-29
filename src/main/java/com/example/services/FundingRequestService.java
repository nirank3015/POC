package com.example.services;

import com.example.models.FundingRequest;
import com.example.exceptions.InvalidFundingRequestException;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class FundingRequestService {

    public void processFundingRequest(FundingRequest request) {
        validateFundingRequest(request);

        // Process the funding request
        // (Existing logic goes here)
    }

    private void validateFundingRequest(FundingRequest request) {
        if (request == null) {
            throw new InvalidFundingRequestException("FundingRequest cannot be null.");
        }

        if (request.getRequestId() == null || request.getRequestId() <= 0) {
            throw new InvalidFundingRequestException("Invalid request ID. It must be a positive number.");
        }

        if (!StringUtils.hasText(request.getRequesterName())) {
            throw new InvalidFundingRequestException("Requester name must not be blank.");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(0.0) <= 0) {
            throw new InvalidFundingRequestException("Funding amount must be greater than zero.");
        }

        if (request.getFundingType() == null) {
            throw new InvalidFundingRequestException("Funding type cannot be null.");
        }

        if (request.getSubmissionDate() == null) {
            throw new InvalidFundingRequestException("Submission date cannot be null.");
        }

        if (request.getDeadline() != null && request.getDeadline().isBefore(request.getSubmissionDate())) {
            throw new InvalidFundingRequestException("Deadline cannot be before the submission date.");
        }
    }
}