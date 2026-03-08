package com.edurite.upload.service;

import org.springframework.stereotype.Service;

@Service
public class StorageService {

    public String putObject(String bucket, String objectName, byte[] bytes) {
        return "s3://" + bucket + "/" + objectName;
    }
}
