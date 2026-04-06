// 파일: UserStatus.java
// 역할: 사용자 상태 enum
// 설명: 사용자 계정의 현재 상태를 나타낸다

package com.mindcompass.api.user.domain;

public enum UserStatus {
    ACTIVE,     // 활성 상태
    INACTIVE,   // 비활성화 (탈퇴)
    SUSPENDED   // 정지 상태
}
