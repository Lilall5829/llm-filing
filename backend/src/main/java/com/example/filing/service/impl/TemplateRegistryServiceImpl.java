package com.example.filing.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.filing.dto.request.TemplateRegistryRequest;
import com.example.filing.entity.TemplateRegistry;
import com.example.filing.exception.BusinessException;
import com.example.filing.repository.TemplateRegistryRepository;
import com.example.filing.service.TemplateRegistryService;
import com.example.filing.util.Result;

/**
 * 模板服务实现类
 */
@Service
public class TemplateRegistryServiceImpl implements TemplateRegistryService {

    @Autowired
    private TemplateRegistryRepository templateRegistryRepository;

    @Override
    public Result<?> findTemplatesByPage(String templateCode, String templateName, String templateType,
            Integer current, Integer pageSize) {
        // 页码从0开始
        int page = current == null ? 0 : current - 1;
        // 默认每页10条
        int size = pageSize == null ? 10 : pageSize;

        // 按创建时间降序排序
        Pageable pageable = PageRequest.of(page, size, Sort.by("createTime").descending());

        Page<TemplateRegistry> templatePage = templateRegistryRepository.findTemplates(
                templateCode, templateName, templateType, pageable);

        // 封装返回结果
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("records", templatePage.getContent());
        resultMap.put("total", templatePage.getTotalElements());
        resultMap.put("pages", templatePage.getTotalPages());
        resultMap.put("current", current);
        resultMap.put("pageSize", pageSize);

        return Result.success(resultMap);
    }

    @Override
    public Result<TemplateRegistry> saveTemplateRegistry(TemplateRegistryRequest request, String userId) {
        if (request == null) {
            throw new BusinessException("请求参数不能为空");
        }

        if (!StringUtils.hasText(request.getTemplateName())) {
            throw new BusinessException("模板名称不能为空");
        }

        TemplateRegistry template;
        boolean isNew = true;

        // 使用ID查找模板
        if (StringUtils.hasText(request.getId())) {
            Optional<TemplateRegistry> existingTemplate = templateRegistryRepository.findById(request.getId());
            if (existingTemplate.isPresent()) {
                template = existingTemplate.get();
                isNew = false;
            } else {
                // ID不存在，创建新模板
                template = new TemplateRegistry();
                template.setCreateTime(LocalDateTime.now());
                template.setCreateBy(userId);
            }
        } else {
            // 没有提供ID，创建新模板
            template = new TemplateRegistry();
            template.setCreateTime(LocalDateTime.now());
            template.setCreateBy(userId);
        }

        // 复制属性
        BeanUtils.copyProperties(request, template);

        // 如果是新模板且没有提供备案编号，则自动生成
        // 检查templateCode是否为null、空字符串或纯空白字符
        if (isNew && (!StringUtils.hasText(request.getTemplateCode()) ||
                !StringUtils.hasText(template.getTemplateCode()))) {
            String generatedCode = generateTemplateCode();
            template.setTemplateCode(generatedCode);
            System.out.println("自动生成备案编号: " + generatedCode); // 调试日志
        }

        // 如果是更新操作，需要保持原始ID不变
        if (!isNew && StringUtils.hasText(request.getId())) {
            template.setId(request.getId());
        }

        // 设置更新信息
        template.setUpdateTime(LocalDateTime.now());
        template.setUpdateBy(userId);

        // 保存到数据库
        TemplateRegistry savedTemplate = templateRegistryRepository.save(template);

        return Result.success(isNew ? "模板创建成功" : "模板更新成功", savedTemplate);
    }

    @Override
    public Result<TemplateRegistry> getTemplateRegistryById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException("模板ID不能为空");
        }

        Optional<TemplateRegistry> template = templateRegistryRepository.findById(id);
        if (template.isPresent()) {
            return Result.success(template.get());
        } else {
            throw new BusinessException("模板不存在");
        }
    }

    @Override
    public Result<String> deleteTemplate(String id) {
        TemplateRegistry template = templateRegistryRepository.findById(id).orElse(null);
        if (template == null) {
            return Result.failed("模板不存在");
        }
        template.setDeleted(1);
        templateRegistryRepository.save(template);
        return Result.success("模板删除成功");
    }

    @Override
    public Result<String> updateTemplateFilePath(String id, String filePath) {
        TemplateRegistry template = templateRegistryRepository.findById(id).orElse(null);
        if (template == null) {
            return Result.failed("模板不存在");
        }
        template.setFilePath(filePath);
        template.setUpdateTime(LocalDateTime.now());
        templateRegistryRepository.save(template);
        return Result.success("模板文件路径更新成功");
    }

    @Override
    public Result<String> updateTemplateContent(String id, String templateContent) {
        TemplateRegistry template = templateRegistryRepository.findById(id).orElse(null);
        if (template == null) {
            return Result.failed("模板不存在");
        }
        template.setTemplateContent(templateContent);
        template.setUpdateTime(LocalDateTime.now());
        templateRegistryRepository.save(template);
        return Result.success("模板内容更新成功");
    }

    /**
     * 自动生成模板备案编号
     * 格式：TPL + 年月日 + 4位序号
     * 例如：TPL202412240001
     */
    private String generateTemplateCode() {
        // 使用Java 17的现代API获取当前日期
        var dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var prefix = "TPL" + dateStr;

        // 查询当天已有的模板编号，以确定下一个序号
        var todayTemplates = templateRegistryRepository.findAll().stream()
                .map(TemplateRegistry::getTemplateCode)
                .filter(code -> code != null && code.startsWith(prefix))
                .mapToInt(code -> {
                    try {
                        return Integer.parseInt(code.substring(prefix.length()));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);

        // 生成下一个序号
        var nextSequence = todayTemplates + 1;
        var templateCode = prefix + String.format("%04d", nextSequence);

        // 双重检查确保唯一性
        while (isTemplateCodeExists(templateCode)) {
            nextSequence++;
            templateCode = prefix + String.format("%04d", nextSequence);
        }

        return templateCode;
    }

    /**
     * 检查备案编号是否已存在
     */
    private boolean isTemplateCodeExists(String templateCode) {
        return templateRegistryRepository.findByTemplateCode(templateCode).isPresent();
    }
}