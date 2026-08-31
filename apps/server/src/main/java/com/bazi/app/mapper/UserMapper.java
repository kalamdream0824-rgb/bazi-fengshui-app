package com.bazi.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bazi.app.domain.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {
  @Select("SELECT * FROM bazi_user WHERE id = #{id} FOR UPDATE")
  User selectByIdForUpdate(@Param("id") Long id);
}
