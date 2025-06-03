package com.dataExtracting.controller;

import com.dataExtracting.domain.model.AjaxResult;
import com.dataExtracting.helper.SqlHelper;
import com.dataExtracting.service.ZfDetailService;
import com.dataExtracting.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operDB")
public class OperDBController extends BaseController{

    @Autowired
    private SqlHelper sqlHelper;

    @GetMapping("/getCountByTableName")
    public AjaxResult getCountByTableName(String tableName) {
        return  success(sqlHelper.getDBCount(tableName));
    }

    /**
     * 危险级别操作，慎用！！！
     * @param tableName 表名
     * @param areaName  区名
     * @return
     */
    @GetMapping("/delByTableAreaName")
    public AjaxResult delByTableAreaName(String tableName, String areaName) {
        if (StringUtils.isBlank(areaName)) {
            return warn("请输入areaName");
        }
        int deleteCount = sqlHelper.deleteData(tableName, areaName);
        return  success("成功删除"+tableName+"表中"+ areaName +"的数据======> " +deleteCount + "条");
    }

}
