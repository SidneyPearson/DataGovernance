package com.dataExtracting.controller;

import com.dataExtracting.domain.model.AjaxResult;
import com.dataExtracting.helper.SqlHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/targetTable")
public class OperTargetDmController extends BaseController{

    @Autowired
    private SqlHelper sqlHelper;

    @GetMapping("/getCountByTableName")
    public AjaxResult getCountByTableName(String tableName) {
        return  success(sqlHelper.getDmCount(tableName));
    }

    /**
     * 危险级别操作，慎用！！！
     * @param tableName
     * @return
     */
    @GetMapping("/truncateByTableName")
    public AjaxResult truncateByTableName(String tableName) {
//        if (!"dwd_dj_xxly_info".equals(tableName) && !"dwd_dj_xxly_zf_detail".equals(tableName)) {
//            return success("警告！不可以删除前置机的其他表！");
//        }
        if (!"dwd_dj_xxly_info_v2".equals(tableName)) {
            return success("警告！不可以删除前置机的其他表！");
        }
        sqlHelper.truncateDm(tableName);
        return  success("清空"+tableName+"目标表数据");
    }

    /**
     * 危险级别操作，慎用！！！
     * @param tableName 表名
     * @param areaName  区名
     * @return
     */
    @GetMapping("/delByTableAreaName")
    public AjaxResult delByTableAreaName(String tableName, String areaName) {
        int deleteCount = sqlHelper.deleteData(tableName, areaName);
        return  success("成功删除"+tableName+"表中"+ areaName +"的数据======> " +deleteCount + "条");
    }

}
