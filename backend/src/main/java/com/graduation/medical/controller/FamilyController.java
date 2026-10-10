package com.graduation.medical.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.medical.common.Result;
import com.graduation.medical.entity.FamilyMember;
import com.graduation.medical.mapper.FamilyMemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/family")
@CrossOrigin
public class FamilyController {

    @Autowired
    private FamilyMemberMapper familyMemberMapper;

    @GetMapping("/list/{residentId}")
    public Result<?> list(@PathVariable Long residentId) {
        LambdaQueryWrapper<FamilyMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMember::getResidentId, residentId)
               .orderByDesc(FamilyMember::getIsPrimary);
        return Result.success(familyMemberMapper.selectList(wrapper));
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<?> add(@RequestBody FamilyMember member) {
        if (member.getResidentId() == null) return Result.error("老人ID不能为空");
        if (member.getName() == null || member.getName().trim().isEmpty()) return Result.error("家属姓名不能为空");
        clearPrimaryContact(member);
        familyMemberMapper.insert(member);
        return Result.success("添加成功");
    }

    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<?> update(@RequestBody FamilyMember member) {
        FamilyMember existing = member.getId() == null ? null : familyMemberMapper.selectById(member.getId());
        if (existing == null) {
            return Result.error("家属信息不存在");
        }
        if (member.getResidentId() == null) member.setResidentId(existing.getResidentId());
        if (member.getName() != null && member.getName().trim().isEmpty()) {
            return Result.error("家属姓名不能为空");
        }
        clearPrimaryContact(member);
        familyMemberMapper.updateById(member);
        return Result.success("更新成功");
    }

    private void clearPrimaryContact(FamilyMember member) {
        if (Integer.valueOf(1).equals(member.getIsPrimary())) {
            LambdaQueryWrapper<FamilyMember> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FamilyMember::getResidentId, member.getResidentId())
                   .eq(FamilyMember::getIsPrimary, 1)
                   .ne(member.getId() != null, FamilyMember::getId, member.getId());
            List<FamilyMember> primaries = familyMemberMapper.selectList(wrapper);
            for (FamilyMember primary : primaries) {
                primary.setIsPrimary(0);
                familyMemberMapper.updateById(primary);
            }
        }
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        if (familyMemberMapper.deleteById(id) == 0) return Result.error("家属信息不存在");
        return Result.success("删除成功");
    }
}
