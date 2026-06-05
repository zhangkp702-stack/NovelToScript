package com.zkp.my12306.ntc.service;

import com.zkp.my12306.ntc.entity.NtcUserEntity;

import java.util.Optional;

public interface AuthUserService {

    Optional<NtcUserEntity> findByAccount(String account);

    int updateLastLoginAtByAccount(String account);
}
