package com.clas.service;

import com.clas.client.IamClient;
import org.springframework.stereotype.Service;

@Service
public class CommentPenaltyService {
    private final IamClient iamClient;

    public CommentPenaltyService(IamClient iamClient) {
        this.iamClient = iamClient;
    }

    public void assertCanComment(String userId) {
        iamClient.assertCanComment(userId);
    }
}
