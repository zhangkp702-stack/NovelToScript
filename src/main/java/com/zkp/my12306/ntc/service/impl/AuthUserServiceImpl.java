package com.zkp.my12306.ntc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zkp.my12306.ntc.entity.NtcUserEntity;
import com.zkp.my12306.ntc.mapper.NtcUserMapper;
import com.zkp.my12306.ntc.service.AuthUserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthUserServiceImpl implements AuthUserService {

    private final NtcUserMapper ntcUserMapper;

    public AuthUserServiceImpl(NtcUserMapper ntcUserMapper) {
        this.ntcUserMapper = ntcUserMapper;
    }

    @Override
    public Optional<NtcUserEntity> findByAccount(String account) {
        LambdaQueryWrapper<NtcUserEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NtcUserEntity::getAccount, account).last("LIMIT 1");
        return Optional.ofNullable(ntcUserMapper.selectOne(queryWrapper));
    }

    @Override
    public int updateLastLoginAtByAccount(String account) {
        LambdaUpdateWrapper<NtcUserEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(NtcUserEntity::getAccount, account)
                .set(NtcUserEntity::getLastLoginAt, LocalDateTime.now())
                .set(NtcUserEntity::getFailedLoginCount, 0);
        return ntcUserMapper.update(null, updateWrapper);
    }
}
