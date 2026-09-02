package com.clas.service;

import com.clas.client.IamClient;
import com.clas.entity.Appeal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AppealService {
    private final IamClient iamClient;

    public AppealService(IamClient iamClient) {
        this.iamClient = iamClient;
    }

    public List<Appeal> listPending() {
        return iamClient.listPendingAppeals();
    }

    public Appeal process(Long appealId, String status, String adminReply, String adminId) {
        return iamClient.processAppeal(appealId, status, adminReply, adminId);
    }
}
