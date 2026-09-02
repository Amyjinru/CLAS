package com.clas.service;

import com.clas.client.IamClient;
import com.clas.dto.PenaltyRequest;
import com.clas.entity.UserPenalty;
import org.springframework.stereotype.Service;

@Service
public class PenaltyService {
    public static final String MUTE = "MUTE";
    public static final String BAN = "BAN";
    public static final String SERVICE_STOP = "SERVICE_STOP";

    private final IamClient iamClient;

    public PenaltyService(IamClient iamClient) {
        this.iamClient = iamClient;
    }

    public void assertCanComment(String userId) {
        iamClient.assertCanComment(userId);
    }

    public void assertCanCommunicate(String userId) {
        iamClient.assertCanComment(userId);
    }

    public void assertCanUsePlatform(String userId) {
        iamClient.assertCanUsePlatform(userId);
    }

    public UserPenalty applyPenalty(PenaltyRequest request, String adminId) {
        return iamClient.applyPenalty(request, adminId);
    }

    public void revokePenalty(Long penaltyId, String adminId) {
        iamClient.revokePenalty(penaltyId, adminId);
    }

    public void restoreAccount(String userId, String adminId) {
        iamClient.restoreAccount(userId, adminId);
    }
}
