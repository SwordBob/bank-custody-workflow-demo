package com.demo.custody.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.custody.entity.Holding;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HoldingMapper extends BaseMapper<Holding> {
}
